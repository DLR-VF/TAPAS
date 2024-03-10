package de.dlr.ivf.tapas.choice.traveltime.functions;

import de.dlr.ivf.tapas.choice.traveltime.TravelTimeFunction;
import de.dlr.ivf.tapas.model.Matrix;
import de.dlr.ivf.tapas.model.MatrixMap;
import de.dlr.ivf.tapas.model.TPS_Geometrics;
import de.dlr.ivf.tapas.model.location.Locatable;
import de.dlr.ivf.tapas.model.location.ScenarioTypeValues;
import de.dlr.ivf.tapas.model.location.TPS_Block;
import de.dlr.ivf.tapas.model.location.TPS_TrafficAnalysisZone;
import de.dlr.ivf.tapas.model.mode.TPS_Mode;
import de.dlr.ivf.tapas.model.parameter.*;


/**
 * temporary class during refactoring process containing extracted "getTravelTime" behaviour from TPS_MassTransportMode class.
 *
 * @author Alain Schengen
 */
public class PtTravelTimeFunction implements TravelTimeFunction {


    private final MatrixMap ttMatrixMap;
    private final Matrix blDistanceMatrix;
    private final double minDist;
    private final MatrixMap arrivalMatrixMap;
    private final MatrixMap egressMatrixMap;
    private final boolean useBlockLevel;

    private final int defaultBlockScore;
    private final double ptTtFactor;
    private final double ptAccessFactor;
    private final double ptEgressFactor;
    private final boolean useIntraTazInfo;
    private final double averageDistancePtStop;
    private final TPS_Mode travelMode;
    private final TPS_Mode accessMode;

    private final SimulationType simType;

    public PtTravelTimeFunction(TPS_ParameterClass parameterClass, TPS_Mode travelMode, TPS_Mode accessMode){
        this.simType = parameterClass.getSimulationType();
        this.blDistanceMatrix = parameterClass.paramMatrixClass.getMatrix(ParamMatrix.DISTANCES_BL);
        this.ttMatrixMap = parameterClass.paramMatrixMapClass.getMatrixMap(ParamMatrixMap.TRAVEL_TIME_PT, simType);
        this.arrivalMatrixMap = parameterClass.paramMatrixMapClass.getMatrixMap(ParamMatrixMap.ARRIVAL_PT, simType);
        this.egressMatrixMap = parameterClass.paramMatrixMapClass.getMatrixMap(ParamMatrixMap.EGRESS_PT, simType);
        this.minDist = parameterClass.getDoubleValue(ParamValue.MIN_DIST);


        this.useBlockLevel = parameterClass.isTrue(ParamFlag.FLAG_USE_BLOCK_LEVEL);
        this.useIntraTazInfo = parameterClass.isTrue(ParamFlag.FLAG_INTRA_INFOS_MATRIX);
        this.defaultBlockScore = parameterClass.getIntValue(ParamValue.DEFAULT_BLOCK_SCORE);
        this.ptTtFactor = parameterClass.getDoubleValue(ParamValue.PT_TT_FACTOR);
        this.ptAccessFactor = parameterClass.getDoubleValue(ParamValue.PT_ACCESS_FACTOR);
        this.ptEgressFactor = parameterClass.getDoubleValue(ParamValue.PT_EGRESS_FACTOR);

        this.travelMode = travelMode;
        this.accessMode = accessMode;
        this.averageDistancePtStop = parameterClass.getDoubleValue(ParamValue.AVERAGE_DISTANCE_PT_STOP);
    }

    @Override
    public double calculateTravelTime(Locatable start, Locatable end, int time) {

        double tt = -1.0; // this is the indicator, of an invalid tt -time
        int startTazId = start.getTrafficAnalysisZone().getTAZId();
        int endTazId = end.getTrafficAnalysisZone().getTAZId();

        double beelineDistanceLoc = TPS_Geometrics.getDistance(start, end, this.minDist);

        double beelineDistanceTAZ = beelineDistanceLoc;

        if (startTazId != endTazId) {
            beelineDistanceTAZ = blDistanceMatrix.getValue(startTazId, endTazId);
        }

        double beelineFaktor = beelineDistanceLoc / beelineDistanceTAZ;

        final double matrixTravelTime = ttMatrixMap.getMatrix(time).getValue(startTazId, endTazId);
        // access time
        final double matrixAccessTime = arrivalMatrixMap == null ? 0 : arrivalMatrixMap.getMatrix(time).getValue(startTazId, endTazId);

        // egress time
        final double matrixEgressTime = egressMatrixMap == null ? 0 : egressMatrixMap.getMatrix(time).getValue(startTazId, endTazId);

        if (startTazId != endTazId) {

            // start and destination are not within the same traffic zone
            if (TPS_Mode.noConnection(matrixTravelTime)) {
                return TPS_Mode.NO_CONNECTION;
            }

            tt = beelineFaktor * matrixTravelTime;
            double accessTime = matrixAccessTime;
            double egressTime = matrixEgressTime;

            if (useBlockLevel) {

                double scoreFrom = this.getScore(start.getTrafficAnalysisZone()) - this.getScore(start.getBlock());
                double scoreTo = this.getScore(end.getTrafficAnalysisZone()) - this.getScore(end.getBlock());

                //travel time
                tt *= 1.0 + ((scoreFrom + scoreTo) * this.ptTtFactor);
                accessTime *= 1.0 + (scoreFrom * this.ptAccessFactor);
                egressTime *= 1.0 + (scoreTo * this.ptEgressFactor);
            }

            tt += accessTime + egressTime;

        } else {
            if (!start.getTrafficAnalysisZone().getSimulationTypeValues(simType).isIntraPTTrafficAllowed()) {
                //no intra traffic allowed!
                return TPS_Mode.NO_CONNECTION;
            }

            // start and destination within the same zone
            if (useBlockLevel) {
                if ((start.hasBlock() && start.getBlock().equals(end.getBlock())) ||
                        (!start.hasBlock() && !end.hasBlock())) {
                    // start and destination within the same block or no block information: No valid tt-time!
                    // tt = this.getParameters().getDoubleValue(ParamValue.PT_MINIMUM_TT);
                } else {
                    // start and destination not within the same block
                    if (!useIntraTazInfo) {
                        ScenarioTypeValues stv = start.getTrafficAnalysisZone().getSimulationTypeValues(simType);
                        double averageSpeedPT = stv.getAverageSpeedPT();
                        if (Double.isNaN(averageSpeedPT))//safety fix
                            averageSpeedPT = 8;
                        tt = beelineFaktor * beelineDistanceLoc / averageSpeedPT;
                    } else {
                        double travelModeVelocity = this.travelMode.getVelocity();
                        if (Double.isNaN(travelModeVelocity))//safety fix
                            travelModeVelocity = 8;
                        tt = beelineFaktor * beelineDistanceLoc / travelModeVelocity;
                    }

                    // access distance to pt
                    double distFrom = start.hasBlock() ? start.getBlock().getNearestPubTransStop() : this.averageDistancePtStop;

                    // egress distance to pt
                    double distTo = end.hasBlock() ? end.getBlock().getNearestPubTransStop() : this.averageDistancePtStop;

                    // !!!dk
                    if (distTo < 0) {
                        distTo = 0;
                    }
                    double ptSpeed = this.accessMode.getVelocity();
                    if (ptSpeed == 0 || Double.isNaN(ptSpeed)) {
                        ptSpeed = 1.0;
                    }
                    double beelineFactor = this.accessMode.getBeelineFactor();
                    if (Double.isNaN(beelineFactor) || beelineFactor == 0) {
                        beelineFactor = 1.4;
                    }
                    // !!!dk
                    tt += (distFrom + distTo) * beelineFactor / ptSpeed;
                }
            } else {
                // no block level
                if (useIntraTazInfo) {
                    // information about situation within traffic analysis zones are in the travel time matrices

                    if (TPS_Mode.noConnection(matrixTravelTime)) {
                        // no connection via PT!
                        return TPS_Mode.NO_CONNECTION;
                    }

                    // now only valid times! zero means walk, because there is no pt
                    // intra-cell traffic!

                    tt = beelineFaktor * matrixTravelTime;
                    //todo check the logical sense behind code block below
//                    // access time
//                    double accessTime = 0;
//                    if (this.getParameters().isDefined(ParamString.DB_NAME_MATRIX_ACCESS_PT) && simType.equals(
//                            SimulationType.SCENARIO) || this.getParameters().isDefined(
//                            ParamString.DB_NAME_MATRIX_ACCESS_PT_BASE) && simType.equals(SimulationType.BASE))
//                        accessTime = this.getParameters().paramMatrixMapClass.getValue(ParamMatrixMap.ARRIVAL_PT, startTazId,
//                                endTazId, simType, time);
//                    // egress time
//                    double egressTime = 0;
//                    if (this.getParameters().isDefined(ParamString.DB_NAME_MATRIX_EGRESS_PT) && simType.equals(
//                            SimulationType.SCENARIO) || this.getParameters().isDefined(
//                            ParamString.DB_NAME_MATRIX_EGRESS_PT_BASE) && simType.equals(SimulationType.BASE))
//                        egressTime = this.getParameters().paramMatrixMapClass.getValue(ParamMatrixMap.EGRESS_PT, startTazId,
//                                endTazId, simType, time);



                    tt += matrixAccessTime + matrixEgressTime;


                } else {
                    // no infos to situation within traffic analysis zones; using standard minimum time
                    // tt = this.getParameters().getDoubleValue(ParamValue.PT_MINIMUM_TT);
                    // Otherwise the travel time is calculated by the beeline distance and a factor and the average speed inside
                    // this traffic analysis zone.
                    double factor = start.getTrafficAnalysisZone().getSimulationTypeValues(simType)
                            .getBeelineFactorMIT();
                    if (Double.isNaN(factor) || factor == 0)//safety fix
                        factor = 1.4;
                    double avSpeed = start.getTrafficAnalysisZone().getSimulationTypeValues(simType)
                            .getAverageSpeedPT();
                    if (Double.isNaN(avSpeed) || avSpeed == 0)//safety fix
                        avSpeed = 8;
                    tt = ((beelineDistanceLoc * factor) / avSpeed);
                    //tt = -1;
                }
            }
        }
        return (tt);
    }

    /**
     * Returns the pt quality score of the block; if no score is known, the score of the traffic zone is used
     *
     * @param block the block the score is to be looked up for
     * @return the score
     */
    private int getScore(TPS_Block block) {
        int score = defaultBlockScore;

        if (useBlockLevel && block != null) {
            score = block.getScoreCat();
        }

        return score;
    }

    /**
     * Returns the pt quality score of the traffic analysis zone
     *
     * @param taz the traffic analysis zone
     * @return the score
     */
    private int getScore(TPS_TrafficAnalysisZone taz) {
        return taz.getScoreCat();
    }

}
