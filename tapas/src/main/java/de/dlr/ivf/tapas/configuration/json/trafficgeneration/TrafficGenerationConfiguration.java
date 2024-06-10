package de.dlr.ivf.tapas.configuration.json.trafficgeneration;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.dlr.ivf.tapas.configuration.json.runner.SchemeProviderConfiguration;

/**
 * Represents the configuration for traffic generation.
 */
public record TrafficGenerationConfiguration(
        @JsonProperty SchemeProviderConfiguration schemeProviderConfiguration
        ) {}
