package de.dlr.ivf.tapas.choice;

import de.dlr.ivf.tapas.model.MatrixMap;
import de.dlr.ivf.tapas.model.TPS_Geometrics;
import de.dlr.ivf.tapas.model.constants.TPS_ActivityConstant;
import de.dlr.ivf.tapas.model.constants.TPS_ActivityConstant.TPS_ActivityConstantAttribute;
import de.dlr.ivf.tapas.model.constants.TPS_PersonType;
import de.dlr.ivf.tapas.model.location.Locatable;
import de.dlr.ivf.tapas.model.mode.TPS_Mode;
import de.dlr.ivf.tapas.model.mode.TPS_Mode.ModeType;
import de.dlr.ivf.tapas.model.parameter.*;
import de.dlr.ivf.tapas.model.person.TPS_Car;
import de.dlr.ivf.tapas.model.person.TPS_Person;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class TravelTimeCalculator {

    private final Map<TPS_Mode, TravelTimeFunction> modeTravelTimeFunctions;
    private final EnumMap<ModeType, TravelTimeFunction> altModeTravelTimeFunctions;
    private final EnumMap<ModeType, TravelTimeFunction> intraTazTravelTimeFunctions;

    private final double minDist;

    private final boolean useSchoolBus;
    private final EnumMap<ModeType, TPS_Mode> modeMap;

    public TravelTimeCalculator(TPS_ParameterClass parameterClass, EnumMap<ModeType, TPS_Mode> modeMap){

        this.modeTravelTimeFunctions = new HashMap<>();
        this.altModeTravelTimeFunctions = new EnumMap<>(ModeType.class);
        this.intraTazTravelTimeFunctions = new EnumMap<>(ModeType.class);
        this.useSchoolBus = parameterClass.isTrue(ParamFlag.FLAG_USE_SCHOOLBUS);
        this.minDist = parameterClass.getDoubleValue(ParamValue.MIN_DIST);
        this.modeMap = modeMap;


        init(parameterClass);
    }

    private void init(TPS_ParameterClass parameterClass){

        //pt
        TPS_Mode modeWalk = modeMap.get(ModeType.WALK);
        TPS_Mode modePt = modeMap.get(ModeType.PT);
        TravelTimeFunction ptTtFunction = new PtTravelTimeFunction(parameterClass,modePt, modeWalk);
        modeTravelTimeFunctions.put(modePt, ptTtFunction);

        //walk
        MatrixMap walkTtMatrixMap = parameterClass.paramMatrixMapClass.getMatrixMap(ParamMatrixMap.TRAVEL_TIME_WALK, parameterClass.getSimulationType());
        MatrixMap walkAccessMatrixMap = parameterClass.paramMatrixMapClass.getMatrixMap(ParamMatrixMap.ARRIVAL_WALK, parameterClass.getSimulationType());
        MatrixMap walkEgressMatrixMap = parameterClass.paramMatrixMapClass.getMatrixMap(ParamMatrixMap.EGRESS_WALK, parameterClass.getSimulationType());
        SimpleMatrixTravelTimeFunction walkTtFunction = new SimpleMatrixTravelTimeFunction(walkTtMatrixMap,walkAccessMatrixMap,walkEgressMatrixMap,parameterClass,modeWalk);
        modeTravelTimeFunctions.put(modeWalk, walkTtFunction);

        //bike
        TPS_Mode modeBike = modeMap.get(ModeType.BIKE);
        MatrixMap bikeTtMatrixMap = parameterClass.paramMatrixMapClass.getMatrixMap(ParamMatrixMap.TRAVEL_TIME_BIKE, parameterClass.getSimulationType());
        MatrixMap bikeAccessMatrixMap = parameterClass.paramMatrixMapClass.getMatrixMap(ParamMatrixMap.ARRIVAL_BIKE, parameterClass.getSimulationType());
        MatrixMap bikeEgressMatrixMap = parameterClass.paramMatrixMapClass.getMatrixMap(ParamMatrixMap.EGRESS_BIKE, parameterClass.getSimulationType());
        SimpleMatrixTravelTimeFunction bikeTtFunction = new SimpleMatrixTravelTimeFunction(bikeTtMatrixMap,bikeAccessMatrixMap,bikeEgressMatrixMap,parameterClass, modeBike);
        modeTravelTimeFunctions.put(modeBike, bikeTtFunction);

        //MIT - MIT_PASS - CAR_SHARING - TAXI
        MatrixMap mitMatrixMap = parameterClass.paramMatrixMapClass.getMatrixMap(ParamMatrixMap.TRAVEL_TIME_MIT, parameterClass.getSimulationType());

    }

    public double getTravelTime(TPS_Mode mode, Locatable start, Locatable end, int time, TPS_ActivityConstant actCodeFrom, TPS_ActivityConstant actCodeTo, TPS_Person person, TPS_Car car){

        double beelineDistance = TPS_Geometrics.getDistance(start, end, minDist);

        ModeType modeType = mode.getModeType();

        if(modeType == ModeType.PT && person.isPupil() && useSchoolBus &&
                (actCodeFrom.hasAttribute(TPS_ActivityConstantAttribute.SCHOOL)
                || actCodeTo.hasAttribute(TPS_ActivityConstantAttribute.SCHOOL))
        ){
            altModeTravelTimeFunctions.get(modeType).apply(start,end,time);
        }else{
            modeTravelTimeFunctions.get(modeType).apply(start,end,time);
        }


        return 0;
    }

    /**
     * Method to check if the travel time is in a valid range
     *
     * @param tt travel time to be checked
     * @return true if tt is a valid travel time
     */
    boolean travelTimeIsInvalid(double tt) {
        boolean returnValue = Double.isNaN(tt) || Double.isInfinite(tt);
        if (!returnValue && (tt < 0.0 || tt >= 100000.0)) { // positive and less than a day + x
            returnValue = true;
        }
        return returnValue;
    }



}
