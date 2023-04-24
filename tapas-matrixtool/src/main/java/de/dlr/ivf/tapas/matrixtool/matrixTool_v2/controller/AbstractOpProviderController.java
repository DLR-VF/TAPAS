package de.dlr.ivf.tapas.matrixtool.matrixTool_v2.controller;

import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.filter.ICriteriaOperation;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.filter.SmallerOperation;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.filter.SmallerOrEqualOperation;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.operation.AddValueOperation;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.operation.ClipBoardOperand;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.operation.CopyColumnOperation;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.operation.CopyMatrixOperation;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.operation.FilterOperand;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.operation.IAggregationFunction;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.operation.IManipulationOperand;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.operation.IManipulationOperation;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.operation.MaxFunction;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.operation.MeanFunction;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.operation.MinFunction;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.operation.MultValueOperation;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.operation.NumberOfValuesFunction;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.operation.SetValueOperation;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.operation.SumFunction;

public abstract class AbstractOpProviderController extends AbstractCheckingController{

	public ICriteriaOperation[] getCritOps() {
		return new ICriteriaOperation[]{
				new SmallerOrEqualOperation(),
				new SmallerOperation()
//				,
//				new BiggerOrEqualOperation(),
//				new BiggerOperation()
		};
	}

	public IManipulationOperation[] getManipOperations() {
		return new IManipulationOperation[]{
				new CopyMatrixOperation(),
				new CopyColumnOperation(),
				new SetValueOperation(),
				new AddValueOperation(),
				new MultValueOperation()
		};
	}

	public IManipulationOperand<Number>[] getManipOperands() {
		return new IManipulationOperand[]{
				new FilterOperand(),
				new ClipBoardOperand(";"),
				new ClipBoardOperand("\t")
		};
	}
	
	public IAggregationFunction<Number>[] getAggrFunctions(){
		return new IAggregationFunction[]{
				new SumFunction(),
				new MinFunction(),
				new MaxFunction(),
				new MeanFunction(),
				new NumberOfValuesFunction()
		};
	}
}
