package de.dlr.ivf.tapas.mode;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import de.dlr.ivf.tapas.distribution.TPS_DiscreteDistribution;
import de.dlr.ivf.tapas.log.LogHierarchy;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface.HierarchyLogLevel;
import de.dlr.ivf.tapas.mode.TPS_Mode.ModeType;
import de.dlr.ivf.tapas.modechoice.TPS_UtilityFunction;
import de.dlr.ivf.tapas.plan.TPS_Plan;
import de.dlr.ivf.tapas.util.TPS_FastMath;

/**
 * Class for calculating the modal distributions in the case of a scenario. Based on the empirical modal splits as pivot
 * point, the new distributions are calculated determining the differences in utility for the different modes available.
 * Therefore, the changes in costs (financial and time-based) associated with the choice of a mode in the scenario and
 * the base scenario are calculated.
 * 
 */
@LogHierarchy(hierarchyLogLevel = HierarchyLogLevel.EPISODE)
public class TPS_ModeDistribution {

	public static Map<Thread, TPS_DiscreteDistribution<TPS_Mode>> MAP = new HashMap<>();

	public static void clearDistributions() {
		synchronized(MAP) {
			MAP.clear();
		}
	}
	
	/**
	 * /** Calculates and stores the differences in modal split between the base and the scenario resulting from the
	 * changes in costs and the initial modal split values
	 * 
	 * @param srcDis
	 *            initial modal split distribution
	 * @param plan
	 *            day plan
	 * @param distanceNet
	 *            distance on the street net
	 * @param mcc
	 *            modecontext, like , bike, car ...
	 * 			  flag if a bike is available
	 * @return resulting probability distribution for the modes
	 */
	public static TPS_DiscreteDistribution<TPS_Mode> calculateDistribution(
			TPS_DiscreteDistribution<TPS_Mode> srcDis, TPS_Plan plan, double distanceNet, TPS_ModeChoiceContext mcc) {

		TPS_DiscreteDistribution<TPS_Mode> dstDis = getDistribution(srcDis);
		dstDis.setValues(srcDis.getValues());
		if (!mcc.isBikeAvailable) {
			dstDis.setValueByKey(TPS_Mode.get(ModeType.BIKE), TPS_UtilityFunction.minModeProbability);
		}
		if (mcc.carForThisPlan==null) {
			dstDis.setValueByKey(TPS_Mode.get(ModeType.MIT), TPS_UtilityFunction.minModeProbability);
		}
	
		if (!dstDis.normalize()) {
			return null;
		}
			
		// calculating differences in utility for the modes with changeable attribute values;
		// assuming the financial and time based costs for using bike, foot and misc to be fix
	
		// calculation for all modes the new probabilities
		// determining first the dividing term for Pi1 = Pi0 exp(ß Delta Ci) / sum of all modes Pj0 exp(ß Delta Cj)
		// non-variable modes keep their probability shares from the base situation
	
		// adding modes with variable costs;
		for( ModeType m  : EnumSet.allOf(ModeType.class)){
			// differences in utility need only be calculated if a mode share exists
			TPS_Mode mode = TPS_Mode.get(m);
			double baseProbability = dstDis.getValueByKey(mode);
			if (baseProbability > 0.0) {
				double delta = mode.calculateDelta(plan, distanceNet, mcc);
				double newProbability = baseProbability * TPS_FastMath.exp(delta);
				if(Double.isNaN(newProbability) || Double.isInfinite(newProbability)) {
					newProbability = 0;
					delta = mode.calculateDelta(plan, distanceNet, mcc);
					TPS_FastMath.exp(delta);
				}
				dstDis.setValueByKey(mode, newProbability);
			}
		}			

		if(!dstDis.normalize()){
			for( ModeType m  : EnumSet.allOf(ModeType.class)){
				TPS_Mode mode = TPS_Mode.get(m);
				// differences in utility need only be calculated if a mode share exists
				double baseProbability = dstDis.getValueByKey(mode);
				if (baseProbability > 0.0) {
					double delta = mode.calculateDelta(plan, distanceNet, mcc);
					delta = baseProbability * TPS_FastMath.exp(delta);
					dstDis.setValueByKey(mode, delta);
				}
			}	
		}
        return dstDis;
	}

	/**
	 * Gets a new distribution object with zero values.  
	 * @param srcDis If specified the result will have the same indices like srcDis, if null a standard distribution is returned
	 * @return A new object of TPS_ArrayIndexProbabilityDistribution
	 */
	public static TPS_DiscreteDistribution<TPS_Mode> getDistribution(TPS_DiscreteDistribution<TPS_Mode> srcDis) {
		TPS_DiscreteDistribution<TPS_Mode> dis = MAP.get(Thread.currentThread());
		if (dis == null) {
			if(srcDis !=null) {
				dis = new TPS_DiscreteDistribution<>(srcDis.getSingletons());
			} else {
				//init a zero array: if this array is not filled it will throw an exception during normalize
				dis = new TPS_DiscreteDistribution<>(TPS_Mode.getConstants());
				for (int i = 0; i < TPS_Mode.MODE_TYPE_ARRAY.length; i++) {
					dis.setValueByPosition(i, 0);
				}
			}
			MAP.put(Thread.currentThread(), dis);
		}
		return dis;
	}

}
