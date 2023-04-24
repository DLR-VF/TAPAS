package de.dlr.ivf.tapas.matrixtool.matrixTool_v2.operation;

import java.util.ArrayList;

import de.dlr.ivf.tapas.matrixtool.common.localisation.Localisation;

public class SumFunction implements IAggregationFunction<Number> {

	public String toString(){
		return Localisation.getLocaleGuiTerm("OPERATION_SUM");
	}

	public Number f(ArrayList<Number> items) {
		
		Double d = 0.;
		
		for (Number n : items){
			d = d + n.doubleValue();
		}
		
		return d;
	}
}
