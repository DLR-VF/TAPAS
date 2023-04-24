package de.dlr.ivf.tapas.matrixtool.matrixTool_v2.filter;

/*
 * binary means, you have to provide two arguments for the criteria
 */
public interface IBinaryCriteria<T extends Comparable<T>> {

	public boolean isMetBy(T d1, T d2);
	
	public String toString();

}
