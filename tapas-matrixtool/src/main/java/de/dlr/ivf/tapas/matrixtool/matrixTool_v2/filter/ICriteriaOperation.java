package de.dlr.ivf.tapas.matrixtool.matrixTool_v2.filter;

public interface ICriteriaOperation<T extends Comparable<T>> {

	public boolean op(T d1, T d2);
	
	public String toString();
}
