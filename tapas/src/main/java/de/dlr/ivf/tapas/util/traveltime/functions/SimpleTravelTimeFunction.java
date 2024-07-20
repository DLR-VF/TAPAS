package de.dlr.ivf.tapas.util.traveltime.functions;

import de.dlr.ivf.tapas.util.traveltime.TravelTimeFunction;
import de.dlr.ivf.tapas.model.MatrixMapLegacy;
import de.dlr.ivf.tapas.model.location.Locatable;


//todo fliegt raus
public class SimpleTravelTimeFunction implements TravelTimeFunction {
    private final MatrixMapLegacy matrixMap;

    public SimpleTravelTimeFunction(MatrixMapLegacy matrixMap) {
        this.matrixMap = matrixMap;
    }

    @Override
    public double calculateTravelTime(Locatable start, Locatable end, int time) {
        return matrixMap.getMatrix(time).getValue(start.getTAZId(), end.getTAZId());
    }
}
