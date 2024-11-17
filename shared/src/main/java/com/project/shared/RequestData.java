package com.project.shared;

import java.io.Serializable;

/**
 * Data class for sending request data to the server
 * 
 * Server reserves the required resources and sends back a response with the task id or an error message
 */
public class RequestData implements Serializable {

    private final int threadCount;

    /**
     * Constructs a new RequestData object with the specified thread count.
     *
     * @param threadCount the number of threads to be used for the operation
     */
    public RequestData(int threadCount) {
        this.threadCount = threadCount;
    }

    /**
     * Returns the number of threads specified for the operation.
     *
     * @return the thread count
     */
    public int getThreadCount() {
        return threadCount;
    }
}
