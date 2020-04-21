package de.dlr.ivf.tapas.analyzer.tum.regionanalyzer;

import de.dlr.ivf.tapas.analyzer.inputfileconverter.TapasTrip;

public class AnalyzerException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2044139247609629757L;

	private final TapasTrip trip;
	private final String message;

	public TapasTrip getTrip() {
		return trip;
	}

	public AnalyzerException(TapasTrip trip, String message) {
		this.trip = trip;
		this.message = message;
	}

	public AnalyzerException(TapasTrip trip) {
		this.trip = trip;
		this.message = "";

	}
	
	@Override
	public String getMessage() {
		return message;
	}


}
