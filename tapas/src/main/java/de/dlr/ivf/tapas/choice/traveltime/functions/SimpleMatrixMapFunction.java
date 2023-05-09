package de.dlr.ivf.tapas.choice.traveltime.functions;

import de.dlr.ivf.tapas.choice.traveltime.MatrixMapFunction;
import de.dlr.ivf.tapas.model.MatrixMap;
import de.dlr.ivf.tapas.model.location.Locatable;

public class SimpleMatrixMapFunction implements MatrixMapFunction {
    private final MatrixMap matrixMap;

    public SimpleMatrixMapFunction(MatrixMap matrixMap) {
        this.matrixMap = matrixMap;
    }

    @Override
    public double apply(Locatable start, Locatable end, int time) {
        return matrixMap.getMatrix(time).getValue(start.getTAZId(), end.getTAZId());
    }
}
