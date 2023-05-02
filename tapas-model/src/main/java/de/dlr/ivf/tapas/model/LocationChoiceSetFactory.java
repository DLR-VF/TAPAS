package de.dlr.ivf.tapas.model;

import de.dlr.ivf.tapas.model.implementation.utilityfunction.TPS_UtilityChaidMNLMixed;
import de.dlr.ivf.tapas.model.implementation.utilityfunction.TPS_UtilityChaidMNLMixedBS;
import de.dlr.ivf.tapas.model.implementation.utilityfunction.TPS_UtilityMNLFullComplex;
import de.dlr.ivf.tapas.model.location.TPS_LocationChoiceSet;
import de.dlr.ivf.tapas.model.location.TPS_MultipleTAZRepresentant;
import de.dlr.ivf.tapas.model.location.TPS_SingleTAZRepresentant;
import de.dlr.ivf.tapas.model.location.TPS_TAZDetourFactor;

public class LocationChoiceSetFactory {

    public Class<? extends TPS_LocationChoiceSet> getClass(String locationChoiceSetName){

        return switch(locationChoiceSetName){
            case "TPS_SingleTAZRepresentant" -> TPS_SingleTAZRepresentant.class;
            case "TPS_MultipleTAZRepresentant" -> TPS_MultipleTAZRepresentant.class;
            case "TPS_TAZDetourFactor" -> TPS_TAZDetourFactor.class;
            default -> throw new IllegalStateException("Unexpected Location choice set name: " + locationChoiceSetName);
        };
    }

    public TPS_UtilityFunction getInstance(String locationChoiceSetName){

        return switch(locationChoiceSetName){
            case "TPS_UtilityChaidMNLMixed" -> new TPS_UtilityChaidMNLMixed();
            case "TPS_UtilityChaidMNLMixedBS" -> new TPS_UtilityChaidMNLMixedBS();
            case "TPS_UtilityMNLFullComplex" -> new TPS_UtilityMNLFullComplex();
            default -> throw new IllegalStateException("Unexpected utility function name: " + locationChoiceSetName);
        };
    }
}
