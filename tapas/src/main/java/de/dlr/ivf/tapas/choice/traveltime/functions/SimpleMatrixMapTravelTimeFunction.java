package de.dlr.ivf.tapas.choice.traveltime.functions;

import de.dlr.ivf.tapas.choice.traveltime.TravelTimeFunction;
import de.dlr.ivf.tapas.model.IntMatrix;
import de.dlr.ivf.tapas.model.MatrixMap;
import de.dlr.ivf.tapas.model.TPS_Geometrics;
import de.dlr.ivf.tapas.model.location.Locatable;
import de.dlr.ivf.tapas.model.mode.TPS_Mode;

import java.util.Map;

public class SimpleMatrixMapTravelTimeFunction implements TravelTimeFunction {

    private final MatrixMap ttMatrixMap;
    private final IntMatrix blDistanceMatrix;
    private final double minDist;
    private final MatrixMap arrivalMatrixMap;
    private final MatrixMap egressMatrixMap;
    private final TPS_Mode travelMode;

    public SimpleMatrixMapTravelTimeFunction(double minDist,
                                             IntMatrix beelineDistanceMatrix,
                                             Map<String,MatrixMap> matrixMaps,
                                             TPS_Mode travelMode) {

        this.ttMatrixMap = matrixMaps.get("TT");
        if(this.ttMatrixMap == null){
            throw new IllegalArgumentException("No travel time matrix map found for mode: "+travelMode.getName());
        }

        this.arrivalMatrixMap = matrixMaps.get("ACCESS");
        if(this.arrivalMatrixMap == null){
            throw new IllegalArgumentException("No access matrix map found for mode: "+travelMode.getName());
        }

        this.egressMatrixMap = matrixMaps.get("EGRESS");
        if(this.egressMatrixMap == null){
            throw new IllegalArgumentException("No egress matrix map found for mode: "+travelMode.getName());
        }

        this.minDist = minDist;
        this.blDistanceMatrix = beelineDistanceMatrix;
        this.travelMode = travelMode;
    }


    @Override
    public double calculateTravelTime(Locatable start, Locatable end, int time) {
        double tt = -1.0; // this is the indicator, of an invalid tt -time

        double beelineDistanceLoc = TPS_Geometrics.getDistance(start, end, this.minDist);

        double beelineDistanceTAZ = beelineDistanceLoc;

        int startTazId = start.getTAZId();
        int endTazId = end.getTAZId();

        if (startTazId != endTazId) {
            beelineDistanceTAZ = blDistanceMatrix.get(startTazId, endTazId);
        }

        double beelineFactorLocToTaz = beelineDistanceLoc / beelineDistanceTAZ;
        double beelineFactor = beelineFactorLocToTaz < 1 ? travelMode.getBeelineFactor() : beelineFactorLocToTaz;

        final double matrixTravelTime = ttMatrixMap.getMatrix(time).get(startTazId, endTazId);

        // access time
        final double matrixAccessTime = arrivalMatrixMap.getMatrix(time).get(startTazId, endTazId);

        // egress time
        final double matrixEgressTime = egressMatrixMap.getMatrix(time).get(startTazId, endTazId);

        if (TPS_Mode.noConnection(matrixTravelTime)) {
            return TPS_Mode.NO_CONNECTION;
        }

        tt = beelineFactor * matrixTravelTime;
        tt += matrixAccessTime + matrixEgressTime;

        return tt;
    }
}
