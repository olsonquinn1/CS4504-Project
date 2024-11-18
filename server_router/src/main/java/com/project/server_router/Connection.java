package com.project.server_router;

import java.net.Socket;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import com.project.shared.Data;

/**
 * Represents a connection between the router and a server/client.
 */
public class Connection {
   
    private final Socket socket;
    private final boolean isServer;

    private final String addr;
    private final int port;
    private int logicalCores;
    private double speedRating;

    //map task id to remaining subtasks (key is the task id, value is the number of subtasks remaining)
    private final Map<Integer, Integer> tasks;
    private final Map<Integer, Integer> taskCores;

    private final RouterThread myThread;

    public BlockingQueue<Data> dataQueue;

    private final int id;
 
    /**
     * Constructs a new Connection object.
     * 
     * @param socket The socket associated with the connection.
     * @param isServer Indicates whether the connection is from a server or a client.
     * @param myThread The RouterThread associated with the connection.
     * @param id The ID of the connection.
     */
    Connection(Socket socket, boolean isServer, RouterThread myThread, int id) {
        this.myThread = myThread;
        logicalCores = 1;
        this.isServer = isServer;
        this.socket = socket;
        dataQueue = new LinkedBlockingQueue<Data>();

        tasks = new ConcurrentHashMap<>();

        taskCores = new ConcurrentHashMap<>();

        this.id = id;

        addr = socket.getInetAddress().getHostAddress();
        port = socket.getPort();
    }

    /**
     * Closes the connection.
     */
    public void close() {
        try {
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets the RouterThread associated with the connection.
     * 
     * @return The RouterThread associated with the connection.
     */
    public RouterThread getThread() {
        return myThread;
    }

    /**
     * Gets the number of logical cores of the connection.
     * 
     * @return The number of logical cores.
     */
    public int getLogicalCores() {
        return logicalCores;
    }

    /**
     * Gets the socket associated with the connection.
     * 
     * @return The socket.
     */
    public Socket getSocket() {
        return socket;
    }

    /**
     * Gets the IP address of the connection.
     * 
     * @return The IP address.
     */
    public String getAddr() {
        return addr;
    }

    /**
     * Gets the port number of the connection.
     * 
     * @return The port number.
     */
    public int getPort() {
        return port;
    }

    /**
     * Gets the ID of the connection.
     * 
     * @return The ID.
     */
    public int getId() {
        return id;
    }

    /**
     * Checks if the connection is from a server.
     * 
     * @return true if the connection is from a server, false otherwise.
     */
    public boolean isServer() {
        return isServer;
    }

    /**
     * Checks if the connection has a specific task ID.
     * 
     * @param taskId The task ID to check.
     * @return true if the connection has the task ID, false otherwise.
     */
    public boolean hasTaskId(int taskId) {
        return tasks.containsKey(taskId);
    }

    /**
     * Adds a new task to the connection.
     * 
     * @param taskId The ID of the task to add.
     */
    public void addNewTask(int taskId, int coreCount) {
        tasks.put(taskId, 0);
        taskCores.put(taskId, coreCount);
    }

    /**
     * Removes a task from the connection.
     * 
     * @param taskId The ID of the task to remove.
     */
    public void removeTask(int taskId) {
        tasks.remove(taskId);
        taskCores.remove(taskId);
    }

    public int getTaskCores(int taskId) {
        return taskCores.get(taskId);
    }

    /**
     * Decrements the number of remaining subtasks for a specific task.
     * 
     * @param taskId The ID of the task.
     * @param count The number of subtasks to decrement.
     */
    public void decrementTask(int taskId, int count) {
        if (tasks.containsKey(taskId)) {
            int remaining = tasks.get(taskId) - count;
            tasks.put(taskId, remaining);
        }
    }

    /**
     * Increments the number of remaining subtasks for a specific task.
     * 
     * @param taskId The ID of the task.
     * @param count The number of subtasks to increment.
     */
    public void incrementTask(int taskId, int count) {
        if (tasks.containsKey(taskId)) {
            tasks.put(taskId, tasks.get(taskId) + count);
        }
    }

    /**
     * Gets the total number of tasks associated with the connection.
     * 
     * @return The total number of tasks.
     */
    public int getTotalTasks() {
        int total = 0;
        for (int i : tasks.values()) {
            total += i;
        }
        return total;
    }

    /**
     * Returns the number of remaining tasks for the given task ID.
     *
     * @param taskId the ID of the task
     * @return the number of remaining tasks for the given task ID
     */
    public int getTasksRemaining(int taskId) {
        return tasks.get(taskId);
    }

    /**
     * Gets a string representation of the tasks associated with the connection.
     * 
     * @return A string representation of the tasks.
     */
    public String getTasksString() {
        StringBuilder sb = new StringBuilder();
        for (int i : tasks.keySet()) {
            sb.append(i).append(": ").append(tasks.get(i)).append("-").append(getTaskCores(i)).append(", ");
        }
        return sb.toString();
    }

    /**
     * Gets the speed rating of the connection.
     * 
     * @return The speed rating.
     */
    public double getSpeedRating() {
        return speedRating;
    }

    /**
     * Sets the speed rating of the connection.
     * 
     * @param speedRating The speed rating to set.
     */
    public void setSpeedRating(double speedRating) {
        this.speedRating = speedRating;
    }

    /**
     * Sets the number of logical cores of the connection.
     * 
     * @param logicalCores The number of logical cores to set.
     */
    public void setLogicalCores(int logicalCores) {
        this.logicalCores = logicalCores;
    }

    /**
     * Gets the number of logical cores of the connection.
     * 
     * @param logicalCores The number of logical cores to get.
     */
    public void getLogicalCores(int logicalCores) {
        this.logicalCores = logicalCores;
    }

    /**
     * Checks if the connection is equal to another connection based on the address and port.
     * 
     * @param addr The address to compare.
     * @param port The port to compare.
     * @return true if the connections are equal, false otherwise.
     */
    public boolean equals(String addr, int port) {
        return socket.getInetAddress().getHostAddress().equals(addr) && socket.getPort() == port;
    }

    /**
     * Checks if the connection is equal to another object based on the address and port.
     * 
     * @param obj The object to compare.
     * @return true if the connections are equal, false otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Connection) {
            Socket s = ((Connection) obj).getSocket();
            return equals(s.getInetAddress().getHostAddress(), s.getPort());
        }
        return false;
    }

    /**
     * Generates a hash code for the connection based on the address and port.
     * 
     * @return The hash code.
     */
    @Override
    public int hashCode() {
        return 31 * socket.getInetAddress().hashCode() + socket.getPort();
    }
}
