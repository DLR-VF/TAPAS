package de.dlr.ivf.tapas.initializers;

import de.dlr.ivf.tapas.model.location.TPS_Location;
import de.dlr.ivf.tapas.model.plan.TourContext;
import de.dlr.ivf.tapas.model.scheme.Stay;
import de.dlr.ivf.tapas.model.scheme.Tour;
import de.dlr.ivf.tapas.model.scheme.Trip;
import de.dlr.ivf.tapas.model.vehicle.Vehicle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * The TourContextFactory class is responsible for creating {@link TourContext} instances.
 */
@Lazy
@Component
public class TourContextFactory {

    private final Vehicle defaultVehicle;

    @Autowired
    public TourContextFactory(@Qualifier("noVehicle") Vehicle defaultVehicle){
        this.defaultVehicle = defaultVehicle;
    }

    /**
     * Creates a new TourContext object based on the specified Tour and a mapping of pre known activities to locations.
     *
     * @param tour The Tour object for which to create a TourContext
     * @param activityToLocationMappings A mapping of activity IDs to TPS_Location objects
     * @return A new TourContext object
     */
    public TourContext newTourContext(Tour tour, Map<Integer, TPS_Location> activityToLocationMappings){

        Map<Stay, Integer> cumulativeTravelDurations = new HashMap<>();
        int totalTravelDuration = 0;

        for(Trip trip : tour.trips()){
            totalTravelDuration +=  trip.durationSeconds();
            cumulativeTravelDurations.put(trip.endStay(), totalTravelDuration);
        }

        TourContext tourContext = new TourContext(tour, cumulativeTravelDurations, totalTravelDuration, defaultVehicle);

        for (Stay stay : tour.stays()) {
            TPS_Location potentialFixLocation = activityToLocationMappings.get(stay.activity());
            if(potentialFixLocation != null){
                tourContext.addLocation(stay, potentialFixLocation);
            }
        }

        return tourContext;
    }
}
