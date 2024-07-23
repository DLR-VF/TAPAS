package de.dlr.ivf.tapas.configuration.spring;

import de.dlr.ivf.tapas.choice.FeasibilityCalculator;
import de.dlr.ivf.tapas.configuration.json.runner.TripPriorityRunnerConfiguration;
import de.dlr.ivf.tapas.model.scheme.Trip;
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
}
