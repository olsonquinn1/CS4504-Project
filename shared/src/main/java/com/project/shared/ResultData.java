package com.project.shared;

import java.io.Serializable;

/**
 * Represents the result data of a computation task.
 */
public class ResultData implements Serializable {

    private final int[][] resultMatrix;
    private final int m;
    private final int taskId;

    /**
     * Constructs a new instance of ResultData.
     *
     * @param resultMatrix The result matrix of the computation task.
     * @param m            The value of m used in the computation task.
     * @param taskId       The ID of the computation task.
     */
    public ResultData(int[][] resultMatrix, int m, int taskId) {
        this.resultMatrix = resultMatrix;
        this.m = m;
        this.taskId = taskId;
    }

    /**
     * Gets the result matrix of the computation task.
     *
     * @return The result matrix.
     */
    public int[][] getResultMatrix() {
        return resultMatrix;
    }

    /**
     * Gets the value of m used in the computation task.
     *
     * @return The value of m.
     */
    public int getM() {
        return m;
    }

    /**
     * Gets the ID of the computation task.
     *
     * @return The task ID.
     */
    public int getTaskId() {
        return taskId;
    }
}
