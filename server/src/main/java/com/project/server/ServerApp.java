package com.project.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;

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
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class ServerApp extends Application {

    private Socket socket = null;

    private ObjectOutputStream out = null;
    private ObjectInputStream in = null;

    private String myAddr = null;
    private int myPort = -1;

    private String routerAddr = null;
    private int routerPort = -1;

    private Stage primaryStage;

    private PrintStream log;
    private final Object logLock = new Object();

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
        System.out.println("Starting application...");
        launch(args);
    }

    @Override
    public void start(Stage stage) {

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/server.fxml"));
        try {
            Parent root = loader.load();
            Scene scene = new Scene(root);
            primaryStage = stage;
            primaryStage.setTitle("Server");
            primaryStage.setScene(scene);
            primaryStage.show();
            primaryStage.setOnCloseRequest(event -> {
                if (socket != null) {
                    try {
                        out.close();
                        in.close();
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        System.exit(1);
                    }
                }
                System.exit(0);
            });
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    @FXML
    public void initialize() {

        setUpLogStream(ta_log);

        tf_addr.textProperty().addListener((observable, oldValue, newValue) -> {
            routerAddr = newValue;
        });

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

        tf_addr.setText("localhost");
        tf_port.setText("5555");

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

    private void setUpLogStream(TextArea ta) {
        OutputStream outStream = new OutputStream() {
           @Override
           public void write(int b) {
              synchronized(logLock) {
                ta.appendText(String.valueOf((char) b));
              }
           }
  
           @Override
           public void write(byte[] b, int off, int len) {
              synchronized(logLock) {
                ta.appendText(new String(b, off, len));
              }
           }
        };

        log = new PrintStream(outStream, true);
    }

    public void connectToRouter() throws IOException, UnknownHostException {

        if (routerAddr == null || routerPort == -1) {
            throw new IOException("Router address and port not set");
        }

        if (socket != null) {
            socket.close();
        }

        socket = new Socket(routerAddr, routerPort);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());

        myAddr = socket.getLocalAddress().getHostAddress();
        myPort = socket.getLocalPort();

        new Thread(this::readLoop).start();
        new Thread(this::writeLoop).start();

        ProfilingData profileData = new ProfilingData(numCores, computeScore);

        Data send = new Data(
                Data.Type.PROFILING_DATA,
                profileData);

        outBuffer.add(send);

        // set addr and port fields to uneditable
        tf_addr.setEditable(false);
        tf_port.setEditable(false);
    }

    // computes 50 matrix multiplications on two 512x512 matrices
    // returns the average time taken to compute the multiplication
    public double profileComputeCapability() {

        progressBar = new ProgressBar(50, 50, log);

        int numTests = 50;
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

    @FXML
    public void connectButtonClicked() {

        try {
            connectToRouter();
        } catch (UnknownHostException e) {
            lb_conn_status.setText("Host not found");
            log.println(routerAddr + ":" + routerPort + " Host not found");
            return;
        } catch (IOException e) {
            lb_conn_status.setText("Error connecting to router");
            log.println(routerAddr + ":" + routerPort + " Error connecting to router");
            return;
        }

        lb_conn_status.setText("Connected to router: " + routerAddr + ":" + routerPort);
        log.println("Connected to " + routerAddr + ":" + routerPort);
    }

    @FXML
    public void disconnectButtonClicked() {
        try {
            closeConnections();
        } catch (IOException e) {
            lb_conn_status.setText("Error disconnecting from router");
            log.println("Error disconnecting from router");
            return;
        }
        lb_conn_status.setText("Disconnected");
        log.println("Disconnected from router");
    }

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
                try {
                    closeConnections();
                } catch (IOException e) {
                    log.println("Error disconnecting from router: " + e.getMessage());
                }
                break;
            }
            else if (recv.getType() == Data.Type.SUBTASK_DATA) {
                log.println("Received task data from server");
                handleComputation((SubTaskData) recv.getData());
            }
        }
    }

    private void handleComputation(SubTaskData subtask) {

        log.println("Processing Task " + subtask.getTaskId() + " - " + subtask.getM());

        //log matrix sizes
        log.println("Matrix A: " + subtask.getMatrixA().length + "x" + subtask.getMatrixA()[0].length);
        log.println("Matrix B: " + subtask.getMatrixB().length + "x" + subtask.getMatrixB()[0].length);

        StrassenExecutor executor = new StrassenExecutor(subtask.getCoresToUse(), 64);

        int[][] result = null;
        try {
            result = executor.run(subtask.getMatrixA(), subtask.getMatrixB());
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

    public void closeConnections() throws IOException {

        if (socket == null || socket.isClosed()) {
            return;
        }

        // send close message
        Data close = new Data(
            Data.Type.CLOSE,
            null
        );

        outBuffer.add(close);

        // sleep thread to allow message to be sent
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            log.println("Error sleeping thread: " + e.getMessage());
        }

        out.close();
        in.close();
        socket.close();

        log.println("Disconnected from router");

        // set addr and port fields to editable
        runOnFxThread(() -> tf_addr.setEditable(true));
        runOnFxThread(() -> tf_port.setEditable(true));
    }

    public synchronized void runOnFxThread(Runnable task) {
        Platform.runLater(task);
    }
}