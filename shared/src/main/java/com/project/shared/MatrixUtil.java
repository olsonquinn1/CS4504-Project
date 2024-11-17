package com.project.shared;

import java.util.Random;

/**
 * The MatrixUtil class provides utility methods for working with matrices.
 */
public class MatrixUtil {

    /**
     * Generates a square matrix of size n, filled with random integers between 0 and 99 (inclusive).
     *
     * @param n The size of the square matrix to generate.
     * @return The generated square matrix.
     */
    public static int[][] generateSquareMatrix(int n) {
        Random rand = new Random();
        int[][] matrix = new int[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                matrix[i][j] = rand.nextInt(100);
            }
        }
        return matrix;
    }

    /**
     * Checks if two matrices are equal.
     *
     * @param A The first matrix.
     * @param B The second matrix.
     * @return true if the matrices are equal, false otherwise.
     */
    public static boolean matricesEqual(int[][] A, int[][] B) {
        int n_rows = A.length;
        int n_cols = A[0].length;

        if (n_rows != B.length || n_cols != B[0].length) {
            return false;
        }

        for (int i = 0; i < n_rows; i++) {
            if (A[i].length != B[i].length) {
                return false;
            }
            for (int j = 0; j < n_cols; j++) {
                if (A[i][j] != B[i][j]) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Multiplies two matrices and returns the result.
     * 
     * @param A the first matrix
     * @param B the second matrix
     * @return the result of multiplying the two matrices
     * @throws IllegalArgumentException if the dimensions of the matrices are invalid
     */
    public static int[][] matrixMult(int[][] A, int[][] B) {

        if(A.length != B[0].length || A[0].length != B.length) {
            throw new IllegalArgumentException("Invalid matrix dimensions");
        }

        int n = A.length;
        int[][] C = new int[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                C[i][j] = 0;
                for (int k = 0; k < n; k++) {
                    C[i][j] += A[i][k] * B[k][j];
                }
            }
        }
        return C;
    }

    /**
     * Performs matrix multiplication of two matrices and stores the result in a third matrix.
     * The dimensions of the matrices must be compatible for multiplication.
     *
     * @param A The first matrix.
     * @param B The second matrix.
     * @param C The matrix to store the result of the multiplication.
     * @throws IllegalArgumentException if the dimensions of the matrices are invalid for multiplication.
     */
    public static void matrixMult(int[][] A, int[][] B, int[][] C) {

        if(A.length != B[0].length || A[0].length != B.length) {
            throw new IllegalArgumentException("Invalid matrix dimensions");
        }

        int n = A.length;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                C[i][j] = 0;
                for (int k = 0; k < n; k++) {
                    C[i][j] += A[i][k] * B[k][j];
                }
            }
        }
    }

    /**
     * Adds two matrices element-wise and returns the result.
     * 
     * @param A the first matrix
     * @param B the second matrix
     * @return the sum of the two matrices
     * @throws IllegalArgumentException if the dimensions of the matrices are not equal
     */
    public static int[][] add(int[][] A, int[][] B) {

        if(A.length != B.length || A[0].length != B[0].length) {
            throw new IllegalArgumentException("Invalid matrix dimensions");
        }

        int n = A.length;
        int[][] C = new int[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                C[i][j] = A[i][j] + B[i][j];
            }
        }
        return C;
    }

    /**
     * Adds two matrices and stores the result in a third matrix.
     *
     * @param A The first matrix to be added.
     * @param B The second matrix to be added.
     * @param C The matrix to store the result of the addition.
     * @throws IllegalArgumentException if the dimensions of the input matrices are not compatible.
     */
    public static void add(int[][] A, int[][] B, int[][] C) {

        if(A.length != B.length || A[0].length != B[0].length) {
            throw new IllegalArgumentException("Invalid matrix dimensions");
        }

        int n = A.length;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                C[i][j] = A[i][j] + B[i][j];
            }
        }
    }

    /**
     * Subtracts two matrices and returns the result.
     *
     * @param A the first matrix
     * @param B the second matrix
     * @return the resulting matrix after subtracting B from A
     * @throws IllegalArgumentException if the dimensions of A and B are not the same
     */
    public static int[][] sub(int[][] A, int[][] B) {

        if(A.length != B.length || A[0].length != B[0].length) {
            throw new IllegalArgumentException("Invalid matrix dimensions");
        }

        int n = A.length;
        int[][] C = new int[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                C[i][j] = A[i][j] - B[i][j];
            }
        }
        return C;
    }

    /**
     * Subtracts two matrices and stores the result in a third matrix.
     * 
     * @param A the first matrix
     * @param B the second matrix
     * @param C the matrix to store the result
     * @throws IllegalArgumentException if the dimensions of A and B are not the same
     */
    public static void sub(int[][] A, int[][] B, int[][] C) {

        if(A.length != B.length || A[0].length != B[0].length) {
            throw new IllegalArgumentException("Invalid matrix dimensions");
        }

        int n = A.length;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                C[i][j] = A[i][j] - B[i][j];
            }
        }
    }

    /**
     * Joins a matrix A into a larger matrix C at the specified position (x, y).
     * The matrix A is copied into the matrix C, starting at the specified position.
     * 
     * @param C the larger matrix to join into
     * @param A the matrix to be joined
     * @param x the starting row position in matrix C
     * @param y the starting column position in matrix C
     * @throws IllegalArgumentException if the matrix A does not fit in the specified position in matrix C
     */
    public static void join(int[][] C, int[][] A, int x, int y) {
        int n_rows = A.length;
        int n_cols = A[0].length;

        if (x + n_rows > C.length || y + n_cols > C[0].length) {
            throw new IllegalArgumentException(
                "Matrix does not fit in specified position: "
                + "x: " + x + ", y: " + y + ", n_rows: " + n_rows + ", n_cols: " + n_cols
                + "\nC: " + C.length + "x" + C[0].length
            );
        }

        for (int i = 0; i < n_rows; i++) {
            for (int j = 0; j < n_cols; j++) {
                C[i + x][j + y] = A[i][j];
            }
        }
    }

    /**
     * Splits a matrix and copies its values into another matrix at the specified position.
     *
     * @param C the matrix to split and copy values from
     * @param A the matrix to copy values into
     * @param x the starting row index in the destination matrix
     * @param y the starting column index in the destination matrix
     * @throws IllegalArgumentException if the destination matrix does not have enough space to fit the source matrix
     */
    public static void split(int[][] C, int[][] A, int x, int y) {

        int n_rows = A.length;
        int n_cols = A[0].length;

        if (x + n_rows > C.length || y + n_cols > C[0].length) {
            throw new IllegalArgumentException(
                "Matrix does not fit in specified position: "
                + "x: " + x + ", y: " + y + ", n_rows: " + n_rows + ", n_cols: " + n_cols
                + "\nC: " + C.length + "x" + C[0].length
            );
        }

        for (int i = 0; i < n_rows; i++) {
            for (int j = 0; j < n_cols; j++) {
                A[i][j] = C[i + x][j + y];
            }
        }
    }

    /**
     * Divides a given matrix into four quadrants.
     *
     * @param A    The input matrix to be divided.
     * @param a11  The top-left quadrant of the input matrix.
     * @param a12  The top-right quadrant of the input matrix.
     * @param a21  The bottom-left quadrant of the input matrix.
     * @param a22  The bottom-right quadrant of the input matrix.
     * @throws IllegalArgumentException if the dimensions of the input matrix are not even.
     */
    public static void divideIntoQuadrants(int[][] A, int[][] a11, int[][] a12, int[][] a21, int[][] a22) {
        
        int n_rows = A.length;
        int n_cols = A[0].length;

        if(n_rows % 2 != 0 || n_cols % 2 != 0) {
            throw new IllegalArgumentException("Matrix dimensions must be even");
        }

        int half_row = n_rows / 2;
        int half_col = n_cols / 2;
        for (int i = 0; i < half_row; i++) {
            for (int j = 0; j < half_col; j++) {
                a11[i][j] = A[i][j];
                a12[i][j] = A[i][j + half_col];
                a21[i][j] = A[i + half_row][j];
                a22[i][j] = A[i + half_row][j + half_col];
            }
        }
    }
}