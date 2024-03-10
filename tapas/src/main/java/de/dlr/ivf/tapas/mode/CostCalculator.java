package de.dlr.ivf.tapas.mode;

import de.dlr.ivf.tapas.mode.cost.ModeDistanceCostFunction;
import de.dlr.ivf.tapas.model.location.TPS_TrafficAnalysisZone;
import de.dlr.ivf.tapas.model.mode.TPS_Mode;
import de.dlr.ivf.tapas.model.parameter.ParamFlag;
import de.dlr.ivf.tapas.model.parameter.ParamValue;
import de.dlr.ivf.tapas.model.parameter.SimulationType;
import de.dlr.ivf.tapas.model.parameter.TPS_ParameterClass;
import de.dlr.ivf.tapas.model.plan.TPS_Plan;
import de.dlr.ivf.tapas.model.vehicle.TPS_Car;

import java.util.Map;

public class CostCalculator {

    private final double mitFuelCostPerKmCommute;
    private final double mitGasolineCostPerKm;
    private final double mitVariableCostPerKm;
    private final int mitIncomeClassCommute;
    private final boolean useExitMaut;

    private final Map<TPS_Mode, ModeDistanceCostFunction> defaultModeDistanceCostFunctions;

    public CostCalculator(TPS_ParameterClass parameters, Map<TPS_Mode, ModeDistanceCostFunction> distanceCostFunctions){
        this.mitFuelCostPerKmCommute = parameters.getDoubleValue(ParamValue.MIT_FUEL_COST_PER_KM_COMMUTE);
        this.mitGasolineCostPerKm = parameters.getDoubleValue(ParamValue.MIT_GASOLINE_COST_PER_KM);
        this.mitVariableCostPerKm = parameters.getDoubleValue(ParamValue.MIT_VARIABLE_COST_PER_KM);
        this.mitIncomeClassCommute = parameters.getIntValue(ParamValue.MIT_INCOME_CLASS_COMMUTE);
        this.useExitMaut = parameters.isTrue(ParamFlag.FLAG_USE_EXIT_MAUT);
        this.defaultModeDistanceCostFunctions = distanceCostFunctions;
    }


    public double calculateTravelCost(TPS_Mode mode, TPS_Car carForTrip, TPS_Plan plan, double distance) {


        double distanceCostFactor = 0;
        if (!(mode.getModeType() == TPS_Mode.ModeType.MIT)) {
            distanceCostFactor = mode.getCostPerKm();
        } else {
            // determine whether working and income over average such that commuting tax benefit is appropriate
            if (plan.getPerson().isWorking() &&
                    plan.getPerson().getHousehold().getRealIncome() >= mitIncomeClassCommute) {
                distanceCostFactor = mitFuelCostPerKmCommute;
            }

            if (carForTrip == null) {
                // default distance related costs
                distanceCostFactor += mitGasolineCostPerKm + mitVariableCostPerKm;
            } else {
                // car-depending distance related costs
                distanceCostFactor += carForTrip.costPerKilometer() + carForTrip.variableCostPerKilometer();
            }
        }

        return distance * 0.001 * distanceCostFactor;
    }

    public double calculateCostStay(TPS_TrafficAnalysisZone goingToTVZ, TPS_TrafficAnalysisZone comingFromTVZ, double duration) {

        // location related costs
        // determine whether parking fee or toll is charged in the destination zone; toll only relevant when coming
        // from an toll free zone
        double costStay = 0.0;

        // toll fee
        if (goingToTVZ.hasToll(SimulationType.SCENARIO) && !comingFromTVZ.hasToll(SimulationType.SCENARIO)) {
            // Scenario: toll has to be payed on entrance into a toll zone (cordon toll)
            costStay += goingToTVZ.getTollFee(SimulationType.SCENARIO);
        }

        // toll fees leaving a toll zone
        if (useExitMaut) {
            if (!goingToTVZ.hasToll(SimulationType.SCENARIO) && comingFromTVZ.hasToll(SimulationType.SCENARIO)) {
                // Scenario: toll has to be payed leaving a toll zone (cordon toll)
                costStay += comingFromTVZ.getTollFee(SimulationType.SCENARIO);
            }
        }

        // parking fee
        if (goingToTVZ.hasParkingFee(SimulationType.SCENARIO)) {
            double costSzen = goingToTVZ.getParkingFee(SimulationType.SCENARIO);
            if (costSzen > 0) {
                double stayingHours = duration * 2.7777777777e-4;// converting into seconds
                costStay += (costSzen * stayingHours);
            }
        }

        return costStay;
    }

    public double calculateCost(TPS_TrafficAnalysisZone goingToTaz, TPS_TrafficAnalysisZone comingFromTaz, int duration,
                                TPS_Mode mode, TPS_Car car, TPS_Plan plan, double distance){

        return calculateCostStay(goingToTaz,comingFromTaz,duration) + calculateTravelCost(mode,car,plan,distance);
    }
}
