package de.dlr.ivf.tapas.configuration.json.runner;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.dlr.ivf.tapas.configuration.json.SimulationRunnerConfiguration;

public record TripPriorityRunnerConfiguration(
        @JsonProperty int maxTriesSchemeSelection,
        @JsonProperty int workerCount

        ) implements SimulationRunnerConfiguration {
}
