package de.dlr.ivf.tapas.model.location;

import de.dlr.ivf.tapas.model.constants.TPS_ActivityConstant;
import de.dlr.ivf.tapas.util.Randomizer;

import java.util.ArrayList;
import java.util.List;

/**
 * A container for locations of the same activity type that are located in this TAZ
 */
public class TypedWeightedLocationDistribution {
    /// The list of locations with the same activity
    public List<TPS_Location> locations = new ArrayList<>();
    /// The sum of all the weights of all locations that are within this TAZ and allow the according activity
    private double freeWeightSum = 0;


    /**
     * Constructor
     */
    public TypedWeightedLocationDistribution() {
    }

    public TypedWeightedLocationDistribution(TPS_ActivityConstant tpsActivityConstant) {
    }

    /**
     * Adds a location
     *
     * @param loc The location to add
     */
    public void addLocation(TPS_Location loc) {
        locations.add(loc);
        freeWeightSum = freeWeightSum + loc.getData().getWeight();
    }

    /**
     * Returns the sum of the stored locations weights
     *
     * @return The sum of the locations weights
     */
    public double getWeightSum() {
        return freeWeightSum;
    }

    /**
     * Chooses a random location, weighted by the locations' weights
     *
     * @return A randomly selected weighted location
     */
    public TPS_Location select() {
        return select(freeWeightSum, locations);
    }

    /**
     * Selects locations from the given list randomly, taking into account their weight
     *
     * @param weightSum The sum of the location's weights
     * @param from      The list of locations to choose from
     * @return A single selected location
     */
    private TPS_Location select(double weightSum, List<TPS_Location> from) {
        /*
         *  A microscopic sum of weights causes strange floating point issues,
         *  which cannot be caught by the 2nd loop: Namely hundreds of
         *  tiny weights sum up to a number barely above zero. But each
         *  weight is interpreted as zero by itself! -> Crash!
         */

        if (weightSum < 1e-15 && from.size() > 0) return from.get(0);

        double rPos = Randomizer.random() * (double) (int) weightSum;
        // use binary search? -> no!
        for (TPS_Location loc : from) {
            double locWeight = loc.getData().getWeight();
            if (rPos < locWeight) {
                return loc;
            }
            rPos -= locWeight;
        }
        // ok, catching potentially occuring floating point issues
        for (TPS_Location loc : from) {
            double locWeight = loc.getData().getWeight();
            if (locWeight != 0) {
                return loc;
            }
        }
        //last resort
        if (from.size() > 0) return from.get(0);
        else return null;
        //throw new RuntimeException("Ran over available weights; Input was: weightSum=" + weightSum);
    }

    /**
     * Chooses the given number of locations randomly, taking their weights into account
     *
     * @param number The number of locations to choose
     * @return List of selected locations
     */
    public ArrayList<TPS_Location> selectActivityLocations(int number) {
        ArrayList<TPS_Location> ret = new ArrayList<>();
        if (this.locations.size() <= number) {
            ret.addAll(locations);
        } else {
            double cCapacitySum = freeWeightSum;
            List<TPS_Location> cLocations = new ArrayList<>(locations);
            for (int i = 0; i < number; ++i) {
                TPS_Location selected = select(cCapacitySum, cLocations);
                if (selected == null) {
                    break;
                }
                ret.add(selected);
                cCapacitySum -= selected.getData().getWeight();
                cLocations.remove(selected);
            }
        }
        return ret;
    }

    /**
     * Updates the weights after changing the weight of a location given by the prior and current weights
     *
     * @param priorWeight The prior weight of the location
     * @param postWeight  The new weight of the location
     */
    public void updateOccupancy(double priorWeight, double postWeight) {
        if (postWeight > 1e-15 && freeWeightSum > 1e-15) {
            freeWeightSum = freeWeightSum - priorWeight + postWeight;
        } else {
            //very tiny numbers: calculate from scratch to avoid strange floating point errors!
            freeWeightSum = 0;
            for (TPS_Location t : locations) {
                freeWeightSum += t.getData().getWeight();
            }
        }
    }

}
