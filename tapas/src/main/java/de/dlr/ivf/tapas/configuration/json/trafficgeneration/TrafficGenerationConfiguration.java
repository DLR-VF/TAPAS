package de.dlr.ivf.tapas.configuration.json.trafficgeneration;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.dlr.ivf.api.io.configuration.DataSource;
import de.dlr.ivf.api.io.configuration.FilterableDataSource;

/**
 * Represents the configuration for traffic generation.
 */
public record TrafficGenerationConfiguration(
        @JsonProperty int timeSlotLength,
        @JsonProperty FilterableDataSource schemeClasses,
        @JsonProperty FilterableDataSource schemes,
        @JsonProperty FilterableDataSource episodes,
        @JsonProperty FilterableDataSource schemeClassDistributions,
        @JsonProperty DataSource activities
        ) {}
