package com.project.server_router;

import java.io.IOException;

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
import javafx.stage.Stage;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.Label;

public class RouterApp extends Application {

   private final int listenPort = 5555;

   private ServerSocket listenSocket = null;

   private final List<Connection> routingTable = Collections.synchronizedList(new ArrayList<Connection>());;

   private ListenThread connectionListener = null;

   private Stage primaryStage;

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

      try {
         listenSocket = new ServerSocket(listenPort);
         writeToConsole("Server Socket created on port: " + listenPort);
      } catch (IOException e) {
         System.err.println("Could not listen on port: " + listenPort + "\n" + e.getMessage());
         e.printStackTrace();
         System.exit(1);
      }

      startListener();
   }

   // sets up the listener thread
   private void startListener() {

      try {

         if (connectionListener != null)
            connectionListener.interrupt();

         connectionListener = new ListenThread(listenSocket, routingTable, this);
         connectionListener.start();
         System.out.println("ServerRouter is listening for clients on port: " + listenPort);
      } catch (IOException e) {
         System.err.println("Could not listen on port: " + listenPort + "\n" + e.getMessage());
         e.printStackTrace();
         System.exit(1);
      }

      writeToConsole("Listener started on port: " + listenPort);
      lb_conn_status.setText("Listening on port: " + listenPort);
   }

   public synchronized void updateConnectionLists() {

      ObservableList<String> servers =
         FXCollections.observableArrayList(
            routingTable.stream()
            .filter(conn -> conn.isServer())
            .map(conn -> conn.getAddr() + ":" + conn.getPort() + "(" + conn.getLogicalCores() + ")")
            .collect(Collectors.toList())
         );

      ObservableList<String> clients =
         FXCollections.observableArrayList(
            routingTable.stream()
            .filter(conn -> !conn.isServer())
            .map(conn -> conn.getAddr() + ":" + conn.getPort())
            .collect(Collectors.toList())
         );

      lv_servers.setItems(servers);
      lv_clients.setItems(clients);

      lv_servers.refresh();
      lv_clients.refresh();
   }

   public synchronized void writeToConsole(String message) {
      ta_log.setText(ta_log.getText() + "\n" + message);
   }

   private void closeConnections() {
      try {
         if(listenSocket != null && !listenSocket.isClosed())
            listenSocket.close();
      } catch (IOException e) {
         System.err.println("Error closing server and client sockets.");
         e.printStackTrace();
         System.exit(1);
      }
   }
}