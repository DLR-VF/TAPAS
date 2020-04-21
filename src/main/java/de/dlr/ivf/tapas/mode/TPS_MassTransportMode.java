package de.dlr.ivf.tapas.mode;

import de.dlr.ivf.tapas.constants.TPS_ActivityConstant;
import de.dlr.ivf.tapas.constants.TPS_ActivityConstant.TPS_ActivityConstantAttribute;
import de.dlr.ivf.tapas.constants.TPS_SettlementSystem.TPS_SettlementSystemType;
import de.dlr.ivf.tapas.constants.TPS_PersonGroup.TPS_PersonType;
import de.dlr.ivf.tapas.loc.Locatable;
import de.dlr.ivf.tapas.loc.TPS_Block;
import de.dlr.ivf.tapas.loc.TPS_TrafficAnalysisZone;
import de.dlr.ivf.tapas.loc.TPS_TrafficAnalysisZone.ScenarioTypeValues;
import de.dlr.ivf.tapas.log.LogHierarchy;
import de.dlr.ivf.tapas.log.TPS_Logger;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface.HierarchyLogLevel;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface.SeverenceLogLevel;
import de.dlr.ivf.tapas.person.TPS_Car;
import de.dlr.ivf.tapas.person.TPS_Person;
import de.dlr.ivf.tapas.util.TPS_Geometrics;
import de.dlr.ivf.tapas.util.parameters.ParamFlag;
import de.dlr.ivf.tapas.util.parameters.ParamMatrix;
import de.dlr.ivf.tapas.util.parameters.ParamMatrixMap;
import de.dlr.ivf.tapas.util.parameters.ParamString;
import de.dlr.ivf.tapas.util.parameters.ParamValue;
import de.dlr.ivf.tapas.util.parameters.SimulationType;
import de.dlr.ivf.tapas.util.parameters.TPS_ParameterClass;

/**
 * This class represents modes 'pubtrans' and 'train'
 *
 * @author mark_ma
 */
@LogHierarchy(hierarchyLogLevel = HierarchyLogLevel.CLIENT)
public class TPS_MassTransportMode extends TPS_Mode {

    /**
     * Calls super constructor with name
     *
     * @param name
     * @param parameterClass parameter class reference
     */
    public TPS_MassTransportMode(String name, String[] attributes, boolean isFix, TPS_ParameterClass parameterClass) {
        super(name, attributes, isFix, parameterClass);
    }


    /*
     * (non-Javadoc)
     *
     * @see mode.TPS_Mode#getDistance(loc.tpsLocation, loc.tpsLocation, int, double)
     */
    @Override
    public double getDistance(Locatable start, Locatable end, SimulationType simType, TPS_Car car) {
        if (this.getParameters().isDefined(ParamMatrix.DISTANCES_PT)) {
            return this.getParameters().paramMatrixClass
                    .getValue(ParamMatrix.DISTANCES_PT, start.getTrafficAnalysisZone().getTAZId(),
                            end.getTrafficAnalysisZone().getTAZId());
        } else {
            double beelineDistanceLoc = TPS_Geometrics.getDistance(start, end, this.getParameters().getDoubleValue(ParamValue.MIN_DIST));
            return this.getDefaultDistance(beelineDistanceLoc);
        }
    }


    /**
     * Method to return the average numbers of interchanges between two locations.
     * The number is a double, because multiple pt-routes might have different numbers of interchanges.
     *
     * @param start   The starting location
     * @param end     The destination location
     * @param time    The starting time for this trip
     * @param simType Scenario or base
     * @return the average number of interchanges of these two locations
     */
    public double getInterchanges(Locatable start, Locatable end, int time, SimulationType simType) {
        double interChanges = 0;

        if (this.getParameters().isDefined(ParamMatrixMap.INTERCHANGES_PT)) {
            interChanges = this.getParameters().paramMatrixMapClass
                    .getValue(ParamMatrixMap.TRAVEL_TIME_PT, start.getTAZId(), end.getTAZId(), simType, time);
        }

        return interChanges;
    }

    /**
     * Returns the pt quality score of the block; if no score is known, the score of the traffic zone is used
     *
     * @param block the block the score is to be looked up for
     * @return the score
     */
    private int getScore(TPS_Block block) {
        int score = this.getParameters().getIntValue(ParamValue.DEFAULT_BLOCK_SCORE);
        if (this.getParameters().isTrue(ParamFlag.FLAG_USE_BLOCK_LEVEL)) {
            if (block != null) {
                score = block.getScoreCat();
            } else {
                if (TPS_Logger.isLogging(SeverenceLogLevel.DEBUG)) {
                    TPS_Logger.log(SeverenceLogLevel.DEBUG,
                            "No block assigned for " + this.toString() + " Using default score");
                }
            }
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

    /*
     * (non-Javadoc)
     *
     * @see mode.TPS_Mode#getTravelTime(loc.tpsLocation, loc.tpsLocation, int, double, boolean, int, int, int)
     */
    @Override
    public double getTravelTime(Locatable start, Locatable end, int time, SimulationType simType,
                                TPS_ActivityConstant actCodeFrom, TPS_ActivityConstant actCodeTo, TPS_Person person,
                                TPS_Car car) {

        double tt = -1.0; // this is the indicator, of an invalid tt -time
        int TVZfrom = start.getTrafficAnalysisZone().getTAZId();
        int TVZto = end.getTrafficAnalysisZone().getTAZId();

        double beelineDistanceLoc = TPS_Geometrics.getDistance(start, end, this.getParameters().getDoubleValue(ParamValue.MIN_DIST));
        double beelineDistanceTAZ;

        if (TVZfrom != TVZto) {
            beelineDistanceTAZ = this.getParameters().paramMatrixClass
                    .getValue(ParamMatrix.DISTANCES_BL, TVZfrom, TVZto);
        } else {
            beelineDistanceTAZ = beelineDistanceLoc;
        }

        double beelineFaktor = beelineDistanceLoc / beelineDistanceTAZ;

        if (!start.getTrafficAnalysisZone().equals(end.getTrafficAnalysisZone())) {
            // start and destination are not within the same traffic zone
            double scoreFrom = 0;
            double scoreTo = 0;


            double ttBuf = this.getParameters().paramMatrixMapClass
                    .getValue(ParamMatrixMap.TRAVEL_TIME_PT, TVZfrom, TVZto, simType, time);


            // if the pt matrix has no valid entry for the relation and the existence of a school bus should be
            // simulated
            if (TPS_Mode.noConnection(ttBuf)) {
                ttBuf = TPS_Mode.NO_CONNECTION;
                if (TPS_PersonType.PUPIL.equals(person.getPersGroup().getPersType()) &&
                        this.getParameters().isTrue(ParamFlag.FLAG_USE_SCHOOLBUS) &&
                        (actCodeFrom.hasAttribute(TPS_ActivityConstantAttribute.SCHOOL) ||
                                actCodeTo.hasAttribute(TPS_ActivityConstantAttribute.SCHOOL))) {
                    // getting average speed per combination of bbr-codes
                    int fromBBR = start.getTrafficAnalysisZone().getBbrType().getCode(TPS_SettlementSystemType.FORDCP);
                    int toBBR = end.getTrafficAnalysisZone().getBbrType().getCode(TPS_SettlementSystemType.FORDCP);
                    double schoolBusSpeed = this.getParameters().paramMatrixMapClass
                            .getValue(ParamMatrixMap.AVERAGE_SPEED_SCHOOLBUS, fromBBR, toBBR, simType, time);

                    // Assumtion: traveltime plus time for access and egress and waiting
                    // beta * beeline / schulBusSpeed + Zugang + Abgang + Wartezeit
                    tt = beelineFaktor * beelineDistanceLoc / schoolBusSpeed +
                            this.getParameters().getDoubleValue(ParamValue.DEFAULT_SCHOOL_BUS_ACCESS) +
                            this.getParameters().getDoubleValue(ParamValue.DEFAULT_SCHOOL_BUS_EGRESS) +
                            this.getParameters().getDoubleValue(ParamValue.DEFAULT_SCHOOL_BUS_WAIT);
                    if (travelTimeIsInvalid(tt)) {
                        TPS_Logger.log(SeverenceLogLevel.DEBUG,
                                "Invalid travel time detected: " + tt + " from " + TVZfrom + " to " + TVZto +
                                        " mode: " + getName());
                        tt = TPS_Mode.NO_CONNECTION;
                    }
                } else {
                    //no connection via PT!
                    tt = TPS_Mode.NO_CONNECTION;
                    //					tt= beelineDistanceLoc * ModeType.WALK.getBeelineFactor().getDoubleValue() /
					//					get(ModeType.WALK).getVelocity().getDoubleValue();
                    //					if(travelTimeIsInvalid(tt))
                    //						TPS_Logger.log(SeverenceLogLevel.FATAL, "NaN detected");
                }

            } else {
                tt = beelineFaktor * ttBuf;
                // access time
                double TT1 = 0;
                if (this.getParameters().isDefined(ParamMatrixMap.ARRIVAL_PT))
                    TT1 = this.getParameters().paramMatrixMapClass
                            .getValue(ParamMatrixMap.ARRIVAL_PT, TVZfrom, TVZto, simType, time);
                // egress time
                double TT2 = 0;
                if (this.getParameters().isDefined(ParamMatrixMap.EGRESS_PT))
                    TT2 = this.getParameters().paramMatrixMapClass
                            .getValue(ParamMatrixMap.EGRESS_PT, TVZfrom, TVZto, simType, time);

                if (this.getParameters().isTrue(ParamFlag.FLAG_USE_BLOCK_LEVEL)) {
                    scoreFrom = this.getScore(start.getTrafficAnalysisZone()) - this.getScore(start.getBlock());
                    scoreTo = this.getScore(end.getTrafficAnalysisZone()) - this.getScore(end.getBlock());
                    //travel time
                    tt *= 1.0 + ((scoreFrom + scoreTo) * this.getParameters().getDoubleValue(ParamValue.PT_TT_FACTOR));
                    // access time
                    TT1 *= 1.0 + (scoreFrom * this.getParameters().getDoubleValue(ParamValue.PT_ACCESS_FACTOR));
                    // egress time
                    TT2 *= 1.0 + (scoreTo * this.getParameters().getDoubleValue(ParamValue.PT_EGRESS_FACTOR));
                }

                tt += TT1 + TT2;
                if (travelTimeIsInvalid(tt)) {
                    TPS_Logger.log(SeverenceLogLevel.DEBUG,
                            "Invalid travel time detected: " + tt + " from " + TVZfrom + " to " + TVZto + " mode: " +
                                    getName());
                    tt = TPS_Mode.NO_CONNECTION;
                }
            }
        } else {
            if (!start.getTrafficAnalysisZone().getSimulationTypeValues(simType).isIntraPTTrafficAllowed()) {
                //no intra traffic allowed!
                return TPS_Mode.NO_CONNECTION;
            }

            // start and destination within the same zone
            if (this.getParameters().isTrue(ParamFlag.FLAG_USE_BLOCK_LEVEL)) {
                if ((start.hasBlock() && start.getBlock().equals(end.getBlock())) ||
                        (!start.hasBlock() && !end.hasBlock())) {
                    // start and destination within the same block or no block information: No valid tt-time!
                    // tt = this.getParameters().getDoubleValue(ParamValue.PT_MINIMUM_TT);
                } else {
                    // start and destination not within the same block
                    if (!this.getParameters().isTrue(ParamFlag.FLAG_INTRA_INFOS_MATRIX)) {
                        ScenarioTypeValues stv = start.getTrafficAnalysisZone().getSimulationTypeValues(simType);
                        double bla0 = stv.getAverageSpeedPT();
                        if (Double.isNaN(bla0))//safety fix
                            bla0 = 8;
                        tt = beelineFaktor * beelineDistanceLoc / bla0;
                    } else {
                        double bla1 = this.getParameters().getDoubleValue(get(ModeType.PT).getVelocity());
                        if (Double.isNaN(bla1))//safety fix
                            bla1 = 8;
                        tt = beelineFaktor * beelineDistanceLoc / bla1;
                    }

                    // access distance to pt
                    double distFrom = start.hasBlock() ? start.getBlock().getNearestPubTransStop() : this
                            .getParameters().getDoubleValue(ParamValue.AVERAGE_DISTANCE_PT_STOP);

                    // egress distance to pt
                    double distTo = end.hasBlock() ? end.getBlock().getNearestPubTransStop() : this.getParameters()
                            .getDoubleValue(ParamValue.AVERAGE_DISTANCE_PT_STOP);

                    // !!!dk
                    if (distTo < 0) {
                        distTo = 0;
                    }
                    double ptSpeed = this.getParameters().getDoubleValue(get(ModeType.WALK).getVelocity());
                    if (ptSpeed == 0 || Double.isNaN(ptSpeed)) {
                        ptSpeed = 1.0;
                    }
                    double beelineFactor = this.getParameters().getDoubleValue(ModeType.WALK.getBeelineFactor());
                    if (Double.isNaN(beelineFactor) || beelineFactor == 0) {
                        beelineFactor = 1.4;
                    }
                    // !!!dk
                    tt += (distFrom + distTo) * beelineFactor / ptSpeed;
                    if (travelTimeIsInvalid(tt)) {
                        TPS_Logger.log(SeverenceLogLevel.DEBUG,
                                "Invalid travel time detected: " + tt + " from " + TVZfrom + " to " + TVZto +
                                        " mode: " + getName());
                        tt = TPS_Mode.NO_CONNECTION;
                    }
                }
            } else {
                // no block level
                if (this.getParameters().isTrue(ParamFlag.FLAG_INTRA_INFOS_MATRIX)) {
                    // information about situation within traffic analysis zones are in the travel time matrices
                    double ttBuf = 0;
                    ttBuf = this.getParameters().paramMatrixMapClass
                            .getValue(ParamMatrixMap.TRAVEL_TIME_PT, TVZfrom, TVZto, simType, time);

                    if (TPS_Mode.noConnection(ttBuf)) {
                        // no connection via PT!
                        return TPS_Mode.NO_CONNECTION;
                    }

                    // now only valid times! zero means walk, because there is no pt
                    // intra-cell trafic!

                    tt = beelineFaktor * ttBuf;
                    // access time
                    double TT1 = 0;
                    if (this.getParameters().isDefined(ParamString.DB_NAME_MATRIX_ACCESS_PT) &&
                            simType.equals(SimulationType.SCENARIO) ||
                            this.getParameters().isDefined(ParamString.DB_NAME_MATRIX_ACCESS_PT_BASE) &&
                                    simType.equals(SimulationType.BASE))
                        TT1 = this.getParameters().paramMatrixMapClass
                                .getValue(ParamMatrixMap.ARRIVAL_PT, TVZfrom, TVZto, simType, time);
                    // egress time
                    double TT2 = 0;
                    if (this.getParameters().isDefined(ParamString.DB_NAME_MATRIX_EGRESS_PT) &&
                            simType.equals(SimulationType.SCENARIO) ||
                            this.getParameters().isDefined(ParamString.DB_NAME_MATRIX_EGRESS_PT_BASE) &&
                                    simType.equals(SimulationType.BASE))
                        TT2 = this.getParameters().paramMatrixMapClass
                                .getValue(ParamMatrixMap.EGRESS_PT, TVZfrom, TVZto, simType, time);

                    tt += TT1 + TT2;
                    if (travelTimeIsInvalid(tt)) {
                        TPS_Logger.log(SeverenceLogLevel.DEBUG,
                                "Invalid travel time detected: " + tt + " from " + TVZfrom + " to " + TVZto +
                                        " mode: " + getName());
                        tt = TPS_Mode.NO_CONNECTION;
                    }

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
}
