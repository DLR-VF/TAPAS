package de.dlr.ivf.tapas.matrixtool.common.compatibility;

public interface Compatibility {

	public void checkForMaxIDLength(String s) throws CompatibilityException;
	
	public void checkForInvalidIDChars(String s) throws CompatibilityException;
}
