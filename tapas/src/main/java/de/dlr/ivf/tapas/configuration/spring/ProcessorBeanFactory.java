package de.dlr.ivf.tapas.configuration.spring;

import de.dlr.ivf.tapas.choice.FeasibilityCalculator;
import de.dlr.ivf.tapas.configuration.json.runner.TripPriorityRunnerConfiguration;
import de.dlr.ivf.tapas.initializers.TourContextFactory;
import de.dlr.ivf.tapas.model.constants.Activities;
import de.dlr.ivf.tapas.model.plan.StayHierarchies;
import de.dlr.ivf.tapas.model.scheme.Trip;
import de.dlr.ivf.tapas.simulation.implementation.StayHierarchyTourProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.util.Comparator;

@Lazy
@Configuration
public class ProcessorBeanFactory {

    /**
     * A method that returns a comparator for sorting trips based on their priority in descending order.
     *
     * @return A comparator that compares trips based on their priority in descending order.
     */
    @Bean
    public Comparator<Trip> tripPriorityComparator(){
        return Comparator.comparingInt(Trip::priority).reversed().thenComparing(Trip::startTime);
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
    public StayHierarchyTourProcessor hierarchicalTourProcessor(StayHierarchies stayHierarchies, Activities activities){
        return new StayHierarchyTourProcessor(Comparator.comparingInt(Trip::priority).reversed(), stayHierarchies, activities);
    }
}
