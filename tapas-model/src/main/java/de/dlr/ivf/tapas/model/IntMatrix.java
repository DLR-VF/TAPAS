package de.dlr.ivf.tapas.model;

public class IntMatrix {

    private final int[][] matrix;

    public IntMatrix(int rows, int cols) {
        matrix = new int[rows][cols];
    }

    public void put(int row, int col, int value) {
        matrix[row][col] = value;
    }

    public int[][] getMatrix(){
        return matrix;
    }
}
