package com.project.shared;

import java.io.Serializable;

public class TaskData implements Serializable {
    private final int taskId;
    private final MatrixData matrixA;
    private final MatrixData matrixB;
    private final boolean useStrassen;

    public TaskData(int taskId, MatrixData matrixA, MatrixData matrixB, boolean useStrassen) {
        this.taskId = taskId;
        this.matrixA = matrixA;
        this.matrixB = matrixB;
        this.useStrassen = useStrassen;
    }

    public int getTaskId() {
        return taskId;
    }

    public MatrixData getMatrixA() {
        return matrixA;
    }

    public MatrixData getMatrixB() {
        return matrixB;
    }

    public boolean isUseStrassen() {
        return useStrassen;
    }
}
