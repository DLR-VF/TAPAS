package de.dlr.ivf.tapas.choice.distance.functions;

import de.dlr.ivf.tapas.choice.distance.DistanceFunction;
import de.dlr.ivf.tapas.model.IntMatrix;
import de.dlr.ivf.tapas.model.location.Locatable;

/**
 * MatrixDistanceFunction is a class that implements the DistanceFunction interface. It calculates the distance between
 * two Locatable objects using a matrix of distances.
 */
public class MatrixDistanceFunction implements DistanceFunction {

    private final IntMatrix distanceMatrix;

    public MatrixDistanceFunction(IntMatrix distanceMatrix){
        this.distanceMatrix = distanceMatrix;
    }
    /**
     * Calculates the distance between two Locatable objects using a matrix of distances.
     *
     * @param start the starting Locatable object
     * @param end the ending Locatable object
     * @return the distance between the start and end Locatable objects
     */
    @Override
    public double apply(Locatable start, Locatable end) {
        return distanceMatrix.get(start.getTAZId(), end.getTAZId());
    }
}
