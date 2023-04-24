package de.dlr.ivf.tapas.matrixtool.matrixTool_v2.operation;

import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.controller.ManipModuleOpsController;

public abstract class AbstractMatrixOperation implements IManipulationOperation {
	

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
			int numberSourceCols = sourceOp.getNumberOfColumns();
			int numberSinkLines = sinkOp.getNumberOfLines();
			int numberSinkCols = sinkOp.getNumberOfColumns();

			if (numberSourceLines == numberSinkLines  && numberSourceCols == numberSinkCols){

				int sourceLineCounter = 0;
				
				for (int i = sinkOp.getMinLineIndex(); i < sinkOp.getMinLineIndex() + numberSinkLines; 
					i++){
					
					int sourceLineIdx = sourceOp.getMinLineIndex() + sourceLineCounter;
					int sourceColCounter = 0;
					
					for (int j = sinkOp.getMinColumnIndex(); j < sinkOp.getMinColumnIndex() + numberSinkCols; 
						j++){
						
						int sourceColIdx = sourceOp.getMinColumnIndex() + sourceColCounter;
				
//						System.out.println("sinkop.shouldbeconsidered ("+i+","+j+") : "+
//								sinkOp.shouldBeConsidered(i, j));
//						System.out.println("sourceop.shouldbeconsidered ("+i+","+j+") : "+
//								sourceOp.shouldBeConsidered(i, j));
						
						if (sinkOp.shouldBeConsidered(i, j)  &&  
								sourceOp.shouldBeConsidered(sourceLineIdx,sourceColIdx)){
							
							Double v = Double.parseDouble(sourceOp.getValue(sourceLineIdx,sourceColIdx).toString());
							
							if (v == null)
//								throw new OperandException("in "+sourceOp.toString()+" keine zahl an " +
//										"("+sourceLineIdx+" , "+sourceColIdx+")");
								throw new OperandException(sourceOp.toString());
							
							op(i,j,sourceLineIdx,sourceColIdx,v);
					
						}
						
						sourceColCounter++;
					}
					sourceLineCounter++;
				}

			} else if (numberSourceLines == 1  && numberSourceCols == 1){
				
				for (int i = sinkOp.getMinLineIndex(); i < sinkOp.getNumberOfLines(); i++){
					for (int j = sinkOp.getMinColumnIndex(); j < sinkOp.getNumberOfColumns(); j++){
						
						if (sinkOp.shouldBeConsidered(i, j)){
							
							Double v = Double.parseDouble(sourceOp.getValue(sourceOp.getMinLineIndex(),
									sourceOp.getMinColumnIndex()).toString());
							
							if (v == null)
//								throw new OperandException("in "+sourceOp.toString()+" keine zahl an " +
//										"("+sourceOp.getMinLineIndex()+" , "+sourceOp.getMinColumnIndex()+")");
								throw new OperandException(sourceOp.toString());
								
							op(i,j,sourceOp.getMinLineIndex(),sourceOp.getMinColumnIndex(),v);
						}
					}
				}

			} else {
//				throw new OperandException("'"+sinkOp+"': "+numberSinkLines+" x "+numberSinkCols+"  !=  "+
//						"'"+sourceOp+"': "+numberSourceLines+" x "+numberSourceCols);
				throw new OperandException(sinkOp.toString()+", "+sourceOp.toString());
			}
			
			sinkOp.commitOperation();		
			
			control.signalFinished();
			
		} catch (OperandException oe){
			control.signalError(oe.getMessage());
		}
	}

	protected abstract void op(int i, int j, int sourceLineIdx, int sourceColIdx, Double v);
}
