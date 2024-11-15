package com.project.server_router;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.project.shared.Data;
import static com.project.shared.MatrixUtil.divideIntoQuadrants;
import com.project.shared.RequestData;
import com.project.shared.ResponseData;
import com.project.shared.SubTaskData;
import com.project.shared.TaskData;

public class ClientThread extends RouterThread {

    //my current best solution at how to split the matrices to balance data transfer and computation
    //via analysis of overlapping required submatrices for each M value
    //we will assume 1 <= servers <= 7
    //for 1 server: [m1, m2, m3, m4, m5, m6, m7]
    //for 2 servers: [m3, m5, m7], [m1, m2, m4, m6]
    //for 3 servers: [m1, m4, m5], [m2, m6], [m3, m7]
    //for 4 servers: [m1, m4], [m2, m6], [m3, m6], [m7]
    //for 5 servers: [m3, m5], [m2, m4], [m7], [m6], [m1]
    //for 6 servers: [m2, m4], [m6], [m1], [m5], [m3], [m7]
    //for 7 servers: [m1], [m4], [m5], [m2], [m6], [m3], [m7]
    private static final int[][][] serverMap = {
        {{0, 1, 2, 3, 4, 5, 6}},
        {{0, 1, 3, 5}, {2, 4, 6}},
        {{0, 3, 4}, {1, 5}, {2, 6}},
        {{0, 3}, {1, 5}, {2, 5}, {6}},
        {{2, 4}, {1, 3}, {0}, {5}, {6}},
        {{1, 3}, {0}, {2}, {4}, {5}, {6}},
        {{0}, {1}, {2}, {3}, {4}, {5}, {6}}
    };

    //maps M values to submatrices (a11, a12, a21, a22, b11, b12, b21, b22)
    private static final int[][] m_subMatrices = {
        {1, 0, 0, 1, 1, 0, 0, 1},   // M1
        {0, 0, 1, 1, 1, 0, 0, 0},   // M2
        {1, 0, 0, 0, 0, 1, 0, 1},   // M3
        {0, 0, 0, 1, 1, 0, 1, 0},   // M4
        {1, 1, 0, 0, 0, 0, 0, 1},   // M5
        {1, 0, 1, 0, 1, 1, 0, 0},   // M6
        {0, 1, 0, 1, 0, 0, 1, 1}    // M7
    };

    private static int[] getGroupEncoding(List<Integer> group) {
        int[] encoding = new int[8];

        for (int m : group) {
            for (int i = 0; i < 8; i++) {
                encoding[i] |= m_subMatrices[m - 1][i]; // m-1 because M values are 1-indexed
            }
        }
    
        return encoding;
    }

    private enum STATE {
        IDLE,
        WAITING_FOR_MATRICES,
        WAITING_FOR_RESULT
    }

    private STATE state = STATE.IDLE;

    public ClientThread(Socket clientSocket, List<Connection> routingTable, RouterApp routerApp) throws IOException {
        super(clientSocket, routingTable, false, routerApp);
    }

    public void run() {

        routerApp.updateConnectionLists();

        while (true) {

            //wait for message from client
            Data recv = null;
            try {
                recv = (Data) in.readObject();
            } catch (ClassNotFoundException e) {
                routerApp.writeToConsole("ClientThread: Failed to deserialize message from client");
            } catch (IOException e) {
                routerApp.writeToConsole("ClientThread: Failed to receive message from client");
            }

            if(recv == null || recv.getType() == Data.Type.CLOSE) {
                handleClose();
            }
            else if(recv.getType() == Data.Type.REQUEST) {
                handleRequest((RequestData)recv.getData());
            }
            else if(recv.getType() == Data.Type.TASK_DATA) {
                handleTask((TaskData)recv.getData());
            }
        }
    }

    private void handleClose() {
        try {
            closeConnection();
        } catch (IOException e) {
            routerApp.writeToConsole("ClientThread: Failed to close connection");
        }
        routerApp.writeToConsole("ClientThread: Connection closed by client");
    }

    private void handleRequest(RequestData requ) {
        if(state != STATE.IDLE) {
            //bad
            routerApp.writeToConsole("ClientThread: Received unexpected request from client");
            return;
        }

        int threadCount = requ.getThreadCount();
        int taskId = -1;

        try {
            taskId = routerApp.allocateServers(threadCount);
        } catch (IOException e) { 
            //unable to allocate, send error message to client
            String message = e.getMessage();

            ResponseData resp = new ResponseData(message, false, -1);
            Data send = new Data(Data.Type.RESPONSE, resp);

            try {
                out.writeObject(send);
                out.flush();
            } catch (IOException e1) {
                routerApp.writeToConsole("ClientThread: Failed to send error message to client");
            }
        }

        //send confirmation to client
        ResponseData resp = new ResponseData("Task " + taskId + " started", true, taskId);
        Data send = new Data(Data.Type.RESPONSE, resp);

        try {
            out.writeObject(send);
            out.flush();
        } catch (IOException e) {
            routerApp.writeToConsole("ClientThread: Failed to send response to client");
        }

        state = STATE.WAITING_FOR_MATRICES;
        myConnection.setTaskId(taskId);
    }

    private void handleTask(TaskData task) {

        if(state != STATE.WAITING_FOR_MATRICES) {
            //bad
            routerApp.writeToConsole("ClientThread: Received unexpected task data from client");
            return;
        }

        //geneate indices for submatrices
        int[][][] subMatrices = getSubMatrices(task);

        //collect all servers that are assigned to this task
        int taskId = myConnection.getTaskId();
        List<Connection> myServers = routingTable.stream()
            .filter(conn -> conn.isServer())
            .filter(conn -> conn.getTaskId() == taskId)
            .collect(Collectors.toList());
        
        //sort servers by speed rating
        myServers.sort((conn1, conn2) -> Double.compare(conn1.speedRating, conn2.speedRating));

        int serverCount = myServers.size();
        int[][] groups = serverMap[serverCount - 1];

        //a list of lists of M values for each group
        List<List<Integer>> taskDivision = new ArrayList<>();
        for (int[] group : groups) {
            taskDivision.add(Arrays.stream(group).boxed().collect(Collectors.toList()));
        }

        //pack data and send to corresponding server's data queue
        for (int i = 0; i < serverCount; i++) {
            Connection server = myServers.get(i);
            List<Integer> group = taskDivision.get(i);

            SubTaskData subTask = new SubTaskData(group);

            for(int m : group) {
                subTask.setSubmatrix(m, subMatrices[m]);
            }

            Data send = new Data(Data.Type.SUBTASK_DATA, subTask);

            try {
                server.dataQueue.put(send);
            } catch (InterruptedException e) {
                routerApp.writeToConsole("ClientThread: Failed to send subtask data to server");
            }
        }

        state = STATE.WAITING_FOR_RESULT;
    }

    private int[][][] getSubMatrices(TaskData task) {
        int[][][] matrices = new int[2][][];
        matrices[0] = task.getMatrixA();
        matrices[1] = task.getMatrixB();

        //geneate indices for submatrices
        int[][][] subMatrices = new int[8][][];
        divideIntoQuadrants(matrices[0], subMatrices[0], subMatrices[1], subMatrices[2], subMatrices[3]);
        divideIntoQuadrants(matrices[1], subMatrices[4], subMatrices[5], subMatrices[6], subMatrices[7]);

        return subMatrices;
    }
}
