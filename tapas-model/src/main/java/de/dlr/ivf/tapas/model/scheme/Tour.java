package de.dlr.ivf.tapas.model.scheme;

import java.util.NavigableSet;

/**
 * The Tour class represents a tour, which consists of a tour number, a set of trips, and a set of stays. Since internal
 * datastructures for trips and stays are based on a {@link NavigableSet} this class offers helper methods for fetching
 * previous and succeeding trips/stays as direct delegates to {@link NavigableSet#lower(Object)} and {@link NavigableSet#higher(Object)}
 */
public record Tour(
        int id,
        int tourNumber,
        NavigableSet<Trip> trips,
        NavigableSet<Stay> stays
){

    /**
     * Returns the previous trip in the tour based on the specified trip.
     *
     * @param trip the trip for which to find the previous trip
     * @return the previous trip, or null if there is no previous trip
     */
    public Trip previousTrip(Trip trip){
        return trips.lower(trip);
    }

    /**
     * Returns the succeeding trip in the tour based on the specified trip.
     *
     * @param trip the trip for which to find the succeeding trip
     * @return the succeeding trip, or null if there is no succeeding trip
     */
    public Trip succeedingTrip(Trip trip){
        return trips.higher(trip);
    }

    /**
     * Returns the previous stay in the tour based on the specified stay.
     *
     * @param stay the stay for which to find the previous stay
     * @return the previous stay, or null if there is no previous stay
     */
    public Stay previousStay(Stay stay){
        return stays.lower(stay);
    }

    /**
     * Returns the succeeding stay in the tour based on the specified stay.
     *
     * @param stay the stay for which to find the succeeding stay
     * @return the succeeding stay, or null if there is no succeeding stay
     */
    public Stay succeedingStay(Stay stay){
        return stays.higher(stay);
    }
}
