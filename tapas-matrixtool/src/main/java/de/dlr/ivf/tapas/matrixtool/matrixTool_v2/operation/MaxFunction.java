package de.dlr.ivf.tapas.matrixtool.matrixTool_v2.operation;

import java.util.ArrayList;

import de.dlr.ivf.tapas.matrixtool.common.localisation.Localisation;

public class MaxFunction implements IAggregationFunction<Number> {

	public String toString(){
		return Localisation.getLocaleGuiTerm("OPERATION_MAX");
	}

	public Number f(ArrayList<Number> items) {
		
		Double d = Double.MIN_VALUE;
		
		for (Number n : items){
			if (n.doubleValue() > d)
				d = n.doubleValue();
		}
		return d;
	}
}
