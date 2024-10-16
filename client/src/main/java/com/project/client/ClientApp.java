package com.project.client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import com.project.shared.Data;

public class ClientApp extends Application {

    private Socket socket = null;

    private ObjectOutputStream out = null;
    private ObjectInputStream in = null;

    private String myAddr = null;
    private int myPort = -1;

    private String routerAddr = null;
    private int routerPort = -1;

    private Stage primaryStage;

    private BlockingQueue<Data> outBuffer = new LinkedBlockingQueue<Data>();

    private long lastPing = 0;

    @FXML
    private TextField tf_addr;
    @FXML
    private TextField tf_port;
    @FXML
    private Label lb_conn_status;
    @FXML
    private TextArea ta_log;

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
        tf_port.setText("5555");
    }

    protected void readLoop() {
        while (true) {
            Data recv = null;
            try {
                recv = (Data) in.readObject();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                break;
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }

            if(recv.getType() == Data.Type.CLOSE) {
                writeToConsole("Connection closed by server");
                try {
                    closeConnections();
                } catch (IOException e) {
                    writeToConsole("Error disconnecting from router");
                    e.printStackTrace();
                }
                break;
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
                e.printStackTrace();
                break;
            }
            catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }
    }

    public void writeToConsole(String message) {
        ta_log.setText(ta_log.getText() + "\n" + message);
    }

    public void connectToRouter() throws IOException, UnknownHostException, SocketTimeoutException {

        if (routerAddr == null || routerPort == -1) {
            throw new IOException("Router address and port not set");
        }

        if (socket != null) {
            socket.close();
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

    public void closeConnections() throws IOException {

        if (socket == null) {
            return;
        }

        out.close();
        in.close();
        socket.close();

        writeToConsole("Disconnected from router");

        // set addr and port fields to editable
        tf_addr.setEditable(true);
        tf_port.setEditable(true);
    }

    @FXML
    public void connectButtonClicked() {

        writeToConsole("Attempting to connect to " + routerAddr + ":" + routerPort);

        try {
            connectToRouter();
        }
        catch (UnknownHostException e) {
            lb_conn_status.setText("Host not found");
            writeToConsole(routerAddr + ":" + routerPort + " Host not found");
            return;
        }
        catch (SocketTimeoutException e) {
            lb_conn_status.setText("Connection timed out");
            writeToConsole(routerAddr + ":" + routerPort + " Connection timed out");
            return;
        }
        catch (IOException e) {
            lb_conn_status.setText("Error connecting to router: " + e.getMessage());
            writeToConsole(routerAddr + ":" + routerPort + " Error connecting to router: " + e.getMessage());
            return;
        }

        lb_conn_status.setText("Connected to router: " + routerAddr + ":" + routerPort);
        writeToConsole("Connected to " + routerAddr + ":" + routerPort);
    }

    @FXML
    public void disconnectButtonClicked() {
        try {
            closeConnections();
        } catch (IOException e) {
            lb_conn_status.setText("Error disconnecting from router");
            writeToConsole("Error disconnecting from router");
            return;
        }
        lb_conn_status.setText("Disconnected");
    }
}