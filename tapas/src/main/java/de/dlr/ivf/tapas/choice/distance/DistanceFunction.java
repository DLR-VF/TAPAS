package de.dlr.ivf.tapas.choice.distance;

import de.dlr.ivf.tapas.model.location.Locatable;

/**
 * The DistanceFunction interface represents a function that calculates the distance
 * between two Locatable objects.
 */
public interface DistanceFunction {

    double apply(Locatable start, Locatable end);
}
