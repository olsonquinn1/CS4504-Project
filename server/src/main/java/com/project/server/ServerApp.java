package com.project.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.net.UnknownHostException;

import static com.project.shared.MatrixUtil.generateSquareMatrix;
import static com.project.shared.MatrixUtil.matrixMult;
import com.project.shared.ProfilingData;
import com.project.shared.Data;
import com.project.shared.ProgressBar;

import javafx.application.Application;
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

   private int numCores = Runtime.getRuntime().availableProcessors();

   private double computeScore = 0;

   ProgressBar progressBar;

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

      setUpLogStream(ta_log, log);

      new Thread(() -> {
         //sleep thread for a few seconds to allow the window to load
         try {
            Thread.sleep(1000);
         } catch (InterruptedException e) {
            e.printStackTrace();
         }
         log.println("Profiling compute capability...");
         computeScore = profileComputeCapability();
         log.println("Compute capability: " + computeScore + "ms");
         log.println("Number of cores: " + numCores);
      }).start();
   }

   private void setUpLogStream(TextArea ta, PrintStream ps) {
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

      ps = new PrintStream(out, true);
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

      ProfilingData profileData = new ProfilingData(computeScore, numCores);

      Data send = new Data(
         Data.Type.PROFILE_DATA,
         routerAddr,
         routerPort,
         myAddr,
         myPort,
         profileData
      );
   }

   public void disconnectFromRouter() throws IOException {
      if (socket == null) {
         return;
      }

      socket.close();

      // set addr and port fields to editable
      tf_addr.setEditable(true);
      tf_port.setEditable(true);
   }

   //computes 50 matrix multiplications on two 512x512 matrices
   //returns the average time taken to compute the multiplication
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

      long sum = 0;
      for (long time : times) {
         sum += time;
      }

      return (double)sum / numTests;
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
         disconnectFromRouter();
      } catch (IOException e) {
         lb_conn_status.setText("Error disconnecting from router");
         log.println("Error disconnecting from router");
         return;
      }
      lb_conn_status.setText("Disconnected");
      log.println("Disconnected from router");
   }
}