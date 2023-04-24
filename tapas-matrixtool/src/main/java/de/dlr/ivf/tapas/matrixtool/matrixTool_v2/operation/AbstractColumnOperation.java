package de.dlr.ivf.tapas.matrixtool.matrixTool_v2.operation;

import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.controller.ManipModuleOpsController;

public abstract class AbstractColumnOperation implements IManipulationOperation {

	protected IManipulationOperand sinkOp;
	protected IManipulationOperand sourceOp;
	protected ManipModuleOpsController control;

	public void init(IManipulationOperand sinkOp, IManipulationOperand sourceOp,
			ManipModuleOpsController manipModuleOpsController)
	throws OperandException {

		this.sinkOp = sinkOp;
		this.sourceOp = sourceOp;
		this.control = manipModuleOpsController;
	}

	public void run() {

		try {

			int numberSourceLines = sourceOp.getNumberOfLines();
			int numberSinkLines = sinkOp.getNumberOfLines();
			//columns in beiden operanden "= 1" (quasi)

			if (numberSinkLines == numberSourceLines){

				int sourceLineCounter = 0;

				for (int i = sinkOp.getMinLineIndex(); i < sinkOp.getMinLineIndex() + numberSinkLines; 
				i++){

					int sourceLineIdx = sourceOp.getMinLineIndex() + sourceLineCounter;

					if (sinkOp.shouldBeConsidered(i)  &&  
							sourceOp.shouldBeConsidered(sourceLineIdx)){

						Double v = Double.parseDouble(sourceOp.getFirstValueFromLine(sourceLineIdx).toString());

						if (v == null)
//							throw new OperandException("in "+sourceOp.toString()+" keine zahl in zeile " +
//									sourceLineIdx);
							throw new OperandException(sourceOp.toString());

						op(i,sourceLineIdx,v);

					}

					sourceLineCounter++;
				}

			} else if (numberSourceLines == 1) {

				for (int i = sinkOp.getMinLineIndex(); i < sinkOp.getNumberOfLines(); i++){

					if (sinkOp.shouldBeConsidered(i)){

						Double v = Double.parseDouble(sourceOp.getValue(sourceOp.getMinLineIndex(),
								sourceOp.getMinColumnIndex()).toString());

						if (v == null)
//							throw new OperandException("in "+sourceOp.toString()+" keine zahl in zeile " +
//									sourceOp.getMinLineIndex());
							throw new OperandException(sourceOp.toString());

						op(i,sourceOp.getMinLineIndex(),v);
					}
				}

			} else {
//				throw new OperandException("'"+sinkOp+"': "+numberSinkLines+" Zeilen  !=  "+
//						"'"+sourceOp+"': "+numberSourceLines+" Zeilen");
				throw new OperandException(sinkOp.toString()+", "+sourceOp.toString());
			}

			sinkOp.commitOperation();		

			control.signalFinished();

		} catch (OperandException e) {
			control.signalError(e.getMessage());
		}
	}

	protected abstract void op(int i, int sourceLineIdx, Double v);
}
