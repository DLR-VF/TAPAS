package de.dlr.ivf.tapas.choice.distance.functions;

import de.dlr.ivf.tapas.choice.distance.MatrixFunction;
import de.dlr.ivf.tapas.model.Matrix;
import de.dlr.ivf.tapas.model.TPS_Geometrics;
import de.dlr.ivf.tapas.model.location.Locatable;
import de.dlr.ivf.tapas.model.parameter.SimulationType;

public class IndividualTransportDistanceFunction implements MatrixFunction {

    private final double minDist;
    private final boolean intraTazInfo;
    private final Matrix streetDistances;
    private final Matrix blDistances;
    private final SimulationType simulationType;

    public IndividualTransportDistanceFunction(double minDist, boolean intraTazInfo, Matrix streetDistances, Matrix blDistances, SimulationType simulationType){
        this.minDist = minDist;
        this.intraTazInfo = intraTazInfo;
        this.streetDistances = streetDistances;
        this.blDistances = blDistances;
        this.simulationType = simulationType;
    }

    @Override
    public double apply(Locatable start, Locatable end) {

        double distance;
        double factor = 1;
        int idStart = start.getTAZId();
        int idDest = end.getTAZId();
        double beelineDistanceLoc = TPS_Geometrics.getDistance(start, end, minDist);

        if (idStart == idDest) {
            // start and end locations are in the same traffic analysis zone
            if (this.intraTazInfo) {
                // there exist information about distances inside a traffic
                // analysis zone. This distance is retrieved from the
                // distances matrix. The factor is 1.
                distance = streetDistances.getValue(idStart, idDest);
            } else {
                // there exist no information about distances inside a traffic
                // analysis zone. The distance is calculated by
                // the beeline distance and a factor which is determined by the
                // traffic analysis zone.
                factor = start.getTrafficAnalysisZone().getSimulationTypeValues(simulationType).getBeelineFactorMIT();
                distance = beelineDistanceLoc;
            }
        } else {
            // if TAZes are different calculate beelinefactor
            factor = beelineDistanceLoc / blDistances.getValue(idStart, idDest);
            // start and end locations are in different traffic analysis zones.
            // The distance is calculated by a distance
            // retrieved from the distances matrix and a factor calculated by
            // the beeline and the real distance of both
            // traffic analysis zones.
            distance = streetDistances.getValue(idStart, idDest);
        }

//		//autonomous vehicles get a virtual distance correction for adjusting the costs
//		if(car!=null && car.getAutomation()>=this.getParameters().getIntValue(ParamValue.AUTOMATIC_VEHICLE_LEVEL)){
//			factor *= distance>this.getParameters().getIntValue(ParamValue.AUTOMATIC_VEHICLE_TIME_MOD_THRESHOLD)?
//								this.getParameters().getDoubleValue(ParamValue.AUTOMATIC_VEHICLE_TIME_MOD_FAR):
//								this.getParameters().getDoubleValue(ParamValue.AUTOMATIC_VEHICLE_TIME_MOD_NEAR);
//		}


        return Math.max(minDist, distance * factor);
    }
}
