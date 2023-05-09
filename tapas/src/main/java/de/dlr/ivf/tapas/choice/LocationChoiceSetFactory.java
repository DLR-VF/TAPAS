package de.dlr.ivf.tapas.choice;

import de.dlr.ivf.tapas.legacy.TPS_LocationChoiceSet;
import de.dlr.ivf.tapas.legacy.TPS_MultipleTAZRepresentant;
import de.dlr.ivf.tapas.legacy.TPS_SingleTAZRepresentant;
import de.dlr.ivf.tapas.legacy.TPS_TAZDetourFactor;
import de.dlr.ivf.tapas.legacy.TPS_UtilityFunction;
import de.dlr.ivf.tapas.legacy.TPS_UtilityChaidMNLMixed;
import de.dlr.ivf.tapas.legacy.TPS_UtilityChaidMNLMixedBS;
import de.dlr.ivf.tapas.legacy.TPS_UtilityMNLFullComplex;
import de.dlr.ivf.tapas.mode.ModeDistributionCalculator;
import de.dlr.ivf.tapas.mode.Modes;
import de.dlr.ivf.tapas.model.parameter.TPS_ParameterClass;

public class LocationChoiceSetFactory {

    public Class<? extends TPS_LocationChoiceSet> getClass(String locationChoiceSetName){

        return switch(locationChoiceSetName){
            case "TPS_SingleTAZRepresentant" -> TPS_SingleTAZRepresentant.class;
            case "TPS_MultipleTAZRepresentant" -> TPS_MultipleTAZRepresentant.class;
            case "TPS_TAZDetourFactor" -> TPS_TAZDetourFactor.class;
            default -> throw new IllegalStateException("Unexpected Location choice set name: " + locationChoiceSetName);
        };
    }

    public TPS_UtilityFunction getInstance(String locationChoiceSetName, TravelDistanceCalculator travelDistanceCalculator, TravelTimeCalculator travelTimeCalculator, ModeDistributionCalculator distributionCalculator, TPS_ParameterClass parameterClass, Modes modes){

        return switch(locationChoiceSetName){
            case "TPS_UtilityChaidMNLMixed" -> new TPS_UtilityChaidMNLMixed(travelDistanceCalculator, travelTimeCalculator, distributionCalculator, parameterClass, modes);
            case "TPS_UtilityChaidMNLMixedBS" -> new TPS_UtilityChaidMNLMixedBS(travelDistanceCalculator, travelTimeCalculator, distributionCalculator, parameterClass, modes);
            case "TPS_UtilityMNLFullComplex" -> new TPS_UtilityMNLFullComplex(travelDistanceCalculator, travelTimeCalculator, distributionCalculator, parameterClass, modes);
            default -> throw new IllegalStateException("Unexpected utility function name: " + locationChoiceSetName);
        };
    }
}
