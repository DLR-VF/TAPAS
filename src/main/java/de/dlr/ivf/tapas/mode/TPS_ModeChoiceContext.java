package de.dlr.ivf.tapas.mode;

import de.dlr.ivf.tapas.loc.TPS_Location;
import de.dlr.ivf.tapas.person.TPS_Car;
import de.dlr.ivf.tapas.scheme.TPS_Stay;

public class TPS_ModeChoiceContext {
	
	public TPS_ModeChoiceContext() {
	}

	public TPS_Stay fromStay, toStay; // !!! fromStay not always set
	//public TPS_LocatedStay fromStayLocated, toStayLocated;
	public TPS_Location fromStayLocation, toStayLocation;
	public double duration;
	public boolean isBikeAvailable;
	public TPS_Car carForThisPlan;
	public int startTime;
	public TPS_Mode combinedMode = null;

}
