package de.dlr.ivf.tapas.configuration.json.modechoice;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SimpleModeParameters(
        @JsonProperty String name,
        @JsonProperty double beelineFactor,
        @JsonProperty double costPerKm,
        @JsonProperty double costPerKmBase,
        @JsonProperty double velocity,
        @JsonProperty boolean useBase,
        @JsonProperty double variableCostPerKm,
        @JsonProperty double variableCostPerKmBase
        ) {
}
