package de.dlr.ivf.tapas.choice.distance.functions;

import de.dlr.ivf.tapas.choice.distance.MatrixFunction;
import de.dlr.ivf.tapas.model.Matrix;
import de.dlr.ivf.tapas.model.location.Locatable;

public class SimpleMatrixDistanceFunction implements MatrixFunction {

    private final Matrix distanceMatrix;

    public SimpleMatrixDistanceFunction(Matrix matrix){

        this.distanceMatrix = matrix;
    }
    @Override
    public double apply(Locatable start, Locatable end) {
        return distanceMatrix.getValue(start.getTAZId(), end.getTAZId());
    }
}
