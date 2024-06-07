package de.dlr.ivf.tapas.configuration.json.agent;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.dlr.ivf.api.io.configuration.DataSource;
import de.dlr.ivf.api.io.configuration.FilterableDataSource;

/**
 * Represents the configuration for a household in the TAPAS application.
 * It contains various properties related to household configuration, such as cars configuration,
 * households data source, member order, persons data source, age classes data source,
 * person groups data source, income classes data source, availability factors for bike and car sharing,
 * shopping motives usage, driving license usage, rejuvenation threshold and parameters, and minimum age for car sharing.
 */
public record HouseholdConfiguration(
        @JsonProperty CarsConfiguration carsConfiguration,
        @JsonProperty FilterableDataSource households,
        @JsonProperty String memberOrder,
        @JsonProperty FilterableDataSource persons,
        @JsonProperty DataSource ageClasses,
        @JsonProperty FilterableDataSource personGroups,
        @JsonProperty DataSource incomeClasses,
        @JsonProperty double availabilityFactorBike,
        @JsonProperty double availabilityFactorCarSharing,
        @JsonProperty boolean useShoppingMotives,
        @JsonProperty boolean useDrivingLicense,
        @JsonProperty int rejuvenationThreshold,
        @JsonProperty int rejuvenateByYears,
        @JsonProperty int minAgeCarSharing
){}
