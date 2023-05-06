package de.dlr.ivf.tapas.choice.traveltime.functions;

import de.dlr.ivf.tapas.model.MatrixMap;
import de.dlr.ivf.tapas.model.parameter.ParamMatrixMap;
import de.dlr.ivf.tapas.model.parameter.ParamValue;
import de.dlr.ivf.tapas.model.parameter.SimulationType;
import de.dlr.ivf.tapas.model.parameter.TPS_ParameterClass;

/**
 * extracted logic for access and egress times for automatic vehicles.
 */
public class AvTravelTimeAdjustment {

    private final MatrixMap mitAccessMatrixMap;
    private final MatrixMap mitEgressMatrixMap;
    private final double automaticParkingAccess;
    private final double automaticParkingEgress;

    public AvTravelTimeAdjustment(TPS_ParameterClass parameterClass) {

        SimulationType simType = parameterClass.getSimulationType();

        this.mitAccessMatrixMap = parameterClass.paramMatrixMapClass.getMatrixMap(ParamMatrixMap.ARRIVAL_MIT,simType);
        this.mitEgressMatrixMap = parameterClass.paramMatrixMapClass.getMatrixMap(ParamMatrixMap.EGRESS_MIT,simType);
        this.automaticParkingAccess = parameterClass.getDoubleValue(ParamValue.AUTOMATIC_PARKING_ACCESS);
        this.automaticParkingEgress = parameterClass.getDoubleValue(ParamValue.AUTOMATIC_PARKING_EGRESS);
    }

    public double adjustAvAccessEgress(int idStart, int idDest, int time){

        double acc = mitAccessMatrixMap == null ? 0 : mitAccessMatrixMap.getMatrix(time).getValue(idStart,idDest);

        double egr = mitEgressMatrixMap == null ? 0 : mitEgressMatrixMap.getMatrix(time).getValue(idStart,idDest);

        // calculate access modification
        double accAdjustment = acc > automaticParkingAccess ? acc - automaticParkingAccess : 0;
        // calculate egress modification
        double egrAdjustment = egr > automaticParkingEgress ? egr - automaticParkingEgress : 0;

        return - accAdjustment - egrAdjustment;
    }
}
