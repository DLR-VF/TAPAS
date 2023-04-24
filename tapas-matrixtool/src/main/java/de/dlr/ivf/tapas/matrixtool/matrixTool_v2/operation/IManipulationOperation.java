package de.dlr.ivf.tapas.matrixtool.matrixTool_v2.operation;

import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.controller.ManipModuleOpsController;

public interface IManipulationOperation extends Runnable{

	public void init(IManipulationOperand o1, IManipulationOperand o2, 
			ManipModuleOpsController manipModuleOpsController) throws OperandException;
	
	public String toString();
}
