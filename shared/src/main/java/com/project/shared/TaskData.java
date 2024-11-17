package com.project.shared;

import java.io.Serializable;

/**
 * Data for a task in a matrix multiplication operation.
 */
public class TaskData implements Serializable {

    private final int[][] matrixA;
    private final int[][] matrixB;
    private final int taskId;
    private final int threadsToUse;

    /**
     * Constructs a new TaskData object with the specified matrices, task ID, and number of threads to use.
     *
     * @param matrixA     the first matrix
     * @param matrixB     the second matrix
     * @param taskId      the ID of the task
     * @param threadsToUse the number of threads to use for the task
     */
    public TaskData(int[][] matrixA, int[][] matrixB, int taskId, int threadsToUse) {
        this.matrixA = matrixA;
        this.matrixB = matrixB;
        this.taskId = taskId;
        this.threadsToUse = threadsToUse;
    }

    /**
     * Returns the first matrix.
     *
     * @return the first matrix
     */
    public int[][] getMatrixA() {
        return matrixA;
    }

    /**
     * Returns the second matrix.
     *
     * @return the second matrix
     */
    public int[][] getMatrixB() {
        return matrixB;
    }

    /**
     * Returns the ID of the task.
     *
     * @return the ID of the task
     */
    public int getTaskId() {
        return taskId;
    }

    /**
     * Returns the number of threads to use for the task.
     *
     * @return the number of threads to use for the task
     */
    public int getThreadsToUse() {
        return threadsToUse;
    }
}
