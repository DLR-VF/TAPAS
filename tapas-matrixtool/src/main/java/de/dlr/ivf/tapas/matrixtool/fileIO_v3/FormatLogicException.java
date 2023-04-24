package de.dlr.ivf.tapas.matrixtool.fileIO_v3;

import de.dlr.ivf.tapas.matrixtool.common.localisation.Localisation;

public class FormatLogicException extends Exception {

	public FormatLogicException(String message) {
		super(Localisation.getLocaleMessageTerm("EXC_FORMAT_LOGIC") + " " +
				Localisation.getLocaleMessageTerm("LOC") + " '" + message + "'");
	}

	public FormatLogicException() {
		super(Localisation.getLocaleMessageTerm("EXC_FORMAT_LOGIC"));
	}

}
