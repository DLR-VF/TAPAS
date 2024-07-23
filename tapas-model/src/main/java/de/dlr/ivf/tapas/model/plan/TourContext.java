package de.dlr.ivf.tapas.model.plan;

import de.dlr.ivf.tapas.model.location.TPS_Location;
import de.dlr.ivf.tapas.model.scheme.Stay;
import de.dlr.ivf.tapas.model.scheme.Tour;
import de.dlr.ivf.tapas.model.vehicle.ModeVehicleContext;
import de.dlr.ivf.tapas.model.vehicle.Vehicle;
import lombok.Getter;

import java.util.*;

/**
 * The TourContext class represents the context of a tour, which includes information such as the tour itself,
 * a mapping of stays to locations, cumulative travel durations to stays, and total travel duration of the tour.
 * Locations that are known upfront like the home location for example can be set in advance through the
 * {@link TourContext#addLocation(Stay, TPS_Location)} method.
 */
public class TourContext {

    private final Map<Stay, TPS_Location> stayToLocationMap;
    private final Map<Stay, Integer> cumulativeTravelDurations;
    private final int totalTravelDuration;
    private final List<ModeVehicleContext> fixModeAndVehicle;
    private final Map<Stay, ModeVehicleContext> stayToModeMap;
    private final Vehicle defaultVehicle;

    @Getter
    private final Tour tour;

    /**
     * Constructs a new {@link TourContext} instance.
     *
     * @param tour The {@link Tour} that this context is build around
     * @param cumulativeTravelDurations A mapping of cumulative travel times to each stay
     * @param totalTravelDuration The total travel time of the tour
     */
    public TourContext(Tour tour, Map<Stay, Integer> cumulativeTravelDurations, int totalTravelDuration, Vehicle defaultVehicle){

        stayToLocationMap = new HashMap<>();
        this.tour = tour;
        this.cumulativeTravelDurations = cumulativeTravelDurations;
        this.totalTravelDuration = totalTravelDuration;
        this.fixModeAndVehicle = new ArrayList<>(1);
        this.stayToModeMap = new HashMap<>();
        this.defaultVehicle = defaultVehicle;
    }

    /**
     * Adds a location to the specified stay.
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

    /**
     * Adds a mode vehicle context for a given stay.
     *
     * @param stay The stay for which to add the mode vehicle context
     * @param modeVehicleContext The mode vehicle context to add
     */
    public void addModeForStay(Stay stay, ModeVehicleContext modeVehicleContext){
        if(modeVehicleContext.usesBoundMode() && fixModeAndVehicle.isEmpty()){
            fixModeAndVehicle.add(modeVehicleContext);
        }
        stayToModeMap.put(stay, modeVehicleContext);
    }

    /**
     * Retrieves the vehicle that is bound to this tour or null if no bound vehicle has been used.
     *
     * @return The vehicle that is bound to this tour or null if not present.
     */
    public Vehicle getBoundVehicle(){

        return fixModeAndVehicle.isEmpty() ? defaultVehicle : fixModeAndVehicle.getFirst().vehicle();
    }


    /**
     * Retrieves the cumulative travel duration to a specific Stay in the Tour.
     *
     * @param stay The Stay for which to retrieve the cumulative travel duration.
     * @return The cumulative travel duration to the specified Stay.
     */
    public int getCumulativeTravelDurationToStay(Stay stay) {
        return cumulativeTravelDurations.get(stay);
    }

    /**
     * Retrieves the remaining travel duration after staying at a specific Stay in the Tour.
     *
     * @param stay The Stay for which to calculate the remaining travel duration.
     * @return The remaining travel duration after staying at the specified Stay.
     */
    public int getRemainingTravelDurationAfterStay(Stay stay) {
        return totalTravelDuration - cumulativeTravelDurations.get(stay);
    }
}
