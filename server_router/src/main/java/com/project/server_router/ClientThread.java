package com.project.server_router;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.project.shared.Data;
import com.project.shared.RequestData;
import com.project.shared.ResponseData;
import com.project.shared.SubTask;
import com.project.shared.TaskData;

public class ClientThread extends RouterThread {

    //we will assume 1 <= servers <= 7
    //for 1 server: [m1, m2, m3, m4, m5, m6, m7]
    //for 2 servers: [m3, m5, m7], [m1, m2, m4, m6]
    //for 3 servers: [m1, m4, m5], [m2, m6], [m3, m7]
    //for 4 servers: [m1, m4], [m2, m6], [m3, m6], [m7]
    //for 5 servers: [m3, m5], [m2, m4], [m7], [m6], [m1]
    //for 6 servers: [m2, m4], [m6], [m1], [m5], [m3], [m7]
    //for 7 servers: [m1], [m4], [m5], [m2], [m6], [m3], [m7]
    private static final int[][][] serverMap = {
        {{1, 2, 3, 4, 5, 6, 7}},
        {{3, 5, 7}, {1, 2, 4, 6}},
        {{1, 4, 5}, {2, 6}, {3, 7}},
        {{1, 4}, {2, 6}, {3, 6}, {7}},
        {{3, 5}, {2, 4}, {7}, {6}, {1}},
        {{2, 4}, {6}, {1}, {5}, {3}, {7}},
        {{1}, {4}, {5}, {2}, {6}, {3}, {7}}
    };

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
                try {
                    closeConnection();
                } catch (IOException e) {
                    routerApp.writeToConsole("ClientThread: Failed to close connection");
                }
                routerApp.writeToConsole("ClientThread: Connection closed by client");
                break;
            }

            if(recv.getType() == Data.Type.REQUEST) {

                if(state != STATE.IDLE) {
                    //bad
                    continue;
                }

                RequestData requ = (RequestData)recv.getData();
                int threadCount = requ.getThreadCount();

                int taskId = -1;

                try {
                    taskId = routerApp.allocateServers(threadCount);
                } catch (IOException e) { 
                    //unable to allocate, send error message to client
                    String message = e.getMessage();

                    ResponseData resp = new ResponseData(message, false, -1);
                    Data send = createData(Data.Type.RESPONSE, resp);

                    try {
                        out.writeObject(send);
                        out.flush();
                    } catch (IOException e1) {
                        routerApp.writeToConsole("ClientThread: Failed to send error message to client");
                    }
                }

                //send confirmation to client
                ResponseData resp = new ResponseData("Task " + taskId + " started", true, taskId);
                Data send = createData(Data.Type.RESPONSE, resp);

                try {
                    out.writeObject(send);
                    out.flush();
                } catch (IOException e) {
                    routerApp.writeToConsole("ClientThread: Failed to send response to client");
                }

                state = STATE.WAITING_FOR_MATRICES;
                myConnection.setTaskId(taskId);
            }

            if(recv.getType() == Data.Type.TASK_DATA) {

                if(state != STATE.WAITING_FOR_MATRICES) {
                    //bad
                    continue;
                }

                TaskData task = (TaskData)recv.getData();

                int[][] matrixA = task.getMatrixA();
                int[][] matrixB = task.getMatrixB();
                int size = matrixA.length;
                int half = size / 2;

                //geneate indices for submatrices
                int[][] subIndices = new int[4][2];
                subIndices[0] = new int[]{0, 0};
                subIndices[1] = new int[]{0, half};
                subIndices[2] = new int[]{half, 0};
                subIndices[3] = new int[]{half, half};

                //collect all servers that are assigned to this task
                int taskId = myConnection.getTaskId();
                List<Connection> myServers = routingTable.stream()
                    .filter(conn -> conn.isServer())
                    .filter(conn -> conn.getTaskId() == taskId)
                    .collect(Collectors.toList());
                
                myServers.sort((conn1, conn2) -> Double.compare(conn1.speedRating, conn2.speedRating));

                int serverCount = myServers.size();
                int[][] groups = serverMap[serverCount - 1];

                List<SubTask> subTasks = new ArrayList<>();
            }
        }
    }
}
