package com.project.shared;

import java.io.Serializable;

public class TaskData implements Serializable {

    private final int[][] matrixA;
    private final int[][] matrixB;
    private final int matrixSize;

    public TaskData(int[][] matrixA, int[][] matrixB, int matrixSize) {
        this.matrixA = matrixA;
        this.matrixB = matrixB;
        this.matrixSize = matrixSize;
    }

    public int[][] getMatrixA() {
        return matrixA;
    }

    public int[][] getMatrixB() {
        return matrixB;
    }

    public int getMatrixSize() {
        return matrixSize;
    }
}
