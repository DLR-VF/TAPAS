package de.dlr.ivf.tapas.matrixtool.matrixTool_v2.filter;

public class MatrixPartCriteria<T extends Comparable<T>> implements IBinaryCriteria<T> {
	
	private ICriteriaOperation<T> op;
	
	public MatrixPartCriteria(ICriteriaOperation<T> op){
		this.op = op;
	}

	/*
	 * d1 is row, d2 is column, op
	 * (non-Javadoc)
	 * @see matrixTool_filter_common.IBinaryCriteria#isMetBy(double, double)
	 */
	public boolean isMetBy(T d1, T d2) {
		return op.op(d1,d2);
	}

	public String toString(){
		return "i " + op.toString() + " j";
	}
}
