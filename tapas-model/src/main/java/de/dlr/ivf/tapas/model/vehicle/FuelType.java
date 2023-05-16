package de.dlr.ivf.tapas.model.vehicle;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class FuelType {

    private final FuelTypeName fuelType;
    private final double fuelCostPerKm;
    private final double variableCostPerKm;
    private final double range;
}
