package de.dlr.ivf.tapas.analyzer.inputfileconverter;

public class InputFileConverterException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 693610472233779379L;
	private int intError;

	InputFileConverterException(int intErrNo) {
		intError = intErrNo;
	}

	InputFileConverterException(String strMessage) {
		super(strMessage);
	}

	public String toString() {
		if (intError == 1) {
			return "TripConverterException[MissingTripFiles]";
		} else {
			return "TripConverterException[" + intError + "]";
		}
	}
}
