package com.project.shared;

import static com.project.shared.MatrixUtil.generateMatrix;

public class Test {
    
    public static void main(String[] args) {

        StrassenExecutor se = new StrassenExecutor(4);

        int n = 1024;

        int[][] A = generateMatrix(n);
        int[][] B = generateMatrix(n);

        long startTime = System.currentTimeMillis();
        try {
            se.run(A, B, false, 4);
        } catch (Exception e) {
            e.printStackTrace();
        }
        long parTime = System.currentTimeMillis() - startTime;

        startTime = System.currentTimeMillis();
        try {
            se.run(A, B, true, 4);
        } catch (Exception e) {
            e.printStackTrace();
        }
        long serTime = System.currentTimeMillis() - startTime;

        System.out.println("Parallel: " + parTime + "ms");
        System.out.println("Serial: " + serTime + "ms");

        se.shutdown();
    }
}