package de.dlr.ivf.tapas.model;

import de.dlr.ivf.tapas.model.implementation.utilityfunction.TPS_UtilityChaidMNLMixed;
import de.dlr.ivf.tapas.model.implementation.utilityfunction.TPS_UtilityChaidMNLMixedBS;
import de.dlr.ivf.tapas.model.implementation.utilityfunction.TPS_UtilityMNLFullComplex;

public class UtilityFunctionFactory {

    public Class<? extends TPS_UtilityFunction> getClass(String utilityFunctionName){

        return switch(utilityFunctionName){
            case "TPS_UtilityChaidMNLMixed" -> TPS_UtilityChaidMNLMixed.class;
            case "TPS_UtilityChaidMNLMixedBS" -> TPS_UtilityChaidMNLMixedBS.class;
            case "TPS_UtilityMNLFullComplex" -> TPS_UtilityMNLFullComplex.class;
            default -> throw new IllegalStateException("Unexpected utility function name: " + utilityFunctionName);
        };
    }

    public TPS_UtilityFunction getInstance(String utilityFunctionName){

        return switch(utilityFunctionName){
            case "TPS_UtilityChaidMNLMixed" -> new TPS_UtilityChaidMNLMixed();
            case "TPS_UtilityChaidMNLMixedBS" -> new TPS_UtilityChaidMNLMixedBS();
            case "TPS_UtilityMNLFullComplex" -> new TPS_UtilityMNLFullComplex();
            default -> throw new IllegalStateException("Unexpected utility function name: " + utilityFunctionName);
        };
    }
}
