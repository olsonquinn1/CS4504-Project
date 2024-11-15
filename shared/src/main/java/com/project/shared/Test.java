package com.project.shared;

import static com.project.shared.MatrixUtil.generateSquareMatrix;

public class Test {
    
    public static void main(String[] args) {
        //args[0] = matrix size
        //args[1] = thread count
        //args[2] = threshold
        //args[3] = task count

        //validate args
        if (args.length != 4) {
            System.out.println("Usage: java Test <matrix size> <thread count> <threshold>");
            System.exit(1);
        }

        int n = Integer.parseInt(args[0]);
        int thread_count = Integer.parseInt(args[1]);
        int threshold = Integer.parseInt(args[2]);

        //int thread_count = Runtime.getRuntime().availableProcessors();

        StrassenExecutor se = new StrassenExecutor(
            thread_count,
            threshold
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