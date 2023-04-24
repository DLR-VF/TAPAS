package de.dlr.ivf.tapas.matrixtool.matrixTool_v2.operation;

import java.util.ArrayList;

import de.dlr.ivf.tapas.matrixtool.common.localisation.Localisation;

public class MeanFunction implements IAggregationFunction<Number> {

	public String toString(){
		return Localisation.getLocaleGuiTerm("OPERATION_MEAN");
	}

	public Number f(ArrayList<Number> items) {
		
		Double d = 0.;
		
		for (Number n : items){
			d = n.doubleValue() + d;
		}
		
		return d / (double)items.size();
	}
}
