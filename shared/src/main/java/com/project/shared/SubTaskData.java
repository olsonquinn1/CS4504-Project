package com.project.shared;

import java.io.Serializable;

public class SubTaskData implements Serializable {
    
    private final int[][] matrixA;
    private final int[][] matrixB;
    private final int m;
    private final int taskId;
    private int coresToUse;

    public SubTaskData(int[][][] matrices, int m, int taskId) {
        this.matrixA = matrices[0];
        this.matrixB = matrices[1];
        this.m = m;
        this.taskId = taskId;
    }

    public SubTaskData(int[][] matrixA, int[][] matrixB, int m, int taskId) {
        this.matrixA = matrixA;
        this.matrixB = matrixB;
        this.m = m;
        this.taskId = taskId;
    }

    public int[][] getMatrixA() {
        return matrixA;
    }

    public int[][] getMatrixB() {
        return matrixB;
    }

    public int getM() {
        return m;
    }

    public int getTaskId() {
        return taskId;
    }

    public int getCoresToUse() {
        return coresToUse;
    }

    public void setCoresToUse(int coresToUse) {
        this.coresToUse = coresToUse;
    }
}
