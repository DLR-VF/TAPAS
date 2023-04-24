package de.dlr.ivf.tapas.matrixtool.common.compatibility;

public class VisumCompatibility implements Compatibility {
	
	private static final String NAME = "Visum";
	private static final int MAX_CHAR = 10;

	public void checkForInvalidIDChars(String s) throws CompatibilityException {
		
		if (s == null)
			return;
		
		for (int i = 0; i < s.length(); i++){
			if (!Character.isDigit(s.charAt(i)))
//				throw new CompatibilityException("invalid character ('"+s.charAt(i) +
//						"') for compatibility to '" + NAME + "'", NAME);
				throw new CompatibilityException(NAME);
		}
		
		if (s.length() == 0)
//			throw new CompatibilityException("IDs must not be empty for compatibility to '" + NAME + "'",
//					NAME);
			throw new CompatibilityException(NAME);
		
		if (s.length() > 0  &&  s.charAt(0) == '0')
//			throw new CompatibilityException("invalid character ('"+s.charAt(0) +
//					"') for compatibility to '" + NAME + "'",NAME);
			throw new CompatibilityException(NAME);
	}

	public void checkForMaxIDLength(String s) throws CompatibilityException {
		
		if (s == null)
			return;
		
		if (s.length() > MAX_CHAR)
//			throw new CompatibilityException("at most " + MAX_CHAR + " characters allowed for " +
//					"compatibility to '" + NAME + "'",NAME);
			throw new CompatibilityException(NAME);
	}
}
