package de.dlr.ivf.tapas.matrixtool.matrixTool_v2.operation;

import de.dlr.ivf.tapas.matrixtool.common.localisation.Localisation;

public class OperandException extends Exception {

	public OperandException(String message) {
		super(Localisation.getLocaleMessageTerm("EXC_OPERAND") + " '"+message+"'");
	}
}
