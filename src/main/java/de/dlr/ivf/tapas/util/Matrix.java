package de.dlr.ivf.tapas.util;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;

/**
 * A matrix is a special value distribution. Here you can create line or column vectors, m*n (diagonal) matrices.
 * 
 * @author mark_ma
 * 
 */
public class Matrix {

	/**
	 * Types for matrix print
	 * 
	 * @author mark_ma
	 * 
	 */
	public enum MatrixPrint {
		/**
		 * Matrix is printed in one line
		 */
		LINE,
		/**
		 * Matrix is printed in its m*n shape
		 */
		MATRIX
	}

	/**
	 * Demo main
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		Matrix matrix = new Matrix(3, true, 1);
		System.out.println(matrix.toString() + "\n");

		matrix.setValue(1, 1, 1.11);
		System.out.println(matrix.toString() + "\n");

		matrix.setValue(2, 1, 2.22);
		System.out.println(matrix.toString() + "\n");

		matrix.setValue(1, 2, 3.33);
		System.out.println(matrix.toString() + "\n");

		System.out.println(matrix.getValue(1, 1) + "\n");

		System.out.println(matrix.toString(MatrixPrint.LINE) + "\n");
	}

	/**
	 * Flag if the matrix is a diagonal matrix
	 */
	@SuppressWarnings("unused")
	private boolean diagonal;

	/**
	 * Number of rows
	 */
	private int m;

	/**
	 * get number of rows 
	 */
	public int getNumberOfRows(){
		return m;
	}

	
	/**
	 * Number of columns
	 */
	private int n;

	/**
	 * get number of columns 
	 */
	public int getNumberOfColums(){
		return n;
	}

	/**
	 * get number of elements 
	 */
	public int getNumberOfElements(){
		return n*m;
	}
	
	/**
	 * Returns the average value of this matrix
	 * @param skipDiagonal flag if the diagonal values should be included or not
	 * @param skipNegative flag if negative values should be included or not
	 * @return the average value of all relevant values
	 */
	public double getAverageValue(boolean skipDiagonal, boolean skipNegative) {
		double average =0;
		int i,j, num =0;
		for(i=0; i< this.n; i++) {
			for(j=0; j<this.m; j++) {
				if(i==j && skipDiagonal)
					continue;
				if(this.vals[i][j]<0 && skipNegative)
					continue;
				num++;
				average += this.vals[i][j];
			}
		}
		if(num>0) {
			average /=num;
		}
		return average;
	}
	
	/**
	 * The start index of the matrix. Normally an Array starts a 0. Here you can define the start index, e.g. 1.
	 */
	private int sIndex;

	/**
	 * The matrix is stored in this value distribution.
	 */
	double[][] vals;

	/**
	 * Builds a n*n matrix.
	 * 
	 * @param n
	 *            number of rows/columns -> n*n
	 */
	public Matrix(int n) {
		this(n, n);
	}

	/**
	 * Builds a (diagonal) n*n matrix.
	 * 
	 * @param n
	 *            number of rows/columns -> n*n
	 * @param diagonal
	 *            flag if you want to create a diagonal matrix
	 */
	public Matrix(int n, boolean diagonal) {
		this(n, n, diagonal, 0);
	}

	/**
	 * Builds a (diagonal) n*n matrix starting at the given index.
	 * 
	 * @param n
	 *            number of rows/columns -> n*n
	 * @param diagonal
	 *            flag if you want to create a diagonal matrix
	 * @param startIndex
	 *            start index of the first element
	 */
	public Matrix(int n, boolean diagonal, int startIndex) {
		this(n, n, diagonal, startIndex);
	}

	/**
	 * Builds a m*n matrix.
	 * 
	 * @param m
	 *            number of rows
	 * @param n
	 *            number of columns
	 */
	public Matrix(int m, int n) {
		this(m, n, 0);
	}

	/**
	 * Builds a (diagonal) m*n matrix starting at the given index.
	 * 
	 * @param m
	 *            number of rows
	 * @param n
	 *            number of columns
	 * @param diagonal
	 *            flag if you want to create a diagonal matrix
	 * @param startIndex
	 *            start index of the first element
	 */
	private Matrix(int m, int n, boolean diagonal, int startIndex) {
		this.m = m;
		this.n = n;
		this.sIndex = startIndex;
		this.diagonal = diagonal;
		this.vals = new double[m][n];
		this.setValues(0);
	}

	/**
	 * Builds a m*n matrix starting at the given index.
	 * 
	 * @param m
	 *            number of rows
	 * @param n
	 *            number of columns
	 * @param startIndex
	 *            start index of the first element
	 */
	public Matrix(int m, int n, int startIndex) {
		this(m, n, false, startIndex);
	}


	/**
	 * Returns the value at the given position of the matrix (row / column)
	 * 
	 * @param row
	 *            row index
	 * @param column
	 *            column index
	 * 
	 * @return value at this position
	 */
	public double getValue(int row, int column) {
		return this.vals[row-sIndex][column-sIndex];
	}


	/**
	 * This method sets the value at position (row,column). It also uses the sIndex member to access arrays which start
	 * counting e.g. by 1;
	 * 
	 * @param row row index
	 * @param column column index
	 * @param value  new value at this position
	 */
	public void setValue(int row, int column, double value) {
		this.vals[row-sIndex][column-sIndex]=value;
	}

	/**
	 * This method sets the value at position index. It also uses the sIndex member to access arrays which start counting
	 * e.g. by 1;
	 * 
	 * @param index
	 *            index in matrix
	 * @param value
	 *            new value at this position
	 */
	public void setValue(int index, double value) {
		index-=sIndex;
		int row= index/this.n;
		int column= index%this.n;
		this.vals[row][column]=value;
	}

	/**
	 * This method sets the value at position index. It ignores the sIndex member to access arrays which start counting e.g.
	 * by 1;
	 * 
	 * @param index
	 *            index in matrix
	 * @param value
	 *            new value at this position
	 */
	public void setRawValue(int index, double value) {
		int row= index/this.n;
		int column= index%this.n;
		this.vals[row][column]=value;
	}


	/**
	 * Sets all values of the matrix to the given value
	 * 
	 * @param value
	 */
	public void setValues(double value) {
		for (double[] val : this.vals) {
			Arrays.fill(val, value);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return toString(MatrixPrint.LINE);
	}

	/**
	 * Returns a matrix as string representation; format of the string depends on the type of printing specified: either as
	 * single line or as matrix
	 * 
	 * @param print
	 *            type of printing
	 * 
	 * @return printed matrix
	 */
	public String toString(MatrixPrint print) {
		String[] begin = null;
		String[] end = null;
		String sep = null;

		switch (print) {
		case LINE:
			begin = new String[] { "[", " ; " };
			end = new String[] { "", "]" };
			sep = ",";
			break;
		case MATRIX:
			begin = new String[] { "| ", "| " };
			end = new String[] { " |\n", " |" };
			sep = "\t";
			break;
		}

		NumberFormat nf = new DecimalFormat("0.00");
		StringBuilder sb = new StringBuilder();
		for (int i = this.sIndex; i < m + sIndex; i++) {
			if (i == sIndex)
				sb.append(begin[0]);
			else
				sb.append(begin[1]);

			for (int j = sIndex; j < n + sIndex; j++) {
				sb.append(nf.format(this.getValue(i, j)) + sep);
			}

			sb.setLength(sb.length() - 1);

			if (i == m + sIndex - 1)
				sb.append(end[1]);
			else
				sb.append(end[0]);
		}
		return sb.toString();
	}
	
	/**
	 * This method generates a clone from this matrix.
	 */
	public Matrix clone(){
		Matrix clone = new Matrix(this.m, this.n,this.sIndex);
		for(int i=0; i<this.vals.length;++i){
		    clone.vals[i] = this.vals[i].clone();
		}
		return clone;
	}
}
