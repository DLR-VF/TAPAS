package de.dlr.ivf.tapas.choice.traveltime.functions;

import de.dlr.ivf.tapas.choice.traveltime.TravelTimeFunction;
import de.dlr.ivf.tapas.model.Matrix;
import de.dlr.ivf.tapas.model.MatrixMap;
import de.dlr.ivf.tapas.model.TPS_Geometrics;
import de.dlr.ivf.tapas.model.location.Locatable;
import de.dlr.ivf.tapas.model.mode.TPS_Mode;
import de.dlr.ivf.tapas.model.parameter.ParamFlag;
import de.dlr.ivf.tapas.model.parameter.ParamMatrix;
import de.dlr.ivf.tapas.model.parameter.ParamValue;
import de.dlr.ivf.tapas.model.parameter.TPS_ParameterClass;

public class SimpleMatrixTravelTimeFunction implements TravelTimeFunction {

    private final MatrixMap ttMatrixMap;
    private final MatrixMap accessMatrixMap;
    private final MatrixMap egressMatrixMap;
    private final boolean useIntraTazInfo;
    private final TPS_Mode mode;
    private final double minDist;
    private final Matrix beelineMatrix;

    public SimpleMatrixTravelTimeFunction(MatrixMap ttMatrixMap, MatrixMap accessMatrixMap, MatrixMap egressMatrixMap,
                                          TPS_ParameterClass parameterClass, TPS_Mode mode){
        this.ttMatrixMap = ttMatrixMap;
        this.accessMatrixMap = accessMatrixMap;
        this.egressMatrixMap = egressMatrixMap;
        this.useIntraTazInfo = parameterClass.isTrue(ParamFlag.FLAG_INTRA_INFOS_MATRIX);
        this.minDist = parameterClass.getDoubleValue(ParamValue.MIN_DIST);
        this.mode = mode;
        this.beelineMatrix = parameterClass.getMatrix(ParamMatrix.DISTANCES_BL);
    }
    @Override
    public double apply(Locatable start, Locatable end, int time) {

        double beelineDistance = TPS_Geometrics.getDistance(start, end, minDist);
        double tt = beelineDistance * mode.getBeelineFactor() / mode.getVelocity();

        int idStart = start.getTrafficAnalysisZone().getTAZId();
        int idDest = end.getTrafficAnalysisZone().getTAZId();
        if (idStart == idDest) {
            // start and end locations are in the same traffic analysis zone
            if (this.useIntraTazInfo) {
                // If there exists travel times inside a traffic
                // analysis zone this value is used.
                // if not use default tt
                tt = this.ttMatrixMap.getMatrix(time).getValue(idStart, idDest);
            }
        } else {
            // start and end locations are in different traffic analysis
            // zones. The travel time is calculated by the travel
            // time from a table and a factor retrieved by the beeline
            // and the real distance.
            tt = this.ttMatrixMap.getMatrix(time).getValue(idStart, idDest);
            tt *= beelineDistance / this.beelineMatrix.getValue(idStart, idDest);
        }
        tt += accessMatrixMap.getMatrix(time).getValue(idStart, idDest);
        tt += egressMatrixMap.getMatrix(time).getValue(idStart, idDest);

        return tt;
    }
}
