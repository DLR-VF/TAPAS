package de.dlr.ivf.tapas.choice;

import de.dlr.ivf.tapas.choice.distance.MatrixFunction;
import de.dlr.ivf.tapas.choice.distance.functions.SimpleMatrixDistanceFunction;
import de.dlr.ivf.tapas.choice.traveltime.MatrixMapFunction;
import de.dlr.ivf.tapas.choice.traveltime.functions.*;
import de.dlr.ivf.tapas.model.MatrixMap;
import de.dlr.ivf.tapas.model.constants.TPS_ActivityConstant;
import de.dlr.ivf.tapas.model.constants.TPS_ActivityConstant.TPS_ActivityConstantAttribute;
import de.dlr.ivf.tapas.model.location.Locatable;
import de.dlr.ivf.tapas.model.mode.TPS_Mode;
import de.dlr.ivf.tapas.model.mode.TPS_Mode.ModeType;
import de.dlr.ivf.tapas.model.parameter.*;
import de.dlr.ivf.tapas.model.vehicle.TPS_Car;
import de.dlr.ivf.tapas.model.person.TPS_Person;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import static de.dlr.ivf.tapas.model.mode.TPS_Mode.ModeType.PT;

/**
 * temporary class during refactoring replacing getTravelTime function in TPS_Mode and its implementations.
 *
 * @author Alain Schengen
 */
public class TravelTimeCalculator {

    private final Map<TPS_Mode, MatrixMapFunction> modeTravelTimeFunctions;
    private final EnumMap<ModeType, MatrixMapFunction> altModeTravelTimeFunctions;
    private final EnumMap<ModeType, MatrixMapFunction> intraTazTravelTimeFunctions;

    private final double minDist;

    private final boolean useSchoolBus;
    private final EnumMap<ModeType, TPS_Mode> modeMap;
    private final MatrixMapFunction schoolBusTravelTimeFunction;
    private final int automaticVehicleLevel;
    private final double automaticVehicleRampUp;
    private final SimulationType simulationType;
    private final MatrixFunction beelineDistanceFunction;
    private final int vehicleTimeModThreshhold;
    private final double vehicleTimeModFar;
    private final double vehicleTimeModNear;
    private final int automaticValetParking;
    private final AvTravelTimeAdjustment avAccessEgressAdjustment;


    public TravelTimeCalculator(TPS_ParameterClass parameterClass, EnumMap<ModeType, TPS_Mode> modeMap){

        this.modeTravelTimeFunctions = new HashMap<>();
        this.altModeTravelTimeFunctions = new EnumMap<>(ModeType.class);
        this.intraTazTravelTimeFunctions = new EnumMap<>(ModeType.class);
        this.useSchoolBus = parameterClass.isTrue(ParamFlag.FLAG_USE_SCHOOLBUS);
        this.minDist = parameterClass.getDoubleValue(ParamValue.MIN_DIST);
        this.modeMap = modeMap;
        this.automaticVehicleLevel = parameterClass.getIntValue(ParamValue.AUTOMATIC_VEHICLE_LEVEL);
        this.automaticVehicleRampUp = parameterClass.getDoubleValue(ParamValue.AUTOMATIC_VEHICLE_RAMP_UP_TIME);
        this.automaticValetParking = parameterClass.getIntValue(ParamValue.AUTOMATIC_VALET_PARKING);
        this.simulationType = parameterClass.getSimulationType();
        this.beelineDistanceFunction = new SimpleMatrixDistanceFunction(parameterClass.getMatrix(ParamMatrix.DISTANCES_BL));
        this.vehicleTimeModThreshhold = parameterClass.getIntValue(ParamValue.AUTOMATIC_VEHICLE_TIME_MOD_THRESHOLD);
        this.vehicleTimeModFar = parameterClass.getDoubleValue(ParamValue.AUTOMATIC_VEHICLE_TIME_MOD_FAR);
        this.vehicleTimeModNear = parameterClass.getDoubleValue(ParamValue.AUTOMATIC_VEHICLE_TIME_MOD_NEAR);
        this.avAccessEgressAdjustment = new AvTravelTimeAdjustment(parameterClass);

        //init school bus function
        this.schoolBusTravelTimeFunction = new SchoolBusTravelTimeFunction(parameterClass);

        //init travel time functions for all modes
        init(parameterClass);
    }

    private void init(TPS_ParameterClass parameterClass){


        //pt
        TPS_Mode modeWalk = modeMap.get(ModeType.WALK);
        TPS_Mode modePt = modeMap.get(PT);
        MatrixMapFunction ptTtFunction = new PtTravelTimeFunction(parameterClass,modePt, modeWalk);
        modeTravelTimeFunctions.put(modePt, ptTtFunction);

        //walk
        MatrixMap walkTtMatrixMap = parameterClass.paramMatrixMapClass.getMatrixMap(ParamMatrixMap.TRAVEL_TIME_WALK, parameterClass.getSimulationType());
        MatrixMap walkAccessMatrixMap = parameterClass.paramMatrixMapClass.getMatrixMap(ParamMatrixMap.ARRIVAL_WALK, parameterClass.getSimulationType());
        MatrixMap walkEgressMatrixMap = parameterClass.paramMatrixMapClass.getMatrixMap(ParamMatrixMap.EGRESS_WALK, parameterClass.getSimulationType());
        MatrixMapFunction walkTtFunction = new WalkBikeTravelTimeFunction(walkTtMatrixMap,walkAccessMatrixMap,walkEgressMatrixMap,parameterClass,modeWalk);
        modeTravelTimeFunctions.put(modeWalk, walkTtFunction);

        //bike
        TPS_Mode modeBike = modeMap.get(ModeType.BIKE);
        MatrixMap bikeTtMatrixMap = parameterClass.paramMatrixMapClass.getMatrixMap(ParamMatrixMap.TRAVEL_TIME_BIKE, parameterClass.getSimulationType());
        MatrixMap bikeAccessMatrixMap = parameterClass.paramMatrixMapClass.getMatrixMap(ParamMatrixMap.ARRIVAL_BIKE, parameterClass.getSimulationType());
        MatrixMap bikeEgressMatrixMap = parameterClass.paramMatrixMapClass.getMatrixMap(ParamMatrixMap.EGRESS_BIKE, parameterClass.getSimulationType());
        MatrixMapFunction bikeTtFunction = new WalkBikeTravelTimeFunction(bikeTtMatrixMap,bikeAccessMatrixMap,bikeEgressMatrixMap,parameterClass, modeBike);
        modeTravelTimeFunctions.put(modeBike, bikeTtFunction);

        //MIT - MIT_PASS - CAR_SHARING - TAXI
        MatrixMapFunction mitTravelTimeFunction = new MitTravelTimeFunction(parameterClass, modeWalk.getVelocity());
        modeTravelTimeFunctions.put(modeMap.get(ModeType.MIT), mitTravelTimeFunction);
        modeTravelTimeFunctions.put(modeMap.get(ModeType.MIT_PASS),mitTravelTimeFunction);
        modeTravelTimeFunctions.put(modeMap.get(ModeType.TAXI), mitTravelTimeFunction);
        modeTravelTimeFunctions.put(modeMap.get(ModeType.CAR_SHARING), mitTravelTimeFunction);


    }

    // signature matches getTravelTime from TPS_Mode
    public double getTravelTime(TPS_Mode mode, Locatable start, Locatable end, int time, TPS_ActivityConstant actCodeFrom, TPS_ActivityConstant actCodeTo, TPS_Person person, TPS_Car car){

        double travelTime = modeTravelTimeFunctions.get(mode).apply(start, end, time);

        ModeType modeType = mode.getModeType();
        if(travelTimeIsInvalid(travelTime)) {
            //extracted from TPS_MassTransportMode
            if (modeType == PT && person.isPupil() && useSchoolBus &&
                    (actCodeFrom.hasAttribute(TPS_ActivityConstantAttribute.SCHOOL)
                            || actCodeTo.hasAttribute(TPS_ActivityConstantAttribute.SCHOOL))
            ) {
                return schoolBusTravelTimeFunction.apply(start, end, time);
            }
            return travelTime;
        }

        //extracted from TPS_IndividualTransportMode
        if (car != null){
            int automationLevel = car.getAutomationLevel();
            if(automationLevel >= automaticVehicleLevel && this.simulationType == SimulationType.SCENARIO) {
                //calculate time perception modification

                if (travelTime > this.automaticVehicleRampUp) {
                    double distanceNet = beelineDistanceFunction.apply(start, end);

                    double timeMod = distanceNet > vehicleTimeModThreshhold ? vehicleTimeModFar : vehicleTimeModNear;

                    travelTime = automaticVehicleRampUp + (timeMod * (travelTime - automaticVehicleRampUp));
                }
            }
            if(automationLevel >= automaticValetParking){
                travelTime += avAccessEgressAdjustment.adjustAvAccessEgress(start.getTAZId(),end.getTAZId(),time);
            }
        }
        return travelTime;
    }

    /**
     * Extracted from TPS_Mode
     * Method to check if the travel time is in a valid range
     *
     * @param tt travel time to be checked
     * @return true if tt is a valid travel time
     */
    private boolean travelTimeIsInvalid(double tt) {
        boolean returnValue = Double.isNaN(tt) || Double.isInfinite(tt);
        if (!returnValue && (tt < 0.0 || tt >= 100000.0)) { // positive and less than a day + x
            returnValue = true;
        }
        return returnValue;
    }



}
