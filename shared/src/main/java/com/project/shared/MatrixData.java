package com.project.shared;

import java.io.Serializable;

public class MatrixData implements Serializable {
    private final int matrixId;
    private final double[][] matrixValues;

    public MatrixData(int matrixId, double[][] matrixValues) {
        this.matrixId = matrixId;
        this.matrixValues = matrixValues;
    }

    public int getMatrixId() {
        return matrixId;
    }

    public double[][] getMatrixValues() {
        return matrixValues;
    }
}
