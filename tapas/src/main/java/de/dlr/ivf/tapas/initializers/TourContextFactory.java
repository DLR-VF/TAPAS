package de.dlr.ivf.tapas.initializers;

import de.dlr.ivf.tapas.model.location.TPS_Location;
import de.dlr.ivf.tapas.model.plan.TourContext;
import de.dlr.ivf.tapas.model.scheme.Stay;
import de.dlr.ivf.tapas.model.scheme.Tour;

import java.util.Map;

/**
 * The TourContextFactory class is responsible for creating {@link TourContext} instances.
 */
public class TourContextFactory {

    /**
     * Creates a new TourContext object based on the specified Tour and a mapping of pre known activities to locations.
     *
     * @param tour The Tour object for which to create a TourContext
     * @param activityToLocationMappings A mapping of activity IDs to TPS_Location objects
     * @return A new TourContext object
     */
    public TourContext newTourContext(Tour tour, Map<Integer, TPS_Location> activityToLocationMappings){

        TourContext tourContext = new TourContext(tour);


        for (Stay stay : tour.stays()) {

            TPS_Location potentialFixLocation = activityToLocationMappings.get(stay.activity());
            if(potentialFixLocation != null){
                tourContext.addLocation(stay, potentialFixLocation);
            }
        }

        return tourContext;
    }
}
