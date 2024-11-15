package com.project.client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.project.shared.Data;
import static com.project.shared.MatrixUtil.generateSquareMatrix;
import com.project.shared.RequestData;
import com.project.shared.ResponseData;
import com.project.shared.TaskData;

import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class ClientApp extends Application {

    private Socket socket = null;

    private ObjectOutputStream out = null;
    private ObjectInputStream in = null;

    private String myAddr = null;
    private int myPort = -1;

    private String routerAddr = null;
    private int routerPort = -1;

    private Stage primaryStage;

    private final BlockingQueue<Data> outBuffer = new LinkedBlockingQueue<>();

    private long lastPing = 0;

    private PrintStream log;

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

    private enum STATE {
        IDLE,
        WAITING_FOR_CONFIRMATION,
        WAITING_FOR_RESULT
    }

    private STATE state = STATE.IDLE;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/client.fxml"));
        try {
            Parent root = loader.load();
            Scene scene = new Scene(root);
            primaryStage = stage;
            primaryStage.setTitle("Client");
            primaryStage.setScene(scene);
            primaryStage.show();
            primaryStage.setOnCloseRequest(event -> {
                if (socket != null) {
                   try {
                      closeConnections(false);
                   } catch (IOException e) {
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

        lb_conn_status.setText("Disconnected");

        tf_addr.setText("localhost");
        tf_port.setText("5556");

        cb_mat_size.getItems().addAll(1024, 2048, 4096, 8192);
        cb_mat_size.setValue(1024);

        cb_thread_count.getItems().addAll(1, 3, 7, 15, 31);
        cb_thread_count.setValue(1);

        setUpLogStream(ta_log);

        updateStatus();
    }

    private void setUpLogStream(TextArea ta) {
      OutputStream outStream = new OutputStream() {
         @Override
         public synchronized void write(int b) {
            ta.appendText(String.valueOf((char) b));
         }

         @Override
         public synchronized void write(byte[] b, int off, int len) {
            ta.appendText(new String(b, off, len));
         }
      };

      log = new PrintStream(outStream, true);
    }

    private void updateStatus() {
        StringBuilder sb = new StringBuilder();

        if (socket == null || socket.isClosed()) {
            sb.append("Disconnected");
        } else {
            sb.append("Connected to ");
            sb.append(routerAddr);
            sb.append(":");
            sb.append(routerPort);
        }

        sb.append("\n");
        sb.append(state == STATE.WAITING_FOR_CONFIRMATION ? "Waiting for confirmation" : state == STATE.WAITING_FOR_RESULT ? "Waiting for result" : "Idle");

        lb_conn_status.setText(sb.toString());
    }

    private void readLoop() {
        while (true) {

            //wait for message from server
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
            
            //check for close message
            if(recv.getType() == Data.Type.CLOSE) {
                log.println("Connection closed by server");
                try {
                    closeConnections(false);
                } catch (IOException e) {
                    log.println("Error disconnecting from router: " + e.getMessage());
                }
                break;
            }

            //check for response message
            if(recv.getType() == Data.Type.RESPONSE) {

                //we should only get a response if we are waiting for confirmation
                if(state != STATE.WAITING_FOR_CONFIRMATION) {
                    log.println("Received unexpected response from server");
                    continue;
                }

                ResponseData resp = (ResponseData)recv.getData();
                if(resp.isSuccess()) {
                    log.println("Server accepted request");
                } else {
                    log.println("Server rejected request: " + resp.getMessage());
                }

                //generate matrices and send to server
                log.println("Generating matrices");

                int[][] A = generateSquareMatrix(cb_mat_size.getValue());
                int[][] B = generateSquareMatrix(cb_mat_size.getValue());

                log.println("Matrices generated, sending to server");

                TaskData task = new TaskData(A, B, cb_mat_size.getValue());

                Data data = new Data(
                    Data.Type.TASK_DATA,
                    myAddr, myPort, routerAddr, routerPort,
                    task
                );

                outBuffer.add(data);

                state = STATE.WAITING_FOR_RESULT;

                updateStatus();
            }

            //check for result message
            if(recv.getType() == Data.Type.RESULT_DATA) {

                //we should only get a result if we are waiting for a result
                if(state != STATE.WAITING_FOR_RESULT) {
                    log.println("Received unexpected result from server");
                    continue;
                }

                log.println("Received result from server");

                //other stuff here

                state = STATE.IDLE;

                updateStatus();
            }
        }
    }

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

        myAddr = socket.getLocalAddress().getHostAddress();
        myPort = socket.getLocalPort();

        new Thread(this::readLoop).start();
        new Thread(this::writeLoop).start();

        // set addr and port fields to uneditable
        tf_addr.setEditable(false);
        tf_port.setEditable(false);
    }

    public void closeConnections(boolean sendMessage) throws IOException {

        if (socket == null || socket.isClosed() || out == null || in == null) {
            log.println("Not connected to router");
            return;
        }

        if(sendMessage) {
            //send close message
            Data close = new Data(
                Data.Type.CLOSE,
                myAddr, myPort, routerAddr, routerPort,
                null
            );

            outBuffer.add(close);
        }
        
        out.close();
        in.close();
        socket.close();

        log.println("Disconnected from router");

        // set addr and port fields to editable
        tf_addr.setEditable(true);
        tf_port.setEditable(true);
    }

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

        updateStatus();
        log.println("Connected to " + routerAddr + ":" + routerPort);
    }

    @FXML
    public void disconnectButtonClicked() {
        try {
            closeConnections(true);
        } catch (IOException e) {
            lb_conn_status.setText("Error disconnecting from router");
            log.println("Error disconnecting from router");
            return;
        }
        updateStatus();
    }

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
            myAddr, myPort, routerAddr, routerPort,
            req
        );

        outBuffer.add(data);
        log.println("Request sent to router");

        state = STATE.WAITING_FOR_CONFIRMATION;
        updateStatus();
    }
}