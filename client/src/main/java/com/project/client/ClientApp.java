package com.project.client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.project.shared.BufferedLogHandler;
import com.project.shared.Data;
import static com.project.shared.MatrixUtil.generateSquareMatrix;
import com.project.shared.RequestData;
import com.project.shared.ResponseData;
import com.project.shared.ResultData;
import com.project.shared.TaskData;
import com.project.shared.Timestamp;

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
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * The main class for the client application.
 * This class handles the GUI and the client-side logic for connecting to the
 * router and sending requests.
 */
public class ClientApp extends Application {

    private Socket socket = null;

    private ObjectOutputStream out = null;
    private ObjectInputStream in = null;

    private String routerAddr = null;
    private int routerPort = -1;

    private Stage primaryStage;

    private final BlockingQueue<Data> outBuffer = new LinkedBlockingQueue<>();

    private BufferedLogHandler logHandler;
    private PrintStream log;

    private Thread readThread;
    private Thread writeThread;

    // maps taskId to timestamps
    private final Map<Integer, List<Timestamp>> timestamps = new HashMap<>();

    private List<Integer> matrixSizes;
    private List<Integer> threadCounts;

    private TimestampHandler[][] testResults;
    private DataViewGenerator dataViewGenerator;

    private List<String> viewNames;
    private List<String[][]> views;

    @FXML
    private TextField tf_addr;
    @FXML
    private TextField tf_port;
    @FXML
    private Label lb_conn_status;
    @FXML
    private TextArea ta_log;
    @FXML
    private ChoiceBox<Integer> cb_mat_size;
    @FXML
    private ChoiceBox<Integer> cb_thread_count;
    @FXML
    private TableView<TableRowData> tv_analysis;
    @FXML
    ChoiceBox<String> cb_view;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {

        // load FXML file
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/client.fxml"));
        try {
            Parent root = loader.load();
            Scene scene = new Scene(root);
            primaryStage = stage;
            primaryStage.setTitle("Client");
            primaryStage.setScene(scene);
            primaryStage.show();

            // things to do on close
            primaryStage.setOnCloseRequest(event -> {
                if (socket != null) {
                    closeConnections(true);
                    if (logHandler != null) {
                        logHandler.stop();
                    }
                }
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
    public void initialize() {

        // listener for address field
        tf_addr.textProperty().addListener((observable, oldValue, newValue) -> {
            routerAddr = newValue;
        });

        // listener for port field, validates for integer input
        tf_port.textProperty().addListener((observable, oldValue, newValue) -> {
            try {
                routerPort = Integer.parseInt(newValue);
            } catch (NumberFormatException e) {
                routerPort = -1;
                tf_port.setStyle("-fx-border-color: red");
                return;
            }
            tf_port.setStyle("-fx-border-color: black");
        });

        lb_conn_status.setText("Disconnected");

        tf_addr.setText("localhost");
        tf_port.setText("5556");

        initializeTable();

        // initialize log
        logHandler = new BufferedLogHandler(ta_log, 50);
        log = logHandler.getLogStream();

        // initialize matrix sizes and thread counts
        matrixSizes = new ArrayList<Integer>() {
            {
                add(256);
                add(512);
                add(1024);
                add(2048);
                add(4096);
            }
        };
        threadCounts = new ArrayList<Integer>() {
            {
                add(1);
                add(3);
                add(7);
                add(15);
                add(31);
            }
        };
        viewNames = new ArrayList<String>() {
            {
                add("Total Time");
                add("Processing Time");
                add("Networking Time");
                add("Networking-Processing Overlap Ratio (exluding client networking time)");
                add("Speedup (exluding client networking time)");
                add("Efficiency (exluding client networking time)");
                add("Speedup (including client networking time)");
                add("Efficiency (including client networking time)");
            }
        };

        testResults = new TimestampHandler[matrixSizes.size()][threadCounts.size()];

        dataViewGenerator = new DataViewGenerator(testResults);

        // listener for view choice box
        cb_view.getItems().addAll(viewNames);
        cb_view.setValue(viewNames.get(0));
        updateViews();

        cb_view.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
            int index = cb_view.getSelectionModel().getSelectedIndex();
            updateTableValues(views.get(index));
        });

        // test scenarios
        cb_mat_size.getItems().addAll(matrixSizes);
        cb_mat_size.setValue(matrixSizes.get(0));

        cb_thread_count.getItems().addAll(threadCounts);
        cb_thread_count.setValue(threadCounts.get(0));
    }

    /**
     * Initializes the table view with column headers and data.
     * Sets the preferred height for each row and aligns the rows to the center.
     * Adds columns to the table view and sets cell value factories for each column.
     * Populates the table view with initial data.
     */
    private void initializeTable() {
        // Initialize column headers in your Controller class, assuming `tv_analysis` is
        // a TableView
        tv_analysis.setRowFactory(tv -> {
            TableRow<TableRowData> row = new TableRow<>();
            row.setPrefHeight(64); // Set the preferred height for each row, adjust as needed
            row.setAlignment(Pos.CENTER);
            return row;
        });

        TableColumn<TableRowData, String> sizeColumn = new TableColumn<>("Matrix Size");
        TableColumn<TableRowData, String> thread1Column = new TableColumn<>("1 Thread");
        TableColumn<TableRowData, String> thread3Column = new TableColumn<>("3 Threads");
        TableColumn<TableRowData, String> thread7Column = new TableColumn<>("7 Threads");
        TableColumn<TableRowData, String> thread15Column = new TableColumn<>("15 Threads");
        TableColumn<TableRowData, String> thread31Column = new TableColumn<>("31 Threads");

        // Add columns to TableView
        tv_analysis.getColumns().addAll(sizeColumn, thread1Column, thread3Column, thread7Column, thread15Column,
                thread31Column);

        sizeColumn.setCellValueFactory(cellData -> cellData.getValue().sizeProperty());
        thread1Column.setCellValueFactory(cellData -> cellData.getValue().thread1Property());
        thread3Column.setCellValueFactory(cellData -> cellData.getValue().thread3Property());
        thread7Column.setCellValueFactory(cellData -> cellData.getValue().thread7Property());
        thread15Column.setCellValueFactory(cellData -> cellData.getValue().thread15Property());
        thread31Column.setCellValueFactory(cellData -> cellData.getValue().thread31Property());

        setColumnCellFactory(sizeColumn);
        setColumnCellFactory(thread1Column);
        setColumnCellFactory(thread3Column);
        setColumnCellFactory(thread7Column);
        setColumnCellFactory(thread15Column);
        setColumnCellFactory(thread31Column);

        ObservableList<TableRowData> data = FXCollections.observableArrayList(
                new TableRowData("512", "-", "-", "-", "-", "-"),
                new TableRowData("1024", "-", "-", "-", "-", "-"),
                new TableRowData("2048", "-", "-", "-", "-", "-"),
                new TableRowData("4096", "-", "-", "-", "-", "-"),
                new TableRowData("8192", "-", "-", "-", "-", "-"));
        tv_analysis.setItems(data);
    }

    /**
     * Sets the cell factory for a TableColumn.
     * The cell factory is responsible for creating and updating the cells within the TableColumn.
     *
     * @param column the TableColumn for which to set the cell factory
     * @param <T> the type of the TableView items
     */
    private void setColumnCellFactory(TableColumn<TableRowData, String> column) {
        column.setCellFactory(tc -> {
            return new TableCell<TableRowData, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item == null || empty) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        setText(item);
                        setPrefHeight(40);
                        setAlignment(Pos.CENTER);
                    }
                }
            };
        });
    }

    /**
     * Updates the values in the table with the provided data matrix.
     *
     * @param dataMatrix The matrix containing the data to be displayed in the table.
     */
    private void updateTableValues(String[][] dataMatrix) {
        ObservableList<TableRowData> tableData = FXCollections.observableArrayList();

        // Populate TableRowData with existing row labels and data from `dataMatrix`
        for (int row = 0; row < dataMatrix.length; row++) {
            String matrixSize = matrixSizes.get(row).toString();
            String thread1 = dataMatrix[row][0];
            String thread3 = dataMatrix[row][1];
            String thread7 = dataMatrix[row][2];
            String thread15 = dataMatrix[row][3];
            String thread31 = dataMatrix[row][4];

            TableRowData rowData = new TableRowData(matrixSize, thread1, thread3, thread7, thread15, thread31);
            tableData.add(rowData);
        }

        Platform.runLater(() -> tv_analysis.setItems(tableData));
    }

    /**
     * Updates the views in the client application.
     * This method initializes a list of views and adds various data views to it.
     * It then retrieves the selected view from a combo box and updates the table values accordingly.
     */
    private void updateViews() {
        views = new ArrayList<>();
        views.add(dataViewGenerator.generateTotalTimeView());
        views.add(dataViewGenerator.generateProcessingTimeView());
        views.add(dataViewGenerator.generateNetworkingTimeView());
        views.add(dataViewGenerator.generateNetworkingProcessingOverlapRatioView());
        views.add(dataViewGenerator.generateSpeedupView("total processing time"));
        views.add(dataViewGenerator.generateEfficiencyView("total processing time"));
        views.add(dataViewGenerator.generateSpeedupView("total time"));
        views.add(dataViewGenerator.generateEfficiencyView("total time"));

        int selectedView = cb_view.getSelectionModel().getSelectedIndex();
        updateTableValues(views.get(selectedView));
    }

    /**
     * Reads messages from the server continuously until a close message is
     * received.
     * Messages can be of type RESPONSE or RESULT_DATA.
     * If a close message is received, the method will close the connections.
     */
    private void readLoop() {
        while (true) {

            // wait for message from server
            Data recv = null;
            try {
                recv = (Data) in.readObject();
            } catch (ClassNotFoundException e) {
                log.println("Error deserializing object from server: " + e.getMessage());
                continue;
            } catch (IOException e) {
                log.println("Error reading object from server: " + e.getMessage());
                break;
            }

            // check for close message
            if (recv.getType() == Data.Type.CLOSE) {
                log.println("Connection closed by server");
                break;
            }

            // check for response message
            if (recv.getType() == Data.Type.RESPONSE) {
                handleResponse((ResponseData) recv.getData());
            }

            // check for result message
            if (recv.getType() == Data.Type.RESULT_DATA) {
                handleResult((ResultData) recv.getData());
            }
        }

        closeConnections(false);
    }

    /**
     * Continuously writes data from the output buffer to the server.
     * This method runs in a loop until interrupted or an error occurs.
     */
    private void writeLoop() {
        while (true) {
            Data data = null;
            try {
                data = outBuffer.take();
                out.writeObject(data);
                out.flush();
            } catch (InterruptedException e) {
                log.println("Write thread interrupted: " + e.getMessage());
                break;
            } catch (IOException e) {
                log.println("Error writing object to server: " + e.getMessage());
                break;
            }
        }
    }

    /**
     * Handles the response received from the server.
     * If the response is successful, generates matrices and sends them to the
     * server.
     * If the response is unsuccessful, logs the rejection message from the server.
     *
     * @param resp The response data received from the server.
     */
    private void handleResponse(ResponseData resp) {

        if (!resp.isSuccess()) {
            log.println("Server rejected request: " + resp.getMessage());
            return;
        }
        log.println("Server accepted request");

        // generate matrices and send to server
        log.println("Generating matrices");

        int[][] A = generateSquareMatrix(cb_mat_size.getValue());
        int[][] B = generateSquareMatrix(cb_mat_size.getValue());

        log.println("Matrices generated, sending to server");

        // prepare task data and send it out
        TaskData task = new TaskData(A, B, resp.getTaskId(), cb_thread_count.getValue());

        timestamps.put(resp.getTaskId(), new ArrayList<>());

        Data data = new Data(
                Data.Type.TASK_DATA,
                task);

        timestamps.get(resp.getTaskId()).add(new Timestamp("task sent by client"));

        outBuffer.add(data);
    }

    private void handleResult(ResultData result) {
        timestamps.get(result.getTaskId()).add(new Timestamp("result received by client"));
        log.println("Received result from server");

        List<Timestamp> allTimestamps = timestamps.get(result.getTaskId());
        allTimestamps.addAll(result.getTimestamps());

        TimestampHandler handler = new TimestampHandler(allTimestamps);

        int sizeIndex = cb_mat_size.getSelectionModel().getSelectedIndex();
        int threadIndex = cb_thread_count.getSelectionModel().getSelectedIndex();

        testResults[sizeIndex][threadIndex] = handler;

        dataViewGenerator = new DataViewGenerator(testResults);
        updateViews();

        //clean up timestamps
        timestamps.remove(result.getTaskId());
    }

    /**
     * Connects to the router using the specified address and port.
     * 
     * @throws IOException            if there is an error connecting to the router
     * @throws UnknownHostException   if the router address is unknown
     * @throws SocketTimeoutException if the connection to the router times out
     */
    public void connectToRouter() throws IOException, UnknownHostException, SocketTimeoutException {

        if (routerAddr == null || routerPort == -1) {
            throw new IOException("Router address and port not set");
        }

        if (socket != null && !socket.isClosed()) {
            log.println("Already connected to router");
            return;
        }

        socket = new Socket();
        socket.connect(new InetSocketAddress(routerAddr, routerPort), 5000);

        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());

        // start read and write threads
        readThread = new Thread(this::readLoop);
        writeThread = new Thread(this::writeLoop);

        readThread.start();
        writeThread.start();

        // set addr and port fields to uneditable
        Platform.runLater(() -> tf_addr.setEditable(false));
        Platform.runLater(() -> tf_port.setEditable(false));
    }

    /**
     * Closes the connections to the router.
     * 
     * @param sendMessage a boolean indicating whether to send a close message to
     *                    the router
     */
    public void closeConnections(boolean sendMessage) {

        if (socket == null || socket.isClosed() || out == null || in == null) {
            log.println("Not connected to router");
            return;
        }

        if (sendMessage) {
            // send close message
            Data close = new Data(
                    Data.Type.CLOSE,
                    null);

            outBuffer.add(close);

            // sleep for a bit to allow the message to be sent
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                log.println("Error sleeping: " + e.getMessage());
            }
        }

        try {
            out.close();
            in.close();
            socket.close();
        } catch (IOException e) {
            log.println("Error closing connections: " + e.getMessage());
        }

        log.println("Disconnected from router");

        // set addr and port fields to editable
        Platform.runLater(() -> tf_addr.setEditable(true));
        Platform.runLater(() -> tf_port.setEditable(true));
    }

    /**
     * Handles the event when the connect button is clicked.
     * Attempts to connect to the specified router address and port.
     * If the connection is successful, updates the status and logs the connection
     * details.
     * If an error occurs during the connection, displays an error message and logs
     * the error details.
     */
    @FXML
    public void connectButtonClicked() {

        log.println("Attempting to connect to " + routerAddr + ":" + routerPort);

        try {
            connectToRouter();
        } catch (UnknownHostException e) {
            lb_conn_status.setText("Host not found");
            log.println(routerAddr + ":" + routerPort + " Host not found");
            return;
        } catch (SocketTimeoutException e) {
            lb_conn_status.setText("Connection timed out");
            log.println(routerAddr + ":" + routerPort + " Connection timed out");
            return;
        } catch (IOException e) {
            lb_conn_status.setText("Error connecting to router: " + e.getMessage());
            log.println(routerAddr + ":" + routerPort + " Error connecting to router: " + e.getMessage());
            return;
        }

        lb_conn_status.setText("Connected\n" + routerAddr + ":" + routerPort);

        log.println("Connected to " + routerAddr + ":" + routerPort);
    }

    /**
     * Handles the event when the disconnect button is clicked.
     * Closes the connections and updates the status.
     */
    @FXML
    public void disconnectButtonClicked() {
        closeConnections(true);
        lb_conn_status.setText("Disconnected");
    }

    /**
     * Handles the event when the send button is clicked.
     * Sends a request to the router if connected, otherwise logs an error message.
     */
    @FXML
    public void sendButtonClicked() {

        if (socket == null || socket.isClosed() || out == null || in == null) {
            log.println("Unable to send: Not connected to router");
            return;
        }

        RequestData req = new RequestData(
                cb_thread_count.getValue());

        Data data = new Data(
                Data.Type.REQUEST,
                req);

        outBuffer.add(data);
        log.println("Request sent to router");
    }
}