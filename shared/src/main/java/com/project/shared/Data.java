package com.project.shared;

import java.io.Serializable;

public class Data implements Serializable {
    
    public enum Type {
        REQUEST,
        RESPONSE,
        CLOSE,
        TASK_DATA,
        PROFILING_DATA,
        RESULT_DATA
    }

    private final Type type;

    private final String destAddr;
    private final int destPort;

    private final String senderAddr;
    private final int senderPort;

    private final Serializable payload;

    public Data(Type type, String destAddr, int destPort, String senderAddr, int senderPort, Serializable payload) {
        this.type = type;
        this.destAddr = destAddr;
        this.destPort = destPort;
        this.senderAddr = senderAddr;
        this.senderPort = senderPort;
        this.payload = payload;
    }

    public Type getType() {
        return type;
    }

    public String getDestAddr() {
        return destAddr;
    }

    public int getDestPort() {
        return destPort;
    }

    public String getSenderAddr() {
        return senderAddr;
    }

    public int getSenderPort() {
        return senderPort;
    }

    public Serializable getData() {
        return payload;
    }
}
