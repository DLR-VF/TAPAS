package de.dlr.ivf.tapas.mode.cost.implementation;

import de.dlr.ivf.tapas.mode.cost.ModeDistanceCostFunction;


/**
 * The `FixCost` class represents a fixed cost for a public transportation mode.
 * This value is used as a constant cost for a specific public transportation mode.
 * It is added to the overall cost calculation for a trip, regardless of the distance traveled.
 */
public final class FixCost implements ModeDistanceCostFunction {

    /**
     * Represents a fixed cost for a public transportation mode.
     *
     * This value is used as a constant cost for a specific public transportation mode.
     * It is added to the overall cost calculation for a trip, regardless of the distance traveled.
     */
    private final double fixCost;

    public FixCost(double fixCost){
        this.fixCost = fixCost;
    }

    /**
     * Computes the cost for a specific public transportation mode.
     *
     * This method calculates the cost based on a fixed cost for the mode, regardless of the distance traveled.
     *
     * @param distance the distance traveled in kilometers.
     * @return the cost of the trip.
     */
    @Override
    public double computeCost(double distance) {
        return fixCost;
    }
}
