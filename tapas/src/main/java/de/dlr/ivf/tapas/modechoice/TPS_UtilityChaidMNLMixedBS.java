/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.modechoice;

import de.dlr.ivf.tapas.constants.TPS_SettlementSystem.TPS_SettlementSystemType;
import de.dlr.ivf.tapas.distribution.TPS_DiscreteDistribution;
import de.dlr.ivf.tapas.logger.TPS_Logger;
import de.dlr.ivf.tapas.logger.HierarchyLogLevel;
import de.dlr.ivf.tapas.logger.SeverityLogLevel;
import de.dlr.ivf.tapas.mode.TPS_Mode;
import de.dlr.ivf.tapas.mode.TPS_Mode.ModeType;
import de.dlr.ivf.tapas.mode.TPS_ModeChoiceContext;
import de.dlr.ivf.tapas.mode.TPS_ModeSet;
import de.dlr.ivf.tapas.plan.TPS_Plan;
import de.dlr.ivf.tapas.parameter.ParamFlag;

/**
 * This class is the biggest joke ever!
 * It calculates the mode share based on the chaid model and replaces train with the guessed carsharing parameters
 * Furthermore it calculates the delta with the MNL-Values but leaves car sharing out.
 * *ouch* RNB3 makes me do funny things!
 *
 * @author hein_mh
 */
public class TPS_UtilityChaidMNLMixedBS extends TPS_UtilityMNLFullComplex {


    /**
     * This method overrides delta calculation for Train but calculates the rest normally.
     */
    @Override
    public double calculateDelta(TPS_Mode mode, TPS_Plan plan, double distanceNet, TPS_ModeChoiceContext mcc) {
        double origValue = 1;
        if (!(mode.isType(ModeType.CAR_SHARING) && plan.getPM().getParameters().isTrue(ParamFlag.FLAG_USE_CARSHARING))) {
            origValue = super.calculateDelta(mode, plan, distanceNet, mcc);
        }
        return origValue;

    }

    /**
     * This method calculates the mode share based on the chaid model, but calculates the delta with the MNL-Values
     */

    @Override
    public TPS_DiscreteDistribution<TPS_Mode> getDistributionSet(TPS_ModeSet modeSet, TPS_Plan plan, double distanceNet, TPS_ModeChoiceContext mcc/*TPS_Location locComingFrom, TPS_Location locGoingTo, double startTime, double durationStay, TPS_Car car, boolean fBike, TPS_Stay stay*/) {

        //get the MNL-Values
        TPS_DiscreteDistribution<TPS_Mode> origDist = super.getDistributionSet(modeSet, plan, distanceNet, mcc);
        boolean expertCheck;
        TPS_Node pNode = modeSet.getModeChoiceTree().getDistributionSet(plan);

        TPS_DiscreteDistribution<TPS_Mode> dist = null;
        do {
            if (plan.getPerson().getHousehold().getLocation().getTrafficAnalysisZone().getBbrType().getCode(
                    TPS_SettlementSystemType.TAPAS) == 2) {
                dist = new TPS_DiscreteDistribution<>(TPS_Mode.getConstants());
                dist.setValues(origDist.getValues());
            } else {
                dist = pNode.getCopyOfValueDistribution();

                if (plan.getPM().getParameters().isTrue(ParamFlag.FLAG_USE_CARSHARING) ||
                        plan.getPM().getParameters().isTrue(ParamFlag.FLAG_USE_ROBOTAXI)) dist.setValueByKey(
                        TPS_Mode.get(ModeType.CAR_SHARING), origDist.getValueByKey(TPS_Mode.get(ModeType.CAR_SHARING)));
            }
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
                dist.setValueByKey(TPS_Mode.get(ModeType.MIT_PASS), 1); // you have to find someone taking you there!
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
