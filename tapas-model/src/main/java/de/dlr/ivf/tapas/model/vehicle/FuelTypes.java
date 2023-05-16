package de.dlr.ivf.tapas.model.vehicle;

import lombok.Builder;
import lombok.Singular;

import java.util.Map;

@Builder
public class FuelTypes {

    @Singular
    Map<FuelTypeName, FuelType> fuelTypes;

    public FuelType getFuelType(FuelTypeName fuelTypeName){
        return fuelTypes.get(fuelTypeName);
    }
}
