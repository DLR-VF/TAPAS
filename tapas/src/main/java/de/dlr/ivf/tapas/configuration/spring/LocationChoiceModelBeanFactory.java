package de.dlr.ivf.tapas.configuration.spring;

import de.dlr.ivf.tapas.simulation.choice.location.LocationChoiceContext;
import de.dlr.ivf.tapas.simulation.choice.location.LocationChoiceModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Lazy
@Configuration
public class LocationChoiceModelBeanFactory {

    @Bean
    public LocationChoiceModel<LocationChoiceContext> locationChoiceModel() {

        return null;
    }
}
