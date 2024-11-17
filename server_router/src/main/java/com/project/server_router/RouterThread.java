package com.project.server_router;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;

/**
 * The abstract class representing a router thread.
 * This class extends the Thread class and provides common functionality for router threads.
 */
public abstract class RouterThread extends Thread {

    protected final Connection myConnection; // Connection object for this thread

    protected final List<Connection> routingTable; // List of all the connections

    protected final ObjectOutputStream out; // writer to send to the client
    protected final ObjectInputStream in; // reader to read from the client

    protected final boolean isServer;

    protected final RouterApp routerApp;

    protected final Socket socket;

    /**
     * Constructor for the RouterThread class.
     * Initializes the socket, routing table, isServer flag, routerApp, and input/output streams.
     * Creates a new Connection object for this thread and adds it to the routing table.
     *
     * @param socket       The socket associated with this thread.
     * @param routingTable The list of all connections.
     * @param isServer     A flag indicating whether this thread is for a server or client.
     * @param routerApp    The RouterApp instance.
     * @throws IOException If an I/O error occurs while creating the input/output streams.
     */
    protected RouterThread(Socket socket, List<Connection> routingTable, boolean isServer, RouterApp routerApp) throws IOException {
        this.socket = socket;
        this.routerApp = routerApp;
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
        this.routingTable = routingTable;
        this.isServer = isServer;
        myConnection = new Connection(socket, isServer, this, routerApp.connectionCounter.getAndIncrement());
        routingTable.add(myConnection);
    }

    /**
     * Closes the connection associated with this thread.
     * Removes the connection from the routing table.
     * Updates the connection lists in the RouterApp on the JavaFX application thread.
     *
     * @throws IOException If an I/O error occurs while closing the connection.
     */
    public void closeConnection() throws IOException {
        myConnection.close();
        routingTable.remove(myConnection);
    }
}