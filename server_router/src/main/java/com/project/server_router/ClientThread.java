package com.project.server_router;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.project.shared.Data;
import static com.project.shared.MatrixUtil.divideIntoQuadrants;
import com.project.shared.RequestData;
import com.project.shared.ResponseData;
import com.project.shared.ResultData;
import com.project.shared.StrassenExecutor;
import com.project.shared.SubTaskData;
import com.project.shared.TaskData;
import com.project.shared.Timestamp;

/**
 * Extends the `RouterThread` class and represents a thread that handles communication with a client.
 * Receives messages from the client, processes them, and sends appropriate responses.
 * Responsible for handling client requests, tasks, and results.
 */
public class ClientThread extends RouterThread {

    private Thread dataQueueThread;
    private Thread socketThread;

    private final Map<Integer, int[][][]> taskMatrices = new HashMap<>();
    private final Map<Integer, List<Timestamp>> taskTimestamps = new HashMap<>();

    public ClientThread(Socket clientSocket, List<Connection> routingTable, RouterApp routerApp) throws IOException {
        super(clientSocket, routingTable, false, routerApp);
    }

    public void run() {

        dataQueueThread = new Thread(this::dataQueueLoop);
        socketThread = new Thread(this::socketLoop);

        dataQueueThread.start();
        socketThread.start();
    }
    
    /**
     * This method represents the main loop for receiving messages from the client.
     * It continuously waits for messages from the client and handles them accordingly.
     */
    private void socketLoop() {

        while (true) {

            //wait for message from client
            Data recv = null;
            try {
                recv = (Data) in.readObject();
            } catch (ClassNotFoundException e) {
                log("Failed to deserialize message from client");
            } catch (IOException e) {
                log("Failed to receive message from client");
            }

            if(recv == null || recv.getType() == Data.Type.CLOSE) {
                break;
            }
            else if(recv.getType() == Data.Type.REQUEST) {
                handleRequest((RequestData)recv.getData());
            }
            else if(recv.getType() == Data.Type.TASK_DATA) {
                handleTask((TaskData)recv.getData());
            }
        }

        handleClose();

        dataQueueThread.interrupt();
    }

    /**
     * This method represents the main loop for processing data received from the data queue.
     * It continuously retrieves data from the queue and handles it accordingly until a CLOSE
     * data type is received.
     */
    private void dataQueueLoop() {
        while (true) { 

            Data recv = null;

            try {
                recv = myConnection.dataQueue.take();
            } catch (InterruptedException e) {
                log("Failed to take data from queue");
            }

            if(recv == null || recv.getType() == Data.Type.CLOSE) {
                break;
            }

            if(recv.getType() == Data.Type.RESULT_DATA) {
                handleResult((ResultData)recv.getData());
            }
            
        }

        handleClose();

        socketThread.interrupt();
    }

    /**
     * Closes the connection with the client.
     * This method is called when the client requests to close the connection.
     * It closes the connection and logs the closure.
     */
    private void handleClose() {
        try {
            closeConnection();
        } catch (IOException e) {
            log("Failed to close connection");
        }
        log("Connection closed by client");
    }

    /**
     * Handles a request from the client.
     *
     * @param requ The request data containing the thread count.
     */
    private void handleRequest(RequestData requ) {

        int threadCount = requ.getThreadCount();
        int taskId = -1;

        log("Received request for " + threadCount + " threads");

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
                log("Failed to send error message to client");
            }

            return;
        }

        log("Request accepted, task id: " + taskId);

        taskTimestamps.put(taskId, new ArrayList<>());
        myConnection.addNewTask(taskId);
        myConnection.incrementTask(taskId, 7);

        //send confirmation to client
        ResponseData resp = new ResponseData("Task " + taskId + " started", true, taskId);
        Data send = new Data(Data.Type.RESPONSE, resp);

        try {
            out.writeObject(send);
            out.flush();
        } catch (IOException e) {
            log("Failed to send response to client");
        }

        try {
            myConnection.addNewTask(taskId);
            myConnection.incrementTask(taskId, 7);
        } catch (Exception e) {
            log("Error adding task to client");
        }
    }

    /**
     * Handles the matrix multiplication task from the client.
     * 
     * Divides and processes the input matrices into the 7 pairs of submatrices required for calculation of M values in Strassen algorithm.
     *
     * @param task The task data containing the matrices and thread count.
     */
    private void handleTask(TaskData task) {

        if(!myConnection.hasTaskId(task.getTaskId())) {
            //bad
            log("Received unauthorized task");
            return;
        }

        taskTimestamps.get(task.getTaskId()).add(new Timestamp("task received by router"));

        log("Processing task... " + task.getTaskId());
        log("Received 2 matrices of size " + task.getMatrixA().length + "x" + task.getMatrixA()[0].length);

        //collect all servers that are assigned to this task (sorted by total tasks then speed)
        int taskId = task.getTaskId();
        List<Connection> myServers = routerApp.getServersByTaskIdSorted(taskId);
        int serverCount = myServers.size();
        int threadCount = task.getThreadsToUse();

        //geneate submatrices
        int[][][] subMatrices = getSubMatrices(task);

        List<SubTaskData> subTasks = Arrays.asList(
            new SubTaskData(StrassenExecutor.stras_M1(subMatrices), 0, taskId),
            new SubTaskData(StrassenExecutor.stras_M2(subMatrices), 1, taskId),
            new SubTaskData(StrassenExecutor.stras_M3(subMatrices), 2, taskId),
            new SubTaskData(StrassenExecutor.stras_M4(subMatrices), 3, taskId),
            new SubTaskData(StrassenExecutor.stras_M5(subMatrices), 4, taskId),
            new SubTaskData(StrassenExecutor.stras_M6(subMatrices), 5, taskId),
            new SubTaskData(StrassenExecutor.stras_M7(subMatrices), 6, taskId)
        );

        subMatrices = null;

        taskMatrices.put(taskId, new int[7][][]);
        taskTimestamps.get(taskId).add(new Timestamp("task divided by router"));

        int tasksPerServer = subTasks.size() / serverCount;
        int remainder = subTasks.size() % serverCount;
        int taskIndex = 0;
        int threadsLeft = threadCount;

        for(int s = 0; s < myServers.size(); s++) {

            int n_threads = myServers.get(s).getLogicalCores();

            if(threadsLeft < n_threads) {
                n_threads = threadsLeft;

                //if not the last server in the list, we have a problem
                if(s != myServers.size() - 1) {
                    log("Not enough threads to allocate to all servers");
                    break;
                }
            } else {
                threadsLeft -= n_threads;
            }

            //each gets tasksPerServer tasks, if s < remainder, add 1 more task
            int taskCount = tasksPerServer + (s < remainder ? 1 : 0);
            Connection server = myServers.get(s);

            server.addNewTask(taskId);
            server.incrementTask(taskId, taskCount);

            for(int t = 0; t < taskCount; t++) {

                SubTaskData subTask = subTasks.get(taskIndex++);
                subTask.setCoresToUse(n_threads);
                Data send = new Data(Data.Type.SUBTASK_DATA, subTask);

                try {
                    server.dataQueue.put(send);
                } catch (InterruptedException e) {
                    log("Failed to send subtask to ServerThread");
                    Thread.currentThread().interrupt();
                }

                taskTimestamps.get(taskId).add(new Timestamp("M" + (subTask.getM() + 1) + " sent to server"));
            }
        }
    }

    /**
     * Handles the result received from the servers.
     * 
     * Combines the results from the 7 submatrices and sends the final result to the client.
     *
     * @param result The result data received from the server.
     */
    private void handleResult(ResultData result) {

        int taskId = result.getTaskId();
        int subTaskId = result.getM();

        if(!myConnection.hasTaskId(taskId)) {
            //bad
            log("Received result with task id not associated with client");
            return;
        }

        if(subTaskId < 0 || subTaskId >= 7) {
            //bad
            log("Received result with invalid subtask id");
            return;
        }

        taskTimestamps.get(taskId).add(new Timestamp("M" + (subTaskId + 1) + " received by router"));
        taskMatrices.get(taskId)[subTaskId] = result.getResultMatrix();

        myConnection.decrementTask(taskId, 1);

        if(myConnection.getTasksRemaining(taskId) == 0) {

            //all results received, combine and send to client
            int[][] resultMatrix = StrassenExecutor.combineMatricesFromM(taskMatrices.get(taskId));

            taskTimestamps.get(taskId).add(new Timestamp("result sent by router"));

            ResultData resultData = new ResultData(
                resultMatrix, -1, taskId,
                taskTimestamps.get(taskId)
            );

            Data send = new Data(Data.Type.RESULT_DATA, resultData);

            try {
                out.writeObject(send);
                out.flush();
            } catch (IOException e) {
                log("Failed to send result to client");
            }

            myConnection.removeTask(taskId);
            taskTimestamps.remove(taskId);
        }
    }

    /**
     * Divides the given matrices into submatrices and returns an array of submatrices.
     *
     * @param task The task data containing the matrices.
     * @return An array of submatrices. size [2][n/2][n/2]
     */
    private int[][][] getSubMatrices(TaskData task) {
        int[][][] matrices = new int[2][][];
        matrices[0] = task.getMatrixA();
        matrices[1] = task.getMatrixB();

        int size = matrices[0].length;
        int halfSize = size / 2;

        //geneate indices for submatrices
        int[][][] subMatrices = new int[8][halfSize][halfSize];
        divideIntoQuadrants(matrices[0], subMatrices[0], subMatrices[1], subMatrices[2], subMatrices[3]);
        divideIntoQuadrants(matrices[1], subMatrices[4], subMatrices[5], subMatrices[6], subMatrices[7]);

        return subMatrices;
    }

    private void log(String message) {
        routerApp.writeToConsole("ClientThread " + myConnection.getId() + ":  " + message);
    }
}