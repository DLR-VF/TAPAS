/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.legacy;

import de.dlr.ivf.tapas.choice.TravelDistanceCalculator;
import de.dlr.ivf.tapas.choice.TravelTimeCalculator;
import de.dlr.ivf.tapas.logger.legacy.TPS_Logger;
import de.dlr.ivf.tapas.logger.legacy.HierarchyLogLevel;
import de.dlr.ivf.tapas.logger.legacy.SeverityLogLevel;
import de.dlr.ivf.tapas.mode.ModeDistributionCalculator;
import de.dlr.ivf.tapas.mode.Modes;
import de.dlr.ivf.tapas.model.distribution.TPS_DiscreteDistribution;
import de.dlr.ivf.tapas.model.mode.TPS_Mode;
import de.dlr.ivf.tapas.model.mode.TPS_Mode.ModeType;
import de.dlr.ivf.tapas.model.mode.TPS_ModeChoiceContext;
import de.dlr.ivf.tapas.model.parameter.TPS_ParameterClass;
import de.dlr.ivf.tapas.model.plan.TPS_Plan;

/**
 * This class calculates the mode share based on the chaid model, but calculates the delta with the MNL-Values
 *
 * @author hein_mh
 */
public class TPS_UtilityChaidMNLMixed extends TPS_UtilityMNLFullComplex {
    public TPS_UtilityChaidMNLMixed(TravelDistanceCalculator travelDistanceCalculator, TravelTimeCalculator travelTimeCalculator, TPS_ParameterClass parameterClass, Modes modes) {
        super(travelDistanceCalculator, travelTimeCalculator, parameterClass, modes);
    }

    /**
     * This method calculates the mode share based on the chaid model, but calculates the delta with the MNL-Values
     */
    @Override
    public TPS_DiscreteDistribution<TPS_Mode> getDistributionSet(TPS_ModeSet modeSet, TPS_Plan plan, double distanceNet, TPS_ModeChoiceContext mcc/*TPS_Location locComingFrom, TPS_Location locGoingTo, double startTime, double durationStay, TPS_Car car, boolean fBike, TPS_Stay stay*/) {

        TPS_Node pNode = modeSet.getModeChoiceTree().getDistributionSet(plan);
        boolean expertCheck;
        TPS_DiscreteDistribution<TPS_Mode> dist = null;
        do {
            dist = pNode.getCopyOfValueDistribution();
            expertCheck = TPS_ExpertKnowledgeTree.applyExpertKnowledge(modeSet, plan, distanceNet, mcc, true, dist);
            if (pNode.getParent() == null) {
                if (TPS_Logger.isLogging(SeverityLogLevel.WARN)) {
                    TPS_Logger.log(SeverityLogLevel.WARN, "Top node in mode choice tree reached!");
                }
            }
            pNode = pNode.getParent(); // for the next cycle: if normalize fails, we take the parent
        } while ((!expertCheck || !dist.normalize()) && pNode != null);

        if (!dist.normalize()) {
            if (TPS_Logger.isLogging(HierarchyLogLevel.EPISODE, SeverityLogLevel.SEVERE)) {
                TPS_Logger.log(HierarchyLogLevel.EPISODE, SeverityLogLevel.SEVERE, "No possible modes!");
                dist.setValueByKey(modes.getMode(ModeType.MIT_PASS), 1); // you have to find someone taking you there!
            }
        }
        //TODO: hack for reducing WALK
//		do{
//			if(dist.getIndexedValue(TPS_Mode.get(ModeType.WALK))>TPS_UtilityFunction.walkDistanceShareAfterBarrier && distanceNet>TPS_UtilityFunction.walkDistanceBarrier){
//				dist.setIndexedValue(TPS_Mode.get(ModeType.WALK), TPS_UtilityFunction.walkDistanceShareAfterBarrier/2.0); // this might get larger due to normalization
//			}
//			if(dist.getIndexedValue(TPS_Mode.get(ModeType.WALK))>TPS_UtilityFunction.walkDistanceShareAfterBarrierStrong && distanceNet>TPS_UtilityFunction.walkDistanceBarrierStrong){
//				dist.setIndexedValue(TPS_Mode.get(ModeType.WALK), TPS_UtilityFunction.walkDistanceShareAfterBarrierStrong/2.0); // this might get larger due to normalization
//			}
//			dist.normalize();
//		}while(	dist.getIndexedValue(TPS_Mode.get(ModeType.WALK))>TPS_UtilityFunction.walkDistanceShareAfterBarrier && distanceNet>TPS_UtilityFunction.walkDistanceBarrier ||
//				dist.getIndexedValue(TPS_Mode.get(ModeType.WALK))>TPS_UtilityFunction.walkDistanceShareAfterBarrierStrong && distanceNet>TPS_UtilityFunction.walkDistanceBarrierStrong); //battle down giant mode shares for walk

        return dist;
    }


}
