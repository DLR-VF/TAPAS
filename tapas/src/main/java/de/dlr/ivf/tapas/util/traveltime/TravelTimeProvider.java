package de.dlr.ivf.tapas.util.traveltime;

import de.dlr.ivf.tapas.model.location.Locatable;

/**
 * The TravelTimeProvider interface provides a method for calculating travel times between locations.
 *
 * @param <T> the type of context to consider when calculating the distance
 */
public interface TravelTimeProvider<T> {
    /**
     * Calculates the travel time between two locations based on the given mode, start location, end location, and time.
     *
     * @param context the context to consider when calculating the distance
     * @param start the start location
     * @param end   the end location
     * @param time  the time of travel
     * @return the travel time between the start and end locations
     */
    double getTravelTime(T context, Locatable start, Locatable end, int time);
}
