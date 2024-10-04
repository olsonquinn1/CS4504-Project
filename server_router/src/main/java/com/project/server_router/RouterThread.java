package com.project.server_router;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;

public abstract class RouterThread extends Thread {

	protected final Connection myConnection; // ConnectionData object for this thread

	protected final List<Connection> routingTable; // List of all the connections

	protected ObjectOutputStream out; // writer to send to the client
	protected ObjectInputStream in; // reader to read from the client

	protected final boolean isServer;

	protected RouterApp routerApp;

	// Constructor
	public RouterThread(Socket clientSocket, List<Connection> routingTable, boolean isServer, RouterApp routerApp) throws IOException {
		this.routerApp = routerApp;
		out = new ObjectOutputStream(clientSocket.getOutputStream());
		in = new ObjectInputStream(clientSocket.getInputStream());
		this.routingTable = routingTable;
		this.isServer = isServer;
		myConnection = new Connection(clientSocket, isServer);
		routingTable.add(myConnection);
	}

	// Run method (will run for each machine that connects to the ServerRouter)
	public void run() {

		// closing open resources
		try {
			out.close();
			in.close();
			myConnection.getSocket().close();
		} catch (IOException e) {
			System.err.println("Could not close Connection.");
			e.printStackTrace();
		} finally {
			routingTable.remove(myConnection);
			System.out.println("Connection to " + myConnection.getAddr() + ":" + myConnection.getPort() + " closed.");
		}
	}
}