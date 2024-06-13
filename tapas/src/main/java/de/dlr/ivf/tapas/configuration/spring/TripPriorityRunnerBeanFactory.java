package de.dlr.ivf.tapas.configuration.spring;

import de.dlr.ivf.tapas.configuration.json.modechoice.ModeChoiceConfiguration;
import de.dlr.ivf.tapas.configuration.json.runner.TripPriorityRunnerConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TripPriorityRunnerBeanFactory {

    @Bean
    public ModeChoiceConfiguration modeChoiceConfiguration(TripPriorityRunnerConfiguration configuration){
        return configuration.modeChoiceConfiguration();
    }

//    @Bean
//    public HouseholdProcessor householdProcessor(){
//        Processor<TPS_Household, Map<TPS_Person, TPS_PlanEnvironment>> hhProcessor = HouseholdProcessor.builder()
//                //.schemeSelector(schemeSelector)
//                .locationAndModeChooser(locationAndModeChooser)
//                .maxTriesScheme(parameters.getIntValue(ParamValue.MAX_TRIES_SCHEME))
//                .planEVA1Acceptance(acceptance)
//                .feasibilityCalculator(feasibilityCalculator)
//                .build();
//
//        return hhProcessor;
//    }


}
