package de.dlr.ivf.tapas.matrixtool.matrixTool_v2.operation;

import java.util.ArrayList;

import de.dlr.ivf.tapas.matrixtool.common.localisation.Localisation;

public class NumberOfValuesFunction implements IAggregationFunction<Number> {

	public String toString(){
		return Localisation.getLocaleGuiTerm("OPERATION_NUMBER");
	}

	public Number f(ArrayList<Number> items) {
		
		int counter = 0;
		
		for (Number n : items){
			counter++;
		}
		
		return counter;
	}
}
