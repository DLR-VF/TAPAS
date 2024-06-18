package de.dlr.ivf.tapas.model.plan;

import de.dlr.ivf.tapas.model.location.TPS_Location;
import de.dlr.ivf.tapas.model.scheme.Stay;
import de.dlr.ivf.tapas.model.scheme.Tour;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

public class TourContext {

    private final Map<Stay, TPS_Location> stayToLocationMap;

    @Getter
    private final Tour tour;

    /**
     * The TourContext class represents a context for a Tour, providing information about the Tour and its associated stays.
     * It maintains a mapping between each Stay and its corresponding location. Locations that are known upfront like the
     * home location for example can be set in advance through the {@link TourContext#addLocation(Stay, TPS_Location)} method.
     */
    public TourContext(Tour tour){

        stayToLocationMap = new HashMap<>();
        this.tour = tour;
    }

    /**
     * Adds a location to the stayToLocationMap in a TourContext object.
     *
     * @param stay The stay for which to add the location
     * @param location The location to add
     * @return The previous location associated with the stay, or null if there was no previous location
     */
    public TPS_Location addLocation(Stay stay, TPS_Location location){
        return stayToLocationMap.putIfAbsent(stay, location);
    }

    /**
     * Returns the location associated with the given Stay.
     *
     * @param stay The Stay for which to retrieve the location.
     * @return The location associated with the given Stay, or null if there is no location.
     */
    public TPS_Location getLocationForStay(Stay stay){
        return stayToLocationMap.get(stay);
    }
}
