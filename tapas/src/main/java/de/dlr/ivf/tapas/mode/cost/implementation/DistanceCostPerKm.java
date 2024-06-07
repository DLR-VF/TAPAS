package de.dlr.ivf.tapas.mode.cost.implementation;

import de.dlr.ivf.tapas.mode.cost.ModeDistanceCostFunction;
import lombok.Getter;

/**
 * Represents the cost per kilometer for a distance-based cost function.
 * The cost is calculated by multiplying the distance by the cost per kilometer.
 */
@Getter
public final class DistanceCostPerKm implements ModeDistanceCostFunction {


    /**
     * Represents the cost per kilometer for a distance-based cost function.
     * The cost is calculated by multiplying the distance by the cost per kilometer.
     */
    private final double costPerKm;

    public DistanceCostPerKm(double costPerKm){
        this.costPerKm = costPerKm;
    }

    /**
     * Calculates the cost based on the distance traveled.
     *
     * @param distance the distance traveled in kilometers.
     * @return the cost of the trip.
     */
    @Override
    public double computeCost(double distance) {
        return distance * costPerKm * 0.001;
    }
}
