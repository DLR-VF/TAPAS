/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.legacy;

import de.dlr.ivf.tapas.model.distribution.TPS_DiscreteDistribution;
import de.dlr.ivf.tapas.model.location.TPS_TrafficAnalysisZone;
import de.dlr.ivf.tapas.model.mode.TPS_Mode;
import de.dlr.ivf.tapas.model.mode.TPS_Mode.ModeType;
import de.dlr.ivf.tapas.model.mode.TPS_Mode.TPS_ModeCodeType;
import de.dlr.ivf.tapas.model.mode.TPS_ModeChoiceContext;
import de.dlr.ivf.tapas.model.plan.TPS_Plan;
import de.dlr.ivf.tapas.model.plan.TPS_PlanningContext;

import de.dlr.ivf.tapas.model.person.TPS_PreferenceParameters.ShoppingPreferenceAccessibility;
import de.dlr.ivf.tapas.model.TPS_AttributeReader.TPS_Attribute;
import de.dlr.ivf.tapas.model.parameter.ParamValue;
import de.dlr.ivf.tapas.model.parameter.SimulationType;
import de.dlr.ivf.tapas.model.parameter.TPS_ParameterClass;
import de.dlr.ivf.tapas.model.TPS_RegionResultSet.Result;

public class TPS_SelectWithMultipleAccessMode extends TPS_SelectLocationWeightBased {

    final double minDist;
    private final int walkMaxDist;

    public TPS_SelectWithMultipleAccessMode(TPS_ParameterClass parameterClass) {
        super(parameterClass);
        this.minDist = parameterClass.getDoubleValue(ParamValue.MIN_DIST);
        this.walkMaxDist = parameterClass.getIntValue(ParamValue.MAX_WALK_DIST);
    }

    public WeightedResult createLocationOption(Result result, double travelTime, double parameter) {
        return new InterveningOpportunitiesWeightedResult(result, travelTime, parameter);
    }

    /**
     * Method to calculate the expected travel time to the locRepresentant.
     *
     * @param plan
     * @param pc
     * @param prevMCC
     * @param nextMCC
     * @param taz
     * @return
     */
    public double getTravelTime(TPS_Plan plan, TPS_PlanningContext pc, TPS_ModeChoiceContext prevMCC, TPS_ModeChoiceContext nextMCC, TPS_TrafficAnalysisZone taz) {
        double weightedTT = 0; // this value stores the weighted traveltime
        double ttC0, ttC1, modeProb, arrModeProb, depModeProb;
        boolean connectionFound = false;

        //now model the accessibility preference
        ShoppingPreferenceAccessibility currentAccessibilityPreference = pc.pe
                .getPerson().currentAccessibilityPreference;
        if (currentAccessibilityPreference.equals(ShoppingPreferenceAccessibility.Naehe)) {
            weightedTT = Math.max(this.minDist, distanceCalculator.getDistance(pc.pe.getPerson().getHousehold()
                                                                                                .getLocation(),
                                                                                                taz,
                                                                                                ModeType.WALK));
            connectionFound = true;
        } else if (currentAccessibilityPreference.equals(ShoppingPreferenceAccessibility.Erreichbarkeit)) {

            double ttCarFrom = travelTimeCalculator.getTravelTime(modeSet.getMode(ModeType.MIT),prevMCC.fromStayLocation,
                    prevMCC.toStayLocation, prevMCC.fromStay.getOriginalEnd(),
                    prevMCC.fromStay.getActCode(), prevMCC.toStay.getActCode(), plan.getPerson(), null);
            double ttCarTo = travelTimeCalculator.getTravelTime(modeSet.getMode(ModeType.MIT), nextMCC.fromStayLocation, nextMCC.toStayLocation,
                    nextMCC.fromStay.getOriginalEnd(), nextMCC.fromStay.getActCode(),
                    nextMCC.toStay.getActCode(), plan.getPerson(), null);
            double ttPTFrom = travelTimeCalculator.getTravelTime(modeSet.getMode(ModeType.PT), prevMCC.fromStayLocation, prevMCC.toStayLocation,
                    prevMCC.fromStay.getOriginalEnd(), prevMCC.fromStay.getActCode(),
                    prevMCC.toStay.getActCode(), plan.getPerson(), null);
            double ttPTTo = travelTimeCalculator.getTravelTime(modeSet.getMode(ModeType.PT), nextMCC.fromStayLocation, nextMCC.toStayLocation,
                    nextMCC.fromStay.getOriginalEnd(), nextMCC.fromStay.getActCode(),
                    nextMCC.toStay.getActCode(), plan.getPerson(), null);
            boolean carConnectionAvailable = ttCarFrom >= 0 && ttCarTo >= 0;
            boolean ptConnectionAvailable = ttPTFrom >= 0 && ttPTTo >= 0;

            if (!carConnectionAvailable && !ptConnectionAvailable) {
                return -1; //no connection!
            }

            // The WALK-mode is used to get distances on the net.
            double distanceNetTo = Math.max(this.minDist, distanceCalculator.getDistance(prevMCC.fromStayLocation, prevMCC.toStayLocation, ModeType.WALK));
            double distanceNetFrom = Math.max(this.minDist, distanceCalculator.getDistance(nextMCC.fromStayLocation, nextMCC.toStayLocation,ModeType.WALK));


            TPS_DiscreteDistribution<TPS_Mode> arrDis = modeSet.getModeDistribution(plan, distanceNetFrom,
                    prevMCC);
            TPS_DiscreteDistribution<TPS_Mode> dstDis = modeSet.getModeDistribution(plan, distanceNetTo,
                    nextMCC);

            double probCarFrom = arrDis.getValueByKey(modeSet.getMode(ModeType.MIT));
            double probCarTo = dstDis.getValueByKey(modeSet.getMode(ModeType.MIT));
            double probPTFrom = arrDis.getValueByKey(modeSet.getMode(ModeType.PT));
            double probPTTo = dstDis.getValueByKey(modeSet.getMode(ModeType.PT));
            double totalProb = 0;

            if (carConnectionAvailable && probCarFrom > 0 && probCarTo > 0) {
                connectionFound = true;
                weightedTT += (ttCarFrom + ttCarTo) * (probCarFrom + probCarTo);
                totalProb += (probCarFrom + probCarTo);
            }
            if (ptConnectionAvailable && probPTFrom > 0 && probPTTo > 0) {
                connectionFound = true;
                weightedTT += (ttPTFrom + ttPTTo) * (probPTFrom + probPTTo);
                totalProb += (probPTFrom + probPTTo);
            }
            if (connectionFound) return weightedTT / totalProb;
            else return -1; //no connection!
        } else { //default weight

            // The WALK-mode is used to get distances on the net.
            double distanceNetTo = Math.max(minDist, distanceCalculator.getDistance(prevMCC.fromStayLocation, prevMCC.toStayLocation, ModeType.WALK));
            double distanceNetFrom = Math.max(minDist, distanceCalculator.getDistance(nextMCC.fromStayLocation, nextMCC.toStayLocation, ModeType.WALK));

            TPS_DiscreteDistribution<TPS_Mode> arrDis = modeSet.getModeDistribution(plan, distanceNetFrom,
                    prevMCC);
            TPS_DiscreteDistribution<TPS_Mode> dstDis = modeSet.getModeDistribution(plan, distanceNetTo,
                    nextMCC);
            for (TPS_Mode mode : modeSet.getModes()) {
                // calculate the mode probabilities and omit forbidden modes
                arrModeProb = arrDis.getValueByKey(mode);
                if (arrModeProb <= 0) {
                    continue;
                }
                depModeProb = dstDis.getValueByKey(mode);
                if (depModeProb <= 0) {
                    continue;
                }
                modeProb = (arrModeProb + depModeProb) * 0.5;

                // restricted cars will not enter restricted areas
                if (pc.carForThisPlan != null && pc.carForThisPlan.isRestricted() && taz.isRestricted() && mode.getModeType() == ModeType.MIT) {
                    continue;
                }

                if (mode.getModeType() == ModeType.WALK && (distanceNetTo + distanceNetFrom) > this.walkMaxDist) {
                    continue;
                }
                // get the traveltime
                plan.setAttributeValue(TPS_Attribute.CURRENT_MODE_CODE_VOT, mode.getCodeVot());

                ttC0 = travelTimeCalculator.getTravelTime(mode, prevMCC.fromStayLocation, prevMCC.toStayLocation,
                        (int) prevMCC.fromStay.getOriginalEnd(), prevMCC.fromStay.getActCode(),
                        prevMCC.toStay.getActCode(), plan.getPerson(), null);
                //prevMCC.carForThisPlan);
                ttC1 = travelTimeCalculator.getTravelTime(mode, nextMCC.fromStayLocation, nextMCC.toStayLocation,
                        (int) nextMCC.fromStay.getOriginalEnd(), nextMCC.fromStay.getActCode(),
                        nextMCC.toStay.getActCode(), plan.getPerson(), null);
                //nextMCC.carForThisPlan);

                if (TPS_Mode.hasConnection(ttC0) && TPS_Mode.hasConnection(ttC1)) { // only valid travel times
                    connectionFound = true;
                    // add the traveltime
                    weightedTT += (ttC0 + ttC1) * modeProb;
                }// no connection check
            }// mode loop
        }

        if (connectionFound) {
            return weightedTT;
        } else {
            return -1; //not reachable!
        }
    }

    class InterveningOpportunitiesWeightedResult extends WeightedResult {
        public InterveningOpportunitiesWeightedResult(Result result, double travelTime, double param) {
            super(result, travelTime,param);
        }

        @Override
        /**
         * The most attractive result has to be in front -> Ascending traveltimes
         */ public int compareTo(WeightedResult arg0) {
            return travelTime.compareTo(arg0.travelTime);
        }

        /**
         * For interveignig opportunities: just the capacity!
         *
         * @return
         */
        public Double getAdaptedWeight() {
            return this.result.sumWeight;
        }
    }

}
