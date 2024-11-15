package com.project.server_router;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;

import javafx.application.Platform;

public abstract class RouterThread extends Thread {

	protected final Connection myConnection; // Connection object for this thread

	protected final List<Connection> routingTable; // List of all the connections

	protected final ObjectOutputStream out; // writer to send to the client
	protected final ObjectInputStream in; // reader to read from the client

	protected final boolean isServer;

	protected final RouterApp routerApp;

	protected final Socket socket;

	// Constructor
	protected RouterThread(Socket socket, List<Connection> routingTable, boolean isServer, RouterApp routerApp) throws IOException {
		this.socket = socket;
		this.routerApp = routerApp;
		out = new ObjectOutputStream(socket.getOutputStream());
		in = new ObjectInputStream(socket.getInputStream());
		this.routingTable = routingTable;
		this.isServer = isServer;
		myConnection = new Connection(socket, isServer, this);
		routingTable.add(myConnection);
	}

	public void closeConnection() throws IOException {
		myConnection.close();
		routingTable.remove(myConnection);
		Platform.runLater(() -> routerApp.updateConnectionLists());
	}
}