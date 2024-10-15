package com.project.shared;

import static com.project.shared.MatrixUtil.generateSquareMatrix;

public class Test {
    
    public static void main(String[] args) {

        int n = 4096;

        StrassenExecutor se = new StrassenExecutor(
            Runtime.getRuntime().availableProcessors(),
            64,
            n/2
        );

        int[][] A = generateSquareMatrix(n);
        int[][] B = generateSquareMatrix(n);

        System.out.println("Matrix size: " + n + "x" + n);
        long startTime = System.currentTimeMillis();
        try {
            se.run(A, B);
        } catch (Exception e) {
            e.printStackTrace();
        }
        long parTime = System.currentTimeMillis() - startTime;
        System.out.println(parTime + "ms");
    }
}