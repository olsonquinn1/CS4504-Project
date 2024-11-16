package com.project.server_router;

import java.net.Socket;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import com.project.shared.Data;

public class Connection {
   
    private final Socket socket;
    private final boolean isServer;

    private final String addr;
    private final int port;
    private int logicalCores;
    private double speedRating;

    //map task id to remaining subtasks (key is the task id, value is the number of subtasks remaining)
    private final Map<Integer, Integer> tasks;

    private final RouterThread myThread;

    public BlockingQueue<Data> dataQueue;

    private final int id;
 
    Connection(Socket socket, boolean isServer, RouterThread myThread, int id) {
        this.myThread = myThread;
        logicalCores = 1;
        this.isServer = isServer;
        this.socket = socket;
        dataQueue = new LinkedBlockingQueue<Data>();

        tasks = new ConcurrentHashMap<>();
        this.id = id;

        addr = socket.getInetAddress().getHostAddress();
        port = socket.getPort();
    }

    public void close() {
        try {
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public RouterThread getThread() {
        return myThread;
    }

    public int getLogicalCores() {
        return logicalCores;
    }

    public Socket getSocket() {
        return socket;
    }

    public String getAddr() {
        return addr;
    }

    public int getPort() {
        return port;
    }

    public int getId() {
        return id;
    }

    public boolean isServer() {
        return isServer;
    }

    public boolean hasTaskId(int taskId) {
        return tasks.containsKey(taskId);
    }

    public void addNewTask(int taskId) throws Exception {
        if (tasks.containsKey(taskId)) {
            throw new Exception("Task already exists");
        } else {
            tasks.put(taskId, 0);
        }
    }

    public void removeTask(int taskId) throws Exception {
        if(!tasks.containsKey(taskId)) {
            throw new Exception("Task does not exist");
        }
        tasks.remove(taskId);
    }

    public void decrementTask(int taskId, int count) throws Exception {
        if (tasks.containsKey(taskId)) {
            int remaining = tasks.get(taskId) - count;
            tasks.put(taskId, remaining);
        } else {
            throw new Exception("Task does not exist");
        }
    }

    public void incrementTask(int taskId, int count) throws Exception {
        if (tasks.containsKey(taskId)) {
            tasks.put(taskId, tasks.get(taskId) + count);
        } else {
            throw new Exception("Task does not exist");
        }
    }

    public int getTotalTasks() {
        int total = 0;
        for (int i : tasks.values()) {
            total += i;
        }
        return total;
    }

    public String getTasksString() {
        StringBuilder sb = new StringBuilder();
        for (int i : tasks.keySet()) {
            sb.append(i).append(": ").append(tasks.get(i)).append(", ");
        }
        return sb.toString();
    }

    public double getSpeedRating() {
        return speedRating;
    }

    public void setSpeedRating(double speedRating) {
        this.speedRating = speedRating;
    }

    public void setLogicalCores(int logicalCores) {
        this.logicalCores = logicalCores;
    }

    public void getLogicalCores(int logicalCores) {
        this.logicalCores = logicalCores;
    }

    public boolean equals(String addr, int port) {
        return socket.getInetAddress().getHostAddress().equals(addr) && socket.getPort() == port;
    }

    //index with the address and port
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Connection) {
            Socket s = ((Connection) obj).getSocket();
            return equals(s.getInetAddress().getHostAddress(), s.getPort());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 31 * socket.getInetAddress().hashCode() + socket.getPort();
    }
 }