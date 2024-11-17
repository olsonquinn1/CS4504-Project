package com.project.server_router;

import java.io.IOException;
import java.net.Socket;
import java.util.List;

import com.project.shared.Data;
import com.project.shared.ProfilingData;
import com.project.shared.ResultData;

import javafx.application.Platform;

/**
 * Extends the `RouterThread` class and represents a thread that handles communication
 * with a server.
 * 
 * Responsible for receiving profiling data from the server, updating the connection lists, and
 * handling data and messages received from the server. It also handles closing the connection with the server
 * and forwarding result data to the appropriate client.
 */
public class ServerThread extends RouterThread {
    
    private enum STATE {
        IDLE,
        BUSY
    }

    private STATE state = STATE.IDLE;

    Thread dataQueueThread;
    Thread socketThread;

    public ServerThread(Socket clientSocket, List<Connection> routingTable, RouterApp routerApp) throws IOException {
        super(clientSocket, routingTable, true, routerApp);
    }

    public void run() {

        //initialization routine
        //receive profiling data from client
        Data recv = null;
        try {
            recv = (Data) in.readObject();
        } catch (ClassNotFoundException e) {
            log("Failed to deserialize message from server");
            try {
                closeConnection();
            } catch (IOException ex) {
                log("Failed to close connection");
            }
            return;
        } catch (IOException e) {
            log("Failed to receive message from server");
            try {
                closeConnection();
            } catch (IOException ex) {
                log("Failed to close connection");
            }
            return;
        }

        if(recv != null && recv.getType() == Data.Type.PROFILING_DATA) {
            ProfilingData data = (ProfilingData)recv.getData();
            myConnection.setLogicalCores(data.getCoreCount());
            myConnection.setSpeedRating(data.getSpeedRating());
        } else {
            log("Failed to receive profiling data from server");
            try {
                closeConnection();
            } catch (IOException e) {
                log("Failed to close connection");
            }
            return;
        }

        Platform.runLater(() -> routerApp.updateConnectionLists());

        //start reader loops
        dataQueueThread = new Thread(this::dataQueueLoop);
        socketThread = new Thread(this::socketLoop);

        dataQueueThread.start();
        socketThread.start();
    }

    /**
     * This method continuously loops and waits for data in the data queue.
     * When data is received, it checks the type of the data and performs the appropriate action.
     */
    private void dataQueueLoop() {
        while (true) { 
            //wait for data in dataqueue
            Data recv = null;

            try {
                recv = myConnection.dataQueue.take();
            } catch (InterruptedException e) {
                log("Failed to take data from queue");
            }

            if(recv == null || recv.getType() == Data.Type.CLOSE) {
                break;
            }

            else if(recv.getType() == Data.Type.SUBTASK_DATA) {
                //forward data to server
                try {
                    out.writeObject(recv);
                    out.flush();
                } catch (IOException e) {
                    log("Failed to send data to server");
                }
            }
        }
        handleClose();

        //interupt socket thread
        socketThread.interrupt();
    }

    /**
     * Continuously listens for messages from the server and handles them accordingly.
     * This method runs in a loop until a CLOSE message is received from the server.
     */
    private void socketLoop() {
        while(true) {
            //wait for message from server
            Data recv = null;

            try {
                recv = (Data) in.readObject();
            } catch (ClassNotFoundException e) {
                log("Failed to deserialize message from server");
            } catch (IOException e) {
                log("Failed to receive message from server");
            }

            if(recv == null || recv.getType() == Data.Type.CLOSE) {
                break;
            }

            else if(recv.getType() == Data.Type.RESULT_DATA) {
                handleResult((ResultData)recv.getData());
            }
        }
        handleClose();

        //interupt dataQueue thread
        dataQueueThread.interrupt();
    }

    /**
     * Closes the connection with the server.
     * This method is called when the server requests to close the connection.
     * It closes the connection and logs the closure.
     */
    private void handleClose() {
        try {
            closeConnection();
        } catch (IOException e) {
            log("Failed to close connection");
        }
        log("Connection closed by server");
    }

    /**
     * Handles the result data received from a task execution.
     * It finds the client associated with the task ID and forwards the result to the client.
     *
     * @param result The result data to be handled.
     */
    private void handleResult(ResultData result) {
        
        int taskId = result.getTaskId();

        //find client with task id
        Connection client = null;
        for(Connection c : routingTable) {
            if(!c.isServer() && c.hasTaskId(taskId)) {
                client = c;
                break;
            }
        }

        if(client == null) {
            log("Failed to find client with task id");
            return;
        }

        //forward result to client
        try {
            client.dataQueue.put(new Data(Data.Type.RESULT_DATA, result));
        } catch (InterruptedException e) {
            log("Failed to forward result to client");
        }
    }

    private void log(String message) {
        routerApp.writeToConsole("ServerThread " + myConnection.getId() + ": " + message);
    }
}