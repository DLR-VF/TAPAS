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
import de.dlr.ivf.tapas.person.TPS_Car;
import de.dlr.ivf.tapas.person.TPS_Person;
import de.dlr.ivf.tapas.util.Matrix;
import de.dlr.ivf.tapas.util.TPS_Geometrics;
import de.dlr.ivf.tapas.util.parameters.*;

/**
 * This class represents the modes 'walk' and 'bike'.
 *
 * @author mark_ma
 */
public class TPS_NonMotorisedMode extends TPS_Mode {

    private static int mycounter = 0;
    private static int lasttime = 0;
    private static Matrix tt_walk_lasttime = new Matrix(1);
    private static Matrix tt_walk_lasttime_all = new Matrix(1);
    private static ParamMatrixMapClass first_matrixmapclass;
    private static TPS_ParameterClass first_parameterclass;
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
    public double getDistance(Locatable start, Locatable end, SimulationType simType, TPS_Car car) {
        if (this.getParameters().isDefined(ParamMatrix.DISTANCES_BIKE) && this.isType(ModeType.BIKE)) {
            return this.getParameters().paramMatrixClass.getValue(ParamMatrix.DISTANCES_BIKE,
                    start.getTrafficAnalysisZone().getTAZId(), end.getTrafficAnalysisZone().getTAZId());
        } else if (this.getParameters().isDefined(ParamMatrix.DISTANCES_WALK) && this.isType(ModeType.WALK)) {
            return this.getParameters().paramMatrixClass.getValue(ParamMatrix.DISTANCES_WALK,
                    start.getTrafficAnalysisZone().getTAZId(), end.getTrafficAnalysisZone().getTAZId());
        } else {
            double beelineDistanceLoc = TPS_Geometrics.getDistance(start, end,
                    this.getParameters().getDoubleValue(ParamValue.MIN_DIST));
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
    public double getTravelTime(Locatable start, Locatable end, int time, SimulationType simType, TPS_ActivityConstant actCodeFrom, TPS_ActivityConstant actCodeTo, TPS_Person person, TPS_Car car) {

        double beelineDistance = TPS_Geometrics.getDistance(start, end,
                this.getParameters().getDoubleValue(ParamValue.MIN_DIST));
        // this is the default value, if no tt-matrix is available
        double tt = this.getDefaultDistance(beelineDistance) / this.getParameters().getDoubleValue(this.getVelocity());
        if (this.isType(ModeType.WALK)) {
            if ((simType.equals(SimulationType.SCENARIO) && this.getParameters().isDefined(
                    ParamString.DB_NAME_MATRIX_TT_WALK)) || (simType.equals(SimulationType.BASE) &&
                    this.getParameters().isDefined(ParamString.DB_NAME_MATRIX_TT_WALK_BASE))) {

                int idStart = start.getTrafficAnalysisZone().getTAZId();
                int idDest = end.getTrafficAnalysisZone().getTAZId();
                Matrix tt_walk = this.getParameters().paramMatrixMapClass.getMatrixFromMap(
                        ParamMatrixMap.TRAVEL_TIME_WALK, simType, time);
                Matrix tt_walk_lasttime2 = this.getParameters().paramMatrixMapClass.getMatrixFromMap(
                        ParamMatrixMap.TRAVEL_TIME_WALK, simType, lasttime);

                if (idStart>1223 || idDest > 1223) {
                    mycounter++;
                    System.out.println("hi " + mycounter
                                    + ", "+ tt_walk.toString()
                            + ", "+ this.getParameters().paramMatrixMapClass.toString()
                            + ", "+ first_matrixmapclass.toString()
                            + ", "+ this.getParameters().toString()
                            + ", "+ first_parameterclass.toString()
                            + ", "+ tt_walk.getNumberOfRows()
                            + ", "+ tt_walk.getNumberOfColums()
                            +  ", " + idStart + ", " + idDest + ", " + simType + ", " + time + ", " + lasttime
                            + ", "+ tt_walk_lasttime2.getNumberOfRows()
                            + ", "+ tt_walk_lasttime2.getNumberOfColums());
                    lasttime = time;
                }
                    System.out.println("Set first matrixmap and parametrclass");
                    first_matrixmapclass = this.getParameters().paramMatrixMapClass;
                    first_parameterclass = this.getParameters();

                if (!first_matrixmapclass.equals(this.getParameters().paramMatrixMapClass)) {
                    System.out.println(first_matrixmapclass.toString());
                    System.out.println(this.getParameters().paramMatrixMapClass.toString());
                    System.out.println(first_parameterclass.toString());
                    System.out.println(this.getParameters().toString());
                }
                if ((idStart>1223 || idDest > 1223) & tt_walk.getNumberOfRows()< 1225){
                    System.out.println("oh oh ");
                }
                if (idStart == idDest) {
                    // start and end locations are in the same traffic analysis zone
                    if (this.getParameters().isTrue(ParamFlag.FLAG_INTRA_INFOS_MATRIX)) {
                        // If there exists travel times inside a traffic
                        // analysis zone this value is used.
                        // if not use default tt
//                        tt = this.getParameters().paramMatrixMapClass.getValue(ParamMatrixMap.TRAVEL_TIME_WALK, idStart,
//                                idDest, simType, time);
                        tt = this.getParameters().paramMatrixMapClass.getMatrixFromMap(
                                ParamMatrixMap.TRAVEL_TIME_WALK, simType, time).getValue(idStart, idDest);
                    }
                } else {
                    // start and end locations are in different traffic analysis
                    // zones. The travel time is calculated by the travel
                    // time from a table and a factor retrieved by the beeline
                    // and the real distance.
//                    tt = this.getParameters().paramMatrixMapClass.getValue(ParamMatrixMap.TRAVEL_TIME_WALK, idStart,
//                            idDest, simType, time);
                    tt = this.getParameters().paramMatrixMapClass.getMatrixFromMap(
                            ParamMatrixMap.TRAVEL_TIME_WALK, simType, time).getValue(idStart, idDest);
                    tt *= beelineDistance / this.getParameters().paramMatrixClass.getValue(ParamMatrix.DISTANCES_BL,
                            idStart, idDest);
                }
                if (idStart>1223 || idDest > 1223) {
                    tt_walk_lasttime = tt_walk;
                }
                tt_walk_lasttime_all = tt_walk;
                if (this.getParameters().isDefined(ParamString.DB_NAME_MATRIX_ACCESS_WALK) && simType.equals(
                        SimulationType.SCENARIO) || this.getParameters().isDefined(
                        ParamString.DB_NAME_MATRIX_ACCESS_WALK_BASE) && simType.equals(SimulationType.BASE))
                    tt += this.getParameters().paramMatrixMapClass.getValue(ParamMatrixMap.ARRIVAL_WALK, idStart,
                            idDest, simType, time);
                if (this.getParameters().isDefined(ParamString.DB_NAME_MATRIX_EGRESS_WALK) && simType.equals(
                        SimulationType.SCENARIO) || this.getParameters().isDefined(
                        ParamString.DB_NAME_MATRIX_EGRESS_WALK_BASE) && simType.equals(SimulationType.BASE))
                    tt += this.getParameters().paramMatrixMapClass.getValue(ParamMatrixMap.EGRESS_WALK, idStart, idDest,
                            simType, time);
            }
        } else if (this.isType(ModeType.BIKE)) {
            if ((simType.equals(SimulationType.SCENARIO) && this.getParameters().isDefined(
                    ParamString.DB_NAME_MATRIX_TT_BIKE)) || (simType.equals(SimulationType.BASE) &&
                    this.getParameters().isDefined(ParamString.DB_NAME_MATRIX_TT_BIKE_BASE))) {

                int idStart = start.getTrafficAnalysisZone().getTAZId();
                int idDest = end.getTrafficAnalysisZone().getTAZId();
                if (idStart == idDest) {
                    // start and end locations are in the same traffic analysis
                    // zone
                    if (this.getParameters().isTrue(ParamFlag.FLAG_INTRA_INFOS_MATRIX)) {
                        // If there exists travel times inside a traffic
                        // analysis zone this value is used.
                        // if not use default tt
                        tt = this.getParameters().paramMatrixMapClass.getValue(ParamMatrixMap.TRAVEL_TIME_BIKE, idStart,
                                idDest, simType, time);
                    }
                } else {
                    // start and end locations are in different traffic analysis
                    // zones. The travel time is calculated by the travel
                    // time from a table and a factor retrieved by the beeline
                    // and the real distance.
                    tt = this.getParameters().paramMatrixMapClass.getValue(ParamMatrixMap.TRAVEL_TIME_BIKE, idStart,
                            idDest, simType, time);
                    tt *= beelineDistance / this.getParameters().paramMatrixClass.getValue(ParamMatrix.DISTANCES_BL,
                            idStart, idDest);
                }
                if (this.getParameters().isDefined(ParamString.DB_NAME_MATRIX_ACCESS_BIKE) && simType.equals(
                        SimulationType.SCENARIO) || this.getParameters().isDefined(
                        ParamString.DB_NAME_MATRIX_ACCESS_BIKE_BASE) && simType.equals(SimulationType.BASE))
                    tt += this.getParameters().paramMatrixMapClass.getValue(ParamMatrixMap.ARRIVAL_BIKE, idStart,
                            idDest, simType, time);
                if (this.getParameters().isDefined(ParamString.DB_NAME_MATRIX_EGRESS_BIKE) && simType.equals(
                        SimulationType.SCENARIO) || this.getParameters().isDefined(
                        ParamString.DB_NAME_MATRIX_EGRESS_BIKE_BASE) && simType.equals(SimulationType.BASE))
                    tt += this.getParameters().paramMatrixMapClass.getValue(ParamMatrixMap.EGRESS_BIKE, idStart, idDest,
                            simType, time);
            }
        }
        return tt;
    }
}
