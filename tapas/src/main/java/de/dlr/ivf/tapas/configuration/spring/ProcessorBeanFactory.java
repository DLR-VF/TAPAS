package de.dlr.ivf.tapas.configuration.spring;

import de.dlr.ivf.tapas.choice.FeasibilityCalculator;
import de.dlr.ivf.tapas.configuration.json.runner.TripPriorityRunnerConfiguration;
import de.dlr.ivf.tapas.configuration.json.trafficgeneration.TrafficGenerationConfiguration;
import de.dlr.ivf.tapas.initializers.TourContextFactory;
import de.dlr.ivf.tapas.model.person.TPS_Household;
import de.dlr.ivf.tapas.model.person.TPS_Person;
import de.dlr.ivf.tapas.model.plan.TPS_PlanEnvironment;
import de.dlr.ivf.tapas.model.scheme.Trip;
import de.dlr.ivf.tapas.runtime.server.HierarchicalSimulator;
import de.dlr.ivf.tapas.simulation.Processor;
import de.dlr.ivf.tapas.simulation.implementation.HierarchicalTourProcessor;
import de.dlr.ivf.tapas.simulation.implementation.HouseholdProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.util.Comparator;
import java.util.Map;

@Lazy
@Configuration
public class ProcessorBeanFactory {

//    @Bean
//    public HouseholdProcessor householdProcessor(SchemePr){
//        Processor<TPS_Household, Map<TPS_Person, TPS_PlanEnvironment>> hhProcessor = HouseholdProcessor.builder()
//                .schemeSelector(schemeProvider)
//                .locationAndModeChooser(locationAndModeChooser)
//                .maxTriesScheme(parameters.getIntValue(ParamValue.MAX_TRIES_SCHEME))
//                .planEVA1Acceptance(acceptance)
//                .feasibilityCalculator(feasibilityCalculator)
//                .build();
//
//        return hhProcessor;
//    }

    /**
     * A method that returns a comparator for sorting trips based on their priority in descending order.
     *
     * @return A comparator that compares trips based on their priority in descending order.
     */
    @Bean
    public Comparator<Trip> trpPriorityComparator(){
        return Comparator.comparingInt(Trip::priority).reversed();
    }

    @Bean(name = "maxTriesSchemeSelection")
    public int maxTriesSchemeSelection(TripPriorityRunnerConfiguration configuration){
        return configuration.maxTriesSchemeSelection();
    }

    @Bean
    public FeasibilityCalculator feasibilityCalculator(){
        return new FeasibilityCalculator();
    }

    @Bean
    public TourContextFactory tourContextFactory() {
        return new TourContextFactory();
    }

    @Bean
    public HierarchicalTourProcessor hierarchicalTourProcessor(){
        return new HierarchicalTourProcessor(Comparator.comparingInt(Trip::priority).reversed());
    }
}
