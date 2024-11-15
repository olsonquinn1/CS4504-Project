package com.project.server_router;

import java.io.IOException;
import java.net.Socket;
import java.util.List;

import com.project.shared.Data;
import com.project.shared.ProfilingData;

public class ServerThread extends RouterThread {
    
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
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(recv.getType() == Data.Type.PROFILING_DATA) {
            ProfilingData data = (ProfilingData)recv.getData();
            myConnection.logicalCores = data.getCoreCount();
            myConnection.speedRating = data.getSpeedRating();
        }

        routerApp.updateConnectionLists();

        //main loop
        while (true) {

        }
    }
}