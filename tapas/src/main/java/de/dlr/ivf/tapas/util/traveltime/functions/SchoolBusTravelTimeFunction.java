package de.dlr.ivf.tapas.util.traveltime.functions;

import de.dlr.ivf.tapas.util.traveltime.TravelTimeFunction;
import de.dlr.ivf.tapas.model.MatrixLegacy;
import de.dlr.ivf.tapas.model.MatrixMapLegacy;
import de.dlr.ivf.tapas.model.TPS_Geometrics;
import de.dlr.ivf.tapas.model.location.Locatable;
import de.dlr.ivf.tapas.model.parameter.ParamMatrix;
import de.dlr.ivf.tapas.model.parameter.ParamValue;
import de.dlr.ivf.tapas.model.parameter.TPS_ParameterClass;

public class SchoolBusTravelTimeFunction implements TravelTimeFunction {

    private final MatrixMapLegacy avgSpeedSchoolBusMatrixMap;
    private final double defaultSchoolBusAccess;
    private final double defaultSchoolBusEgress;
    private final double defaultSchoolBusWait;
    private final double minDist;
    private final MatrixLegacy blDistMatrix;

    public SchoolBusTravelTimeFunction(TPS_ParameterClass parameterClass){
        //todo fix school bus matrix
        this.avgSpeedSchoolBusMatrixMap = null; //parameterClass.paramMatrixMapClass.getMatrixMap(ParamMatrixMap.AVERAGE_SPEED_SCHOOLBUS, parameterClass.getSimulationType());
        this.defaultSchoolBusAccess = parameterClass.getDoubleValue(ParamValue.DEFAULT_SCHOOL_BUS_ACCESS);
        this.defaultSchoolBusEgress = parameterClass.getDoubleValue(ParamValue.DEFAULT_SCHOOL_BUS_EGRESS);
        this.defaultSchoolBusWait = parameterClass.getDoubleValue(ParamValue.DEFAULT_SCHOOL_BUS_WAIT);
        this.minDist = parameterClass.getDoubleValue(ParamValue.MIN_DIST);
        this.blDistMatrix = parameterClass.getMatrix(ParamMatrix.DISTANCES_BL);

    }
    @Override
    public double calculateTravelTime(Locatable start, Locatable end, int time) {


        int startTazId = start.getTrafficAnalysisZone().getTAZId();
        int endTazId = end.getTrafficAnalysisZone().getTAZId();
        double beelineDistanceLoc = TPS_Geometrics.getDistance(start, end,minDist);
        double beelineDistanceTAZ = beelineDistanceLoc;

        if (startTazId != endTazId) {
            beelineDistanceTAZ = blDistMatrix.getValue(startTazId, endTazId);
        }

        double beelineFaktor = beelineDistanceLoc / beelineDistanceTAZ;

        int fromBBR = start.getTrafficAnalysisZone().getBbrType();
        int toBBR = end.getTrafficAnalysisZone().getBbrType();
        double schoolBusSpeed = avgSpeedSchoolBusMatrixMap.getMatrix(time).getValue(fromBBR, toBBR);

        // Assumtion: traveltime plus time for access and egress and waiting
        // beta * beeline / schulBusSpeed + Zugang + Abgang + Wartezeit
        return beelineFaktor * beelineDistanceLoc / schoolBusSpeed + defaultSchoolBusAccess + defaultSchoolBusEgress + defaultSchoolBusWait;
    }
}