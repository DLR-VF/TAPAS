package de.dlr.ivf.tapas.model.mode;

import de.dlr.ivf.tapas.model.parameter.SimulationType;
import de.dlr.ivf.tapas.model.vehicle.CarSize;
import de.dlr.ivf.tapas.model.vehicle.FuelTypeName;
import de.dlr.ivf.tapas.model.vehicle.TPS_Car;

public class ModeUtils {

    /**
     * Returns the SUMO cade for the HBEFA-class of this car
     *
     * @param use7Code use 7 or 14 classes?
     * @return the sumo HBEFA-code
     */
    public static int getSumoHBEFACode(boolean use7Code, TPS_Car car) {

        if (use7Code) {
            return switch (car.getCarSize()) {
                case SMALL -> 7;
                case MEDIUM, LARGE, TRANSPORTER ->
                    switch (car.getEmissionClass()) {
                        case EURO_0, EURO_1 -> car.getFuelTypeName() == FuelTypeName.BENZINE ? 3 : 5;
                        case EURO_2, EURO_3, EURO_4, EURO_5, EURO_6 -> 7;
                        default -> -1; //unknown
                    };
                default -> -1;
            };
        } else {
            return switch (car.getCarSize()) {
                case SMALL ->
                    switch (car.getEmissionClass()) {
                        case EURO_0, EURO_1 -> car.getFuelTypeName() == FuelTypeName.BENZINE ? 14 : 7;
                        case EURO_2, EURO_3, EURO_4, EURO_5, EURO_6 -> car.getFuelTypeName() == FuelTypeName.BENZINE ? 9 : 8;
                        default -> -1;
                    };
                case MEDIUM ->
                    switch (car.getEmissionClass()) {
                        case EURO_0, EURO_1 -> car.getFuelTypeName() == FuelTypeName.BENZINE ? 14 : 10;
                        case EURO_2, EURO_3, EURO_4, EURO_5, EURO_6 -> car.getFuelTypeName() == FuelTypeName.BENZINE ? 9 : 8;
                        default -> -1;
                    };
                case LARGE, TRANSPORTER ->
                    switch (car.getEmissionClass()) {
                        case EURO_0, EURO_1 -> car.getFuelTypeName() == FuelTypeName.BENZINE ? 14 : 10;
                        case EURO_2, EURO_3 -> car.getFuelTypeName() == FuelTypeName.BENZINE ? 13 : 4;
                        case EURO_4, EURO_5, EURO_6 -> car.getFuelTypeName() == FuelTypeName.BENZINE ? 13 : 8;
                        default -> -1;
                    };
                default -> -1; //unknown!
            };
        }
    }

    /**
     * This method gives the fuel factor derived from internal variables.
     * <p>
     * These factors come from the KBA and MID flre analysis from the ecoMove Project
     *
     * @param type
     * @return the fuel factor
     */
    @SuppressWarnings("unused")
    public static double getKBAFuelCostPerKilometerFactor(FuelTypeName type, SimulationType simType, int kbaNo) {
        double dieselReductionFactor =  type == FuelTypeName.DIESEL ? 0.714 : 1; //according to KBA
        boolean isBaseScenario = simType == SimulationType.BASE;
        return switch (kbaNo) {
            case 1, 2 -> 0.7993 * dieselReductionFactor;
            //case 3, 4, 9, 10 -> 1.0000 * dieselReductionFactor;
            case 5, 6, 7, 8, 11, 12, 95 -> 1.3506 * dieselReductionFactor;
            case 101 -> (isBaseScenario ? 0.7993 : 0.6390) * dieselReductionFactor; //ecomove Small
            case 102 -> (isBaseScenario ? 1.0000 : 0.8000) * dieselReductionFactor; //ecomove Medium
            case 103 -> (isBaseScenario ? 1.3506 : 1.0807) * dieselReductionFactor; //ecomove Large
            case 104 -> (isBaseScenario ? 1.3506 : 1.0807) * dieselReductionFactor; //ecomove Transporter
            default -> dieselReductionFactor;
        };
    }

    /**
     * This method gives the variable cost factor derived from internal variables. Guessed by Matthias Heinrichs
     *
     * @return the variable cost factor
     */
    public static double getKBAVariableCostPerKilometerFactor(int kbaNo) {
        return switch (kbaNo) {
            case 1, 2 -> 0.8;
           // case 3, 4, 9, 10 ->  1.0;
            case 5, 6, 7, 8, 11, 12, 95 -> 1.2;
            default -> 1.0;
        };
    }

    /**
     * Returns the SUMO cade for the vehilce-class of thet given car size
     *
     * @param size         the size of this car
     * @param type         the engine-type of this car
     * @param isRestricted Has this car access restrictions?
     * @return the sumo vclass-code
     */
    public static int getSumoVehicleClass(CarSize size, FuelTypeName type, boolean isRestricted) {
        return switch (size) {
            case SMALL ->
                switch (type) {
                    case BENZINE -> isRestricted ? 28 : 0;
                    case DIESEL -> isRestricted ? 29 : 1;
                    case GAS, LPG -> isRestricted ? 30 : 2;
                    case EMOBILE -> isRestricted ? 31 : 3;
                    case PLUGIN -> isRestricted ? 32 : 4;
                    case FUELCELL ->isRestricted ? 33 : 5;
                    default -> 0;
                };
            case MEDIUM ->
                switch (type) {
                    case BENZINE -> isRestricted ? 34 : 6;
                    case DIESEL -> isRestricted ? 35 : 7;
                    case GAS, LPG -> isRestricted ? 36 : 8;
                    case EMOBILE -> isRestricted ? 37 : 9;
                    case PLUGIN -> isRestricted ? 38 : 10;
                    case FUELCELL -> isRestricted ? 39 : 11;
                    default -> 0;
                };
            case LARGE ->
                switch (type) {
                    case BENZINE -> isRestricted ? 40 : 12;
                    case DIESEL -> isRestricted ? 41 : 13;
                    case GAS, LPG -> isRestricted ? 42 : 14;
                    case EMOBILE -> isRestricted ? 43 : 15;
                    case PLUGIN -> isRestricted ? 44 : 16;
                    case FUELCELL -> isRestricted ? 45 : 17;
                    default -> 0;
                };
            case TRANSPORTER ->
                switch (type) {
                    case BENZINE -> isRestricted ? 46 : 18;
                    case DIESEL -> isRestricted ? 47 : 19;
                    case GAS,LPG -> isRestricted ? 48 : 20;
                    case EMOBILE -> isRestricted ? 49 : 21;
                    case PLUGIN -> isRestricted ? 50 : 22;
                    case FUELCELL -> isRestricted ? 51 : 23;
                    default -> 0;
                };
            case TRUCK75 -> isRestricted ? 52 : 24;
            case TRUCK12 -> isRestricted ? 53 : 25;
            case HEAVYDUTY ->  isRestricted ? 54 : 26;
            case TRAILER -> isRestricted ? 55 : 27;
            case BUS ->
                switch (type) {
                    case BENZINE, GAS, LPG -> isRestricted ? 61 : 58;
                    case DIESEL, FUELCELL -> isRestricted ? 57 : 56;
                    case EMOBILE -> isRestricted ? 59 : 58;
                    case PLUGIN -> isRestricted ? 63 : 62;
                    default -> 0;
                };

            case COACH -> isRestricted ? 65 : 64;

        };
    }

    /**
     * This method converts the given kba number to a size
     *
     * @param kba_no the kba number to convert
     * @return the car size
     */
    public static CarSize getCarSize(int kba_no) {
        return switch (kba_no) {
            case 1, 2, 101 -> CarSize.SMALL; //ecomove Small
            case 3, 4, 9, 10, 102 -> CarSize.MEDIUM; //ecomove Medium
            case 5, 6, 7, 8, 95, 103 -> CarSize.LARGE; //ecomove Large
            case 11, 12, 104 -> CarSize.TRANSPORTER; //ecomove Transporter
            default ->  CarSize.MEDIUM;
        };
    }
}
