package de.dlr.ivf.tapas.configuration.json.region;

import de.dlr.ivf.api.io.configuration.DataSource;


/**
 * The TrafficAnalysisZoneConfiguration interface represents the configuration for traffic analysis zones.
 * It is a sealed interface which permits the subclasses ExtendedTrafficAnalysisZoneConfiguration and
 * SimpleTrafficAnalysisZoneConfiguration.
 */
public sealed interface TrafficAnalysisZoneConfiguration permits ExtendedTrafficAnalysisZoneConfiguration, SimpleTrafficAnalysisZoneConfiguration{
    DataSource dataSource();
}
