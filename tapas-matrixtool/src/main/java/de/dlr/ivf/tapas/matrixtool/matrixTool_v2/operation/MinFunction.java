package de.dlr.ivf.tapas.matrixtool.matrixTool_v2.operation;

import java.util.ArrayList;

import de.dlr.ivf.tapas.matrixtool.common.localisation.Localisation;

public class MinFunction implements IAggregationFunction<Number> {

	public String toString(){
		return Localisation.getLocaleGuiTerm("OPERATION_MIN");
	}

	public Number f(ArrayList<Number> items) {
		
		Double d = Double.MAX_VALUE;
		
		for (Number n : items){
			if (n.doubleValue() < d)
				d = n.doubleValue();
		}
		
		return d;
	}
}
