package de.dlr.ivf.tapas.matrixtool.common.compatibility;

import de.dlr.ivf.tapas.matrixtool.common.localisation.Localisation;

public class CompatibilityException extends Exception {

	public CompatibilityException(String message) {
		super(Localisation.getLocaleMessageTerm("EXC_COMPATIB") + " '"+
				message+"'");
	}
}
