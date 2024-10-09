package com.project.server_router;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

public class ListenThread extends Thread {

    private final ServerSocket listenSocket;
    private final List<Connection> routingTable;
    private final RouterApp routerApp;

    public ListenThread(ServerSocket listenSocket, List<Connection> routingTable, RouterApp routerApp) throws IOException {
        this.routerApp = routerApp;
        this.listenSocket = listenSocket;
        this.routingTable = routingTable;
    }

    public void run() {
        try {
            while (true) {
                Socket incomingSocket = listenSocket.accept();
                boolean isServer = false;
                RouterThread t = new ClientThread(incomingSocket, routingTable, routerApp);
                t.start();
                routerApp.writeToConsole("ListenThread: Accepted Machine: " + incomingSocket.getInetAddress().getHostAddress() + ":" + incomingSocket.getPort());
                routerApp.updateConnectionLists();
            }
        } catch (IOException e) {
            routerApp.writeToConsole("ListenThread: connection failed, " + e.getMessage());
            e.printStackTrace();
        }
    }
}