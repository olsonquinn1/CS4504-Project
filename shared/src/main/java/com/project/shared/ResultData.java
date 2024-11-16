package com.project.shared;

import java.io.Serializable;


public class ResultData implements Serializable {

    private final int[][] resultMatrix;
    private final int m;
    private final int taskId;

    public ResultData(int[][] resultMatrix, int m, int taskId) {
        this.resultMatrix = resultMatrix;
        this.m = m;
        this.taskId = taskId;
    }

    public int[][] getResultMatrix() {
        return resultMatrix;
    }

    public int getM() {
        return m;
    }

    public int getTaskId() {
        return taskId;
    }
}
