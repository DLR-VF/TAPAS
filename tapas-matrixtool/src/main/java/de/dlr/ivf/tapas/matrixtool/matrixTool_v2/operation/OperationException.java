package de.dlr.ivf.tapas.matrixtool.matrixTool_v2.operation;

import de.dlr.ivf.tapas.matrixtool.common.localisation.Localisation;

public class OperationException extends Exception {

	public OperationException(String message){
		super(Localisation.getLocaleMessageTerm("EXC_OPERATION") + " '"+message+"'");
	}
}
