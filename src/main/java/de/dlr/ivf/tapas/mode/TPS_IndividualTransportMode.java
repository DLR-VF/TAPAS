/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.mode;

import de.dlr.ivf.tapas.constants.TPS_ActivityConstant;
import de.dlr.ivf.tapas.loc.Locatable;
import de.dlr.ivf.tapas.log.TPS_Logger;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface.SeverenceLogLevel;
import de.dlr.ivf.tapas.person.TPS_Car;
import de.dlr.ivf.tapas.person.TPS_Person;
import de.dlr.ivf.tapas.util.TPS_Geometrics;
import de.dlr.ivf.tapas.util.parameters.*;

/**
 * This class represents modes 'taxi', 'miv' and 'miv pass'
 *
 * @author mark_ma
 */
public class TPS_IndividualTransportMode extends TPS_Mode {

    /**
     * Calls super constructor with name
     *
     * @param name
     * @param parameterClass parameter class reference
     */
    public TPS_IndividualTransportMode(String name, String[] attributes, boolean isFix, TPS_ParameterClass parameterClass) {
        super(name, attributes, isFix, parameterClass);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * de.dlr.ivf.tapas.mode.TPS_Mode#getDistance(de.dlr.ivf.tapas.loc.TPS_Location
     * , de.dlr.ivf.tapas.loc.TPS_Location, int, double)
     */
    @Override
    public double getDistance(Locatable start, Locatable end, SimulationType simType, TPS_Car car) {
        double distance;
        double factor = 1;
        int idStart = start.getTrafficAnalysisZone().getTAZId();
        int idDest = end.getTrafficAnalysisZone().getTAZId();
        double beelineDistanceLoc = TPS_Geometrics.getDistance(start, end,
                this.getParameters().getDoubleValue(ParamValue.MIN_DIST));
        if (idStart == idDest) {
            // start and end locations are in the same traffic analysis zone
            if (this.getParameters().isTrue(ParamFlag.FLAG_INTRA_INFOS_MATRIX)) {
                // there exist information about distances inside a traffic
                // analysis zone. This distance is retrieved from the
                // distances matrix. The factor is 1.
                distance = this.getParameters().paramMatrixClass.getValue(ParamMatrix.DISTANCES_STREET, idStart,
                        idDest);
            } else {
                // there exist no information about distances inside a traffic
                // analysis zone. The distance is calculated by
                // the beeline distance and a factor which is determined by the
                // traffic analysis zone.
                factor = start.getTrafficAnalysisZone().getSimulationTypeValues(simType).getBeelineFactorMIT();
                distance = beelineDistanceLoc;
            }
        } else {
            // if TAZes are different calculate beelinefactor
            factor = beelineDistanceLoc / this.getParameters().paramMatrixClass.getValue(ParamMatrix.DISTANCES_BL,
                    idStart, idDest);
            // start and end locations are in different traffic analysis zones.
            // The distance is calculated by a distance
            // retrieved from the distances matrix and a factor calculated by
            // the beeline and the real distance of both
            // traffic analysis zones.
            distance = this.getParameters().paramMatrixClass.getValue(ParamMatrix.DISTANCES_STREET, idStart, idDest);
        }

//		//autonomous vehicles get a virtual distance correction for adjusting the costs
//		if(car!=null && car.getAutomation()>=this.getParameters().getIntValue(ParamValue.AUTOMATIC_VEHICLE_LEVEL)){
//			factor *= distance>this.getParameters().getIntValue(ParamValue.AUTOMATIC_VEHICLE_TIME_MOD_THRESHOLD)?
//								this.getParameters().getDoubleValue(ParamValue.AUTOMATIC_VEHICLE_TIME_MOD_FAR):
//								this.getParameters().getDoubleValue(ParamValue.AUTOMATIC_VEHICLE_TIME_MOD_NEAR);
//		}	


        return Math.max(this.getParameters().getDoubleValue(ParamValue.MIN_DIST), distance * factor);
    }

    /*
     * (non-Javadoc)
     *
     * @see mode.TPS_Mode#getTravelTime(loc.Locatable, loc.Locatable, int,
     * double, boolean, int, int, int)
     */
    @Override
    public double getTravelTime(Locatable start, Locatable end, int time, SimulationType simType, TPS_ActivityConstant actCodeFrom, TPS_ActivityConstant actCodeTo, TPS_Person person, TPS_Car car) {
        int idStart = start.getTAZId();
        int idDest = end.getTAZId();
        double tt;
        double beelineDistanceLoc = TPS_Geometrics.getDistance(start, end,
                this.getParameters().getDoubleValue(ParamValue.MIN_DIST));

        if (idStart == idDest) {
            if (!start.getTrafficAnalysisZone().getSimulationTypeValues(simType).isIntraMITTrafficAllowed()) {
                // no intra traffic allowed!
                return TPS_Mode.NO_CONNECTION;
            }
            // start and end locations are in the same traffic analysis zone
            if (this.getParameters().isTrue(ParamFlag.FLAG_INTRA_INFOS_MATRIX)) {
                // If there exists travel times inside a traffic analysis zone
                // this value is used.
                tt = this.getParameters().paramMatrixMapClass.getValue(ParamMatrixMap.TRAVEL_TIME_MIT, idStart, idDest,
                        simType, time);
                if (tt < 0) { // no connection via MIT!
                    tt = beelineDistanceLoc * this.getParameters().getDoubleValue(ModeType.WALK.getBeelineFactor()) /
                            this.getParameters().getDoubleValue(get(ModeType.WALK).getVelocity());
                    if (travelTimeIsInvalid(tt)) {
                        TPS_Logger.log(SeverenceLogLevel.FATAL, "NaN detected");
                    }
                }
            } else {
                // Otherwise the travel time is calculated by the beeline
                // distance and a factor and the average speed inside
                // this traffic analysis zone.
                double factor = start.getTrafficAnalysisZone().getSimulationTypeValues(simType).getBeelineFactorMIT();
                if (Double.isNaN(factor) || factor == 0.0) { //safety fix
                    factor = 1.4;
                }
                double avSpeed = start.getTrafficAnalysisZone().getSimulationTypeValues(simType).getAverageSpeedMIT();
                if (Double.isNaN(avSpeed) || avSpeed == 0.0) {
                    avSpeed = 14;//safety fix
                }
                tt = ((beelineDistanceLoc * factor) / avSpeed);
            }
        } else {
            // start and end locations are in different traffic analysis zones.
            // The travel time is calculated by the travel
            // time from a table and a factor retrieved by the beeline and the
            // real distance.
            tt = this.getParameters().paramMatrixMapClass.getValue(ParamMatrixMap.TRAVEL_TIME_MIT, idStart, idDest,
                    simType, time);
            // if TAZes are different use beeline factor
            if (idStart != idDest) {
                tt *= beelineDistanceLoc / this.getParameters().paramMatrixClass.getValue(ParamMatrix.DISTANCES_BL,
                        idStart, idDest);
            }
        }

        if (car != null && car.getAutomation() >= this.getParameters().getIntValue(
                ParamValue.AUTOMATIC_VEHICLE_LEVEL) && SimulationType.SCENARIO.equals(simType)) {
            //calculate time perception modification
            double rampUp = this.getParameters().getDoubleValue(ParamValue.AUTOMATIC_VEHICLE_RAMP_UP_TIME);
            if (tt > rampUp) {
                double distanceNet = this.getParameters().paramMatrixClass.getValue(ParamMatrix.DISTANCES_STREET,
                        idStart, idDest);
                double timeMod = distanceNet > this.getParameters().getIntValue(
                        ParamValue.AUTOMATIC_VEHICLE_TIME_MOD_THRESHOLD) ? this.getParameters().getDoubleValue(
                        ParamValue.AUTOMATIC_VEHICLE_TIME_MOD_FAR) : this.getParameters().getDoubleValue(
                        ParamValue.AUTOMATIC_VEHICLE_TIME_MOD_NEAR);

                tt = rampUp + (timeMod * (tt - rampUp));
            }
        }

        double acc = 0, egr = 0;

        if (this.getParameters().isDefined(ParamString.DB_NAME_MATRIX_ACCESS_MIT) && simType.equals(
                SimulationType.SCENARIO) || this.getParameters().isDefined(
                ParamString.DB_NAME_MATRIX_ACCESS_MIT_BASE) && simType.equals(SimulationType.BASE))
            acc = this.getParameters().paramMatrixMapClass.getValue(ParamMatrixMap.ARRIVAL_MIT, idStart, idDest,
                    simType, time);
        if (this.getParameters().isDefined(ParamString.DB_NAME_MATRIX_EGRESS_MIT) && simType.equals(
                SimulationType.SCENARIO) || this.getParameters().isDefined(
                ParamString.DB_NAME_MATRIX_EGRESS_MIT_BASE) && simType.equals(SimulationType.BASE))
            egr = this.getParameters().paramMatrixMapClass.getValue(ParamMatrixMap.EGRESS_MIT, idStart, idDest, simType,
                    time);

        if (car != null && car.getAutomation() >= this.getParameters().getIntValue(
                ParamValue.AUTOMATIC_VALET_PARKING)) {
            // calculate access modification
            acc = Math.min(acc, this.getParameters().getDoubleValue(ParamValue.AUTOMATIC_PARKING_ACCESS));
            // calculate egress modification
            egr = Math.min(egr, this.getParameters().getDoubleValue(ParamValue.AUTOMATIC_PARKING_EGRESS));
        }
        tt += acc + egr;

        return tt;
    }

}
