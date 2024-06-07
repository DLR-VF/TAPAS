package de.dlr.ivf.tapas.configuration.json.region;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.dlr.ivf.api.io.configuration.DataSource;
import de.dlr.ivf.api.io.configuration.FilterableDataSource;

/**
 * ExtendedTrafficAnalysisZoneConfiguration is a class that represents the extended configuration for traffic analysis zones.
 * It implements the TrafficAnalysisZoneConfiguration interface.
 */
public record ExtendedTrafficAnalysisZoneConfiguration(
        @JsonProperty DataSource trafficAnalysisZones,
        @JsonProperty FilterableDataSource tazScores,
        @JsonProperty FilterableDataSource intraMitInfo,
        @JsonProperty FilterableDataSource intraMitInfoBase,
        @JsonProperty FilterableDataSource intraPtInfo,
        @JsonProperty FilterableDataSource intraPtInfoBase,
        @JsonProperty FilterableDataSource feesAndTolls
        ) implements TrafficAnalysisZoneConfiguration{
    @Override
    public DataSource dataSource() {
        return trafficAnalysisZones;
    }
}
