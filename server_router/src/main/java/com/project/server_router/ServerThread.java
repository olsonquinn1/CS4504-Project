package com.project.server_router;

import java.io.IOException;
import java.net.Socket;
import java.util.List;

import com.project.shared.Data;
import com.project.shared.ProfilingData;

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
            routerApp.writeToConsole("ServerThread: Failed to deserialize message from server");
        } catch (IOException e) {
            routerApp.writeToConsole("ServerThread: Failed to receive message from server");
        }

        if(recv != null && recv.getType() == Data.Type.PROFILING_DATA) {
            ProfilingData data = (ProfilingData)recv.getData();
            myConnection.logicalCores = data.getCoreCount();
            myConnection.speedRating = data.getSpeedRating();
        } else {
            routerApp.writeToConsole("ServerThread: Failed to receive profiling data from server");
        }

        routerApp.updateConnectionLists();

        //start reader loops
        dataQueueThread = new Thread(this::dataQueueLoop);
        socketThread = new Thread(this::socketLoop);

        dataQueueThread.start();
        socketThread.start();
    }

    private void dataQueueLoop() {
        while (true) { 
            //wait for data in dataqueue
            Data recvData = null;

            try {
                recvData = myConnection.dataQueue.take();
            } catch (InterruptedException e) {
                routerApp.writeToConsole("ServerThread: Failed to take data from queue");
            }

            if(recvData == null || recvData.getType() == Data.Type.CLOSE) {
                break;
            }

            else if(recvData.getType() == Data.Type.SUBTASK_DATA) {
                //forward data to server
                try {
                    out.writeObject(recvData);
                    out.flush();
                } catch (IOException e) {
                    routerApp.writeToConsole("ServerThread: Failed to send data to server");
                }
            }
        }
        handleClose();

        //interupt socket thread
        socketThread.interrupt();
    }

    private void socketLoop() {
        while(true) {
            //wait for message from server
            Data recv = null;

            try {
                recv = (Data) in.readObject();
            } catch (ClassNotFoundException e) {
                routerApp.writeToConsole("ServerThread: Failed to deserialize message from server");
            } catch (IOException e) {
                routerApp.writeToConsole("ServerThread: Failed to receive message from server");
            }

            if(recv == null || recv.getType() == Data.Type.CLOSE) {
                break;
            }

            else if(recv.getType() == Data.Type.RESULT_DATA) {
                
            }
        }
        handleClose();

        //interupt dataQueue thread
        dataQueueThread.interrupt();
    }

    private void handleClose() {
        try {
            closeConnection();
        } catch (IOException e) {
            routerApp.writeToConsole("ServerThread: Failed to close connection");
        }
        routerApp.writeToConsole("ServerThread: Connection closed by server");
    }

}