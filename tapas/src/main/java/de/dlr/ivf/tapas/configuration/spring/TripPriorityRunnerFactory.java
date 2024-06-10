package de.dlr.ivf.tapas.configuration.spring;

import de.dlr.ivf.tapas.configuration.json.modechoice.ModeChoiceConfiguration;
import de.dlr.ivf.tapas.configuration.json.runner.TripPriorityRunnerConfiguration;
import de.dlr.ivf.tapas.model.parameter.ParamValue;
import de.dlr.ivf.tapas.model.person.TPS_Household;
import de.dlr.ivf.tapas.model.person.TPS_Person;
import de.dlr.ivf.tapas.model.plan.TPS_PlanEnvironment;
import de.dlr.ivf.tapas.simulation.Processor;
import de.dlr.ivf.tapas.simulation.TrafficGeneration;
import de.dlr.ivf.tapas.simulation.implementation.HouseholdProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class TripPriorityRunnerFactory {

    private final TrafficGeneration<?> trafficGeneration;
    private final TripPriorityRunnerConfiguration configuration;

    @Autowired
    public TripPriorityRunnerFactory(TrafficGeneration<?> trafficGeneration, TripPriorityRunnerConfiguration configuration){
        this.trafficGeneration = trafficGeneration;
        this.configuration = configuration;
    }

    @Bean
    public ModeChoiceConfiguration modeChoiceConfiguration(){
        return configuration.modeChoiceConfiguration();
    }

    @Bean
    public HouseholdProcessor householdProcessor(){
        Processor<TPS_Household, Map<TPS_Person, TPS_PlanEnvironment>> hhProcessor = HouseholdProcessor.builder()
                //.schemeSelector(schemeSelector)
                .locationAndModeChooser(locationAndModeChooser)
                .maxTriesScheme(parameters.getIntValue(ParamValue.MAX_TRIES_SCHEME))
                .planEVA1Acceptance(acceptance)
                .feasibilityCalculator(feasibilityCalculator)
                .build();

        return hhProcessor;
    }


}
