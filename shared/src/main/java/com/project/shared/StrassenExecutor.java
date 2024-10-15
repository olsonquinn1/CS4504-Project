package com.project.shared;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static com.project.shared.MatrixUtil.add;
import static com.project.shared.MatrixUtil.divideIntoQuadrants;
import static com.project.shared.MatrixUtil.join;
import static com.project.shared.MatrixUtil.matrixMult;
import static com.project.shared.MatrixUtil.sub;

public class StrassenExecutor {

    private ExecutorService executor;
    private int strassenThreshold;
    private int threadThreshold;
    private int nThreads;
    
    private AtomicInteger multCounter = new AtomicInteger(0);
    private AtomicLong timeMultiplying = new AtomicLong(0);
    private AtomicLong timeCalculatingC = new AtomicLong(0);
    private AtomicLong timeJoining = new AtomicLong(0);
    private AtomicLong timeDividing = new AtomicLong(0);

    public StrassenExecutor(int nThreads, int strassenThreshold, int threadThreshold) {
        this.strassenThreshold = strassenThreshold;
        this.threadThreshold = threadThreshold;
        this.nThreads = nThreads;
    }

    public int[][] run(int[][] A, int[][] B) throws InterruptedException, ExecutionException, IllegalArgumentException {
        
        this.executor = Executors.newFixedThreadPool(nThreads);

        int matrixSize = A.length;

        if(A.length != A[0].length || B.length != B[0].length) {
            throw new IllegalArgumentException("Matrix dimensions must be square");
        }

        if(A.length != B.length) {
            throw new IllegalArgumentException("Matrix dimensions must be equal");
        }

        if((matrixSize & (matrixSize - 1)) != 0) {
            throw new IllegalArgumentException("Matrix dimensions must be a power of 2");
        }

        int[][] res = strassen(A, B);

        System.out.println("Multiplications: " + multCounter.get());
        System.out.println("Time multiplying: " + timeMultiplying.get() + "ms");
        System.out.println("Time calculating C: " + timeCalculatingC.get() + "ms");
        System.out.println("Time joining: " + timeJoining.get() + "ms");
        System.out.println("Time dividing: " + timeDividing.get() + "ms");

        executor.shutdown();

        return res;
    }

    public int[][] strassen(int[][] A, int[][] B) throws InterruptedException, ExecutionException {
        
        int matrixSize = A.length;
        boolean makeNewTasks = true;
        int half = matrixSize / 2;
        long t = 0;

        if (matrixSize <= strassenThreshold) {
            multCounter.incrementAndGet();
            t = System.currentTimeMillis();
            int[][] res = matrixMult(A, B);
            timeMultiplying.addAndGet(System.currentTimeMillis() - t);
            return res;
        }

        if (matrixSize <= threadThreshold) {
            makeNewTasks = false;
        }

        int[][] A11 = new int[half][half];
        int[][] A12 = new int[half][half];
        int[][] A21 = new int[half][half];
        int[][] A22 = new int[half][half];

        int[][] B11 = new int[half][half];
        int[][] B12 = new int[half][half];
        int[][] B21 = new int[half][half];
        int[][] B22 = new int[half][half];

        t = System.currentTimeMillis();
        divideIntoQuadrants(A, A11, A12, A21, A22);
        divideIntoQuadrants(B, B11, B12, B21, B22);
        timeDividing.addAndGet(System.currentTimeMillis() - t);

        int[][] M1 = null;
        int[][] M2 = null;
        int[][] M3 = null;
        int[][] M4 = null;
        int[][] M5 = null;
        int[][] M6 = null;
        int[][] M7 = null;

        if(makeNewTasks) {

            Future<int[][]> M1f = executor.submit(() -> strassen( // M1 = (A11 + A22)(B11 + B22)
                    add(A11, A22), add(B11, B22)));

            Future<int[][]> M2f = executor.submit(() -> strassen( // M2 = (A21 + A22)B11
                    add(A21, A22), B11));

            Future<int[][]> M3f = executor.submit(() -> strassen( // M3 = A11(B12 - B22)
                    A11, sub(B12, B22)));

            Future<int[][]> M4f = executor.submit(() -> strassen( // M4 = A22(B21 - B11)
                    A22, sub(B21, B11)));

            Future<int[][]> M5f = executor.submit(() -> strassen( // M5 = (A11 + A12)B22
                    add(A11, A12), B22));

            Future<int[][]> M6f = executor.submit(() -> strassen( // M6 = (A21 - A11)(B11 + B12)
                    sub(A21, A11), add(B11, B12)));

            Future<int[][]> M7f = executor.submit(() -> strassen( // M7 = (A12 - A22)(B21 + B22)
                    sub(A12, A22), add(B21, B22)));

            M1 = M1f.get();
            M2 = M2f.get();
            M3 = M3f.get();
            M4 = M4f.get();
            M5 = M5f.get();
            M6 = M6f.get();
            M7 = M7f.get();

        } else {
            M1 = strassen( // M1 = (A11 + A22)(B11 + B22)
                    add(A11, A22), add(B11, B22));
            M2 = strassen( // M2 = (A21 + A22)B11
                    add(A21, A22), B11);
            M3 = strassen( // M3 = A11(B12 - B22)
                    A11, sub(B12, B22));
            M4 = strassen( // M4 = A22(B21 - B11)
                    A22, sub(B21, B11));
            M5 = strassen( // M5 = (A11 + A12)B22
                    add(A11, A12), B22);
            M6 = strassen( // M6 = (A21 - A11)(B11 + B12)
                    sub(A21, A11), add(B11, B12));
            M7 = strassen( // M7 = (A12 - A22)(B21 + B22)
                    sub(A12, A22), add(B21, B22));
        }

        int[][] C11 = new int[half][half];
        int[][] C12 = new int[half][half];
        int[][] C21 = new int[half][half];
        int[][] C22 = new int[half][half];

        int[][] C = new int[matrixSize][matrixSize];

        t = System.currentTimeMillis();
        add(sub(add(M1, M4), M5), M7, C11); // C11 = M1 + M4 - M5 + M7
        add(M3, M5, C12); // C12 = M3 + M5
        add(M2, M4, C21); // C21 = M2 + M4
        add(sub(add(M1, M3), M2), M6, C22); // C22 = M1 - M2 + M3 + M6
        timeCalculatingC.addAndGet(System.currentTimeMillis() - t);

        t = System.currentTimeMillis();
        join(C, C11, 0, 0);
        join(C, C12, 0, half);
        join(C, C21, half, 0);
        join(C, C22, half, half);
        timeJoining.addAndGet(System.currentTimeMillis() - t);

        return C;
    }

    //returns the amount of multiplications for a given matrix size and strassen threshold
    // 7 ^ log2(matrixSize / strassenThreshold)
    public static int multCount(int matrixSize, int strassenThreshold) {
        int ratio = matrixSize / strassenThreshold;
        int log = 0;
        while (ratio > 1) {
            ratio  = ratio >> 1;
            log++;
        }
        return (int) Math.pow(7, log);
    }
}