/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.legacy;

import de.dlr.ivf.tapas.choice.distance.providers.ModeMatrixDistanceProvider;
import de.dlr.ivf.tapas.choice.traveltime.providers.TravelTimeCalculator;
import de.dlr.ivf.tapas.mode.ModeDistributionCalculator;
import de.dlr.ivf.tapas.model.distribution.TPS_DiscreteDistribution;
import de.dlr.ivf.tapas.model.location.TPS_TrafficAnalysisZone;
import de.dlr.ivf.tapas.model.mode.TPS_Mode;
import de.dlr.ivf.tapas.model.mode.TPS_Mode.ModeType;
import de.dlr.ivf.tapas.model.mode.TPS_ModeChoiceContext;
import de.dlr.ivf.tapas.model.TPS_RegionResultSet.Result;
import de.dlr.ivf.tapas.model.plan.TPS_Plan;
import de.dlr.ivf.tapas.model.plan.TPS_PlanningContext;
import de.dlr.ivf.tapas.model.TPS_AttributeReader.TPS_Attribute;
import de.dlr.ivf.tapas.model.parameter.ParamValue;
import de.dlr.ivf.tapas.model.parameter.TPS_ParameterClass;

public class TPS_SelectWithSingleAccessMode extends TPS_SelectLocationWeightBased {

    private final int maxWalkDistance;

    public TPS_SelectWithSingleAccessMode(TPS_ParameterClass parameterClass, ModeMatrixDistanceProvider distanceCalculator,
                                          ModeDistributionCalculator distributionCalculator,TPS_ModeSet modeSet,
                                          TravelTimeCalculator travelTimeCalculator) {
        super(parameterClass, distanceCalculator, distributionCalculator, modeSet, travelTimeCalculator);
        this.maxWalkDistance = parameterClass.getIntValue(ParamValue.MAX_WALK_DIST);
    }

    public WeightedResult createLocationOption(Result result, double travelTime, double parameter) {
        return new InterveningOpportunitiesWeightedResult(result, travelTime,parameter);
    }

    /**
     * Method to calculate the expected tavel time to the locRepresentant.
     *
     * @param plan
     * @param pc
     * @param prevMCC
     * @param nextMCC
     * @param taz
     * @return
     */
    public double getTravelTime(TPS_Plan plan, TPS_PlanningContext pc, TPS_ModeChoiceContext prevMCC, TPS_ModeChoiceContext nextMCC, TPS_TrafficAnalysisZone taz) {
        double distanceNetFrom = 0, distanceNetTo = 0;
        double weightedTT = -1;
        double ttC0, ttC1, modeProb, arrModeProb, depModeProb;

        // The WALK-mode is used to get distances on the net.
        distanceNetFrom = 0;//distanceCalculator.getDistance(prevMCC.fromStayLocation, prevMCC.toStayLocation, ModeType.WALK);
        distanceNetTo = 0;//distanceCalculator.getDistance(nextMCC.fromStayLocation, nextMCC.toStayLocation, ModeType.WALK);

        TPS_DiscreteDistribution<TPS_Mode> arrDis = modeSet.getModeDistribution(plan, distanceNetFrom, prevMCC);
        TPS_DiscreteDistribution<TPS_Mode> dstDis = modeSet.getModeDistribution(plan, distanceNetTo, nextMCC);
        TPS_DiscreteDistribution<TPS_Mode> dist = new TPS_DiscreteDistribution<>(modeSet.getModes());

        for (TPS_Mode mode : modeSet.getModes()) {
            // calc the mode probabilities and omit forbidden modes
            arrModeProb = arrDis.getValueByKey(mode);
            if (arrModeProb <= 0) {
                continue;
            }
            depModeProb = dstDis.getValueByKey(mode);
            if (depModeProb <= 0) {
                continue;
            }
            // restricted cars will not enter restricted areas
            if (pc.carForThisPlan != null && pc.carForThisPlan.isRestricted() && taz.isRestricted() && mode.getModeType() == ModeType.MIT) {
                continue;
            }
            if (mode.getModeType() == ModeType.WALK && (distanceNetTo + distanceNetFrom) > maxWalkDistance) {
                continue;
            }
            modeProb = (arrModeProb + depModeProb) * 0.5;
            // adjust the mode probability
            dist.setValueByKey(mode, modeProb);
        }// mode loop

        dist.normalize();
        while (weightedTT < 0) {
            TPS_Mode mode = dist.drawKey();
            // get the traveltime
            plan.setAttributeValue(TPS_Attribute.CURRENT_MODE_CODE_VOT, mode.getCodeVot());
            ttC0 = travelTimeCalculator.getTravelTime(mode,prevMCC.fromStayLocation, prevMCC.toStayLocation,
                    (int) prevMCC.fromStay.getOriginalEnd());
            //prevMCC.carForThisPlan);
            ttC1 = travelTimeCalculator.getTravelTime(mode, nextMCC.fromStayLocation, nextMCC.toStayLocation,
                    (int) nextMCC.fromStay.getOriginalEnd());
            //nextMCC.carForThisPlan);
            // modify travel time for specuial cases
            if (TPS_Mode.hasConnection(ttC0) && TPS_Mode.hasConnection(ttC1)) { // only valid travel times
//				// Check, ob Maut- und Parkkosten bei der Gelegenheitswahl bei Einfahrt in Mautzone berücksichtigt
//				// werden sollen; relevant nur für MIV
//				// nicht bei einmaliger Gelegenheitswahl im Basiszustand
//				if (mode.isType(ModeType.MIT) && !pc.fixLocationAtBase
//						&& (ParamFlag.FLAG_RUN_SZENARIO.isTrue() || ParamFlag.FLAG_LOCATION_POCKET_COSTS.isTrue())) {
//
//					plan.setAttributeValue(TPS_Attribute.CURRENT_DISTANCE_CLASS_CODE_VOT,
//							TPS_Distance.getCode(TPS_DistanceCodeTypes.VOT, distanceNetFrom));
//					double ttPlus = region.relocateLocationMIVArr(plan, distanceNetFrom, nextMCC.fromStay.getActCode(),
//							nextMCC.toStay.getOriginalDuration(), prevMCC.fromStayLocation.getTrafficAnalysisZone(),
//							nextMCC.fromStayLocation.getTrafficAnalysisZone());
//					// TODO!!! Fehler? Mischung from/to
//					ttC0 += ttPlus;
//
//					// Check, ob Maut- und Parkkosten bei der Gelegenheitswahl bei Ausfahrt aus Mautzone berücksichtigt
//					// werden sollen; relevant nur für MIV
//					if (ParamFlag.FLAG_USE_EXIT_MAUT.isTrue()) {
//						// Kosten auf Rückweg aufschlagen
//						ttPlus = region.relocateLocationMIVDep(plan, distanceNetTo, nextMCC.fromStay.getActCode(),
//								nextMCC.toStay.getOriginalDuration(), nextMCC.fromStayLocation.getTrafficAnalysisZone(),
//								nextMCC.toStayLocation.getTrafficAnalysisZone());
//						ttC1 += ttPlus;
//					}
//					plan.removeAttribute(TPS_Attribute.CURRENT_DISTANCE_CLASS_CODE_VOT);
//				}
                weightedTT = (ttC0 + ttC1);
            }// no connection check
            else {
                dist.setValueByKey(mode, 0);
                if (!dist.normalize()) {
                    //we can't normalize this distribution because all probabilities are zero!
                    return -1;
                }
            }
        }

        if (!dist.normalize()) {
            //we can't normalize this distribution because all probabilities are zero!
            return -1;
        }
        return weightedTT;
    }

    class InterveningOpportunitiesWeightedResult extends WeightedResult {
        public InterveningOpportunitiesWeightedResult(Result result, double travelTime, double param) {
            super(result, travelTime, param);
        }


        @Override
        public int compareTo(WeightedResult arg0) {
            return travelTime.compareTo(arg0.travelTime);
        }

        /**
         * For intervening opportunities: just the capacity!
         *
         * @return
         */
        public Double getAdaptedWeight() {
            return this.result.sumWeight;
        }
    }
}
