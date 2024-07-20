package de.dlr.ivf.tapas.choice.distance.providers;

import de.dlr.ivf.tapas.choice.distance.DistanceFunction;
import de.dlr.ivf.tapas.choice.distance.DistanceProvider;
import de.dlr.ivf.tapas.model.location.Locatable;
import de.dlr.ivf.tapas.model.mode.TPS_Mode;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * The ModeMatrixDistanceProvider class is responsible for calculating travel distances using a matrix-based approach.
 * It implements the DistanceProvider interface for the TPS_Mode type.
 */
@Lazy
@Component
public class ModeMatrixDistanceProvider implements DistanceProvider<TPS_Mode> {

    private final Map<TPS_Mode, DistanceFunction> travelDistanceFunctions;

    public ModeMatrixDistanceProvider(Map<TPS_Mode, DistanceFunction> travelDistanceFunctions){

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
