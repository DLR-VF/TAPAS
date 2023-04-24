package de.dlr.ivf.tapas.matrixtool.common.compatibility;

public class CompatibilityFactory {
	
	private static Compatibility[] compatibilities = {
		new VisumCompatibility()
	};
	
	public static Compatibility[] getCompatibilities(){
		return compatibilities;
	}
}
