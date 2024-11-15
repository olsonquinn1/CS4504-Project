package com.project.shared;

import java.io.Serializable;

/**
 * Data class for sending request data to the server
 * 
 * Server reserves the required resources and sends back a response with the task id or an error message
 */
public class RequestData implements Serializable {

    private final int threadCount;

    public RequestData(int threadCount) {
        this.threadCount = threadCount;
    }

    public int getThreadCount() {
        return threadCount;
    }
}
