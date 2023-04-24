package de.dlr.ivf.tapas.matrixtool.matrixTool_v2.controller;

import de.dlr.ivf.tapas.matrixtool.common.localisation.Localisation;

public class MatrixStructureManipulationException extends Exception {

	public MatrixStructureManipulationException(String message){
		super(Localisation.getLocaleMessageTerm("EXC_MTX_STRUC") +" "+
				Localisation.getLocaleMessageTerm("LOC") +" '"+
				message+"'");
	}
}
