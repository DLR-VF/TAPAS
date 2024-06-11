package de.dlr.ivf.tapas.configuration.json.trafficgeneration;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents the configuration for traffic generation.
 */
public record TrafficGenerationConfiguration(
        @JsonProperty SchemeProviderConfiguration schemeProviderConfiguration
        ) {}
