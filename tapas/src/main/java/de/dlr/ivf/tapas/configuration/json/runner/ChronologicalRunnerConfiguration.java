package de.dlr.ivf.tapas.configuration.json.runner;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.dlr.ivf.tapas.configuration.json.SimulationRunnerConfiguration;
import de.dlr.ivf.tapas.configuration.json.locationchoice.LocationChoiceConfiguration;
import de.dlr.ivf.tapas.configuration.json.modechoice.ModeChoiceConfiguration;
import de.dlr.ivf.tapas.configuration.json.trafficgeneration.TrafficGenerationConfiguration;

public record ChronologicalRunnerConfiguration(
        @JsonProperty TrafficGenerationConfiguration trafficGenerationConfiguration,
        @JsonProperty LocationChoiceConfiguration locationChoiceConfiguration,
        @JsonProperty ModeChoiceConfiguration modeChoiceConfiguration) implements SimulationRunnerConfiguration {

    @Override
    public TrafficGenerationConfiguration getTrafficGenerationConfiguration() {
        return trafficGenerationConfiguration;
    }
}