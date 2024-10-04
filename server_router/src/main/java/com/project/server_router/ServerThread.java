package com.project.server_router;

import java.io.IOException;
import java.net.Socket;
import java.util.List;

import com.project.shared.Data;

public class ServerThread extends RouterThread {
    
    public ServerThread(Socket clientSocket, List<Connection> routingTable, RouterApp routingApp) throws IOException {
        super(clientSocket, routingTable, true, routingApp);
    }

    public void run() {
        while (true) {

        }
    }
}