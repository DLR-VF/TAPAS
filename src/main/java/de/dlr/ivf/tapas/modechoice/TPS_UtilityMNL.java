package de.dlr.ivf.tapas.modechoice;

import java.util.HashMap;

import de.dlr.ivf.tapas.constants.TPS_ActivityConstant;
import de.dlr.ivf.tapas.distribution.TPS_DiscreteDistribution;
import de.dlr.ivf.tapas.log.TPS_Logger;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface.HierarchyLogLevel;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface.SeverenceLogLevel;
import de.dlr.ivf.tapas.mode.*;
import de.dlr.ivf.tapas.mode.TPS_Mode.ModeType;
import de.dlr.ivf.tapas.plan.TPS_Plan;
import de.dlr.ivf.tapas.util.TPS_FastMath;
import de.dlr.ivf.tapas.util.parameters.ParamFlag;
import de.dlr.ivf.tapas.util.parameters.SimulationType;
/**
 * Utility function, which implements the pivot-point modifications according to the simple linear model in König/Axhausen 2001: "Verkehrsentscheidungen in Mobidrive" 
 * @author hein_mh
 *
 */
public abstract class TPS_UtilityMNL implements TPS_UtilityFunction {
	
	/**
	 * A Hashmap for the mode depending parameters
	 */
	protected HashMap<TPS_Mode,double[]> parameterMap = new HashMap<>();
	
	public void setParameterSet(TPS_Mode mode, double[] parameters){
		this.parameterMap.put(mode, parameters);
	}
	
	
	
	public double calculateDelta(TPS_Mode mode, TPS_Plan plan, double distanceNet, TPS_ModeChoiceContext mcc) {
		double tt1 = mode.getTravelTime(mcc.fromStayLocation, mcc.toStayLocation, mcc.startTime, SimulationType.SCENARIO,
                TPS_ActivityConstant.DUMMY, TPS_ActivityConstant.DUMMY, plan.getPerson(), mcc.carForThisPlan);
		double tt2 = 0;
		if (mode.isUseBase()) { //differences in times
			tt2 = mode.getTravelTime(mcc.fromStayLocation, mcc.toStayLocation, mcc.startTime, SimulationType.BASE,
                    TPS_ActivityConstant.DUMMY, TPS_ActivityConstant.DUMMY, plan.getPerson(), mcc.carForThisPlan);
		} 
		else {
			tt2 = tt1;
		}
		double cost1 = this.getCostOfMode(mode, plan, distanceNet, tt1, mcc, SimulationType.SCENARIO);
		double cost2 = this.getCostOfMode(mode, plan, distanceNet, tt2, mcc, SimulationType.BASE);
		if(Double.isNaN(cost1)) {
			return 0;
		}
		if(Double.isNaN(cost2)) {
			return 0;
		}
		//chop to reasonable deltas
		return Math.min(cost1-cost2, 2.31);
	}

	
	public TPS_DiscreteDistribution<TPS_Mode> getDistributionSet(TPS_ModeSet modeSet, TPS_Plan plan, double distanceNet, TPS_ModeChoiceContext mcc) {
		// init
		TPS_DiscreteDistribution<TPS_Mode> dist = TPS_ModeDistribution.getDistribution(null);
        double[] utilities = new double[dist.getValues().length];
		double sumOfUtilities = 0;
		// calculate utilities		
		for(int i=0; i< utilities.length; ++i) {
			// get the parameter set
			TPS_Mode mode = TPS_Mode.get(TPS_Mode.MODE_TYPE_ARRAY[i]);
			if (!mcc.isBikeAvailable && mode.isType(ModeType.BIKE) || //no bike
				mcc.carForThisPlan==null && mode.isType(ModeType.MIT) || //no car
				(mode.isType(ModeType.TAXI) && mode.getParameters().isFalse(ParamFlag.FLAG_USE_TAXI)) //disable TAXI
				) {
				utilities[i] = minModeProbability;
			} else {
				//travel time
				double travelTime	= mode.getTravelTime(mcc.fromStayLocation, mcc.toStayLocation, mcc.startTime, SimulationType.SCENARIO,
                        TPS_ActivityConstant.DUMMY, TPS_ActivityConstant.DUMMY, plan.getPerson(), mcc.carForThisPlan);
				if(	TPS_Mode.noConnection(travelTime)){ //no connection
					utilities[i] = minModeProbability;
				} else {
					//calc the value
					utilities[i] = this.getCostOfMode(mode, plan, distanceNet, travelTime, mcc, SimulationType.SCENARIO);
					if(Double.isNaN(utilities[i])) {
						utilities[i] = minModeProbability;
					} else {
						utilities[i] = TPS_FastMath.exp(utilities[i]);
					}
				}
			}
			sumOfUtilities += utilities[i];
		}		

		if(sumOfUtilities>0){
			// normalize
			sumOfUtilities = 1.0/sumOfUtilities;
			for(int i=0; i< utilities.length; ++i) {
				utilities[i] *= sumOfUtilities;
			}
			// calc probabilities
			for(int i=0; i< utilities.length; ++i) {
				//dist.setIndexedValue(TPS_Mode.get(TPS_Mode.MODE_TYPE_ARRAY[i]), utilities[i]/(sumOfUtilities-utilities[i]));
				dist.setValueByKey(TPS_Mode.get(TPS_Mode.MODE_TYPE_ARRAY[i]), utilities[i]); //according to http://en.wikipedia.org/wiki/Multinomial_logistic_regression this the mnl!
			}
		} else { //no possible mode!
			// clear probabilities
			for(int i=0; i< utilities.length; ++i) {
				dist.setValueByKey(TPS_Mode.get(TPS_Mode.MODE_TYPE_ARRAY[i]), 0);
			}
			dist.setValueByKey(TPS_Mode.get(ModeType.MIT_PASS), 1);// if we erased all possible modes we have to prepare an exit plan!
			if(TPS_Logger.isLogging(HierarchyLogLevel.EPISODE, SeverenceLogLevel.SEVERE)) {
				TPS_Logger.log(HierarchyLogLevel.EPISODE, SeverenceLogLevel.SEVERE,	"No possible mode! Enabling MIT_PASS!");
			}
		}
			
		boolean expertCheck = TPS_ExpertKnowledgeTree.applyExpertKnowledge(modeSet, plan, distanceNet, mcc, false, dist);
		if(!expertCheck) {
			if(TPS_Logger.isLogging(HierarchyLogLevel.EPISODE, SeverenceLogLevel.SEVERE)) {
				TPS_Logger.log(HierarchyLogLevel.EPISODE, SeverenceLogLevel.SEVERE,	"No possible modes!");
				dist.setValueByKey(TPS_Mode.get(ModeType.MIT_PASS),1); // you have to find someone taking you there!
			}
		}
		return dist;
	}



	/**
	 * This method calculates the costs of the given mode.
	 * 
	 * @param mode The mode to calculate
	 * @param distanceNet The net distance
	 * @param simType The simulation type (BASE or SCENARIO)
	 * @return The cost for the mode
	 */
	public abstract double getCostOfMode(TPS_Mode mode, TPS_Plan plan, double distanceNet, double travelTime, TPS_ModeChoiceContext mcc/*TPS_Location locComingFrom, TPS_Location locGoingTo, double startTime, double durationStay, TPS_Car car, boolean fBike*/, SimulationType simType/*, TPS_Stay stay*/);	
	
}
