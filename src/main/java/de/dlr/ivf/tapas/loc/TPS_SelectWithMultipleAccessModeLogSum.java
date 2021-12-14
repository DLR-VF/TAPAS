/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.loc;

import de.dlr.ivf.tapas.constants.TPS_ActivityConstant;
import de.dlr.ivf.tapas.constants.TPS_ActivityConstant.TPS_ActivityCodeType;
import de.dlr.ivf.tapas.constants.TPS_Distance;
import de.dlr.ivf.tapas.constants.TPS_Distance.TPS_DistanceCodeType;
import de.dlr.ivf.tapas.constants.TPS_SettlementSystem;
import de.dlr.ivf.tapas.distribution.TPS_DiscreteDistribution;
import de.dlr.ivf.tapas.log.TPS_Logger;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface.SeverenceLogLevel;
import de.dlr.ivf.tapas.mode.TPS_Mode;
import de.dlr.ivf.tapas.mode.TPS_Mode.ModeType;
import de.dlr.ivf.tapas.mode.TPS_ModeChoiceContext;
import de.dlr.ivf.tapas.mode.TPS_ModeDistribution;
import de.dlr.ivf.tapas.modechoice.TPS_UtilityFunction;
import de.dlr.ivf.tapas.modechoice.TPS_UtilityMNL;
import de.dlr.ivf.tapas.persistence.TPS_RegionResultSet;
import de.dlr.ivf.tapas.persistence.TPS_RegionResultSet.Result;
import de.dlr.ivf.tapas.plan.TPS_LocatedStay;
import de.dlr.ivf.tapas.plan.TPS_Plan;
import de.dlr.ivf.tapas.plan.TPS_PlanningContext;
import de.dlr.ivf.tapas.scheme.TPS_Stay;
import de.dlr.ivf.tapas.scheme.TPS_TourPart;
import de.dlr.ivf.tapas.util.Randomizer;
import de.dlr.ivf.tapas.util.TPS_AttributeReader.TPS_Attribute;
import de.dlr.ivf.tapas.util.TPS_FastMath;
import de.dlr.ivf.tapas.util.parameters.ParamFlag;
import de.dlr.ivf.tapas.util.parameters.ParamValue;
import de.dlr.ivf.tapas.util.parameters.SimulationType;
import de.dlr.ivf.tapas.util.parameters.TPS_ParameterClass;

import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Supplier;

public class TPS_SelectWithMultipleAccessModeLogSum extends TPS_SelectWithMultipleAccessMode {


    public WeightedResult createLocationOption(Result result, double travelTime, double parameter) {
        return new LogSumWeightedResult(result, travelTime, parameter);
    }

    /**
     * Method to calculate the expected log sum to the locRepresentant.
     *
     * @param plan
     * @param pc
     * @param prevMCC
     * @param nextMCC
     * @param taz
     * @return
     */
    public Double getLogSum(TPS_Plan plan, TPS_PlanningContext pc, TPS_ModeChoiceContext prevMCC, TPS_ModeChoiceContext nextMCC, TPS_TrafficAnalysisZone taz, TPS_ParameterClass parameterClass, double mu) {

        Double logSum = (double) 0;
        double arrModeLogSum, depModeLogSum;


        // The WALK-mode is used to get distances on the net.
        double distanceNetTo = Math.max(parameterClass.getDoubleValue(ParamValue.MIN_DIST), TPS_Mode.get(ModeType.WALK)
                                                                                                    .getDistance(
                                                                                                            prevMCC.fromStayLocation,
                                                                                                            prevMCC.toStayLocation,
                                                                                                            SimulationType.SCENARIO,
                                                                                                            null));
        double distanceNetFrom = Math.max(parameterClass.getDoubleValue(ParamValue.MIN_DIST),
                TPS_Mode.get(ModeType.WALK)
                        .getDistance(nextMCC.fromStayLocation, nextMCC.toStayLocation, SimulationType.SCENARIO, null));

        arrModeLogSum = this.getModeLogSum(plan, pc, taz, distanceNetFrom, distanceNetFrom + distanceNetTo, prevMCC,
                parameterClass, mu);
        depModeLogSum = this.getModeLogSum(plan, pc, taz, distanceNetTo, distanceNetFrom + distanceNetTo, nextMCC,
                parameterClass, mu);

        if (arrModeLogSum > 0 && depModeLogSum > 0) {
            logSum = Math.log(arrModeLogSum + depModeLogSum) / mu;

            return logSum;
        } else {
            return Double.NaN; //not reachable!
        }
    }

    public double getModeLogSum(TPS_Plan plan, TPS_PlanningContext pc, TPS_TrafficAnalysisZone taz, double distanceNet, double distanceNetInclReturn, TPS_ModeChoiceContext mcc, TPS_ParameterClass parameterClass, double mu) {
        // getting the distribution of modes from the mode choice tree according to the attributes of the plan
        plan.setAttributeValue(TPS_Attribute.CURRENT_DISTANCE_CLASS_CODE_MCT,
                TPS_Distance.getCode(TPS_DistanceCodeType.MCT, distanceNet));
        //these attributes need to be set here again, because it is not guarantied, that they are set elsewhere
        TPS_ActivityConstant currentActCode = mcc.toStay.getActCode();
        plan.setAttributeValue(TPS_Attribute.CURRENT_EPISODE_ACTIVITY_CODE_MCT,
                currentActCode.getCode(TPS_ActivityCodeType.MCT));
        plan.setAttributeValue(TPS_Attribute.CURRENT_EPISODE_ACTIVITY_CODE_VOT,
                currentActCode.getCode(TPS_ActivityCodeType.VOT));
        plan.setAttributeValue(TPS_Attribute.CURRENT_EPISODE_ACTIVITY_CODE_TAPAS,
                currentActCode.getCode(TPS_ActivityCodeType.TAPAS));

        // init

        TPS_DiscreteDistribution<TPS_Mode> dist = TPS_ModeDistribution.getDistribution(null);

        double[] utilities = new double[dist.size()];
        double sumOfUtilities = 0;
        // calculate utilities
        for (int i = 0; i < utilities.length; ++i) {
            utilities[i] = 0; //old habbit: init the value...
            // get the parameter set
            TPS_Mode mode = TPS_Mode.get(TPS_Mode.MODE_TYPE_ARRAY[i]);
            if (!mcc.isBikeAvailable && mode.isType(ModeType.BIKE) || //no bike
                    mcc.carForThisPlan == null && mode.isType(ModeType.MIT) || //no car
                    (mode.isType(ModeType.TAXI) && mode.getParameters().isFalse(ParamFlag.FLAG_USE_TAXI)) //disable TAXI
            ) {
                utilities[i] = TPS_UtilityFunction.minModeProbability;
                continue;
            }
            //travel time
            double travelTime = mode.getTravelTime(mcc.fromStayLocation, mcc.toStayLocation, mcc.startTime,
                    SimulationType.SCENARIO, TPS_ActivityConstant.DUMMY, TPS_ActivityConstant.DUMMY, plan.getPerson(),
                    mcc.carForThisPlan);
            if (TPS_Mode.noConnection(travelTime)) { //no connection
                utilities[i] = TPS_UtilityFunction.minModeProbability;
                continue;
            }
            //calc the value
            utilities[i] = ((TPS_UtilityMNL) (TPS_Mode.getUtilityFunction())).getCostOfMode(mode, plan,
                    travelTime, mcc, SimulationType.SCENARIO);
            if (Double.isNaN(utilities[i])) {
                utilities[i] = TPS_UtilityFunction.minModeProbability;
                continue;
            }
            // restricted cars will not enter restricted areas
            if (pc.carForThisPlan != null && pc.carForThisPlan.isRestricted() && taz.isRestricted() && mode.isType(
                    ModeType.MIT)) {
                continue;
            }

            //too long trips get capped for walk
            if (mode.isType(ModeType.BIKE) && distanceNetInclReturn > parameterClass.getIntValue(
                    ParamValue.MAX_WALK_DIST)) {
                continue;
            }


            utilities[i] = TPS_FastMath.exp(mu * utilities[i]);

            sumOfUtilities += utilities[i];
        }

        if (sumOfUtilities > 0) {
            return sumOfUtilities;
        }
        return -1;
    }

    @Override
    public TPS_Location selectLocationFromChoiceSet(TPS_RegionResultSet choiceSet, TPS_Plan plan, TPS_PlanningContext pc, TPS_LocatedStay locatedStay, Supplier<TPS_Stay> coming_from, Supplier<TPS_Stay> going_to) {
        if (this.PM == null) {
            TPS_Logger.log(SeverenceLogLevel.FATAL,
                    "TPS_LocationSelectModel not properly initialized! Persistance manager is null?!?! Driving home!");
            return plan.getPerson().getHousehold().getLocation();
        }
        if (this.region == null) {
            TPS_Logger.log(SeverenceLogLevel.FATAL,
                    "TPS_LocationSelectModel not properly initialized! Region is null?!?! Driving home!");
            return plan.getPerson().getHousehold().getLocation();
        }

        if (!(TPS_Mode.getUtilityFunction() instanceof TPS_UtilityMNL)) { // no LogSum for non MNL-Models!
            super.selectLocationFromChoiceSet(choiceSet, plan, pc, locatedStay,coming_from,going_to);
        }


        TPS_Stay stay = locatedStay.getStay();
        TPS_ActivityConstant actCode = stay.getActCode();
        TPS_TourPart tourpart = (TPS_TourPart) locatedStay.getStay().getSchemePart();
        TPS_Stay comingFrom = coming_from.get();
        TPS_Stay goingTo = going_to.get();
        TPS_Location locComingFrom = plan.getLocatedStay(comingFrom).getLocation();
        TPS_Location locGoingTo = plan.getLocatedStay(goingTo).getLocation();
        TPS_SettlementSystem regionType = locComingFrom.getTrafficAnalysisZone().getBbrType();
        double muScale = this.PM.getParameters().getDoubleValue(
                ParamValue.LOGSUM_WEIGHT_MU); // should be 1 if the logsums are in a normal range and not completely out of range
        double muInteract = this.PM.getParameters().getDoubleValue(
                ParamValue.LOGSUM_INTERACT_MU); // this should be estimated in the logit model

        double calibFactor = 1;
        switch (actCode.getCode(TPS_ActivityCodeType.TAPAS)) {
            case 0: //free time home
                calibFactor = this.PM.getParameters().getDoubleValue(ParamValue.LOGSUM_CALIB_FREETIME_HOME);
                break;
            case 1: //work
                calibFactor = this.PM.getParameters().getDoubleValue(ParamValue.LOGSUM_CALIB_WORK);
                break;
            case 2: //education
                calibFactor = this.PM.getParameters().getDoubleValue(ParamValue.LOGSUM_CALIB_EDUCATION);
                break;
            case 3: //shopping
                calibFactor = this.PM.getParameters().getDoubleValue(ParamValue.LOGSUM_CALIB_SHOP);
                break;
            case 4: //private matters
                calibFactor = this.PM.getParameters().getDoubleValue(ParamValue.LOGSUM_CALIB_ERRANT);
                break;
            case 5: //free time
                calibFactor = this.PM.getParameters().getDoubleValue(ParamValue.LOGSUM_CALIB_FREETIME);
                break;
            case 6: //misc
                calibFactor = this.PM.getParameters().getDoubleValue(ParamValue.LOGSUM_CALIB_MISC);
                break;
            case 7: //university
                calibFactor = this.PM.getParameters().getDoubleValue(ParamValue.LOGSUM_CALIB_STUDENT);
                break;
            default:
                calibFactor = 1;
                break;
        }
        muInteract *= calibFactor;

        // different cnf4-params according to the type of trip
        double cfn4 = region.getCfn().getCFN4Value(regionType, actCode);
        double cnfX = region.getCfn().getCFNXValue(regionType);
        double param = 1;
        TPS_CFN pot = region.getPotential();
        if(pot!=null){
            double val = pot.getParamValue(regionType, actCode);
            if (val>=0)
                param = val;
        };

        SortedSet<WeightedResult> weightedChoiceSet = new TreeSet<>();
        TPS_Location locRepresentant = null;
        double sumWeight = 0;
        for (Result result : choiceSet.getResultIterable()) {
            if (result.sumWeight <= 0) continue;
            // Draw a location at random. This location represents the zone for this round.
            TPS_TrafficAnalysisZone taz = result.taz;
            locRepresentant = result.loc;
            //here we can switch between different travel time models
            TPS_ModeChoiceContext prevMCC = new TPS_ModeChoiceContext();
            prevMCC.isBikeAvailable = pc.isBikeAvailable;
            prevMCC.carForThisPlan = pc.carForThisPlan;
            prevMCC.duration = stay.getOriginalDuration();
            prevMCC.startTime = stay.getOriginalStart();
            prevMCC.fromStayLocation = locComingFrom;
            prevMCC.fromStay = comingFrom;
            prevMCC.toStayLocation = locRepresentant;
            prevMCC.toStay = stay;
            TPS_ModeChoiceContext nextMCC = new TPS_ModeChoiceContext();
            nextMCC.isBikeAvailable = pc.isBikeAvailable;
            nextMCC.carForThisPlan = pc.carForThisPlan;
            nextMCC.duration = stay.getOriginalDuration();
            nextMCC.startTime = stay.getOriginalStart();
            nextMCC.fromStayLocation = locRepresentant;
            nextMCC.fromStay = stay;
            nextMCC.toStayLocation = locGoingTo;
            nextMCC.toStay = goingTo;

//			//we need the beeline for calibration
//			Double beeLineDist = this.PM.getParameters().paramMatrixClass.getValue(ParamMatrix.DISTANCES_BL, locComingFrom.getTAZId(), taz.getTAZId())+
//								 this.PM.getParameters().paramMatrixClass.getValue(ParamMatrix.DISTANCES_BL, taz.getTAZId(), locGoingTo.getTAZId())	;

            Double weightedTT = this.getLogSum(plan, pc, prevMCC, nextMCC, taz, this.PM.getParameters(), muInteract);
            if (!weightedTT.isNaN()) {
                weightedTT = TPS_FastMath.exp(muScale * weightedTT); //use the activity based µ here..
                //here we can switch between different Opportunity-weighting
                WeightedResult weightedResult = this.createLocationOption(result, weightedTT, param);
                //now the calib the weight with the distance
                //weightedResult.result.sumWeight *= calibFactor/beeLineDist;

                sumWeight += weightedResult.getAdaptedWeight();
                weightedChoiceSet.add(weightedResult);
            }
        }


        if (weightedChoiceSet.size() > 0) {
            double rand = Randomizer.random(); //uniform distribution from 0 to 1
            double posMicro = rand;

            if (this.PM.getParameters().isDefined(ParamValue.GAMMA_LOCATION_WEIGHT)) {
                posMicro = Math.pow(posMicro, this.PM.getParameters().getDoubleValue(ParamValue.GAMMA_LOCATION_WEIGHT));
            }

            posMicro *= sumWeight; // norm to max weight
            posMicro *= cnfX; //apply region-specific restiction factor
            posMicro *= cfn4;  // apply activity based restiction factor

            double weightPos = 0;
            for (WeightedResult entry : weightedChoiceSet) {
                weightPos += entry.getAdaptedWeight();
                if (weightPos >= posMicro) {
                    locRepresentant = entry.result.loc;
                    break;
                }
            }
        } else {
            TPS_Logger.log(SeverenceLogLevel.WARN,
                    "No specific location found for activity " + actCode.getCode(TPS_ActivityCodeType.ZBE));
            return region.selectDefaultLocation(plan, pc, locatedStay,coming_from,going_to);
        }
        return locRepresentant;
    }

    class LogSumWeightedResult extends WeightedResult {
        public LogSumWeightedResult(Result result, double travelTime, double param) {
            super(result, travelTime, param);
        }

        @Override
        /**
         * The most attractive result has to be in front -> Descending Logsums
         */ public int compareTo(WeightedResult arg0) {
            return arg0.travelTime.compareTo(travelTime);
        }

        /**
         * For log sum: the capacity multiplied by the logsum
         *
         * @return
         */
        public Double getAdaptedWeight() {
            return this.result.sumWeight * travelTime;
        }
    }
}
