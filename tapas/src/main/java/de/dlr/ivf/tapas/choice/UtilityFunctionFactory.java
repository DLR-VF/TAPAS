package de.dlr.ivf.tapas.choice;

import de.dlr.ivf.tapas.choice.distance.providers.ModeMatrixDistanceProvider;
import de.dlr.ivf.tapas.choice.traveltime.providers.TravelTimeCalculator;
import de.dlr.ivf.tapas.legacy.TPS_UtilityChaidMNLMixed;
import de.dlr.ivf.tapas.legacy.TPS_UtilityChaidMNLMixedBS;
import de.dlr.ivf.tapas.legacy.TPS_UtilityFunction;
import de.dlr.ivf.tapas.legacy.TPS_UtilityMNLFullComplex;
import de.dlr.ivf.tapas.model.mode.Modes;
import de.dlr.ivf.tapas.model.parameter.TPS_ParameterClass;

public class UtilityFunctionFactory {

    public Class<? extends TPS_UtilityFunction> getClass(String utilityFunctionName){

        return switch(utilityFunctionName){
            case "TPS_UtilityChaidMNLMixed" -> TPS_UtilityChaidMNLMixed.class;
            case "TPS_UtilityChaidMNLMixedBS" -> TPS_UtilityChaidMNLMixedBS.class;
            case "TPS_UtilityMNLFullComplex" -> TPS_UtilityMNLFullComplex.class;
            default -> throw new IllegalStateException("Unexpected utility function name: " + utilityFunctionName);
        };
    }

    public TPS_UtilityFunction getInstance(String utilityFunctionName, ModeMatrixDistanceProvider travelDistanceCalculator, TravelTimeCalculator travelTimeCalculator, TPS_ParameterClass parameterClass, Modes modes){

        return switch(utilityFunctionName){
            case "TPS_UtilityChaidMNLMixed" -> new TPS_UtilityChaidMNLMixed(travelDistanceCalculator, travelTimeCalculator, parameterClass, modes);
            case "TPS_UtilityChaidMNLMixedBS" -> new TPS_UtilityChaidMNLMixedBS(travelDistanceCalculator, travelTimeCalculator, parameterClass, modes);
            case "TPS_UtilityMNLFullComplex" -> new TPS_UtilityMNLFullComplex(travelDistanceCalculator, travelTimeCalculator, parameterClass, modes);
            default -> throw new IllegalStateException("Unexpected utility function name: " + utilityFunctionName);
        };
    }
}
