package de.dlr.ivf.tapas.configuration.json.agent;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.dlr.ivf.api.io.configuration.FilterableDataSource;

/**
 * Represents the configuration for cars in the TAPAS application.
 * It contains various properties related to car configuration, such as cost per kilometer for different fuel types,
 * range of electric cars, plugin range, and conventional range.
 */
public record CarsConfiguration(
        @JsonProperty FilterableDataSource cars,
        @JsonProperty double mitElectricCostPerKm,
        @JsonProperty double mitGasolineCostPerKm,
        @JsonProperty double mitDieselCostPerKm,
        @JsonProperty double mitGasCostPerKm,
        @JsonProperty double mitPluginCostPerKm,
        @JsonProperty double mitFuelCellCostPerKm,
        @JsonProperty double mitVariableCostPerKm,
        @JsonProperty double mitElectricRange,
        @JsonProperty double mitPluginRange,
        @JsonProperty double mitConventionalRange
){}