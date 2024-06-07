package de.dlr.ivf.tapas.mode.cost.implementation;

import de.dlr.ivf.tapas.mode.cost.ModeDistanceCostFunction;

/**
 * Represents the cost of a taxi ride in Berlin.
 */
public final class BerlinTaxiCost implements ModeDistanceCostFunction {

    /**
     * Represents the base charge for a taxi ride in Berlin.
     */
    private final double baseCharge;

    /**
     * Represents the cost per kilometer for short distance taxi rides in Berlin.
     */
    private final double shortCostPerKm;

    /**
     * Represents the cost per kilometer for long distance taxi rides in Berlin.
     */
    private final double longCostPerKm;

    /**
     * Represents the threshold distance between short and long taxi rides in Berlin.
     */
    private final double shortToLongDistanceThreshold;

    public BerlinTaxiCost(){
        this.baseCharge = 3.0;
        this.shortCostPerKm = 1.58;
        this.longCostPerKm = 1.2;
        this.shortToLongDistanceThreshold = 7000;
    }
    /**
     * Computes the cost of a taxi ride based on the distance traveled.
     *
     * @param distance the distance traveled in kilometers.
     * @return the cost of the taxi ride.
     */
    @Override
    public double computeCost(double distance) {

        double shortCharge = Math.min(shortToLongDistanceThreshold, distance) * 0.001 * shortCostPerKm;
        double longCharge = Math.max(0, distance - shortToLongDistanceThreshold) * 0.001 * longCostPerKm;
        return baseCharge + shortCharge + longCharge;
    }
}
