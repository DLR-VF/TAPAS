package de.dlr.ivf.tapas.util;

import java.util.Arrays;

import de.dlr.ivf.tapas.log.TPS_Logger;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface.SeverenceLogLevel;
import de.dlr.ivf.tapas.util.Matrix.MatrixPrint;

/**
 * Class for a set of matrices, which correspond to a given distribution
 * @author hein_mh
 *
 */
public class MatrixMap {

	public static void main(String[] args) {
		//Splitting the day into four slices à six hours
		double[] distribution = new double[]{6, 12, 18, 24};
		
		//creating random matrices;
		Matrix[] matrices = new Matrix[distribution.length];
		for (int i=0; i<matrices.length; i++){
			matrices[i] = new Matrix(2, 1);
			matrices[i].setValues(i);
		}
		
		MatrixMap map = new MatrixMap(distribution, matrices);
		
		System.out.println("  2: "+map.getMatrix(2*60*60).toString(MatrixPrint.LINE));
		System.out.println("  6: "+map.getMatrix(6*60*60).toString(MatrixPrint.LINE));
		System.out.println("  7: "+map.getMatrix(7*60*60).toString(MatrixPrint.LINE));
		System.out.println("12.1:"+map.getMatrix((int)(12.1*60*60)).toString(MatrixPrint.LINE));
		System.out.println(" 23: "+map.getMatrix(23*60*60).toString(MatrixPrint.LINE));
		System.out.println(" 24.4: "+map.getMatrix((int)(24.4*60*60)).toString(MatrixPrint.LINE));
		System.out.println(" -5: "+map.getMatrix(-5 *60*60).toString(MatrixPrint.LINE));
		System.out.println(" -7: "+map.getMatrix(-7 *60*60).toString(MatrixPrint.LINE));
		System.out.println(" 35: "+map.getMatrix(35 *60*60).toString(MatrixPrint.LINE));
		System.out.println(" 47: "+map.getMatrix(47 *60*60).toString(MatrixPrint.LINE));
		System.out.println(map.toString(MatrixPrint.LINE));
	}
	
	static int oneDay=  24*60*60;
	
	public class MatrixTuple implements Comparable<MatrixTuple>{
		public int value;
		public Matrix matrix;
		
		public MatrixTuple(int value, Matrix matrix) {
			
			//make the value between 0 and oneDay
			if(value != oneDay) {
				value = value %oneDay;
				if(value<0)
					value+=oneDay;
			}
			this.value=value;
			this.matrix=matrix;
		}
		@Override
		public int compareTo(MatrixTuple o) {			
			return this.value-o.value;
		}
		
	}
	
	public MatrixTuple[] matrices = new MatrixTuple[0];
	
	
	public void logStatistics() {
		for(MatrixTuple  e: this.matrices) {
			TPS_Logger.log(SeverenceLogLevel.INFO, "Matrix values: Time: "+e.value+" average Time: "+e.matrix.getAverageValue(true, true));
		}
	}
	
	public MatrixMap(double[] distribution, Matrix[] matrices) {
		if(distribution.length != matrices.length) {
			TPS_Logger.log(SeverenceLogLevel.ERROR, "Length of matrices and distribution differs: Matrix elements:"+matrices.length+" distribution elements :"+distribution.length);
			return;
		}
		//instanciate an array of the exact length
		this.matrices = new MatrixTuple[distribution.length];
		for(int i=0; i< distribution.length;i++) {
			this.matrices[i] =new MatrixTuple((int)(distribution[i]*60*60), matrices[i]);
		}
		Arrays.sort(this.matrices);
	}

	
	/**
	 * Method to select the appropriate matrix for the given time slot
	 * @param time inputtime in minutes since midnight.
	 * @return matrix for the given time
	 */
	public Matrix getMatrix(int time){
		
		//make the time between 0 and oneDay
		time = time %oneDay;
		if(time<0)
			time+=oneDay;
		
		int index=  Arrays.binarySearch(this.matrices, new MatrixTuple(time,null));
		if(index <0) 	
			index = (-index)-1;

			
		return this.matrices[index].matrix;
		
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return this.toString(MatrixPrint.LINE);
	}	
	/**
	 * Returns a matrixmap as string representation; format of the string depends on the type of printing specified: either as
	 * single line or as matrix
	 * 
	 * @param print
	 *            type of printing
	 * 
	 * @return printed matrix
	 */
	public String toString(MatrixPrint print) {
		String sb = "";
		for (MatrixTuple matrix : this.matrices) {
			sb = sb.concat(matrix.matrix.toString(print));
		}	
		return sb;
	}
	/**
	 * 
	 */
	public MatrixMap clone(){
        Matrix[] matrixClones = new Matrix[this.matrices.length];
		double[] distribution = new double[this.matrices.length];
		int i=0;
		for(MatrixTuple e: this.matrices) {
			matrixClones[i] = e.matrix.clone();
			distribution[i] = e.value;
			i++;
		}
	
		return new MatrixMap(distribution,matrixClones);
	}
}
