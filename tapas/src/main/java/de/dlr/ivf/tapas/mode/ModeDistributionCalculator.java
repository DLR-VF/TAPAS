package de.dlr.ivf.tapas.mode;

import de.dlr.ivf.tapas.legacy.TPS_UtilityFunction;
import de.dlr.ivf.tapas.model.distribution.TPS_DiscreteDistribution;
import de.dlr.ivf.tapas.model.mode.Modes;
import de.dlr.ivf.tapas.model.mode.TPS_Mode;
import de.dlr.ivf.tapas.model.mode.TPS_Mode.ModeType;
import de.dlr.ivf.tapas.model.mode.TPS_ModeChoiceContext;
import de.dlr.ivf.tapas.model.plan.TPS_Plan;

import de.dlr.ivf.tapas.util.TPS_FastMath;

import java.util.HashMap;
import java.util.Map;

/**
 * temporary class containing extraction from TPS_Distribution
 */
public class ModeDistributionCalculator {

    private final Modes modes;
    private final Map<Thread, TPS_DiscreteDistribution<TPS_Mode>> threadDiscreteDistributionsMap;
    private final TPS_UtilityFunction utilityFunction;

    public ModeDistributionCalculator(Modes modes, TPS_UtilityFunction utilityFunction){
        this.modes = modes;
        this.threadDiscreteDistributionsMap = new HashMap<>();
        this.utilityFunction = utilityFunction;
    }

    public TPS_DiscreteDistribution<TPS_Mode> calculateDistribution(TPS_DiscreteDistribution<TPS_Mode> srcDis, TPS_Plan plan, double distanceNet, TPS_ModeChoiceContext mcc) {

        TPS_DiscreteDistribution<TPS_Mode> dstDis = getDistribution(srcDis);
        dstDis.setValues(srcDis.getValues());
        if (!mcc.isBikeAvailable) {
            dstDis.setValueByKey(modes.getModeByName(ModeType.BIKE.name()), TPS_UtilityFunction.minModeProbability);
        }
        if (mcc.carForThisPlan == null) {
            dstDis.setValueByKey(modes.getModeByName(ModeType.MIT.name()), TPS_UtilityFunction.minModeProbability);
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
        for (TPS_Mode mode : modes.getModes()) {
            // differences in utility need only be calculated if a mode share exists

            double baseProbability = dstDis.getValueByKey(mode);
            if (baseProbability > 0.0) {
                double delta = utilityFunction.calculateDelta(mode, plan, distanceNet, mcc);
                double newProbability = baseProbability * TPS_FastMath.exp(delta);
                if (Double.isNaN(newProbability) || Double.isInfinite(newProbability)) {
                    newProbability = 0;
                    delta = utilityFunction.calculateDelta(mode, plan, distanceNet, mcc);
                    TPS_FastMath.exp(delta);
                }
                dstDis.setValueByKey(mode, newProbability);
            }
        }

        if (!dstDis.normalize()) {
            for (TPS_Mode mode : modes.getModes()) {
                // differences in utility need only be calculated if a mode share exists
                double baseProbability = dstDis.getValueByKey(mode);
                if (baseProbability > 0.0) {
                    double delta = utilityFunction.calculateDelta(mode, plan, distanceNet, mcc);
                    delta = baseProbability * TPS_FastMath.exp(delta);
                    dstDis.setValueByKey(mode, delta);
                }
            }
        }
        return dstDis;
    }


    /**
     * Gets a new distribution object with zero values.
     *
     * @param srcDis If specified the result will have the same indices like srcDis, if null a standard distribution is returned
     * @return A new object of TPS_ArrayIndexProbabilityDistribution
     */
    public TPS_DiscreteDistribution<TPS_Mode> getDistribution(TPS_DiscreteDistribution<TPS_Mode> srcDis) {
        TPS_DiscreteDistribution<TPS_Mode> dis = threadDiscreteDistributionsMap.get(Thread.currentThread());

        if(dis != null){
            return dis;
        }

        if (srcDis != null) {
            dis = new TPS_DiscreteDistribution<>(srcDis.getSingletons());
        } else {
            //init a zero array: if this array is not filled it will throw an exception during normalize
            dis = new TPS_DiscreteDistribution<>(modes.getModes());
            for (int i = 0; i < TPS_Mode.MODE_TYPE_ARRAY.length; i++) {
                dis.setValueByPosition(i, 0);
            }
        }
        threadDiscreteDistributionsMap.put(Thread.currentThread(), dis);
        return dis;
    }
}
