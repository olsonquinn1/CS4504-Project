package com.project.shared;

import static com.project.shared.MatrixUtil.generateSquareMatrix;

public class Test {
    
    public static void main(String[] args) {

        int n = 2048;

        int thread_count = Runtime.getRuntime().availableProcessors();

        StrassenExecutor se = new StrassenExecutor(
            1,
            64,
            n
        );

        int[][] A = generateSquareMatrix(n);
        int[][] B = generateSquareMatrix(n);

        System.out.println("Matrix size: " + n + "x" + n);

        try {
            se.run(A, B, System.out);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println(se.getRunningtime() + "ms");
    }
}