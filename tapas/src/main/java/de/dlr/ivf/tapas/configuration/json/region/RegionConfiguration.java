package de.dlr.ivf.tapas.configuration.json.region;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeId;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import de.dlr.ivf.api.io.configuration.DataSource;
import de.dlr.ivf.api.io.configuration.FilterableDataSource;

/**
 * The RegionConfiguration class represents the configuration settings for a region.
 */
public record RegionConfiguration(

        @JsonProperty
        LocationConfiguration locationConfiguration,

        @JsonProperty
        DataSource activityToLocationsMapping,

        @JsonProperty
        DataSource distanceClasses,

        @JsonProperty
        FilterableDataSource personGroups,

        @JsonTypeId
        @JsonProperty
        String trafficAnalysisZonesToUse,

        @JsonProperty
        @JsonTypeInfo(
                use = JsonTypeInfo.Id.NAME,
                include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
                property = "trafficAnalysisZonesToUse"
        )
        @JsonSubTypes({
                @JsonSubTypes.Type(value = SimpleTrafficAnalysisZoneConfiguration.class, name = "simple"),
                @JsonSubTypes.Type(value = ExtendedTrafficAnalysisZoneConfiguration.class, name = "extended")
        })
        TrafficAnalysisZoneConfiguration trafficAnalysisZoneConfiguration
) {}
