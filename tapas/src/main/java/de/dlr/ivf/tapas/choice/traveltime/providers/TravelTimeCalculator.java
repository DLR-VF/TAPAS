package de.dlr.ivf.tapas.choice.traveltime.providers;

import de.dlr.ivf.tapas.choice.traveltime.TravelTimeFunction;
import de.dlr.ivf.tapas.choice.traveltime.TravelTimeProvider;
import de.dlr.ivf.tapas.model.location.Locatable;
import de.dlr.ivf.tapas.model.mode.TPS_Mode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * The TravelTimeCalculator class is responsible for calculating travel times between locations taking into account a
 * mode of transportation.
 */
@Lazy
@Component("tazTravelTimeCalculator")
public class TravelTimeCalculator implements TravelTimeProvider<TPS_Mode> {

    private final Map<TPS_Mode, TravelTimeFunction> interTravelTimeFunctions;
    private final Map<TPS_Mode, TravelTimeFunction> intraTazTravelTimeFunctions;

    @Autowired
    public TravelTimeCalculator(@Qualifier("interTazTravelTimeFunctions") Map<TPS_Mode, TravelTimeFunction> interTravelTimeFunctions,
                                @Qualifier("intraTazTravelTimeFunctions") Map<TPS_Mode, TravelTimeFunction> intraTazTravelTimeFunctions){

        this.interTravelTimeFunctions = interTravelTimeFunctions;
        this.intraTazTravelTimeFunctions = intraTazTravelTimeFunctions;
    }

    /**
     * Calculates the travel time between two locations based on the given mode, start location, end location, and time.
     *
     *
     * @param mode the mode of transportation
     * @param start the start location
     * @param end the end location
     * @param time the time of travel
     * @return the travel time between the start and end locations
     */
    public double getTravelTime(TPS_Mode mode, Locatable start, Locatable end, int time){

        return start.getTAZId() == end.getTAZId()
                ? intraTazTravelTimeFunctions.get(mode).calculateTravelTime(start, end, time)
                : interTravelTimeFunctions.get(mode).calculateTravelTime(start, end, time);
    }

    /**
     * Extracted from TPS_Mode
     * Method to check if the travel time is in a valid range
     *
     * @param tt travel time to be checked
     * @return true if tt is a valid travel time
     */
    private boolean travelTimeIsInvalid(double tt) {
        boolean returnValue = Double.isNaN(tt) || Double.isInfinite(tt);
        if (!returnValue && (tt < 0.0 || tt >= 100000.0)) { // positive and less than a day + x
            returnValue = true;
        }
        return returnValue;
    }
}
