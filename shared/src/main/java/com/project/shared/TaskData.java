package com.project.shared;

import java.io.Serializable;

public class TaskData implements Serializable {

    private final int[][] matrixA;
    private final int[][] matrixB;
    private final int taskId;
    private final int threadsToUse;

    public TaskData(int[][] matrixA, int[][] matrixB, int taskId, int threadsToUse) {
        this.matrixA = matrixA;
        this.matrixB = matrixB;
        this.taskId = taskId;
        this.threadsToUse = threadsToUse;
    }

    public int[][] getMatrixA() {
        return matrixA;
    }

    public int[][] getMatrixB() {
        return matrixB;
    }

    public int getTaskId() {
        return taskId;
    }

    public int getThreadsToUse() {
        return threadsToUse;
    }
}
