package de.dlr.ivf.tapas.matrixtool.matrixTool_v2.filter;

public class SmallerOperation<T extends Comparable<T>> implements ICriteriaOperation<T> {

	public boolean op(T op1, T op2) {
//		return op1 < op2;
		return op1.compareTo(op2) < 0;
	}

	public String toString(){
		return "<";
	}
}
