package de.dlr.ivf.tapas.choice.distance.functions;

import de.dlr.ivf.tapas.choice.distance.DistanceFunction;
import de.dlr.ivf.tapas.choice.distance.TravelDistanceProvider;
import de.dlr.ivf.tapas.model.location.Locatable;
import de.dlr.ivf.tapas.model.mode.TPS_Mode;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * The MatrixTravelDistanceProvider class is responsible for calculating travel distances using a matrix-based approach.
 * It implements the TravelDistanceProvider interface for the TPS_Mode type.
 */
@Lazy
@Component
public class MatrixTravelDistanceProvider implements TravelDistanceProvider<TPS_Mode> {

    private final Map<TPS_Mode, DistanceFunction> travelDistanceFunctions;

    public MatrixTravelDistanceProvider(Map<TPS_Mode, DistanceFunction> travelDistanceFunctions){

        this.travelDistanceFunctions = travelDistanceFunctions;
    }
    /**
     * Calculates the travel distance between two locatable objects based on the provided mode, start location, and end location.
     *
     * @param mode the mode of transportation
     * @param start the starting location
     * @param end the ending location
     * @return the travel distance between the start and end locations
     */
    @Override
    public double getDistance(TPS_Mode mode, Locatable start, Locatable end) {
        return travelDistanceFunctions.get(mode).apply(start, end);
    }
}
