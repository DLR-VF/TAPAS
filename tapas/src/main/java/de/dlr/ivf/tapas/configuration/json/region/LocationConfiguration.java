package de.dlr.ivf.tapas.configuration.json.region;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.dlr.ivf.api.io.configuration.FilterableDataSource;

/**
 * The LocationConfiguration class represents the configuration settings for locations.
 */
public record LocationConfiguration(
        @JsonProperty FilterableDataSource locations,
        @JsonProperty boolean useLocationsGroups,
        @JsonProperty boolean updateLocationWeights,
        @JsonProperty double occupancyWeight
        ) {
}
