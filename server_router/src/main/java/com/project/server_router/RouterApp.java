package com.project.server_router;

import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.project.shared.BufferedLogHandler;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.ChoiceBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.util.Callback;

/**
 * The main class for the Router application.
 * It initializes the GUI, starts the listener threads, and manages the routing
 * table.
 */
public class RouterApp extends Application {

    private final int serverPort = 5555;
    private final int clientPort = 5556;

    private ServerSocket listenSocket_server;
    private ServerSocket listenSocket_client;

    private final List<Connection> routingTable = Collections.synchronizedList(new ArrayList<>());;

    private ListenThread clientListener;
    private ListenThread serverListener;

    private Stage primaryStage;

    private BufferedLogHandler logHandler;
    private PrintStream log;

    public AtomicInteger connectionCounter = new AtomicInteger(0);
    public AtomicInteger taskCounter = new AtomicInteger(0);

    private final Timer connListTimer = new Timer(true);

    @FXML
    private TextArea ta_log;
    @FXML
    private ListView<String> lv_servers;
    @FXML
    private ListView<String> lv_clients;
    @FXML
    private Label lb_conn_status;
    @FXML
    private ChoiceBox<String> cb_distribution_method;

    public static void main(String[] args) throws IOException {
        launch(args);
    }

    @Override
    public void start(Stage stage) {

        // load FXML file
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/router.fxml"));
        try {
            Parent root = loader.load();
            Scene scene = new Scene(root);
            primaryStage = stage;
            primaryStage.setTitle("Router");
            primaryStage.setScene(scene);
            primaryStage.show();

            // things to do on close
            primaryStage.setOnCloseRequest(event -> {
                closeConnections();
                Platform.exit();
                System.exit(0);
            });
        } catch (IOException e) {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Error loading FXML file");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
            System.exit(1);
        }
    }

    @FXML
    private void initialize() {

        logHandler = new BufferedLogHandler(ta_log, 50);
        log = logHandler.getLogStream();

        startListener(serverListener, listenSocket_server, serverPort, true);
        startListener(clientListener, listenSocket_client, clientPort, false);

        setConnectionListCellFactory(lv_servers);
        setConnectionListCellFactory(lv_clients);

        cb_distribution_method.getItems().addAll("Balanced", "Front-Loaded");
        cb_distribution_method.setValue("Balanced");

        // update connection lists every 500ms
        connListTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                ObservableList<String> servers = FXCollections.observableArrayList(
                        routingTable.stream()
                                .filter(conn -> conn.isServer())
                                .map(conn -> conn.getAddr() + ":" + conn.getPort()
                                        + "(" + conn.getLogicalCores() + ", " + conn.getSpeedRating() + ")"
                                        + (conn.getTotalTasks() > 0 ? "\n(" + conn.getTasksString() + ")"
                                                : "\nNo Active Tasks"))
                                .collect(Collectors.toList()));

                ObservableList<String> clients = FXCollections.observableArrayList(
                        routingTable.stream()
                                .filter(conn -> !conn.isServer())
                                .map(conn -> conn.getAddr() + ":" + conn.getPort()
                                        + (conn.getTotalTasks() > 0 ? "\n(" + conn.getTasksString() + ")"
                                                : "\nNo Active Tasks"))
                                .collect(Collectors.toList()));

                Platform.runLater(() -> lv_servers.setItems(servers));
                Platform.runLater(() -> lv_clients.setItems(clients));

                Platform.runLater(() -> lv_servers.refresh());
                Platform.runLater(() -> lv_clients.refresh());

                updateStatus();
            }
        }, 0, 500);
    }

    private void setConnectionListCellFactory(ListView<String> listView) {
        listView.setCellFactory(new Callback<ListView<String>, ListCell<String>>() {
            @Override
            public ListCell<String> call(ListView<String> param) {
                return new ListCell<String>() {
                    private final Text text;
                    private final VBox vbox;

                    {
                        text = new Text();
                        text.setStyle("-fx-font-size: 12px;");
                        text.setWrappingWidth(200); // Adjust width as needed
                        setPrefHeight(50); // Adjust height to fit two lines

                        vbox = new VBox(text);
                        vbox.setAlignment(Pos.CENTER);
                    }

                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setGraphic(null);
                        } else {
                            text.setText(item);
                            setGraphic(vbox);
                        }
                    }
                };
            }
        });
    }

    /**
     * Updates the status of the router application by displaying the number of
     * connected servers and total core count,
     * the number of clients, and the number of active tasks.
     */
    private void updateStatus() {
        StringBuilder sb = new StringBuilder();
        // display # connected servers and total core count, and clients and # of active
        // tasks
        int serverCount = (int) routingTable.stream().filter(conn -> conn.isServer()).count();
        int coreCount = routingTable.stream().filter(conn -> conn.isServer()).mapToInt(Connection::getLogicalCores)
                .sum();
        int clientCount = (int) routingTable.stream().filter(conn -> !conn.isServer()).count();
        int taskCount = (int) routingTable.stream().filter(conn -> conn.getTotalTasks() > 0).count();

        sb.append("Servers: ").append(serverCount).append(" (").append(coreCount).append(" cores)")
                .append("\nClients: ").append(clientCount)
                .append("\nTasks: ").append(taskCount);

        Platform.runLater(() -> lb_conn_status.setText(sb.toString()));
    }

    /**
     * Starts the listener thread to listen for incoming connections on the
     * specified port.
     * If the server socket is not provided, it creates a new server socket on the
     * specified port.
     * If the listener thread is already running, it interrupts the current thread
     * and starts a new one.
     *
     * @param listener     The current listener thread (can be null).
     * @param serverSocket The server socket to listen on (can be null).
     * @param port         The port number to listen on.
     * @param isServer     Indicates whether the listener is running on a server or
     *                     client.
     */
    private void startListener(ListenThread listener, ServerSocket serverSocket, int port, boolean isServer) {

        if (serverSocket == null) {
            try {
                serverSocket = new ServerSocket(port);
            } catch (IOException e) {
                Alert alert = new Alert(AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("Error starting listener");
                alert.setContentText("Could not create socket on port: " + port + "\n" + e.getMessage());
                alert.showAndWait();
                System.exit(1);
            }
        }

        try {

            if (listener != null)
                listener.interrupt();

            listener = new ListenThread(serverSocket, routingTable, this, isServer);
            listener.start();
        } catch (IOException e) {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Error starting listener");
            alert.setContentText("Could not listen on port: " + port + "\n" + e.getMessage());
            alert.showAndWait();
            System.exit(1);
        }

        log.println("Listener started on port: " + port);
    }

    /**
     * public access for logging from client and server threads to the GUI.
     * 
     * @param s
     */
    public void writeToConsole(String s) {
        log.println(s);
    }

    /**
     * Allocates servers based on the specified thread count and distribution method.
     *
     * @param threadCount the number of threads to allocate servers for
     * @return the number of servers allocated
     * @throws IOException if an I/O error occurs
     */
    public synchronized int allocateServers(int threadCount) throws IOException {
        if (cb_distribution_method.getValue().equals("Balanced")) {
            return allocateServersBalanced(threadCount);
        } else {
            return allocateServersFrontLoaded(threadCount);
        }
    }

    /**
     * Allocates servers in a balanced manner based on the number of cores required
     * 
     * allocates in batches of 4 cores, round-robin fashion
     * 
     * @param threadCount number of cores required
     * @return taskId
     * @throws IOException if no servers are available or not enough cores are
     *                     available
     */
    public synchronized int allocateServersBalanced(int threadCount) throws IOException {

        int n = 4; // Batch size of cores to allocate per server

        // Collect and sort all servers by total tasks then speed rating
        List<Connection> availableServers = getServersSortedByTasksThenSpeed();

        if (availableServers.isEmpty()) {
            throw new IOException("No servers available");
        }

        // Check if enough cores are available
        int availableCores = availableServers.stream().mapToInt(Connection::getLogicalCores).sum();
        if (availableCores < threadCount) {
            throw new IOException("Not enough cores available");
        }

        List<Connection> selectedServers = new ArrayList<>();
        Map<Integer, Integer> coreAllocation = new HashMap<>(); // maps server id to core count
        int usedCores = 0;
        int serverIndex = 0;

        // Loop through the required cores and allocate in batches of `n`
        while (usedCores < threadCount) {
            Connection currentServer = availableServers.get(serverIndex % availableServers.size());
            int serverId = currentServer.getId();
            int coresToAllocate = Math.min(n, currentServer.getLogicalCores());

            // Ensure we don't allocate more cores than needed
            if (usedCores + coresToAllocate > threadCount) {
                coresToAllocate = threadCount - usedCores;
            }

            if (!selectedServers.contains(currentServer)) {
                selectedServers.add(currentServer);
            }

            usedCores += coresToAllocate;

            if (coreAllocation.containsKey(serverId)) {
                coreAllocation.put(serverId, coreAllocation.get(serverId) + coresToAllocate);
            } else {
                coreAllocation.put(serverId, coresToAllocate);
            }

            // Move to the next server in round-robin fashion
            serverIndex++;
        }

        // We can utilize a max of 7 servers per task.
        if (selectedServers.size() > 7) {
            throw new IOException("Can't allocate enough cores with max server size of 7");
        }

        // Assign the task ID and update task tracking
        int taskId = taskCounter.incrementAndGet();
        for (Connection server : selectedServers) {
            int coreCount = coreAllocation.get(server.getId());
            server.addNewTask(taskId, coreCount);
        }

        return taskId;
    }

    /**
     * Allocates servers in a front-loaded manner based on the specified thread count.
     * Servers are selected based on their speed rating, with the highest-rated servers
     * being allocated first. The method checks if there are enough available servers
     * and cores to meet the requested thread count. If a single server can handle the
     * entire load, it is allocated with the requested number of cores. Otherwise, the
     * load is distributed across multiple servers, filling each server to its maximum
     * capacity or until the thread count requirement is met. The method limits the
     * server count to a maximum of 7 servers per task. A unique task ID is assigned
     * to the allocated servers, and the core usage per server is tracked.
     *
     * @param threadCount the number of threads to allocate
     * @return the task ID assigned to the allocated servers
     * @throws IOException if no servers are available, not enough cores are available,
     *                     or the maximum server count per task is exceeded
     */
    public synchronized int allocateServersFrontLoaded(int threadCount) throws IOException {

        // Collect all servers sorted by speed rating (highest to lowest)
        List<Connection> availableServers = getServersSortedByTasksThenSpeed();

        if (availableServers.isEmpty()) {
            throw new IOException("No servers available");
        }

        // Check if enough total cores are available
        int availableCores = availableServers.stream().mapToInt(Connection::getLogicalCores).sum();
        if (availableCores < threadCount) {
            throw new IOException("Not enough cores available to meet the request");
        }

        List<Connection> selectedServers = new ArrayList<>();
        Map<Integer, Integer> coreAllocation = new HashMap<>(); // Maps server id to core count
        int usedCores = 0;

        // First, check if a single server can handle the entire load
        for (Connection server : availableServers) {
            int cores = server.getLogicalCores();
            if (cores >= threadCount) {
                selectedServers.add(server);
                coreAllocation.put(server.getId(), threadCount);
                usedCores = threadCount;
                break;
            }
        }

        // If no single server can handle the load, distribute across multiple servers
        if (usedCores < threadCount) {
            for (Connection server : availableServers) {
                if (usedCores >= threadCount)
                    break; // Stop if the required cores are allocated

                int serverId = server.getId();
                int coresAvailable = server.getLogicalCores();
                int coresToAllocate = Math.min(coresAvailable, threadCount - usedCores); // Fill server to its max or
                                                                                         // until requirement is met

                selectedServers.add(server);
                coreAllocation.put(serverId, coresToAllocate);
                usedCores += coresToAllocate;
            }
        }

        // Limit server count to a max of 7 servers per task
        if (selectedServers.size() > 7) {
            throw new IOException("Can't allocate enough cores with max server size of 7");
        }

        // Assign a unique task ID and track core usage per server
        int taskId = taskCounter.incrementAndGet();
        for (Connection server : selectedServers) {
            int coreCount = coreAllocation.get(server.getId());
            try {
                server.addNewTask(taskId, coreCount);
            } catch (Exception e) {
                throw new IOException("Error adding task to server: " + e.getMessage());
            }
        }

        return taskId;
    }

    /**
     * Closes all connections and sockets used by the router application.
     * This method closes the server socket, client socket, and all connections in
     * the routing table.
     */
    private void closeConnections() {
        try {
            if (listenSocket_server != null)
                listenSocket_server.close();
            if (listenSocket_client != null)
                listenSocket_client.close();

            // iterate through all connections and close them
            for (Connection conn : routingTable) {
                conn.close();
            }

        } catch (IOException e) {
            System.exit(1);
        }
    }

    /**
     * Removes the specified connection from the routing table.
     *
     * @param conn the connection to be removed
     */
    public void removeConnection(Connection conn) {
        routingTable.remove(conn);
    }

    /**
     * Returns a list of server connections sorted by the total number of tasks and
     * speed rating.
     *
     * @return a list of server connections sorted by tasks and speed
     */
    public List<Connection> getServersSortedByTasksThenSpeed() {
        return routingTable.stream()
                .filter(Connection::isServer)
                .sorted(Comparator.comparingInt(Connection::getTotalTasks)
                        .thenComparingDouble(Connection::getSpeedRating))
                .collect(Collectors.toList());
    }

    /**
     * Returns a list of connections representing servers that have the specified
     * task ID.
     *
     * @param taskId the ID of the task
     * @return a list of connections representing servers with the specified task ID
     */
    public List<Connection> getServersByTaskId(int taskId) {
        return routingTable.stream()
                .filter(Connection::isServer)
                .filter(conn -> conn.hasTaskId(taskId))
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a list of server connections that have the specified task ID,
     * sorted by tasks and then speed.
     *
     * @param taskId The ID of the task to filter the server connections by.
     * @return A list of server connections that have the specified task ID, sorted
     *         by tasks and then speed.
     */
    public List<Connection> getServersByTaskIdSorted(int taskId) {
        List<Connection> servers = getServersSortedByTasksThenSpeed();
        return servers.stream()
                .filter(conn -> conn.hasTaskId(taskId))
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a client connection that has the specified task ID.
     *
     * @param taskId The ID of the task to filter the client connections by.
     * @return A client connection that has the specified task ID.
     */
    public Connection getClientByTaskId(int taskId) {
        return routingTable.stream()
                .filter(conn -> !conn.isServer())
                .filter(conn -> conn.hasTaskId(taskId))
                .findFirst()
                .orElse(null);
    }
}