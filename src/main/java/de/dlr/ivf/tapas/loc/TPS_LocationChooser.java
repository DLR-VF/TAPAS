package de.dlr.ivf.tapas.loc;

import java.util.EnumMap;

import de.dlr.ivf.tapas.constants.TPS_ActivityConstant;
import de.dlr.ivf.tapas.mode.TPS_Mode;
import de.dlr.ivf.tapas.persistence.TPS_RegionResultSet;
import de.dlr.ivf.tapas.persistence.db.TPS_DB_IOManager;
import de.dlr.ivf.tapas.person.TPS_Car;
import de.dlr.ivf.tapas.person.TPS_Person;
import de.dlr.ivf.tapas.util.parameters.SimulationType;

public abstract class TPS_LocationChooser {

	TPS_DB_IOManager PM = null;
	TPS_Region region = null;

	/**
	 * This method sets the needed references to the classes we need
	 * 
	 * @param region
	 *            The region we are in
	 * @param pm
	 *            The DB handler to use.
	 */
	public void setClassReferences(TPS_Region region, TPS_DB_IOManager pm) {
		this.region = region;
		this.PM = pm;
	}

	/**
	 * This method checks which traffic analysis zones are reachable and selects
	 * one location representant for each zone.
	 * 
	 * @param comingFrom
	 *            location where you come from before the current stay
	 * @param arrivalDuration
	 *            time for arriving in the new location
	 * @param goingTo
	 *            location where you go to after the current stay
	 * @param departureDuration
	 *            time for departing the new location
	 * @param activityCode
	 *            activity code of the current stay
	 * @param person
	 *            who is traveling
	 * @param startTime
	 *            start time of the journey
	 * @param simType
	 * @return instance of {@link TPS_RegionResultSet} with all reachable
	 *         traffic analysis zone and location representants
	 */
	abstract public EnumMap<TPS_Mode.ModeType, TPS_RegionResultSet> getLocationRepresentatives(
            TPS_Location comingFrom, double arrivalDuration,
            TPS_Location goingTo, double departureDuration,
            TPS_ActivityConstant activityCode, TPS_Person person, double startTime,
            SimulationType simType, TPS_Car car);
}
