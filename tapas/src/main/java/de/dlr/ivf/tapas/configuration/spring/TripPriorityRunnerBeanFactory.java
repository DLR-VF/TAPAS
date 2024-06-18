package de.dlr.ivf.tapas.configuration.spring;

import de.dlr.ivf.tapas.configuration.json.modechoice.ModeChoiceConfiguration;
import de.dlr.ivf.tapas.configuration.json.runner.TripPriorityRunnerConfiguration;
import de.dlr.ivf.tapas.simulation.trafficgeneration.SchemeProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.util.Map;

@Lazy
@Configuration
public class TripPriorityRunnerBeanFactory {

    @Bean
    public ModeChoiceConfiguration modeChoiceConfiguration(TripPriorityRunnerConfiguration configuration,
                                                           SchemeProvider schemeProvider){
        return configuration.modeChoiceConfiguration();
    }




}
