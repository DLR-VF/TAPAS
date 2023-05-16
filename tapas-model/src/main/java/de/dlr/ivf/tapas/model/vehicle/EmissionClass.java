package de.dlr.ivf.tapas.model.vehicle;

import java.util.Arrays;

public enum EmissionClass {
    EURO_0(0), EURO_1(1), EURO_2(2), EURO_3(3), EURO_4(4), EURO_5(5), EURO_6(6), NO_EMISSION_CLASS(-1);

    private final int id;

    EmissionClass(int id){
        this.id = id;
    }

    public static EmissionClass getById(int id){
        return Arrays.stream(values())
                .filter(emissionClass -> emissionClass.id == id)
                .findFirst()
                .orElse(NO_EMISSION_CLASS);
    }
}
