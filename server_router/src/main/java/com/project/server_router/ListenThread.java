package com.project.server_router;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

/**
 * The ListenThread class represents a thread that listens for incoming connections on a server socket.
 * It accepts incoming connections and creates separate threads to handle the communication with each client.
 */
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

    /**
     * Listens for incoming socket connections and handles them accordingly.
     * If the current instance is a server, it creates a new ServerThread to handle the connection.
     * If the current instance is a client, it creates a new ClientThread to handle the connection.
     * Writes information about the accepted connection to the console.
     * 
     * @throws IOException if an I/O error occurs while accepting the connection
     */
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