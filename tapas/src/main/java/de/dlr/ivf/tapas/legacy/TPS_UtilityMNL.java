/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.legacy;

import de.dlr.ivf.tapas.util.traveltime.providers.TravelTimeCalculator;
import de.dlr.ivf.tapas.logger.legacy.HierarchyLogLevel;
import de.dlr.ivf.tapas.logger.legacy.SeverityLogLevel;
import de.dlr.ivf.tapas.logger.legacy.TPS_Logger;
import de.dlr.ivf.tapas.mode.ModeDistributionCalculator;
import de.dlr.ivf.tapas.model.mode.Modes;
import de.dlr.ivf.tapas.model.distribution.TPS_DiscreteDistribution;
import de.dlr.ivf.tapas.model.mode.TPS_Mode;
import de.dlr.ivf.tapas.model.mode.TPS_Mode.ModeType;
import de.dlr.ivf.tapas.model.mode.TPS_ModeChoiceContext;
import de.dlr.ivf.tapas.model.parameter.ParamFlag;
import de.dlr.ivf.tapas.model.parameter.TPS_ParameterClass;
import de.dlr.ivf.tapas.model.plan.TPS_Plan;
import de.dlr.ivf.tapas.model.parameter.SimulationType;
import de.dlr.ivf.tapas.util.TPS_FastMath;

import java.util.HashMap;

/**
 * Utility function, which implements the pivot-point modifications according to the simple linear model in König/Axhausen 2001: "Verkehrsentscheidungen in Mobidrive"
 *
 * @author hein_mh
 */
public abstract class TPS_UtilityMNL implements TPS_UtilityFunction {

    private ModeDistributionCalculator distributionCalculator;
    private final boolean useTaxi;
    final Modes modes;
    /**
     * A Hashmap for the mode depending parameters
     */
    protected HashMap<TPS_Mode, double[]> parameterMap = new HashMap<>();

    final TravelTimeCalculator travelTimeCalculator;

    public TPS_UtilityMNL(TravelTimeCalculator travelTimeCalculator, TPS_ParameterClass parameterClass, Modes modes){
        this.travelTimeCalculator = travelTimeCalculator;
        this.useTaxi = parameterClass.isTrue(ParamFlag.FLAG_USE_TAXI);
        this.modes = modes;
    }

    public double calculateDelta(TPS_Mode mode, TPS_Plan plan, double distanceNet, TPS_ModeChoiceContext mcc) {
        double tt1 = 0;
        tt1 = travelTimeCalculator.getTravelTime(mode, mcc.fromStayLocation, mcc.toStayLocation, mcc.startTime);
        double tt2 = 0;

        if (mode.isUseBase()) { //differences in times
            tt2 = travelTimeCalculator.getTravelTime(mode, mcc.fromStayLocation, mcc.toStayLocation, mcc.startTime);
        } else {
            tt2 = tt1;
        }
        double cost1 = this.getCostOfMode(mode, plan, tt1, mcc, SimulationType.SCENARIO);
        double cost2 = this.getCostOfMode(mode, plan, tt2, mcc, SimulationType.BASE);
        if (Double.isNaN(cost1)) {
            return 0;
        }
        if (Double.isNaN(cost2)) {
            return 0;
        }
        //chop to reasonable deltas
        return Math.min(cost1 - cost2, 2.31);
    }

    /**
     * This method calculates the costs of the given mode.
     *
     * @param mode        The mode to calculate
     * @param simType     The simulation type (BASE or SCENARIO)
     * @return The cost for the mode
     */
    public abstract double getCostOfMode(TPS_Mode mode, TPS_Plan plan, double travelTime, TPS_ModeChoiceContext mcc, SimulationType simType);

    public TPS_DiscreteDistribution<TPS_Mode> getDistributionSet(TPS_ModeSet modeSet, TPS_Plan plan, double distanceNet, TPS_ModeChoiceContext mcc) {
        // init
        TPS_DiscreteDistribution<TPS_Mode> dist = distributionCalculator.getDistribution(null);
        double[] utilities = new double[dist.getValues().length];
        double sumOfUtilities = 0;
        // calculate utilities
        for (int i = 0; i < utilities.length; ++i) {
            // get the parameter set
            TPS_Mode mode = modes.getModeById(i);
            if (!mcc.isBikeAvailable && mode.getModeType() == ModeType.BIKE || //no bike
                    mcc.carForThisPlan == null && mode.getModeType() == ModeType.MIT || //no car
                    (mode.getModeType() == ModeType.TAXI && useTaxi) //disable TAXI
            ) {
                utilities[i] = minModeProbability;
            } else {
                //travel time
                double travelTime = travelTimeCalculator.getTravelTime(mode, mcc.fromStayLocation, mcc.toStayLocation, mcc.startTime);
                if (TPS_Mode.noConnection(travelTime)) { //no connection
                    utilities[i] = minModeProbability;
                } else {
                    //calc the value
                    utilities[i] = this.getCostOfMode(mode, plan, travelTime, mcc,
                            SimulationType.SCENARIO);
                    if (Double.isNaN(utilities[i])) {
                        utilities[i] = minModeProbability;
                    } else {
                        utilities[i] = TPS_FastMath.exp(utilities[i]);
                    }
                }
            }
            sumOfUtilities += utilities[i];
        }

        if (sumOfUtilities > 0) {
            // normalize
            sumOfUtilities = 1.0 / sumOfUtilities;
            for (int i = 0; i < utilities.length; ++i) {
                utilities[i] *= sumOfUtilities;
            }
            // calc probabilities
            for (int i = 0; i < utilities.length; ++i) {
                //dist.setIndexedValue(TPS_Mode.get(TPS_Mode.MODE_TYPE_ARRAY[i]), utilities[i]/(sumOfUtilities-utilities[i]));
                dist.setValueByKey(modes.getModeById(i),
                        utilities[i]); //according to http://en.wikipedia.org/wiki/Multinomial_logistic_regression this the mnl!
            }
        } else { //no possible mode!
            // clear probabilities
            //todo revise this
            for (int i = 0; i < utilities.length; ++i) {
                dist.setValueByKey(modes.getModeById(i), 0);
            }
            dist.setValueByKey(modes.getModeByName(ModeType.MIT_PASS.name()),
                    1);// if we erased all possible modes we have to prepare an exit plan!
            if (TPS_Logger.isLogging(HierarchyLogLevel.EPISODE, SeverityLogLevel.SEVERE)) {
                TPS_Logger.log(HierarchyLogLevel.EPISODE, SeverityLogLevel.SEVERE,
                        "No possible mode! Enabling MIT_PASS!");
            }
        }

        boolean expertCheck = TPS_ExpertKnowledgeTree.applyExpertKnowledge(modeSet, plan, distanceNet, mcc, false,
                dist);
        if (!expertCheck) {
            if (TPS_Logger.isLogging(HierarchyLogLevel.EPISODE, SeverityLogLevel.SEVERE)) {
                TPS_Logger.log(HierarchyLogLevel.EPISODE, SeverityLogLevel.SEVERE, "No possible modes!");
                dist.setValueByKey(modes.getModeByName(ModeType.MIT_PASS.name()), 1); // you have to find someone taking you there!
            }
        }
        return dist;
    }

    //todo there is a circular dependency between utility function and ModeDistributionCalculator

    @Override
    public void setDistributionCalculator(ModeDistributionCalculator modeDistributionCalculator) {
        this.distributionCalculator = modeDistributionCalculator;
    }

    public void setParameterSet(TPS_Mode mode, double[] parameters) {
        this.parameterMap.put(mode, parameters);
    }
}
