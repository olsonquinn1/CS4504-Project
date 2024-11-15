package com.project.server_router;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

public class ListenThread extends Thread {

    private final ServerSocket listenSocket;
    private final List<Connection> routingTable;
    private final RouterApp routerApp;
    private final boolean isServer;

    public ListenThread(ServerSocket listenSocket, List<Connection> routingTable, RouterApp routerApp, boolean isServer) throws IOException {
        this.routerApp = routerApp;
        this.listenSocket = listenSocket;
        this.routingTable = routingTable;
        this.isServer = isServer;
    }

    public void run() {
        try {
            while (true) {
                Socket incomingSocket = listenSocket.accept();

                if (isServer) {
                    new ServerThread(incomingSocket, routingTable, routerApp).start();
                } else {
                    new ClientThread(incomingSocket, routingTable, routerApp).start();
                }

                routerApp.writeToConsole(
                    "ListenThread: Accepted "
                    + (isServer ? "Server" : "Client")
                    + ": " + incomingSocket.getInetAddress().getHostAddress()
                    + ":" + incomingSocket.getPort()
                );
            }
        } catch (IOException e) {
            routerApp.writeToConsole("ListenThread: connection failed, " + e.getMessage());
            e.printStackTrace();
        }
    }
}