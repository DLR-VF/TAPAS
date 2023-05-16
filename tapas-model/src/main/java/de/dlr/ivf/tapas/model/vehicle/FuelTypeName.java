package de.dlr.ivf.tapas.model.vehicle;


import java.util.Arrays;


/**
 * This enum represents all known fuel types of TAPAS
 */
public enum FuelTypeName {
    BENZINE(0),
    DIESEL(1),
    GAS(2),
    EMOBILE(3),
    PLUGIN(4),
    FUELCELL(5),
    LPG(6),
    NO_FUEL_TYPE(-1);

    private final int id;


    FuelTypeName(int id) {
        this.id = id;
    }

    public static FuelTypeName getById(int id){
        return Arrays.stream(values())
                .filter(fuelType -> fuelType.id == id)
                .findFirst()
                .orElse(NO_FUEL_TYPE);
    }

    public int getId() {
        return id;
    }
}
