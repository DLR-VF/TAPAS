package de.dlr.ivf.tapas.tools.matrixMap;

/**
 * @author Holger Siedel
 */
public enum Modus {
    // PT(0.470), MIV(0.380), BIKE(0.21), WALK(0.15);
    // PT(0.670), MIV(0.480), BIKE(0.145), WALK(0.09);
    WALK(0.15, 0, "WALK"), BIKE(0.145, 1, "BIKE"), PT(0.670, 2, "PT"), MIV(0.480, 3, "MIV");

    private final double correctionFactor;
    private final int value;
    private final String name;

    Modus(double correctionFactor, int value, String name) {
        this.correctionFactor = correctionFactor;
        this.value = value;
        this.name = name;
    }

    public double getCorrectionFactor() {
        return this.correctionFactor;
    }

    public String getName() {
        return name;
    }

    public int getValue() {
        return value;
    }
}
