package com.project.client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.project.shared.BufferedLogHandler;
import com.project.shared.Data;
import static com.project.shared.MatrixUtil.generateSquareMatrix;
import com.project.shared.RequestData;
import com.project.shared.ResponseData;
import com.project.shared.TaskData;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * The main class for the client application.
 * This class handles the GUI and the client-side logic for connecting to the router and sending requests.
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

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {

        //load FXML file
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/client.fxml"));
        try {
            Parent root = loader.load();
            Scene scene = new Scene(root);
            primaryStage = stage;
            primaryStage.setTitle("Client");
            primaryStage.setScene(scene);
            primaryStage.show();

            //things to do on close
            primaryStage.setOnCloseRequest(event -> {
                if (socket != null) {
                   closeConnections(true);
                   if(logHandler != null) {
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

        //listener for address field
        tf_addr.textProperty().addListener((observable, oldValue, newValue) -> {
            routerAddr = newValue;
        });

        //listener for port field, validates for integer input
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

        //initialize log
        logHandler = new BufferedLogHandler(ta_log, 100);
        log = logHandler.getLogStream();

        //test scenarios
        cb_mat_size.getItems().addAll(1024, 2048, 4096, 8192);
        cb_mat_size.setValue(1024);

        cb_thread_count.getItems().addAll(1, 3, 7, 15, 31);
        cb_thread_count.setValue(1);
    }

    /**
     * Reads messages from the server continuously until a close message is received.
     * Messages can be of type RESPONSE or RESULT_DATA.
     * If a close message is received, the method will close the connections.
     */
    private void readLoop() {
        while (true) {

            //wait for message from server
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
            
            //check for close message
            if(recv.getType() == Data.Type.CLOSE) {
                log.println("Connection closed by server");
                break;
            }

            //check for response message
            if(recv.getType() == Data.Type.RESPONSE) {
                handleResponse((ResponseData) recv.getData());
            }

            //check for result message
            if(recv.getType() == Data.Type.RESULT_DATA) {
                log.println("Received result from server");
            }
        }

        closeConnections(false);
    }

    /**
     * Continuously writes data from the output buffer to the server.
     * This method runs in a loop until interrupted or an error occurs.
     */
    private void writeLoop() {
        while(true) {
            Data data = null;
            try {
                data = outBuffer.take();
                out.writeObject(data);
                out.flush();
            }
            catch (InterruptedException e) {
                log.println("Write thread interrupted: " + e.getMessage());
                break;
            }
            catch (IOException e) {
                log.println("Error writing object to server: " + e.getMessage());
                break;
            }
        }
    }

    /**
     * Handles the response received from the server.
     * If the response is successful, generates matrices and sends them to the server.
     * If the response is unsuccessful, logs the rejection message from the server.
     *
     * @param resp The response data received from the server.
     */
    private void handleResponse(ResponseData resp) {

        if(!resp.isSuccess()) {
            log.println("Server rejected request: " + resp.getMessage());
            return;
        }
        log.println("Server accepted request");

        //generate matrices and send to server
        log.println("Generating matrices");

        int[][] A = generateSquareMatrix(cb_mat_size.getValue());
        int[][] B = generateSquareMatrix(cb_mat_size.getValue());

        log.println("Matrices generated, sending to server");

        //prepare task data and send it out
        TaskData task = new TaskData(A, B, resp.getTaskId(), cb_thread_count.getValue());

        Data data = new Data(
            Data.Type.TASK_DATA,
            task
        );

        outBuffer.add(data);
    }

    /**
     * Connects to the router using the specified address and port.
     * 
     * @throws IOException           if there is an error connecting to the router
     * @throws UnknownHostException if the router address is unknown
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

        //start read and write threads
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
     * @param sendMessage a boolean indicating whether to send a close message to the router
     */
    public void closeConnections(boolean sendMessage) {

        if (socket == null || socket.isClosed() || out == null || in == null) {
            log.println("Not connected to router");
            return;
        }

        if(sendMessage) {
            //send close message
            Data close = new Data(
                Data.Type.CLOSE,
                null
            );

            outBuffer.add(close);
            
            //sleep for a bit to allow the message to be sent
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
     * If the connection is successful, updates the status and logs the connection details.
     * If an error occurs during the connection, displays an error message and logs the error details.
     */
    @FXML
    public void connectButtonClicked() {

        log.println("Attempting to connect to " + routerAddr + ":" + routerPort);

        try {
            connectToRouter();
        }
        catch (UnknownHostException e) {
            lb_conn_status.setText("Host not found");
            log.println(routerAddr + ":" + routerPort + " Host not found");
            return;
        }
        catch (SocketTimeoutException e) {
            lb_conn_status.setText("Connection timed out");
            log.println(routerAddr + ":" + routerPort + " Connection timed out");
            return;
        }
        catch (IOException e) {
            lb_conn_status.setText("Error connecting to router: " + e.getMessage());
            log.println(routerAddr + ":" + routerPort + " Error connecting to router: " + e.getMessage());
            return;
        }

        lb_conn_status.setText("Connected to " + routerAddr + ":" + routerPort);

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
            cb_thread_count.getValue()
        );

        Data data = new Data(
            Data.Type.REQUEST,
            req
        );

        outBuffer.add(data);
        log.println("Request sent to router");
    }
}