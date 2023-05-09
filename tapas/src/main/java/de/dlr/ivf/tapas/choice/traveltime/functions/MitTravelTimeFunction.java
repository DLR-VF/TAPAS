package de.dlr.ivf.tapas.choice.traveltime.functions;

import de.dlr.ivf.tapas.choice.traveltime.MatrixMapFunction;
import de.dlr.ivf.tapas.model.Matrix;
import de.dlr.ivf.tapas.model.MatrixMap;
import de.dlr.ivf.tapas.model.TPS_Geometrics;
import de.dlr.ivf.tapas.model.location.Locatable;
import de.dlr.ivf.tapas.model.location.TPS_TrafficAnalysisZone;
import de.dlr.ivf.tapas.model.mode.TPS_Mode;
import de.dlr.ivf.tapas.model.mode.TPS_Mode.ModeType;
import de.dlr.ivf.tapas.model.parameter.*;

public class MitTravelTimeFunction implements MatrixMapFunction {

    private final SimulationType simType;
    private final int minDist;
    private final boolean useIntraTazInfo;
    private final MatrixMap mitTtMatrixMap;
    private final double walkBeelineFactor;
    private final double walkVelocity;
    private final Matrix distanceMatrix;
    private final MatrixMap mitAccessMatrixMap;
    private final MatrixMap mitEgressMatrixMap;

    public MitTravelTimeFunction(TPS_ParameterClass parameterClass, double walkVelocity){

        this.simType = parameterClass.getSimulationType();
        this.minDist = parameterClass.getIntValue(ParamValue.MIN_DIST);
        this.useIntraTazInfo = parameterClass.isTrue(ParamFlag.FLAG_INTRA_INFOS_MATRIX);
        this.mitTtMatrixMap = parameterClass.paramMatrixMapClass.getMatrixMap(ParamMatrixMap.TRAVEL_TIME_MIT, simType);
        this.walkBeelineFactor = parameterClass.getDoubleValue(ModeType.WALK.getBeelineFactor());
        this.walkVelocity = walkVelocity;
        this.distanceMatrix = parameterClass.getMatrix(ParamMatrix.DISTANCES_BL);
        this.mitAccessMatrixMap = parameterClass.paramMatrixMapClass.getMatrixMap(ParamMatrixMap.ARRIVAL_MIT,simType);
        this.mitEgressMatrixMap = parameterClass.paramMatrixMapClass.getMatrixMap(ParamMatrixMap.EGRESS_MIT,simType);

    }


    @Override
    public double apply(Locatable start, Locatable end, int time) {

        int idStart = start.getTAZId();
        int idDest = end.getTAZId();
        TPS_TrafficAnalysisZone startZone = start.getTrafficAnalysisZone();
        double tt;
        double beelineDistanceLoc = TPS_Geometrics.getDistance(start, end, this.minDist);

        if (idStart == idDest) {
            if (!startZone.getSimulationTypeValues(simType).isIntraMITTrafficAllowed()) {
                // no intra traffic allowed!
                return TPS_Mode.NO_CONNECTION;
            }
            // start and end locations are in the same traffic analysis zone
            if (useIntraTazInfo) {
                // If there exists travel times inside a traffic analysis zone
                // this value is used.
                tt = this.mitTtMatrixMap.getMatrix(time).getValue(idStart, idDest);
                if (tt < 0) { // no connection via MIT!
                    tt = beelineDistanceLoc * this.walkBeelineFactor / this.walkVelocity;
                }
            } else {
                // Otherwise the travel time is calculated by the beeline
                // distance and a factor and the average speed inside
                // this traffic analysis zone.
                double factor = startZone.getSimulationTypeValues(simType).getBeelineFactorMIT();
                if (Double.isNaN(factor) || factor == 0.0) { //safety fix
                    factor = 1.4;
                }
                double avSpeed = startZone.getSimulationTypeValues(simType).getAverageSpeedMIT();
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
            tt = this.mitTtMatrixMap.getMatrix(time).getValue(idStart, idDest);

            // since TAZes are different use beeline factor
            tt *= beelineDistanceLoc / distanceMatrix.getValue(idStart, idDest);
        }

        //add access and egress times
        double acc = mitAccessMatrixMap == null ? 0 : mitAccessMatrixMap.getMatrix(time).getValue(idStart,idDest);

        double egr = mitEgressMatrixMap == null ? 0 : mitEgressMatrixMap.getMatrix(time).getValue(idStart,idDest);

        tt += acc + egr;

        return tt;
    }

}
