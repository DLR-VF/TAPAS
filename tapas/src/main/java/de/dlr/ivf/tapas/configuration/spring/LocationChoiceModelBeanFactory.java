package de.dlr.ivf.tapas.configuration.spring;

import de.dlr.ivf.tapas.configuration.json.locationchoice.LocationChoiceConfiguration;
import de.dlr.ivf.tapas.model.mode.Modes;
import de.dlr.ivf.tapas.model.mode.TPS_Mode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Lazy
@Configuration
public class LocationChoiceModelBeanFactory {

    @Bean("numTazRepresentatives")
    public int numTazRepresentatives(LocationChoiceConfiguration configuration) {
        return configuration.numTazRepresentatives();
    }

    @Bean("modeForDistance")
    public TPS_Mode modeForDistance(LocationChoiceConfiguration configuration, Modes modes) {
        String modeName = configuration.modeForDistance();

        TPS_Mode mode = modes.getModeByName(modeName);

        if (mode == null) {
            throw new IllegalArgumentException("Invalid mode in 'modeForDistance' parameter. No mode called: '" + modeName + "' has been defined as mode.");
        }

        return mode;
    }

    @Bean("maxTriesLocationSelection")
    public int maxTriesLocationSelection(LocationChoiceConfiguration configuration) {
        return configuration.maxTriesLocationSelection();
    }
}
