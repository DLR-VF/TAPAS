package de.dlr.ivf.tapas.matrixtool.fileIO_v3;

import de.dlr.ivf.tapas.matrixtool.common.localisation.Localisation;

public class UnitProcessingException extends Exception {

	public UnitProcessingException(String message) {
		super(Localisation.getLocaleMessageTerm("EXC_UNIT_PROC") +" "+
				Localisation.getLocaleMessageTerm("LOC") + " '"+
				message+"'");
	}
	
	public UnitProcessingException() {
		super(Localisation.getLocaleMessageTerm("EXC_UNIT_PROC"));
	}
}
