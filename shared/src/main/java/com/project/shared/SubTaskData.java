package com.project.shared;

import java.io.Serializable;

/**
 * Data for a subtask in a matrix multiplication task.
 */
public class SubTaskData implements Serializable {
    
    private final int[][] matrixA;
    private final int[][] matrixB;
    private final int m;
    private final int taskId;
    private int coresToUse;

    /**
     * Constructs a SubTaskData object with the given matrices, size, and task ID.
     * 
     * @param matrices the matrices to be multiplied
     * @param m the size of the matrices
     * @param taskId the ID of the task
     */
    public SubTaskData(int[][][] matrices, int m, int taskId) {
        this.matrixA = matrices[0];
        this.matrixB = matrices[1];
        this.m = m;
        this.taskId = taskId;
    }

    /**
     * Constructs a SubTaskData object with the given matrices, size, and task ID.
     * 
     * @param matrixA the first matrix
     * @param matrixB the second matrix
     * @param m the size of the matrices
     * @param taskId the ID of the task
     */
    public SubTaskData(int[][] matrixA, int[][] matrixB, int m, int taskId) {
        this.matrixA = matrixA;
        this.matrixB = matrixB;
        this.m = m;
        this.taskId = taskId;
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
     * Returns the size of the matrices.
     * 
     * @return the size of the matrices
     */
    public int getM() {
        return m;
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
     * Returns the number of cores to use for the subtask.
     * 
     * @return the number of cores to use
     */
    public int getCoresToUse() {
        return coresToUse;
    }

    /**
     * Sets the number of cores to use for the subtask.
     * 
     * @param coresToUse the number of cores to use
     */
    public void setCoresToUse(int coresToUse) {
        this.coresToUse = coresToUse;
    }
}
