package de.dlr.ivf.tapas.model.location;

import de.dlr.ivf.tapas.util.TPS_FastMath;
import lombok.Builder;

@Builder
public class LocationData {
    private final boolean updateLocationWeights;
    private final double weightOccupancy;
    /// Capacity of the location
    private final int capacity = 0;

    /**
     * This flag indicates if the location has a fixed capacity, i.e.
     * occupancy <= capacity (e.g. theatres, enterprises, etc). If the flag
     * is false more people can choose this location than its capacity
     * provides (e.g. parks, museums, shops, etc.).
     */
    private final boolean fixCapacity;

    /// number of persons that chose that location
    private int occupancy = 0;

    /// Weight represents the amount of free capacities
    private double weight = 0;

    /**
     * This method calculates the weight of the location by a decreasing
     * exponential function, so the weight is always greater than 0.
     * <p>
     * Since very small weights produce nasty cast errors in sql an
     * overloadfactor is introduced: Locations with an overload of 10 get a
     * weight of 0!
     */
    public void calculateWeight() {
        double val;
        if (this.capacity == 0) {
            val = 0;
        } else if (this.occupancy <= 0) {
            val = this.capacity;
            this.occupancy = 0;
        } else if (fixCapacity) {
            val = this.capacity - this.occupancy;
        } else {
            if (this.occupancy < 10.0 * this.capacity) {
                val = this.capacity * TPS_FastMath.exp(
                        -weightOccupancy * this.occupancy /
                                this.capacity);
            } else {
                val = 0;
            }
        }
        this.weight = val;
    }

    /**
     * Returns the capacity of a location
     *
     * @return capacity
     */
    public int getCapacity() {
        return capacity;
    }

    /**
     * Returns the occupancy of the location
     *
     * @return occupancy
     */
    public int getOccupancy() {
        return occupancy;
    }

    /**
     * Returns the weight of the location
     *
     * @return weight (always greater than 0)
     */
    public double getWeight() {
        return weight;
    }

    /**
     * Flag if the capacity of the location is fix
     *
     * @return true if fix, false else
     */
    public boolean hasFixCapacity() {
        return fixCapacity;
    }

    /**
     * Initialises this location's capacity and occupancy and computes the respective weight
     *
     * */
    public void init() {
        this.calculateWeight();
    }


    /**
     * Alters an occupancy
     *
     * @param deltaValue The delta to change
     */
    public void changeOccupancy(int deltaValue) {
        synchronized (this) {
            this.occupancy += deltaValue;
            if (updateLocationWeights) {
                this.calculateWeight();
            }
        }
    }

    /**
     * Returns this object's string representation
     *
     * @see Object#toString()
     */
    @Override
    public String toString() {
        return "[" + this.getOccupancy() + "/" + this.getCapacity() + ", fix=" + this.hasFixCapacity() +
                ", weight=" + this.getWeight() + "]";
    }
}
