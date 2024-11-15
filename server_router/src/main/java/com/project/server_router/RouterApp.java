package com.project.server_router;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

public class RouterApp extends Application {

    private final int serverPort = 5555;
    private final int clientPort = 5556;

    private ServerSocket listenSocket_server = null;
    private ServerSocket listenSocket_client = null;

    private final List<Connection> routingTable = Collections.synchronizedList(new ArrayList<>());;

    private ListenThread clientListener = null;
    private ListenThread serverListener = null;

    private Stage primaryStage;

    private PrintStream log;

    private int taskCounter = 0;

    @FXML
    private TextArea ta_log;
    @FXML
    private Label lb_conn_status;
    @FXML
    private ListView<String> lv_servers;
    @FXML
    private ListView<String> lv_clients;

    public static void main(String[] args) throws IOException {
        launch(args);
    }

    @Override
    public void start(Stage stage) {

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/router.fxml"));
        try {
            Parent root = loader.load();
            Scene scene = new Scene(root);
            primaryStage = stage;
            primaryStage.setTitle("Router");
            primaryStage.setScene(scene);
            primaryStage.setOnCloseRequest(event -> {
                closeConnections();
                Platform.exit();
                System.exit(0);
            });
            primaryStage.show();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    @FXML
    private void initialize() {

        setUpLogStream(ta_log);

        startListener(serverListener, listenSocket_server, serverPort, true);
        startListener(clientListener, listenSocket_client, clientPort, false);
    }

    // sets up the listener thread
    private void startListener(ListenThread listener, ServerSocket serverSocket, int port, boolean isServer) {

        if (serverSocket == null) {
            try {
                serverSocket = new ServerSocket(port);
            } catch (IOException e) {
                System.err.println("Could not listen on port: " + port + "\n" + e.getMessage());
                e.printStackTrace();
                System.exit(1);
            }
        }

        try {

            if (listener != null)
                listener.interrupt();

            listener = new ListenThread(serverSocket, routingTable, this, isServer);
            listener.start();
        } catch (IOException e) {
            System.err.println("Could not listen on port: " + port + "\n" + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }

        log.println("Listener started on port: " + port);
    }

    public synchronized void updateConnectionLists() {

        ObservableList<String> servers = FXCollections.observableArrayList(
                routingTable.stream()
                        .filter(conn -> conn.isServer())
                        .map(conn ->
                            conn.getAddr() + ":" + conn.getPort()
                            + "(" + conn.getLogicalCores() + ", " + conn.speedRating + ")"
                            + (conn.isInUse() ? " (in use: " + conn.getTaskId() + ")" : "")
                        )
                        .collect(Collectors.toList()));

        ObservableList<String> clients = FXCollections.observableArrayList(
                routingTable.stream()
                        .filter(conn -> !conn.isServer())
                        .map(conn -> conn.getAddr() + ":" + conn.getPort())
                        .collect(Collectors.toList()));

        lv_servers.setItems(servers);
        lv_clients.setItems(clients);

        lv_servers.refresh();
        lv_clients.refresh();
    }

    private void setUpLogStream(TextArea ta) {
        OutputStream out = new OutputStream() {
            @Override
            public synchronized void write(int b) {
                ta.appendText(String.valueOf((char) b));
            }

            @Override
            public synchronized void write(byte[] b, int off, int len) {
                ta.appendText(new String(b, off, len));
            }
        };

        log = new PrintStream(out, true);
    }

    public void writeToConsole(String s) {
        log.println(s);
    }

    /**
     * Reserves servers for a task to meet the required number of cores. Servers with higher speed ratings are preferred.
     * 
     * @param threadCount number of cores required
     * @return taskId
     * @throws IOException if no servers are available or not enough cores are available
     */
    public synchronized int allocateServers(int threadCount) throws IOException {

        //collect all available servers (inUse == false)
        List<Connection> availableServers = routingTable.stream()
                .filter(conn -> conn.isServer())
                .filter(conn -> !conn.isInUse())
                .collect(Collectors.toList());

        if (availableServers.isEmpty()) {
            throw new IOException("No servers available");
        }

        //check if enough cores are available
        int availableCores = availableServers.stream().mapToInt(Connection::getLogicalCores).sum();

        if (availableCores < threadCount) {
            throw new IOException("Not enough cores available");
        }

        //sort by speed rating
        availableServers.sort((a, b) -> Double.compare(a.speedRating, b.speedRating));

        //select servers until enough cores are allocated
        List<Connection> selectedServers = new ArrayList<>();
        int usedCores = 0;

        for(int i = 0; i < availableServers.size(); i++) {
            Connection server = availableServers.get(i);
            if(server.isInUse()) {
                continue;
            }
            selectedServers.add(server);
            int cores = server.getLogicalCores();
            usedCores += cores;

            if(usedCores >= threadCount) {
                break;
            }
        }

        int taskId = taskCounter++;

        for(Connection server : selectedServers) {
            server.setInUse(true);
            server.setTaskId(taskId);
        }

        updateConnectionLists();

        return taskId;
    }

    private void closeConnections() {
        try {
            if (listenSocket_server != null)
                listenSocket_server.close();
            if (listenSocket_client != null)
                listenSocket_client.close();
        } catch (IOException e) {
            System.err.println("Error closing server and client sockets.");
            System.exit(1);
        }
    }
}