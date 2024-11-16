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

    public ResponseData(String message, boolean success, int taskId) {
        this.message = message;
        this.success = success;
        this.taskId = taskId;
    }

    public String getMessage() {
        return message;
    }

    public boolean isSuccess() {
        return success;
    }

    public int getTaskId() {
        return taskId;
    }
}
