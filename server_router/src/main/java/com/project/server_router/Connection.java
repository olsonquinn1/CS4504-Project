package com.project.server_router;

import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.project.shared.Data;

public class Connection {
   
    private final Socket socket;
    private final boolean isServer;

    private final String addr;
    private final int port;
    public int logicalCores;
    public double speedRating;

    private boolean inUse = false;
    private int taskId = -1;
    private int allocatedCores = 0;

    private final RouterThread myThread;

    public BlockingQueue<Data> dataQueue;
 
    Connection(Socket socket, boolean isServer, RouterThread myThread) {
        this.myThread = myThread;
        logicalCores = 1;
        this.isServer = isServer;
        this.socket = socket;
        dataQueue = new LinkedBlockingQueue<Data>();

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

    public boolean isServer() {
        return isServer;
    }

    public boolean isInUse() {
        return inUse;
    }

    public void setInUse(boolean inUse) {
        this.inUse = inUse;
    }

    public int getTaskId() {
        return taskId;
    }

    public void setTaskId(int taskId) {
        this.taskId = taskId;
    }

    public int getAllocatedCores() {
        return allocatedCores;
    }

    public void setAllocatedCores(int allocatedCores) {
        this.allocatedCores = allocatedCores;
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