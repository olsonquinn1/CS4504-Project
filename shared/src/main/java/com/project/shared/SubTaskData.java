package com.project.shared;

import java.io.Serializable;
import java.util.List;

public class SubTaskData implements Serializable {
    
    private final int[][][] subMatrices;
    private final List<Integer> mVals;

    public SubTaskData(List<Integer> mVals) {
        this.subMatrices = new int[8][][];
        this.mVals = mVals;
    }

    public void setSubmatrix(int index, int[][] subMatrix) {
        this.subMatrices[index] = subMatrix;
    }

    public List<Integer> getMVals() {
        return mVals;
    }

    public int[][][] getSubMatrices() {
        return subMatrices;
    }
}
