package de.dlr.ivf.tapas.analyzer.inputfileconverter;

/**
 * Interface um Bedingungen aufzustellen die angeben ob ein Trip gültig ist oder nicht
 * 
 * @author Marco
 * 
 */
public interface ICondition {

	/**
	 * Der übergebene Parameter wird auf Gültigkeit geprüft. Sollten alle Bedingungen
	 * 
	 * @param trip
	 * @return
	 */
    boolean isValid(TapasTrip trip);
}
