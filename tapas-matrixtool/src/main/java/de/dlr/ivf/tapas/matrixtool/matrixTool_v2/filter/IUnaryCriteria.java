package de.dlr.ivf.tapas.matrixtool.matrixTool_v2.filter;

/*
 * unary means, you have to provide one arguments for the criteria
 */
public interface IUnaryCriteria<T extends Comparable<T>> {

	public boolean isMetBy(T d);
}
