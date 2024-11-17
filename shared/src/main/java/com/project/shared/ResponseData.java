package com.project.shared;

import java.io.Serializable;

/**
 * Data class for sending response data to the client
 * 
 * Response data contains a message, a success flag and the task id
 */
public class ResponseData implements Serializable {

    private final String message;
    private final int taskId;
    private final boolean success;

    /**
     * Constructs a new ResponseData object with the specified message, success status, and task ID.
     *
     * @param message The message associated with the response.
     * @param success The success status of the response.
     * @param taskId  The ID of the task associated with the response.
     */
    public ResponseData(String message, boolean success, int taskId) {
        this.message = message;
        this.success = success;
        this.taskId = taskId;
    }

    /**
     * Returns the message associated with the response.
     *
     * @return The message associated with the response.
     */
    public String getMessage() {
        return message;
    }

    /**
     * Returns the success status of the response.
     *
     * @return The success status of the response.
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Returns the ID of the task associated with the response.
     *
     * @return The ID of the task associated with the response.
     */
    public int getTaskId() {
        return taskId;
    }
}
