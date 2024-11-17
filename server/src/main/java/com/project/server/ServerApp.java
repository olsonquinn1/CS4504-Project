package com.project.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;

import com.project.shared.BufferedLogHandler;
import com.project.shared.Data;
import static com.project.shared.MatrixUtil.generateSquareMatrix;
import static com.project.shared.MatrixUtil.matrixMult;
import com.project.shared.ProfilingData;
import com.project.shared.ProgressBar;
import com.project.shared.ResultData;
import com.project.shared.StrassenExecutor;
import com.project.shared.SubTaskData;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * The main class for the server application.
 * This class extends the JavaFX `Application` class and represents the server-side of the application.
 * It handles the GUI, networking, and computation tasks.
 */
public class ServerApp extends Application {

    private Socket socket = null;

    private ObjectOutputStream out = null;
    private ObjectInputStream in = null;

    private String routerAddr = null;
    private int routerPort = -1;

    private Stage primaryStage;

    private BufferedLogHandler logHandler;
    private PrintStream log;

    private final int numCores = Runtime.getRuntime().availableProcessors();

    private double computeScore = 0;

    ProgressBar progressBar;

    private final BlockingQueue<Data> outBuffer = new LinkedBlockingQueue<>();

    @FXML
    private TextField tf_addr;
    @FXML
    private TextField tf_port;
    @FXML
    private Label lb_conn_status;
    @FXML
    private TextArea ta_log;

    public static void main(String[] args) throws IOException {
        launch(args);
    }

    @Override
    public void start(Stage stage) {

        //load FXML file
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/server.fxml"));
        try {
            Parent root = loader.load();
            Scene scene = new Scene(root);
            primaryStage = stage;
            primaryStage.setTitle("Server");
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
        tf_port.setText("5555");

        //initialize log
        logHandler = new BufferedLogHandler(ta_log, 1000);
        log = logHandler.getLogStream();

        //run profiling in a separate thread
        new Thread(() -> {
            // sleep thread for a few seconds to allow the window to load
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                log.println("Error sleeping thread: " + e.getMessage());
            }
            log.println("Profiling compute capability...");
            computeScore = profileComputeCapability();
            log.println("Compute capability: " + computeScore + "ms");
            log.println("Number of cores: " + numCores);
        }).start();
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

        //start the read and write loops
        new Thread(this::readLoop).start();
        new Thread(this::writeLoop).start();

        //send profiling data to router
        ProfilingData profileData = new ProfilingData(numCores, computeScore);

        Data send = new Data(
                Data.Type.PROFILING_DATA,
                profileData);

        outBuffer.add(send);

        // set addr and port fields to uneditable
        runOnFxThread(() -> tf_addr.setEditable(false));
        runOnFxThread(() -> tf_port.setEditable(false));
    }

    /**
     * Computes the average time taken to perform 50 matrix multiplications on two 512x512 matrices.
     * 
     * @return The average time taken to compute the matrix multiplications.
     */
    public double profileComputeCapability() {

        progressBar = new ProgressBar(50, 50, log);

        int numTests = 20;
        int matrixSize = 512;

        long[] times = new long[numTests];

        int[][] A = generateSquareMatrix(matrixSize);
        int[][] B = generateSquareMatrix(matrixSize);

        progressBar.start();
        for (int i = 0; i < numTests; i++) {
            long startTime = System.currentTimeMillis();
            matrixMult(A, B);
            times[i] = System.currentTimeMillis() - startTime;
            progressBar.progress(1);
        }
        progressBar.stop();

        long sum = 0;
        for (long time : times) {
            sum += time;
        }

        return (double) sum / numTests;
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
            runOnFxThread(() -> lb_conn_status.setText("Host not found"));
            log.println(routerAddr + ":" + routerPort + " Host not found");
            return;
        }
        catch (SocketTimeoutException e) {
            runOnFxThread(() -> lb_conn_status.setText("Connection timed out"));
            log.println(routerAddr + ":" + routerPort + " Connection timed out");
            return;
        }
        catch (IOException e) {
            runOnFxThread(() -> lb_conn_status.setText("Error connecting to router: " + e.getMessage()));
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
     * Reads messages from the server continuously until a close message is received.
     * Messages can be of type RESPONSE or RESULT_DATA.
     * If a close message is received, the method will close the connections.
     */
    private void readLoop() {
        while (true) {
            Data recv = null;
            try {
                recv = (Data) in.readObject();
            } catch (ClassNotFoundException e) {
                log.println("Error deserializing object from server: " + e.getMessage());
                break;
            } catch (IOException e) {
                log.println("Error reading object from server: " + e.getMessage());
                break;
            }

            if (recv == null || recv.getType() == Data.Type.CLOSE) {
                log.println("Connection closed by server");
                break;
            }
            else if (recv.getType() == Data.Type.SUBTASK_DATA) {
                log.println("Received task data from server");
                handleComputation((SubTaskData) recv.getData());
            } else {
                log.println("Received unknown data type from server");
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
        closeConnections(false);
    }

    /**
     * Handles the computation of a subtask.
     * 
     * This method uses the Strassen algorithm to compute the matrix multiplication.
     * @see com.project.shared.StrassenExecutor
     *
     * @param subtask The subtask data containing the matrices and task information.
     */
    private void handleComputation(SubTaskData subtask) {

        log.println("Processing Task " + subtask.getTaskId() + " - " + subtask.getM());

        //log matrix sizes
        log.println("Matrix A: " + subtask.getMatrixA().length + "x" + subtask.getMatrixA()[0].length);
        log.println("Matrix B: " + subtask.getMatrixB().length + "x" + subtask.getMatrixB()[0].length);

        StrassenExecutor executor = new StrassenExecutor(subtask.getCoresToUse(), 64);

        int[][] result = null;
        try {
            result = executor.run(subtask.getMatrixA(), subtask.getMatrixB(), log);
        } catch (InterruptedException e) {
            log.println("Error running Strassen algorithm (0): " + e.getMessage());
        } catch(ExecutionException e) {
            log.println("Error running Strassen algorithm (1): " + e.getMessage());
        } catch(IllegalArgumentException e) {
            log.println("Error running Strassen algorithm (2): " + e.getMessage());
        }

        ResultData resultData = new ResultData(
            result,
            subtask.getM(),
            subtask.getTaskId()
        );

        Data send = new Data(
            Data.Type.RESULT_DATA,
            resultData
        );

        log.println("Sending result data to server");

        outBuffer.add(send);
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
        runOnFxThread(() -> tf_addr.setEditable(true));
        runOnFxThread(() -> tf_port.setEditable(true));
    }

    public synchronized void runOnFxThread(Runnable task) {
        Platform.runLater(task);
    }
}