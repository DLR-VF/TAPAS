package de.dlr.ivf.tapas.matrixtool.matrixTool_v2.operation;

import java.util.ArrayList;

import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.controller.AnalyseStatisticAggrController;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.filter.CheckableMatrixPartCriteria;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.filter.RangeCriteria;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.model.MemoryModel;

public interface IAggregationLogic<T extends Number> extends Runnable{

	public String toString();

	public void commitOperation();
	
	public void init(AnalyseStatisticAggrController analyseStatisticAggrController,
			IAggregationFunction<T> f,
			ArrayList<RangeCriteria<Integer>> lineCrits,
			ArrayList<RangeCriteria<Integer>> colCrits,
			ArrayList<RangeCriteria<Double>> valCrits,
			ArrayList<CheckableMatrixPartCriteria<Integer>> matrixCrits,
			MemoryModel model);
	
}
