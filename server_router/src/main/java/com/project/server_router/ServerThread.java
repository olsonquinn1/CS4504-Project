package com.project.server_router;

import java.io.IOException;
import java.net.Socket;
import java.util.List;

public class ServerThread extends RouterThread {
    
    public ServerThread(Socket clientSocket, List<Connection> routingTable, RouterApp routerApp) throws IOException {
        super(clientSocket, routingTable, true, routerApp);
    }

    public void run() {
        while (true) {

        }
    }
}