package de.dlr.ivf.tapas.choice;

import de.dlr.ivf.tapas.choice.distance.MatrixFunction;
import de.dlr.ivf.tapas.choice.distance.functions.IndividualTransportDistanceFunction;
import de.dlr.ivf.tapas.choice.distance.functions.SimpleMatrixDistanceFunction;
import de.dlr.ivf.tapas.model.Matrix;
import de.dlr.ivf.tapas.model.location.Locatable;
import de.dlr.ivf.tapas.model.mode.TPS_Mode.ModeType;
import de.dlr.ivf.tapas.model.parameter.*;

import java.util.EnumMap;


/**
 * This class is temporary and wraps the 'getDistance()' methods from the TPS_Mode class and its implementations.
 *
 * @author Alain Schengen
 */
public class TravelDistanceCalculator {

    private final EnumMap<ModeType, MatrixFunction> modeDistanceFunctions;

    public TravelDistanceCalculator(TPS_ParameterClass parameterClass){
        this.modeDistanceFunctions = new EnumMap<>(ModeType.class);

        double minDist = parameterClass.getDoubleValue(ParamValue.MIN_DIST);
        boolean hasIntraTazInfos = parameterClass.isTrue(ParamFlag.FLAG_INTRA_INFOS_MATRIX);

        init(parameterClass.paramMatrixClass, minDist, hasIntraTazInfos, SimulationType.SCENARIO);
    }

    private void init(ParamMatrixClass paramMatrixClass, double minDist, boolean intraTazInfo, SimulationType simulationType) {

        //walk
        Matrix walkDistanceMatrix = paramMatrixClass.getMatrix(ParamMatrix.DISTANCES_WALK);
        MatrixFunction walkDistanceFunction = new SimpleMatrixDistanceFunction(walkDistanceMatrix);
        this.modeDistanceFunctions.put(ModeType.WALK, walkDistanceFunction);

        //bike
        Matrix bikeDistanceMatrix = paramMatrixClass.getMatrix(ParamMatrix.DISTANCES_BIKE);
        MatrixFunction bikeDistanceFunction = new SimpleMatrixDistanceFunction(bikeDistanceMatrix);
        this.modeDistanceFunctions.put(ModeType.BIKE, bikeDistanceFunction);

        //mit - mit_pass - taxi - car_sharing
        Matrix streetDistanceMatrix = paramMatrixClass.getMatrix(ParamMatrix.DISTANCES_STREET);
        Matrix beelineDistaanceMatrix = paramMatrixClass.getMatrix(ParamMatrix.DISTANCES_BL);
        MatrixFunction mitDistanceFunction = new IndividualTransportDistanceFunction(minDist,intraTazInfo,streetDistanceMatrix, beelineDistaanceMatrix, simulationType);

        this.modeDistanceFunctions.put(ModeType.MIT, mitDistanceFunction);
        this.modeDistanceFunctions.put(ModeType.MIT_PASS, mitDistanceFunction);
        this.modeDistanceFunctions.put(ModeType.TAXI, mitDistanceFunction);
        this.modeDistanceFunctions.put(ModeType.CAR_SHARING, mitDistanceFunction);

        //pt
        Matrix ptDistanceMatrix = paramMatrixClass.getMatrix(ParamMatrix.DISTANCES_PT);
        MatrixFunction ptDistanceFunction = new SimpleMatrixDistanceFunction(ptDistanceMatrix);
        this.modeDistanceFunctions.put(ModeType.PT, ptDistanceFunction);
    }

    public double getDistance(Locatable start, Locatable end, ModeType modeType){

        return this.modeDistanceFunctions.get(modeType).apply(start, end);

    }
}
