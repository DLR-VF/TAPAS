package de.dlr.ivf.tapas.loc;

import de.dlr.ivf.tapas.distribution.TPS_DiscreteDistribution;
import de.dlr.ivf.tapas.mode.TPS_Mode;
import de.dlr.ivf.tapas.mode.TPS_Mode.ModeType;
import de.dlr.ivf.tapas.mode.TPS_Mode.TPS_ModeCodeType;
import de.dlr.ivf.tapas.mode.TPS_ModeChoiceContext;
import de.dlr.ivf.tapas.persistence.TPS_RegionResultSet.Result;
import de.dlr.ivf.tapas.person.TPS_PreferenceParameters.ShoppingPreferenceAccessibility;
import de.dlr.ivf.tapas.plan.TPS_Plan;
import de.dlr.ivf.tapas.plan.TPS_PlanningContext;
import de.dlr.ivf.tapas.util.TPS_AttributeReader.TPS_Attribute;
import de.dlr.ivf.tapas.util.parameters.ParamValue;
import de.dlr.ivf.tapas.util.parameters.SimulationType;
import de.dlr.ivf.tapas.util.parameters.TPS_ParameterClass;

public class TPS_SelectWithMultipleAccessMode extends TPS_SelectLocationWeigthBased {


    public WeightedResult createLocationOption(Result result, double travelTime) {
        return new InterveningOpportunitiesWeightedResult(result, travelTime);
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
    public double getTravelTime(TPS_Plan plan, TPS_PlanningContext pc, TPS_ModeChoiceContext prevMCC, TPS_ModeChoiceContext nextMCC, TPS_TrafficAnalysisZone taz, TPS_ParameterClass parameterClass) {
        double weightedTT = 0; // this value stores the weighted traveltime
        double ttC0, ttC1, modeProb, arrModeProb, depModeProb;
        boolean connectionFound = false;

        //now model the accessibility preference
        ShoppingPreferenceAccessibility currentAccessibilityPreference = pc.pe
                .getPerson().currentAccessibilityPreference;
        if (currentAccessibilityPreference.equals(ShoppingPreferenceAccessibility.Naehe)) {
            weightedTT = Math.max(parameterClass.getDoubleValue(ParamValue.MIN_DIST), TPS_Mode.get(ModeType.WALK)
                                                                                              .getDistance(
                                                                                                      pc.pe.getPerson()
                                                                                                           .getHousehold()
                                                                                                           .getLocation(),
                                                                                                      taz,
                                                                                                      SimulationType.SCENARIO,
                                                                                                      null));
            connectionFound = true;
        } else if (currentAccessibilityPreference.equals(ShoppingPreferenceAccessibility.Erreichbarkeit)) {

            double ttCarFrom = TPS_Mode.get(ModeType.MIT).getTravelTime(prevMCC.fromStayLocation,
                    prevMCC.toStayLocation, (int) prevMCC.fromStay.getOriginalEnd(), SimulationType.SCENARIO,
                    prevMCC.fromStay.getActCode(), prevMCC.toStay.getActCode(), plan.getPerson(), null);
            double ttCarTo = TPS_Mode.get(ModeType.MIT).getTravelTime(nextMCC.fromStayLocation, nextMCC.toStayLocation,
                    (int) nextMCC.fromStay.getOriginalEnd(), SimulationType.SCENARIO, nextMCC.fromStay.getActCode(),
                    nextMCC.toStay.getActCode(), plan.getPerson(), null);
            double ttPTFrom = TPS_Mode.get(ModeType.PT).getTravelTime(prevMCC.fromStayLocation, prevMCC.toStayLocation,
                    (int) prevMCC.fromStay.getOriginalEnd(), SimulationType.SCENARIO, prevMCC.fromStay.getActCode(),
                    prevMCC.toStay.getActCode(), plan.getPerson(), null);
            double ttPTTo = TPS_Mode.get(ModeType.PT).getTravelTime(nextMCC.fromStayLocation, nextMCC.toStayLocation,
                    (int) nextMCC.fromStay.getOriginalEnd(), SimulationType.SCENARIO, nextMCC.fromStay.getActCode(),
                    nextMCC.toStay.getActCode(), plan.getPerson(), null);
            boolean carConnectionAvailable = ttCarFrom >= 0 && ttCarTo >= 0;
            boolean ptConnectionAvailable = ttPTFrom >= 0 && ttPTTo >= 0;

            if (!carConnectionAvailable && !ptConnectionAvailable) {
                return -1; //no connection!
            }

            // The WALK-mode is used to get distances on the net.
            TPS_Mode walkMode = TPS_Mode.get(ModeType.WALK);
            double distanceNetTo = Math.max(parameterClass.getDoubleValue(ParamValue.MIN_DIST),
                    walkMode.getDistance(prevMCC.fromStayLocation, prevMCC.toStayLocation, SimulationType.SCENARIO,
                            null));
            double distanceNetFrom = Math.max(parameterClass.getDoubleValue(ParamValue.MIN_DIST),
                    walkMode.getDistance(nextMCC.fromStayLocation, nextMCC.toStayLocation, SimulationType.SCENARIO,
                            null));


            TPS_DiscreteDistribution<TPS_Mode> arrDis = PM.getModeSet().getModeDistribution(plan, distanceNetFrom,
                    prevMCC);
            TPS_DiscreteDistribution<TPS_Mode> dstDis = PM.getModeSet().getModeDistribution(plan, distanceNetTo,
                    nextMCC);

            double probCarFrom = arrDis.getValueByKey(TPS_Mode.get(ModeType.MIT));
            double probCarTo = dstDis.getValueByKey(TPS_Mode.get(ModeType.MIT));
            double probPTFrom = arrDis.getValueByKey(TPS_Mode.get(ModeType.PT));
            double probPTTo = dstDis.getValueByKey(TPS_Mode.get(ModeType.PT));
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
            TPS_Mode walkMode = TPS_Mode.get(ModeType.WALK);
            double distanceNetTo = Math.max(parameterClass.getDoubleValue(ParamValue.MIN_DIST),
                    walkMode.getDistance(prevMCC.fromStayLocation, prevMCC.toStayLocation, SimulationType.SCENARIO,
                            null));
            double distanceNetFrom = Math.max(parameterClass.getDoubleValue(ParamValue.MIN_DIST),
                    walkMode.getDistance(nextMCC.fromStayLocation, nextMCC.toStayLocation, SimulationType.SCENARIO,
                            null));
            TPS_Mode mode = null;

            TPS_DiscreteDistribution<TPS_Mode> arrDis = PM.getModeSet().getModeDistribution(plan, distanceNetFrom,
                    prevMCC);
            TPS_DiscreteDistribution<TPS_Mode> dstDis = PM.getModeSet().getModeDistribution(plan, distanceNetTo,
                    nextMCC);
            for (ModeType mt : TPS_Mode.MODE_TYPE_ARRAY) {
                // calculate the mode probabilities and omit forbidden modes
                mode = TPS_Mode.get(mt);
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
                if (pc.carForThisPlan != null && pc.carForThisPlan.isRestricted() && taz.isRestricted() && mode.isType(
                        ModeType.MIT)) {
                    continue;
                }

                if (mt.equals(ModeType.WALK) && (distanceNetTo + distanceNetFrom) > parameterClass.getIntValue(
                        ParamValue.MAX_WALK_DIST)) {
                    continue;
                }
                // get the traveltime
                plan.setAttributeValue(TPS_Attribute.CURRENT_MODE_CODE_VOT, mode.getCode(TPS_ModeCodeType.VOT));

                ttC0 = mode.getTravelTime(prevMCC.fromStayLocation, prevMCC.toStayLocation,
                        (int) prevMCC.fromStay.getOriginalEnd(), SimulationType.SCENARIO, prevMCC.fromStay.getActCode(),
                        prevMCC.toStay.getActCode(), plan.getPerson(), null);
                //prevMCC.carForThisPlan);
                ttC1 = mode.getTravelTime(nextMCC.fromStayLocation, nextMCC.toStayLocation,
                        (int) nextMCC.fromStay.getOriginalEnd(), SimulationType.SCENARIO, nextMCC.fromStay.getActCode(),
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
        public InterveningOpportunitiesWeightedResult(Result result, double travelTime) {
            super(result, travelTime);
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
