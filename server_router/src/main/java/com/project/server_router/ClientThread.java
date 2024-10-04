package com.project.server_router;

import java.io.IOException;
import java.net.Socket;
import java.util.List;

public class ClientThread extends RouterThread {

    public ClientThread(Socket clientSocket, List<Connection> routingTable, RouterApp routerApp) throws IOException {
        super(clientSocket, routingTable, false, routerApp);
    }

    public void run() {
        while (true) {
            
        }
    }
}
