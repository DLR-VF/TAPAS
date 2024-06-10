package de.dlr.ivf.tapas.model.scheme;

import java.util.Set;

public class Tour{

    private final int tourNumber;
    private final Set<Trip> trips;

    public Tour(int tourNumber, Set<Trip> trips){
        this.tourNumber = tourNumber;
        this.trips = trips;
    }


    public boolean addTrip(Trip trip) {
        return trips.add(trip);
    }

    public int getTourNumber() {
        return tourNumber;
    }
}
