package de.dlr.ivf.tapas.configuration.json.region;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.dlr.ivf.api.io.configuration.DataSource;


/**
 * SimpleTrafficAnalysisZoneConfiguration represents the configuration for simple traffic analysis zones.
 * This class implements the TrafficAnalysisZoneConfiguration interface.
 */
public record SimpleTrafficAnalysisZoneConfiguration(
        @JsonProperty DataSource trafficAnalysisZones
) implements TrafficAnalysisZoneConfiguration{
    @Override
    public DataSource dataSource() {
        return trafficAnalysisZones;
    }
}
