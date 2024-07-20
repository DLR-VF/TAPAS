package de.dlr.ivf.tapas.choice.distance.providers;

import de.dlr.ivf.tapas.choice.distance.DistanceFunction;
import de.dlr.ivf.tapas.choice.distance.TravelDistanceProvider;
import de.dlr.ivf.tapas.model.location.Locatable;
import de.dlr.ivf.tapas.model.mode.TPS_Mode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Map;


/**
 * This class is temporary and wraps the 'getDistance()' methods from the TPS_Mode class and its implementations.
 *
 * @author Alain Schengen
 */
@Lazy
@Component
public class TravelDistanceCalculator implements TravelDistanceProvider<TPS_Mode> {

    private final Map<TPS_Mode, DistanceFunction> modeDistanceFunctions;

    @Autowired
    public TravelDistanceCalculator(Map<TPS_Mode, DistanceFunction> modeDistanceFunctions){

        this.modeDistanceFunctions = modeDistanceFunctions;
    }


    @Override
    public double getDistance(TPS_Mode mode, Locatable start, Locatable end){

        return this.modeDistanceFunctions.get(mode).apply(start, end);
    }
}
