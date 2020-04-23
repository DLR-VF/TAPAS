package de.dlr.ivf.tapas.mode;

import de.dlr.ivf.tapas.mode.TPS_Mode.ModeType;
import de.dlr.ivf.tapas.mode.TPS_Mode.TPS_ModeCodeType;

public class TPS_ExtMode {
	public TPS_Mode primary;
	public TPS_Mode secondary;

	final public static TPS_ExtMode simpleWalk = new TPS_ExtMode(TPS_Mode.get(ModeType.WALK), null);
	final public static TPS_ExtMode simplePT = new TPS_ExtMode(TPS_Mode.get(ModeType.PT), null);
	final public static TPS_ExtMode simpleMIT = new TPS_ExtMode(TPS_Mode.get(ModeType.MIT), null);

	
	
	public TPS_ExtMode(TPS_Mode primary, TPS_Mode secondary) {
		this.primary = primary;
		this.secondary = secondary;
	}
	
	
	public int getMCTCode() {
		int mid = secondary==null?0:secondary.getCode(TPS_ModeCodeType.MCT);
		return (mid<<8) | primary.getCode(TPS_ModeCodeType.MCT);
	}
	
	
	public boolean isFix() {
		if(primary.isFix()) {
			return true;
		}
		return secondary != null && secondary.isFix();
	}
	
	
	public boolean isBikeUsed() {
		if(primary.isType(ModeType.BIKE)) {
			return true;
		}
		return secondary != null && secondary.isType(ModeType.BIKE);
	}
	

	public boolean isCarUsed() {
		if(primary.isType(ModeType.MIT)) {
			return true;
		}
		return secondary != null && secondary.isType(ModeType.MIT);
	}


	public String getName() {
		String name = primary.getName();
		if(secondary!=null) {
			name = name + "(" + secondary.getName() + ")";
		}
		return name;
	}
	

	
}
