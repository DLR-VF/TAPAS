package de.dlr.ivf.tapas.choice;

import de.dlr.ivf.tapas.legacy.*;
import de.dlr.ivf.tapas.mode.ModeDistributionCalculator;
import de.dlr.ivf.tapas.model.mode.Modes;
import de.dlr.ivf.tapas.model.DistanceClasses;
import de.dlr.ivf.tapas.model.parameter.TPS_ParameterClass;

public class LocationChoiceSetFactory {

    public TPS_LocationChoiceSet newLocationChoiceSetModel(String locationChoiceSetName, TPS_ParameterClass parameters){

        return switch(locationChoiceSetName){
            case "TPS_SingleTAZRepresentant" -> new TPS_SingleTAZRepresentant(parameters);
            case "TPS_MultipleTAZRepresentant" -> new TPS_MultipleTAZRepresentant(parameters);
            case "TPS_TAZDetourFactor" -> new TPS_TAZDetourFactor();
            default -> throw new IllegalStateException("Unexpected Location choice set name: " + locationChoiceSetName);
        };
    }

    public TPS_UtilityFunction newUtilityFunctionInstance(String utilityFunctionName, TravelDistanceCalculator travelDistanceCalculator, TravelTimeCalculator travelTimeCalculator, TPS_ParameterClass parameterClass, Modes modes){

        return switch(utilityFunctionName){
            case "TPS_UtilityChaidMNLMixed" -> new TPS_UtilityChaidMNLMixed(travelDistanceCalculator, travelTimeCalculator, parameterClass, modes);
            case "TPS_UtilityChaidMNLMixedBS" -> new TPS_UtilityChaidMNLMixedBS(travelDistanceCalculator, travelTimeCalculator, parameterClass, modes);
            case "TPS_UtilityMNLFullComplex" -> new TPS_UtilityMNLFullComplex(travelDistanceCalculator, travelTimeCalculator, parameterClass, modes);
            default -> throw new IllegalStateException("Unexpected utility function name: " + utilityFunctionName);
        };
    }

    public TPS_LocationSelectModel newLocationSelectionModel(String locationSelectionModelName,
                                                             TPS_ParameterClass parameters,
                                                             TPS_UtilityFunction utilityFunction,
                                                             TravelDistanceCalculator distanceCalculator,
                                                             ModeDistributionCalculator distributionCalculator,
                                                             TPS_ModeSet modeSet,
                                                             TravelTimeCalculator travelTimeCalculator,
                                                             DistanceClasses distanceClasses){

        return switch (locationSelectionModelName){
            case "TPS_SelectWithMultipleAccessModeGravity" -> new TPS_SelectWithMultipleAccessModeGravity(parameters,distanceCalculator,distributionCalculator,modeSet,travelTimeCalculator);
            case "TPS_SelectWithMultipleAccessMode" -> new TPS_SelectWithMultipleAccessMode(parameters,distanceCalculator,distributionCalculator,modeSet,travelTimeCalculator);
            case "TPS_SelectWithMultipleAccessModeGravityPow2" -> new TPS_SelectWithMultipleAccessModeGravityPow2(parameters,distanceCalculator,distributionCalculator,modeSet,travelTimeCalculator);
            case "TPS_SelectWithMultipleAccessModeLogSum" -> new TPS_SelectWithMultipleAccessModeLogSum(parameters, utilityFunction,distanceCalculator,distributionCalculator,modeSet,travelTimeCalculator, distanceClasses);
            case "TPS_SelectWithSingleAccessMode" -> new TPS_SelectWithSingleAccessMode(parameters, distanceCalculator, distributionCalculator, modeSet, travelTimeCalculator);

            default -> throw new IllegalStateException("Unexpected location selection model name: " + locationSelectionModelName);
        };
    }
}
