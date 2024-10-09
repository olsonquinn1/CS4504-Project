package com.project.shared;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.project.shared.MatrixUtil.add;
import static com.project.shared.MatrixUtil.divideIntoQuadrants;
import static com.project.shared.MatrixUtil.join;
import static com.project.shared.MatrixUtil.matrixMult;
import static com.project.shared.MatrixUtil.sub;

public class StrassenExecutor {

    private ProgressBar pb;

    private ExecutorService executor;
    private int strassenThreshold;
    private int threadThreshold;
    private int nThreads;

    public StrassenExecutor(int nThreads) {
        this.nThreads = nThreads;
    }

    public int[][] run(int[][] A, int[][] B, boolean serial, int strassenThreshold) throws InterruptedException, ExecutionException, IllegalArgumentException {
        
        int matrixSize = A.length;

        int multCount = (int)Math.pow(7, matrixSize / strassenThreshold);

        pb = new ProgressBar(multCount, 150);

        if(A.length != B.length || A[0].length != B[0].length) {
            throw new IllegalArgumentException("Matrices must be of the same size");
        }

        if(matrixSize % 2 != 0) {
            throw new IllegalArgumentException("Matrix size must be a power of 2");
        }

        if(matrixSize % strassenThreshold != 0) {
            throw new IllegalArgumentException("Matrix size must be divisible by the Strassen threshold");
        }

        if(serial || nThreads == 1) {
            pb.reset();
            pb.start();
            return strassen_serial(A, B);
        } else {
            executor = Executors.newFixedThreadPool(nThreads);
            threadThreshold = matrixSize / 2;
            pb.reset();
            pb.start();
            return strassen_parallel(A, B);
        }
    }

    public void shutdown() {
        executor.shutdown();
    }

    private int[][] strassen_parallel(int[][] A, int[][] B) throws InterruptedException, ExecutionException {
        int n = A.length;

        if (n <= strassenThreshold) {
            pb.progress(1);
            return matrixMult(A, B);
        }

        if (n <= threadThreshold) {
            return strassen_serial(A, B);
        }

        int halfSize = n / 2;

        int[][] A11 = new int[halfSize][halfSize];
        int[][] A12 = new int[halfSize][halfSize];
        int[][] A21 = new int[halfSize][halfSize];
        int[][] A22 = new int[halfSize][halfSize];

        int[][] B11 = new int[halfSize][halfSize];
        int[][] B12 = new int[halfSize][halfSize];
        int[][] B21 = new int[halfSize][halfSize];
        int[][] B22 = new int[halfSize][halfSize];

        if(n < 16384) { //making threads for this is faster when n >= 16384
            divideIntoQuadrants(A, A11, A12, A21, A22);
            divideIntoQuadrants(B, B11, B12, B21, B22);
        }
        else {
            List<Callable<Void>> divideTasks = new ArrayList<>();
            divideTasks.add(() -> {
                divideIntoQuadrants(A, A11, A12, A21, A22);
                return null;
            });
            divideTasks.add(() -> {
                divideIntoQuadrants(B, B11, B12, B21, B22);
                return null;
            });
            executor.invokeAll(divideTasks);
        }

        Future<int[][]> M1f = executor.submit(() -> strassen_parallel(
                add(A11, A22), add(B11, B22)));     // M1 = (A11 + A22)(B11 + B22)
        Future<int[][]> M2f = executor.submit(() -> strassen_parallel(
                add(A21, A22), B11));               // M2 = (A21 + A22)B11
        Future<int[][]> M3f = executor.submit(() -> strassen_parallel(
                A11, sub(B12, B22)));               // M3 = A11(B12 - B22)
        Future<int[][]> M4f = executor.submit(() -> strassen_parallel(
                A22, sub(B21, B11)));               // M4 = A22(B21 - B11)
        Future<int[][]> M5f = executor.submit(() -> strassen_parallel(
                add(A11, A12), B22));               // M5 = (A11 + A12)B22
        Future<int[][]> M6f = executor.submit(() -> strassen_parallel( 
                sub(A21, A11), add(B11, B12)));     // M6 = (A21 - A11)(B11 + B12)
        Future<int[][]> M7f = executor.submit(() -> strassen_parallel(
                sub(A12, A22), add(B21, B22)));     // M7 = (A12 - A22)(B21 + B22)

        int[][] M1 = M1f.get();
        int[][] M2 = M2f.get();
        int[][] M3 = M3f.get();
        int[][] M4 = M4f.get();
        int[][] M5 = M5f.get();
        int[][] M6 = M6f.get();
        int[][] M7 = M7f.get();

        int[][] C11;
        int[][] C12;
        int[][] C21;
        int[][] C22;

        int[][] C = new int[n][n];
        int newSize = n / 2;

        if(n < 8192) {
            C11 = add(sub(add(M1, M4), M5), M7); // C11 = M1 + M4 - M5 + M7
            C12 = add(M3, M5); // C12 = M3 + M5
            C21 = add(M2, M4); // C21 = M2 + M4
            C22 = add(sub(add(M1, M3), M2), M6); // C22 = M1 - M2 + M3 + M6

            join(C, C11, 0, 0);
            join(C, C12, 0, newSize);
            join(C, C21, newSize, 0);
            join(C, C22, newSize, newSize);
        }
        else {
            Future<int[][]> C11f = executor.submit(() -> add(sub(add(M1, M4), M5), M7));
            Future<int[][]> C12f = executor.submit(() -> add(M3, M5));
            Future<int[][]> C21f = executor.submit(() -> add(M2, M4));
            Future<int[][]> C22f = executor.submit(() -> add(sub(add(M1, M3), M2), M6));

            C11 = C11f.get();
            C12 = C12f.get();
            C21 = C21f.get();
            C22 = C22f.get();

            List<Callable<Void>> joinTasks = new ArrayList<>();

            joinTasks.add(() -> {
                join(C, C11, 0, 0);
                return null;
            });
            joinTasks.add(() -> {
                join(C, C12, 0, newSize);
                return null;
            });
            joinTasks.add(() -> {
                join(C, C21, newSize, 0);
                return null;
            });
            joinTasks.add(() -> {
                join(C, C22, newSize, newSize);
                return null;
            });

            executor.invokeAll(joinTasks);
        }
        
        return C;
    }

    private int[][] strassen_serial(int[][] A, int[][] B) {

        int n = A.length;

        if (A.length != B.length) {
            throw new IllegalArgumentException("Matrices must be of the same size");
        }

        if (n % 2 != 0) {
            throw new IllegalArgumentException("Matrix size must be a power of 2");
        }

        if (n <= strassenThreshold) {
            pb.progress(1);
            return matrixMult(A, B);
        }

        int halfSize = n / 2;

        int[][] A11 = new int[halfSize][halfSize];
        int[][] A12 = new int[halfSize][halfSize];
        int[][] A21 = new int[halfSize][halfSize];
        int[][] A22 = new int[halfSize][halfSize];

        int[][] B11 = new int[halfSize][halfSize];
        int[][] B12 = new int[halfSize][halfSize];
        int[][] B21 = new int[halfSize][halfSize];
        int[][] B22 = new int[halfSize][halfSize];

        divideIntoQuadrants(A, A11, A12, A21, A22);
        divideIntoQuadrants(B, B11, B12, B21, B22);

        int[][] M1 = strassen_serial( // M1 = (A11 + A22)(B11 + B22)
                add(A11, A22), add(B11, B22));
        int[][] M2 = strassen_serial( // M2 = (A21 + A22)B11
                add(A21, A22), B11);
        int[][] M3 = strassen_serial( // M3 = A11(B12 - B22)
                A11, sub(B12, B22));
        int[][] M4 = strassen_serial( // M4 = A22(B21 - B11)
                A22, sub(B21, B11));
        int[][] M5 = strassen_serial( // M5 = (A11 + A12)B22
                add(A11, A12), B22);
        int[][] M6 = strassen_serial( // M6 = (A21 - A11)(B11 + B12)
                sub(A21, A11), add(B11, B12));
        int[][] M7 = strassen_serial( // M7 = (A12 - A22)(B21 + B22)
                sub(A12, A22), add(B21, B22));

        int[][] C11 = add(sub(add(M1, M4), M5), M7); // C11 = M1 + M4 - M5 + M7
        int[][] C12 = add(M3, M5); // C12 = M3 + M5
        int[][] C21 = add(M2, M4); // C21 = M2 + M4
        int[][] C22 = add(sub(add(M1, M3), M2), M6); // C22 = M1 - M2 + M3 + M6

        int[][] C = new int[n][n];
        int newSize = n / 2;

        join(C, C11, 0, 0);
        join(C, C12, 0, newSize);
        join(C, C21, newSize, 0);
        join(C, C22, newSize, newSize);

        return C;
    }
}