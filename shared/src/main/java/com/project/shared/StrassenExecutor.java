package com.project.shared;

import java.io.PrintStream;
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

/**
 * A class to execute the Strassen algorithm on two matrices in parallel
 */
public class StrassenExecutor {

    private ExecutorService executor;
    private final int strassenThreshold;
    private final int nThreads;

    private ProgressBar progressBar;
    private boolean useProgressBar = false;

    private final Object activeTasksLock = new Object();
    private int activeTasks;

    private long runTime;

    /**
     * Create a new StrassenExecutor with the given number of threads, Strassen threshold, and thread threshold
     * 
     * @param nThreads The number of threads to use
     * @param strassenThreshold The Strassen threshold (below which the algorithm will use normal matrix multiplication)
     * @param maxTasks The maximum number of tasks to run in parallel
     */
    public StrassenExecutor(int nThreads, int strassenThreshold) {
        this.strassenThreshold = strassenThreshold;
        this.nThreads = nThreads;
        this.activeTasks = 0;
    }

    /**
     * Run the Strassen algorithm on the given matrices A and B, and print a progress bar to the given PrintStream
     * 
     * @param A The first matrix
     * @param B The second matrix
     * @param out The PrintStream to print the progress bar to
     * @return The resulting matrix
     * @throws InterruptedException 
     * @throws ExecutionException
     * @throws IllegalArgumentException if the matrix dimensions are not square or equal, or if the matrix dimensions are not a power of 2
     */
    public int[][] run(int[][] A, int[][] B, PrintStream out) throws InterruptedException, ExecutionException, IllegalArgumentException {
    
        useProgressBar = true;
        progressBar = new ProgressBar(multCount(A.length, strassenThreshold), 50, out);
        progressBar.start();

        return run(A, B);
    }

    /**
     * Run the Strassen algorithm on the given matrices A and B
     * 
     * @param A The first matrix
     * @param B The second matrix
     * @return The resulting matrix
     * @throws InterruptedException 
     * @throws ExecutionException
     * @throws IllegalArgumentException if the matrix dimensions are not square or equal, or if the matrix dimensions are not a power of 2
     */
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

        long startTime = System.currentTimeMillis();

        int[][] res = strassen(A, B);

        runTime = System.currentTimeMillis() - startTime;

        if(executor != null)
            executor.shutdown();
        
        if(useProgressBar) {
            progressBar.stop();
            useProgressBar = false;
        }
            
        
        return res;
    }

    /**
     * Run the Strassen algorithm on the given matrices A and B
     * 
     * @param A The first matrix
     * @param B The second matrix
     * @return The resulting matrix
     * @throws InterruptedException 
     * @throws ExecutionException
     */
    public int[][] strassen(int[][] A, int[][] B) throws InterruptedException, ExecutionException {
        
        int matrixSize = A.length;
        int half = matrixSize / 2;

        if (matrixSize <= strassenThreshold) {
            if(useProgressBar) {
                progressBar.progress(1);
            }
            int[][] res = matrixMult(A, B);
            return res;
        }

        int[][] A11 = new int[half][half];
        int[][] A12 = new int[half][half];
        int[][] A21 = new int[half][half];
        int[][] A22 = new int[half][half];

        int[][] B11 = new int[half][half];
        int[][] B12 = new int[half][half];
        int[][] B21 = new int[half][half];
        int[][] B22 = new int[half][half];

        //Divide matrices into quadrants
        divideIntoQuadrants(A, A11, A12, A21, A22);
        divideIntoQuadrants(B, B11, B12, B21, B22);

        int[][] M1 = null;
        int[][] M2 = null;
        int[][] M3 = null;
        int[][] M4 = null;
        int[][] M5 = null;
        int[][] M6 = null;
        int[][] M7 = null;

        //Generate the 7 new matrices
        Future<int[][]> M1f = createTaskIfNeeded(() -> strassen(add(A11, A22), add(B11, B22)));
        Future<int[][]> M2f = createTaskIfNeeded(() -> strassen(add(A21, A22), B11));
        Future<int[][]> M3f = createTaskIfNeeded(() -> strassen(A11, sub(B12, B22)));
        Future<int[][]> M4f = createTaskIfNeeded(() -> strassen(A22, sub(B21, B11)));
        Future<int[][]> M5f = createTaskIfNeeded(() -> strassen(add(A11, A12), B22));
        Future<int[][]> M6f = createTaskIfNeeded(() -> strassen(sub(A21, A11), add(B11, B12)));
        Future<int[][]> M7f = createTaskIfNeeded(() -> strassen(sub(A12, A22), add(B21, B22)));
    
        //Extract results and manage active task count
        try {
            M1 = extractResult(M1f, () -> strassen(add(A11, A22), add(B11, B22)));
            M2 = extractResult(M2f, () -> strassen(add(A21, A22), B11));
            M3 = extractResult(M3f, () -> strassen(A11, sub(B12, B22)));
            M4 = extractResult(M4f, () -> strassen(A22, sub(B21, B11)));
            M5 = extractResult(M5f, () -> strassen(add(A11, A12), B22));
            M6 = extractResult(M6f, () -> strassen(sub(A21, A11), add(B11, B12)));
            M7 = extractResult(M7f, () -> strassen(sub(A12, A22), add(B21, B22)));
        } catch (Exception e) {
            throw new ExecutionException(e);
        }

        //Calculate the 4 quadrants of the result matrix
        int[][] C11 = new int[half][half];
        int[][] C12 = new int[half][half];
        int[][] C21 = new int[half][half];
        int[][] C22 = new int[half][half];

        int[][] C = new int[matrixSize][matrixSize];

        add(sub(add(M1, M4), M5), M7, C11); // C11 = M1 + M4 - M5 + M7
        add(M3, M5, C12); // C12 = M3 + M5
        add(M2, M4, C21); // C21 = M2 + M4
        add(sub(add(M1, M3), M2), M6, C22); // C22 = M1 - M2 + M3 + M6

        //Join the 4 quadrants into the result matrix
        join(C, C11, 0, 0);
        join(C, C12, 0, half);
        join(C, C21, half, 0);
        join(C, C22, half, half);

        return C;
    }

    /**
     * Creates a task and submits it to the executor if there are not too many active tasks.
     * If there are already too many active tasks, the task will be run sequentially.
     *
     * @param task The task to be executed.
     * @return A Future representing the pending completion of the task, or null if the task cannot be submitted.
     */
    private Future<int[][]> createTaskIfNeeded(Callable<int[][]> task) {
        synchronized (activeTasksLock) {
            if (activeTasks < nThreads) {
                activeTasks++;
                return executor.submit(task);
            }
        }
        return null; // Run sequentially if too many active tasks
    }

    /**
     * Extracts the result from a Future or executes a sequential task if the Future is null.
     *
     * @param future          The Future representing the asynchronous computation.
     * @param sequentialTask  The Callable representing the sequential task to be executed.
     * @return The result of the asynchronous computation or the result of the sequential task.
     * @throws Exception if an error occurs during the execution of the sequential task.
     */
    private int[][] extractResult(Future<int[][]> future, Callable<int[][]> sequentialTask) throws Exception {
        if (future != null) {
            try {
                return future.get();
            } finally {
                synchronized (activeTasksLock) {
                    activeTasks--;
                }
            }
        } else {
            return sequentialTask.call();
        }
    }

    //submatrices = {a11, a12, a21, a22, b11, b12, b21, b22}
    public static int[][][] stras_M1(int[][][] submatrices) {
        int[][][] res = new int[2][][];

        res[0] = add(submatrices[0], submatrices[3]);
        res[1] = add(submatrices[4], submatrices[7]);

        return res;
    }

    public static int[][][] stras_M2(int[][][] submatrices) {
        int[][][] res = new int[2][][];

        res[0] = add(submatrices[2], submatrices[3]);
        res[1] = submatrices[4];

        return res;
    }

    public static int[][][] stras_M3(int[][][] submatrices) {
        int[][][] res = new int[2][][];

        res[0] = submatrices[0];
        res[1] = sub(submatrices[5], submatrices[7]);

        return res;
    }

    public static int[][][] stras_M4(int[][][] submatrices) {
        int[][][] res = new int[2][][];

        res[0] = submatrices[3];
        res[1] = add(submatrices[5], submatrices[7]);

        return res;
    }

    public static int[][][] stras_M5(int[][][] submatrices) {
        int[][][] res = new int[2][][];

        res[0] = add(submatrices[0], submatrices[1]);
        res[1] = submatrices[7];

        return res;
    }

    public static int[][][] stras_M6(int[][][] submatrices) {
        int[][][] res = new int[2][][];

        res[0] = sub(submatrices[2], submatrices[0]);
        res[1] = add(submatrices[4], submatrices[5]);

        return res;
    }

    public static int[][][] stras_M7(int[][][] submatrices) {
        int[][][] res = new int[2][][];

        res[0] = sub(submatrices[1], submatrices[3]);
        res[1] = add(submatrices[6], submatrices[7]);

        return res;
    }

    /**
     * Combine the 4 quadrants of the result matrix from the 7 M submatrices 
     * @param matrices The 7 M submatrices ([7][size][siez])
     * @return The resulting matrix ([2 * size][2 * size])
     */
    public static int[][] combineMatricesFromM(int[][][] matrices) {

        int size = matrices[0].length;
        int newSize = size * 2;

        //Calculate the 4 quadrants of the result matrix
        int[][] C12 = new int[size][size];
        int[][] C11 = new int[size][size];
        int[][] C21 = new int[size][size];
        int[][] C22 = new int[size][size];

        int[][] C = new int[newSize][newSize];

        add(sub(add(matrices[0], matrices[3]), matrices[4]), matrices[6], C11); // C11 = M1 + M4 - M5 + M7
        add(matrices[2], matrices[4], C12); // C12 = M3 + M5
        add(matrices[1], matrices[3], C21); // C21 = M2 + M4
        add(sub(add(matrices[0], matrices[2]), matrices[1]), matrices[5], C22); // C22 = M1 - M2 + M3 + M6

        //Join the 4 quadrants into the result matrix
        join(C, C11, 0, 0);
        join(C, C12, 0, size);
        join(C, C21, size, 0);
        join(C, C22, size, size);

        return C;
    }

    public long getRunningtime() {
        return runTime;
    }

    /**
     * Calculate the number of matrix multiplications for a given matrix size and Strassen threshold
     * 
     * Equal to 7 ^ log2(matrixSize / strassenThreshold)
     * 
     * @param matrixSize The size of the matrix
     * @param strassenThreshold The Strassen threshold (below which the algorithm will use normal matrix multiplication)
     * @return The number of matrix multiplications
     */
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