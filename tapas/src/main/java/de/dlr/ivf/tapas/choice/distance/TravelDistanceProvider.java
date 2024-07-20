package de.dlr.ivf.tapas.choice.distance;

import de.dlr.ivf.tapas.model.location.Locatable;

/**
 * The TravelDistanceProvider interface provides a method to calculate the travel distance between two locatable objects.
 * Implementing classes should define how the distance is calculated based on the specific context and the start and end locations.
 *
 * @param <T> the type of context to consider when calculating the distance
 */
public interface TravelDistanceProvider<T> {

    /**
     * Calculates the travel distance between two locatable objects based on the provided context.
     *
     * @param context the context to consider when calculating the distance
     * @param start the starting location
     * @param end the ending location
     * @return the travel distance between the start and end locations
     */
    double getDistance(T context, Locatable start, Locatable end);
}
