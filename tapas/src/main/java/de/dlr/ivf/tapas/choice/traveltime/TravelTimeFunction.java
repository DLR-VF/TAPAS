package de.dlr.ivf.tapas.choice.traveltime;

import de.dlr.ivf.tapas.model.location.Locatable;

/**
 * Interface representing a travel time function that calculates the travel time between two locations at a given time.
 */
public interface TravelTimeFunction {
    /**
     * Calculates the travel time between two locations at a given time.
     *
     * @param start the start location
     * @param end the end location
     * @param time the time of travel
     * @return the travel time between the start and end locations
     */
    double calculateTravelTime(Locatable start, Locatable end, int time);
}
