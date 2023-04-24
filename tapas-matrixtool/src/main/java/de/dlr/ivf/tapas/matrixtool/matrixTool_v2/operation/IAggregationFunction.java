package de.dlr.ivf.tapas.matrixtool.matrixTool_v2.operation;

import java.util.ArrayList;

public interface IAggregationFunction<Number> {

	public Number f(ArrayList<Number> items);
	
	public String toString();
}
