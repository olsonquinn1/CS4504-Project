package com.project.shared;

import java.util.Random;

public class MatrixUtil {

    public static int[][] generateMatrix(int n) {
        Random rand = new Random();
        int[][] matrix = new int[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                matrix[i][j] = rand.nextInt(100);
            }
        }
        return matrix;
    }

    public static boolean matricesEqual(int[][] A, int[][] B) {
        int n = A.length;
        if (n != B.length) {
            return false;
        }
        for (int i = 0; i < n; i++) {
            if (A[i].length != B[i].length) {
                return false;
            }
            for (int j = 0; j < n; j++) {
                if (A[i][j] != B[i][j]) {
                    return false;
                }
            }
        }
        return true;
    }

    public static int[][] matrixMult(int[][] A, int[][] B) {
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

    public static int[][] add(int[][] A, int[][] B) {
        int n = A.length;
        int[][] C = new int[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                C[i][j] = A[i][j] + B[i][j];
            }
        }
        return C;
    }

    public static int[][] sub(int[][] A, int[][] B) {
        int n = A.length;
        int[][] C = new int[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                C[i][j] = A[i][j] - B[i][j];
            }
        }
        return C;
    }

    public static void join(int[][] C, int[][] A, int x, int y) {
        int n = A.length;

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                C[i + x][j + y] = A[i][j];
            }
        }
    }

    public static void split(int[][] C, int[][] A, int x, int y) {
        int n = A.length;

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                A[i][j] = C[i + x][j + y];
            }
        }
    }

    public static void divideIntoQuadrants(int[][] matrix, int[][] a11, int[][] a12, int[][] a21, int[][] a22) {
        int n = matrix.length;
        int half = n / 2;
        for (int i = 0; i < half; i++) {
            for (int j = 0; j < half; j++) {
                a11[i][j] = matrix[i][j];
                a12[i][j] = matrix[i][j + half];
                a21[i][j] = matrix[i + half][j];
                a22[i][j] = matrix[i + half][j + half];
            }
        }
    }
}