package de.dlr.ivf.tapas.mode;

import de.dlr.ivf.tapas.constants.TPS_ActivityConstant;
import de.dlr.ivf.tapas.loc.Locatable;
import de.dlr.ivf.tapas.person.TPS_Car;
import de.dlr.ivf.tapas.person.TPS_Person;
import de.dlr.ivf.tapas.util.TPS_Geometrics;
import de.dlr.ivf.tapas.util.parameters.*;

/**
 * This class represents the modes 'walk' and 'bike'.
 * 
 * @author mark_ma
 * 
 */
public class TPS_NonMotorisedMode extends TPS_Mode {

	/**
	 * Calls super constructor with name
	 * 
	 * @param name
	 * @param parameterClass parameter class reference
	 */
	public TPS_NonMotorisedMode(String name, String[] attributes, boolean isFix, TPS_ParameterClass parameterClass) {
		super(name, attributes, isFix, parameterClass);
	}
	

	/*
	 * (non-Javadoc)
	 * 
	 * @see mode.TPS_Mode#getDistance(loc.tpsLocation, loc.tpsLocation, int,
	 * double)
	 */
	@Override
	public double getDistance(Locatable start, Locatable end,
			SimulationType simType, TPS_Car car) {
		if(this.getParameters().isDefined(ParamMatrix.DISTANCES_BIKE) && this.isType(ModeType.BIKE)){
			return this.getParameters().paramMatrixClass.getValue(ParamMatrix.DISTANCES_BIKE, start.getTrafficAnalysisZone().getTAZId(), end.getTrafficAnalysisZone().getTAZId());
		} else if(this.getParameters().isDefined(ParamMatrix.DISTANCES_WALK) && this.isType(ModeType.WALK)){
			return this.getParameters().paramMatrixClass.getValue(ParamMatrix.DISTANCES_WALK, start.getTrafficAnalysisZone().getTAZId(), end.getTrafficAnalysisZone().getTAZId());
		} else {		
			double beelineDistanceLoc = TPS_Geometrics.getDistance(start, end, this.getParameters().getDoubleValue(ParamValue.MIN_DIST));
			return this.getDefaultDistance(beelineDistanceLoc);
		}
	}
	

	/*
	 * (non-Javadoc)
	 * 
	 * @see mode.TPS_Mode#getTravelTime(loc.tpsLocation, loc.tpsLocation, int,
	 * double, boolean, int, int, int)
	 */
	@Override
	public double getTravelTime(Locatable start, Locatable end, int time,
                                SimulationType simType, TPS_ActivityConstant actCodeFrom,
                                TPS_ActivityConstant actCodeTo, TPS_Person person, TPS_Car car) {

		double beelineDistance = TPS_Geometrics.getDistance(start, end, this.getParameters().getDoubleValue(ParamValue.MIN_DIST));
		// this is the default value, if no tt-matrix is available
		double tt = this.getDefaultDistance(beelineDistance) / this.getParameters().getDoubleValue(this.getVelocity());
		if (this.isType(ModeType.WALK)) {
			if (		(simType.equals(SimulationType.SCENARIO)&& this.getParameters().isDefined(ParamString.DB_NAME_MATRIX_TT_WALK))
					|| 	(simType.equals(SimulationType.BASE) 	&& this.getParameters().isDefined(ParamString.DB_NAME_MATRIX_TT_WALK_BASE))) {

				int idStart = start.getTrafficAnalysisZone().getTAZId();
				int idDest = end.getTrafficAnalysisZone().getTAZId();
				if (idStart == idDest) {
					// start and end locations are in the same traffic analysis zone
					if (this.getParameters().isTrue(ParamFlag.FLAG_INTRA_INFOS_MATRIX)) {
						// If there exists travel times inside a traffic
						// analysis zone this value is used.
						// if not use default tt
						tt = this.getParameters().paramMatrixMapClass.getValue(ParamMatrixMap.TRAVEL_TIME_WALK, idStart, idDest, simType, time);
					}
				} else {
					// start and end locations are in different traffic analysis
					// zones. The travel time is calculated by the travel
					// time from a table and a factor retrieved by the beeline
					// and the real distance.
					tt = this.getParameters().paramMatrixMapClass.getValue(ParamMatrixMap.TRAVEL_TIME_WALK, idStart, idDest, simType, time);
					tt *= beelineDistance / this.getParameters().paramMatrixClass.getValue(ParamMatrix.DISTANCES_BL, idStart, idDest);
				}
				if (	this.getParameters().isDefined(ParamString.DB_NAME_MATRIX_ACCESS_WALK) && simType.equals(SimulationType.SCENARIO) ||
						this.getParameters().isDefined(ParamString.DB_NAME_MATRIX_ACCESS_WALK_BASE) && simType.equals(SimulationType.BASE)
						)
					tt += this.getParameters().paramMatrixMapClass.getValue(ParamMatrixMap.ARRIVAL_WALK, idStart, idDest, simType, time);
				if (	this.getParameters().isDefined(ParamString.DB_NAME_MATRIX_EGRESS_WALK) && simType.equals(SimulationType.SCENARIO) ||
						this.getParameters().isDefined(ParamString.DB_NAME_MATRIX_EGRESS_WALK_BASE) && simType.equals(SimulationType.BASE)
						)
					tt += this.getParameters().paramMatrixMapClass.getValue(ParamMatrixMap.EGRESS_WALK, idStart, idDest, simType, time);
			}
		} else if (this.isType(ModeType.BIKE)) {
			if (	(simType.equals(SimulationType.SCENARIO)&& this.getParameters().isDefined(ParamString.DB_NAME_MATRIX_TT_BIKE))
				|| 	(simType.equals(SimulationType.BASE) 	&& this.getParameters().isDefined(ParamString.DB_NAME_MATRIX_TT_BIKE_BASE))) {

				int idStart = start.getTrafficAnalysisZone().getTAZId();
				int idDest = end.getTrafficAnalysisZone().getTAZId();
				if (idStart == idDest) {
					// start and end locations are in the same traffic analysis
					// zone
					if (this.getParameters().isTrue(ParamFlag.FLAG_INTRA_INFOS_MATRIX)) {
						// If there exists travel times inside a traffic
						// analysis zone this value is used.
						// if not use default tt
						tt = this.getParameters().paramMatrixMapClass.getValue(ParamMatrixMap.TRAVEL_TIME_BIKE, idStart, idDest, simType, time);
					}
				} else {
					// start and end locations are in different traffic analysis
					// zones. The travel time is calculated by the travel
					// time from a table and a factor retrieved by the beeline
					// and the real distance.
					tt = this.getParameters().paramMatrixMapClass.getValue(ParamMatrixMap.TRAVEL_TIME_BIKE, idStart, idDest, simType, time);
					tt *= beelineDistance / this.getParameters().paramMatrixClass.getValue(ParamMatrix.DISTANCES_BL, idStart, idDest);
				}
				if (	this.getParameters().isDefined(ParamString.DB_NAME_MATRIX_ACCESS_BIKE) && simType.equals(SimulationType.SCENARIO) ||
						this.getParameters().isDefined(ParamString.DB_NAME_MATRIX_ACCESS_BIKE_BASE) && simType.equals(SimulationType.BASE))
					tt += this.getParameters().paramMatrixMapClass.getValue(ParamMatrixMap.ARRIVAL_BIKE, idStart, idDest, simType, time);
				if (	this.getParameters().isDefined(ParamString.DB_NAME_MATRIX_EGRESS_BIKE) && simType.equals(SimulationType.SCENARIO) ||
						this.getParameters().isDefined(ParamString.DB_NAME_MATRIX_EGRESS_BIKE_BASE) && simType.equals(SimulationType.BASE))
					tt += this.getParameters().paramMatrixMapClass.getValue(ParamMatrixMap.EGRESS_BIKE, idStart, idDest, simType, time);
			}
		}
		return tt;
	}
}
