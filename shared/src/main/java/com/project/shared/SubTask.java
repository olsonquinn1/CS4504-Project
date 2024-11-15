package com.project.shared;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SubTask {

    private final List<Integer> Mvals;

    private final Map<String, int[][]> subMatrices;

    public SubTask(List<Integer> Mvals) {
        this.Mvals = Mvals;
        this.subMatrices = new HashMap<>();
    }

    public void addSubMatrix(String key, int[][] matrix) {
        subMatrices.put(key, matrix);
    }

    public List<Integer> getMvals() {
        return Mvals;
    }

    public Map<String, int[][]> getSubMatrices() {
        return subMatrices;
    }

}
