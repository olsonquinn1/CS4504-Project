package com.project.shared;

import java.util.Random;

public class MatrixUtil {

    //generates a square matrix of size n with random values 0-99
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

    //returns true if two matrices are equal
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

    //returns a new matrix that is the result of multiplying two matrices
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

    //stores the result of multiplying two matrices in a third matrix
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

    //returns a new matrix that is the result of adding two matrices
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

    //stores the result of adding two matrices in a third matrix
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

    //returns a new matrix that is the result of subtracting two matrices
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

    //stores the result of subtracting two matrices in a third matrix
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

    //joins a smaller matrix A into a larger matrix C at a specified position
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

    //splits a larger matrix C into a smaller matrix A at a specified position
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

    //divides a matrix into four quadrants, storing each in a separate matrix
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