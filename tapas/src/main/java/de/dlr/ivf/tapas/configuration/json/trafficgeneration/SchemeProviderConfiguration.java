package de.dlr.ivf.tapas.configuration.json.trafficgeneration;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.dlr.ivf.api.io.configuration.DataSource;
import de.dlr.ivf.api.io.configuration.FilterableDataSource;

/**
 * SchemeProviderConfiguration represents the configuration of a scheme provider.
 *
 * It contains the following properties:
 * - timeSlotLength: The length of time slot.
 * - tripActivityCode: the code in the episodes datasource that marks trips
 * - schemeClasses: The data source for scheme classes.
 * - schemes: The data source for schemes.
 * - episodes: The data source for episodes.
 * - schemeClassDistributions: The data source for scheme class distributions.
 * - activities: The data source for activities.
 * - personGroups: The data source for person groups.
 */
public record SchemeProviderConfiguration(
        @JsonProperty int schemeClassChoiceSeed,
        @JsonProperty int schemeChoiceSeed,
        @JsonProperty int timeSlotLength,
        @JsonProperty int tripActivityCode,
        @JsonProperty FilterableDataSource schemeClasses,
        @JsonProperty FilterableDataSource schemes,
        @JsonProperty FilterableDataSource episodes,
        @JsonProperty FilterableDataSource schemeClassDistributions,
        @JsonProperty DataSource activities,
        @JsonProperty FilterableDataSource personGroups
) {
}
