/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.person;

import de.dlr.ivf.tapas.log.LogHierarchy;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface.HierarchyLogLevel;
import de.dlr.ivf.tapas.plan.TPS_Plan;
import de.dlr.ivf.tapas.scheme.TPS_TourPart;
import de.dlr.ivf.tapas.util.Timeline;
import de.dlr.ivf.tapas.util.parameters.ParamValue;
import de.dlr.ivf.tapas.util.parameters.SimulationType;
import de.dlr.ivf.tapas.util.parameters.TPS_ParameterClass;


/**
 * Class for different car types#
 * Size Attributes: Large, Medium, Small, Transporter
 * Fuel Attributes: Benzine, Diesel, Plugin (hybrid), EMobil (Range limited)
 * Internal attributes: available, costPerKilometer, variableCostsPerKilometer, range
 *
 * @author hein_mh
 */
@LogHierarchy(hierarchyLogLevel = HierarchyLogLevel.THREAD)
public class TPS_Car {

    public static FuelType[] FUEL_TYPE_ARRAY = new FuelType[]{FuelType.BENZINE, FuelType.DIESEL, FuelType.GAS, FuelType.EMOBILE, FuelType.PLUGIN, FuelType.FUELCELL, FuelType.LPG};
    public static EmissionClass[] EMISSION_TYPE_ARRAY = new EmissionClass[]{EmissionClass.EURO_0, EmissionClass.EURO_1, EmissionClass.EURO_2, EmissionClass.EURO_3, EmissionClass.EURO_4, EmissionClass.EURO_5, EmissionClass.EURO_6};
    public boolean hasPaidToll = false;
    public int index = -1; // index of this car in the household entry
    /**
     * indicates if this car is available (standing in front of the home)
     */
    private Timeline availability;
    /**
     * this fuel type
     */
    private FuelType type;
    /**
     * this car size
     */
    private int kbaNo;
    /**
     * this car id
     */
    private int id;
    /**
     * Enum for the engine emissions class
     */
    private EmissionClass emissionClass;
    /**
     * Flag if this car is restricted in access of certain areas
     */

    private boolean restricted = false;
    /**
     * Level of automation for this car
     */
    private int automationLevel = 0;
    /**
     * this range left
     */
    private double rangeLeft = 0.0;
    /**
     * is this car a company car?
     */
    private boolean companyCar = false;
    /**
     * fix costs, eg. insurance, tax, parking-zone in Euro
     */
    private double fixCosts = 0.0;
    private TPS_ParameterClass parameterClass;

    /**
     * Constructor class for a new car.
     *
     * @param id of this car
     */
    public TPS_Car(int id) {
        this.id = id;
    }

    /**
     * This method converts the given kba number to a size
     *
     * @param kba_no the kba number to convert
     * @return the car size
     */
    public static CarSize getCarSize(int kba_no) {
        switch (kba_no) {
            case 1:
            case 2:
            case 101: //ecomove Small
                return CarSize.SMALL;
            case 3:
            case 4:
            case 9:
            case 10:
            case 102: //ecomove Medium
                return CarSize.MEDIUM;
            case 5:
            case 6:
            case 7:
            case 8:
            case 95:
            case 103: //ecomove Large
                return CarSize.LARGE;
            case 11:
            case 12:
            case 104: //ecomove Transporter
                return CarSize.TRANSPORTER;
        }
        return CarSize.MEDIUM;

    }

    /**
     * Returns the SUMO cade for the vehilce-class of thet given car size
     *
     * @param size         the size of this car
     * @param type         the engine-type of this car
     * @param isRestricted Has this car access restrictions?
     * @return the sumo vclass-code
     */
    public static int getSumoVehicleClass(CarSize size, FuelType type, boolean isRestricted) {
        switch (size) {
            case SMALL:
                switch (type) {
                    case BENZINE:
                        return isRestricted ? 28 : 0;
                    case DIESEL:
                        return isRestricted ? 29 : 1;
                    case GAS:
                        return isRestricted ? 30 : 2;
                    case EMOBILE:
                        return isRestricted ? 31 : 3;
                    case PLUGIN:
                        return isRestricted ? 32 : 4;
                    case FUELCELL:
                        return isRestricted ? 33 : 5;
                }
                break;
            case MEDIUM:
                switch (type) {
                    case BENZINE:
                        return isRestricted ? 34 : 6;
                    case DIESEL:
                        return isRestricted ? 35 : 7;
                    case GAS:
                        return isRestricted ? 36 : 8;
                    case EMOBILE:
                        return isRestricted ? 37 : 9;
                    case PLUGIN:
                        return isRestricted ? 38 : 10;
                    case FUELCELL:
                        return isRestricted ? 39 : 11;
                }
                break;
            case LARGE:
                switch (type) {
                    case BENZINE:
                        return isRestricted ? 40 : 12;
                    case DIESEL:
                        return isRestricted ? 41 : 13;
                    case GAS:
                        return isRestricted ? 42 : 14;
                    case EMOBILE:
                        return isRestricted ? 43 : 15;
                    case PLUGIN:
                        return isRestricted ? 44 : 16;
                    case FUELCELL:
                        return isRestricted ? 45 : 17;
                }
                break;
            case TRANSPORTER:
                switch (type) {
                    case BENZINE:
                        return isRestricted ? 46 : 18;
                    case DIESEL:
                        return isRestricted ? 47 : 19;
                    case GAS:
                        return isRestricted ? 48 : 20;
                    case EMOBILE:
                        return isRestricted ? 49 : 21;
                    case PLUGIN:
                        return isRestricted ? 50 : 22;
                    case FUELCELL:
                        return isRestricted ? 51 : 23;
                }
                break;
            case TRUCK75:
                switch (type) {
                    case BENZINE:
                        return isRestricted ? 52 : 24;
                    case DIESEL:
                        return isRestricted ? 52 : 24;
                    case GAS:
                        return isRestricted ? 52 : 24;
                    case EMOBILE:
                        return isRestricted ? 52 : 24;
                    case PLUGIN:
                        return isRestricted ? 52 : 24;
                    case FUELCELL:
                        return isRestricted ? 52 : 24;
                }
                break;
            case TRUCK12:
                switch (type) {
                    case BENZINE:
                        return isRestricted ? 53 : 25;
                    case DIESEL:
                        return isRestricted ? 53 : 25;
                    case GAS:
                        return isRestricted ? 53 : 25;
                    case EMOBILE:
                        return isRestricted ? 53 : 25;
                    case PLUGIN:
                        return isRestricted ? 53 : 25;
                    case FUELCELL:
                        return isRestricted ? 53 : 25;
                }
                break;
            case HEAVYDUTY:
                switch (type) {
                    case BENZINE:
                        return isRestricted ? 54 : 26;
                    case DIESEL:
                        return isRestricted ? 54 : 26;
                    case GAS:
                        return isRestricted ? 54 : 26;
                    case EMOBILE:
                        return isRestricted ? 54 : 26;
                    case PLUGIN:
                        return isRestricted ? 54 : 26;
                    case FUELCELL:
                        return isRestricted ? 54 : 26;
                }
                break;
            case TRAILER:
                switch (type) {
                    case BENZINE:
                        return isRestricted ? 55 : 27;
                    case DIESEL:
                        return isRestricted ? 55 : 27;
                    case GAS:
                        return isRestricted ? 55 : 27;
                    case EMOBILE:
                        return isRestricted ? 55 : 27;
                    case PLUGIN:
                        return isRestricted ? 55 : 27;
                    case FUELCELL:
                        return isRestricted ? 55 : 27;
                }
                break;
            case BUS:
                switch (type) {
                    case BENZINE:
                        return isRestricted ? 61 : 58;
                    case DIESEL:
                        return isRestricted ? 57 : 56;
                    case GAS:
                        return isRestricted ? 61 : 58;
                    case EMOBILE:
                        return isRestricted ? 59 : 58;
                    case PLUGIN:
                        return isRestricted ? 63 : 62;
                    case FUELCELL:
                        return isRestricted ? 57 : 56;
                }
                break;
            case COACH:
                switch (type) {
                    case BENZINE:
                        return isRestricted ? 65 : 64;
                    case DIESEL:
                        return isRestricted ? 65 : 64;
                    case GAS:
                        return isRestricted ? 65 : 64;
                    case EMOBILE:
                        return isRestricted ? 65 : 64;
                    case PLUGIN:
                        return isRestricted ? 65 : 64;
                    case FUELCELL:
                        return isRestricted ? 65 : 64;
                }
                break;
        }
        return 0;
    }

    /**
     * Method to select a car from the household for the tourpart
     *
     * @param plan     The plan which is processed.
     * @param tourpart The tourpart for this query
     * @return null if no car is a available, otherwise a car, which can be used.
     */
    public static TPS_Car selectCar(TPS_Plan plan, TPS_TourPart tourpart) {

        //check if a car is already attached to this tourpart
        if (tourpart.isCarUsed()) {
            return tourpart.getCar();
        }

        //check for available cars
        TPS_Car[] availableCars = plan.getPerson().getHousehold().getAvailableCars(
                tourpart.getOriginalSchemePartStart(), tourpart.getOriginalSchemePartEnd());

        //select a car if available
        if (availableCars != null) {
            //TODO: make a choice here!
            for (TPS_Car availableCar : availableCars) {
                if (!availableCar.isRestricted()) // first take unrestricted cars
                    return availableCar;
            }
            return availableCars[0];
        } else {
            return null;
        }

    }

    /**
     * Method to clone the parameters from a given car type to this instance.
     * DO NOT COPY THE INDEX!
     *
     * @param original the car which holds the desired values.
     */
    public void cloneCar(TPS_Car original) {
        this.type = original.type;
        this.kbaNo = original.kbaNo;
        this.emissionClass = original.emissionClass;
        this.fixCosts = original.fixCosts;
        this.companyCar = original.companyCar;
        this.restricted = original.restricted;
        this.automationLevel = original.automationLevel;
    }

    /**
     * @return the automation
     */
    public int getAutomationLevel() {
        return automationLevel;
    }

    /**
     * @param automation the automation to set
     */
    public void setAutomation(int automation) {
        this.automationLevel = automation;
    }

    /**
     * This method converts the internal kba number to a size
     *
     * @return the car size
     */
    public CarSize getCarSize() {
        return TPS_Car.getCarSize(this.kbaNo);
    }

    /**
     * Method to get the cost per kilometre, depending on fueltype
     */
    public double getCostPerKilometer(SimulationType simType) {
        try {
            return this.companyCar ? 0 : this.parameterClass.getDoubleValue(type.getCostPerKilometer(simType)) * fixCosts;
        }
        catch (NullPointerException e){

            return 0.1;
        }
    }

    /**
     * @return the engineClass
     */
    public EmissionClass getEmissionClass() {
        return emissionClass;
    }

    /**
     * Method to get the fixed costs of this car
     *
     * @return the actual fix costs
     */
    public double getFixCosts() {
        return fixCosts;
    }

    /**
     * Method to set the fixed costs of this car
     *
     * @param newCosts the new fixed costs for this car
     */
    public void setFixCosts(double newCosts) {
        fixCosts = newCosts;
    }

    /**
     * Method to get the fuel type of this car
     *
     * @return the reference of the fueltype, which is a singelton
     */
    public FuelType getFuelType() {
        return type;
    }

    /**
     * GEts the id of this car
     *
     * @return the car id
     */
    public int getId() {
        return id;
    }

    /**
     * sets the id for this car.
     *
     * @param id the id to set
     */
    public void setId(int id) {
        this.id = id;
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
    private double getKBAFuelCostPerKilometerFactor(SimulationType type) {
        double dieselReductionFactor = this.getFuelType().equals(FuelType.DIESEL) ? 0.714 : 1; //according to KBA
        boolean baseSzen = type.equals(SimulationType.BASE);
        switch (this.kbaNo) {
            case 1:
            case 2:
                return 0.7993 * dieselReductionFactor;
            case 3:
            case 4:
            case 9:
            case 10:
                return 1.0000 * dieselReductionFactor;
            case 5:
            case 6:
            case 7:
            case 8:
            case 11:
            case 12:
            case 95:
                return 1.3506 * dieselReductionFactor;
            case 101: //ecomove Small
                return (baseSzen ? 0.7993 : 0.6390) * dieselReductionFactor;
            case 102: //ecomove Medium
                return (baseSzen ? 1.0000 : 0.8000) * dieselReductionFactor;
            case 103: //ecomove Large
                return (baseSzen ? 1.3506 : 1.0807) * dieselReductionFactor;
            case 104: //ecomove Transporter
                return (baseSzen ? 1.3506 : 1.0807) * dieselReductionFactor;

            default:
                return 1.0 * dieselReductionFactor;
        }
    }

    /**
     * Method to get the kba-number of this car
     *
     * @return the kba number
     */
    public int getKBANumber() {
        return this.kbaNo;
    }

    /**
     * This method gives the variable cost factor derived from internal variables. Guessed by Matthias Heinrichs
     *
     * @return the variable cost factor
     */
    private double getKBAVariableCostPerKilometerFactor() {
        switch (this.kbaNo) {
            case 1:
            case 2:
                return 0.8;
            case 3:
            case 4:
            case 9:
            case 10:
                return 1.0;
            case 5:
            case 6:
            case 7:
            case 8:
            case 11:
            case 12:
            case 95:
                return 1.2;
        }
        return 1.0;
    }

    /**
     * Method to get the actual range, which can be driven by this car today
     *
     * @return the left range
     */
    public double getRangeLeft() {
        return this.rangeLeft;
    }

    /**
     * Returns the SUMO cade for the HBEFA-class of this car
     *
     * @param use7Code use 7 or 14 classes?
     * @return the sumo HBEFA-code
     */
    public int getSumoHBEFACode(boolean use7Code) {

        if (use7Code) {
            switch (this.getCarSize()) {
                case SMALL:
                    return 7;
                case MEDIUM:
                case LARGE:
                case TRANSPORTER:
                    switch (this.emissionClass) {
                        case EURO_0:
                        case EURO_1:
                            return this.type == FuelType.BENZINE ? 3 : 5;
                        case EURO_2:
                        case EURO_3:
                            return 7;
                        case EURO_4:
                        case EURO_5:
                        case EURO_6:
                            return 7;
                    }
                    break;
                default:
                    return -1; //unknown!
            }
        } else {
            switch (this.getCarSize()) {
                case SMALL:
                    switch (this.emissionClass) {
                        case EURO_0:
                        case EURO_1:
                            return this.type == FuelType.BENZINE ? 14 : 7;
                        case EURO_2:
                        case EURO_3:
                            return this.type == FuelType.BENZINE ? 9 : 8;
                        case EURO_4:
                        case EURO_5:
                        case EURO_6:
                            return this.type == FuelType.BENZINE ? 9 : 8;
                    }
                    break;
                case MEDIUM:
                    switch (this.emissionClass) {
                        case EURO_0:
                        case EURO_1:
                            return this.type == FuelType.BENZINE ? 14 : 10;
                        case EURO_2:
                        case EURO_3:
                            return this.type == FuelType.BENZINE ? 9 : 8;
                        case EURO_4:
                        case EURO_5:
                        case EURO_6:
                            return this.type == FuelType.BENZINE ? 9 : 8;
                    }
                    break;
                case LARGE:
                case TRANSPORTER:
                    switch (this.emissionClass) {
                        case EURO_0:
                        case EURO_1:
                            return this.type == FuelType.BENZINE ? 14 : 10;
                        case EURO_2:
                        case EURO_3:
                            return this.type == FuelType.BENZINE ? 13 : 4;
                        case EURO_4:
                        case EURO_5:
                        case EURO_6:
                            return this.type == FuelType.BENZINE ? 13 : 8;
                    }
                    break;
                default:
                    return -1; //unknown!
            }
        }
        return 0;
    }

    /**
     * Returns the SUMO cade for the vehilce-class of this car
     *
     * @return the sumo vclass-code
     */
    public int getSumoVehicleClass() {
        return getSumoVehicleClass(this.getCarSize(), this.type, this.isRestricted());
    }

    /**
     * Method to get the cost per kilometre, depending on fueltype
     */
    public double getVariableCostPerKilometer(SimulationType simType) {
        return this.companyCar ? 0 : this.parameterClass.getDoubleValue(type.getVariableCostsPerKilometer(simType)) *
                this.getKBAVariableCostPerKilometerFactor();
    }

    /**
     * Initialisies this instance
     *
     * @param type           fuel type of this car
     * @param kba            the kba type of this car
     * @param emissionClass  the emission class of this car (Euro0-6)
     * @param fixCosts       the fix costs for this car, e.g. taxes, insurance
     * @param companyCar     flag if this is a company car or not. Company cars do not produce individual costs
     * @param isRestricted   flag to indicate that this car is restricted for entering certain areas
     * @param parameterClass parameter class reference
     */

    public void init(FuelType type, int kba, EmissionClass emissionClass, double fixCosts, boolean companyCar, boolean isRestricted, TPS_ParameterClass parameterClass, int index) {
        this.type = type;
        this.emissionClass = emissionClass;
        this.fixCosts = fixCosts;
        this.companyCar = companyCar;
        this.kbaNo = kba;
        this.restricted = isRestricted;
        this.availability = new Timeline();
        this.parameterClass = parameterClass;
        this.rangeLeft = this.parameterClass.getDoubleValue(this.type.getRange(SimulationType.SCENARIO));
        this.index = index;
    }

    /**
     * checks if this car is available and standing at home
     *
     * @return returns the availability status
     */
    public boolean isAvailable(double start, double end) {
        return !availability.clash((int) (start + 0.5), (int) (end + 0.5));
    }

    /**
     * checks if this car is available and standing at home
     *
     * @return returns the availability status
     */
    public boolean isAvailable(int start, int end) {
        return !availability.clash(start, end);
    }

    public boolean isCompanyCar() {
        return companyCar;
    }

    public void setCompanyCar(boolean companyCar) {
        this.companyCar = companyCar;
    }

    /**
     * @return the restricted
     */
    public boolean isRestricted() {
        return restricted;
    }

    /**
     * @param restricted the restricted to set
     */
    public void setRestricted(boolean restricted) {
        this.restricted = restricted;
    }

    /**
     * Picks the car for the time period specified
     *
     * @param start time period start
     * @param end   time period end
     * @return success of this action
     */
    public boolean pickCar(double start, double end, double distance, boolean payToll) {
        if (pickCar((int) (start + 0.5), (int) (end + 0.5), payToll)) {
            this.rangeLeft -= distance;
            return true;
        } else {
            return false;
        }

    }

    /**
     * Picks the car for the time period specified
     *
     * @param start   time period start
     * @param end     time period end
     * @param payToll flag to indicate that this car pays a entry/exit toll
     * @return success of this action
     */
    public boolean pickCar(int start, int end, boolean payToll) {
        boolean success = false;

        if (isAvailable(start, end)) {
            success = availability.add(start, end);
        }
        this.hasPaidToll = payToll;
        return success;
    }

    /**
     * reduces the actual range for this car after a return to home
     *
     * @param tripLength
     */
    public void reduceRange(double tripLength) {
        rangeLeft -= tripLength;
    }

    /**
     * Method to trigger, that the car is refuelled, e.g. long stay at home
     */
    public void refuelCar() {
        this.rangeLeft = this.parameterClass.getDoubleValue(this.type.getRange(SimulationType.SCENARIO));
    }

    /**
     * @param emissionClass the EmissionClass to set
     */
    public void setEngineClass(EmissionClass emissionClass) {
        this.emissionClass = emissionClass;
    }

    /**
     * Unpicks the car for the time period specified
     *
     * @param start time period start
     * @param end   time period end
     * @return success of this action
     */
    public boolean unPickCar(double start, double end) {
        return unPickCar((int) (start + 0.5), (int) (end + 0.5));
    }

    /**
     * Unpicks the car for the time period specified
     *
     * @param start time period start
     * @param end   time period end
     * @return success of this action
     */
    public boolean unPickCar(int start, int end) {
        boolean success = false;
        if (availability.remove(start,
                end)) { //remove returns true if a timeline-entry with the given start and end existed and was removed
            success = true;
        }
        return success;
    }


    /**
     * This enum represents all known fuel types of TAPAS
     */
    public enum FuelType {
        BENZINE(ParamValue.MIT_GASOLINE_COST_PER_KM, ParamValue.MIT_GASOLINE_COST_PER_KM_BASE,
                ParamValue.MIT_VARIABLE_COST_PER_KM, ParamValue.MIT_VARIABLE_COST_PER_KM_BASE,
                ParamValue.MIT_RANGE_CONVENTIONAL, ParamValue.MIT_RANGE_CONVENTIONAL), DIESEL(
                ParamValue.MIT_DIESEL_COST_PER_KM, ParamValue.MIT_DIESEL_COST_PER_KM_BASE,
                ParamValue.MIT_VARIABLE_COST_PER_KM, ParamValue.MIT_VARIABLE_COST_PER_KM_BASE,
                ParamValue.MIT_RANGE_CONVENTIONAL, ParamValue.MIT_RANGE_CONVENTIONAL), GAS(
                ParamValue.MIT_GAS_COST_PER_KM, ParamValue.MIT_GAS_COST_PER_KM_BASE,
                ParamValue.MIT_VARIABLE_COST_PER_KM, ParamValue.MIT_VARIABLE_COST_PER_KM_BASE,
                ParamValue.MIT_RANGE_CONVENTIONAL, ParamValue.MIT_RANGE_CONVENTIONAL), EMOBILE(
                ParamValue.MIT_ELECTRO_COST_PER_KM, ParamValue.MIT_ELECTRO_COST_PER_KM_BASE,
                ParamValue.MIT_VARIABLE_COST_PER_KM, ParamValue.MIT_VARIABLE_COST_PER_KM_BASE,
                ParamValue.MIT_RANGE_EMOBILE, ParamValue.MIT_RANGE_EMOBILE), PLUGIN(ParamValue.MIT_PLUGIN_COST_PER_KM,
                ParamValue.MIT_PLUGIN_COST_PER_KM_BASE, ParamValue.MIT_VARIABLE_COST_PER_KM,
                ParamValue.MIT_VARIABLE_COST_PER_KM_BASE, ParamValue.MIT_RANGE_PLUGIN,
                ParamValue.MIT_RANGE_PLUGIN), FUELCELL(ParamValue.MIT_FUELCELL_COST_PER_KM,
                ParamValue.MIT_FUELCELL_COST_PER_KM_BASE, ParamValue.MIT_VARIABLE_COST_PER_KM,
                ParamValue.MIT_VARIABLE_COST_PER_KM_BASE, ParamValue.MIT_RANGE_CONVENTIONAL,
                ParamValue.MIT_RANGE_CONVENTIONAL),LPG(
                ParamValue.MIT_GAS_COST_PER_KM, ParamValue.MIT_GAS_COST_PER_KM_BASE,
                ParamValue.MIT_VARIABLE_COST_PER_KM, ParamValue.MIT_VARIABLE_COST_PER_KM_BASE,
                ParamValue.MIT_RANGE_CONVENTIONAL, ParamValue.MIT_RANGE_CONVENTIONAL);

        private final ParamValue costPerKilometer;
        private final ParamValue variableCostsPerKilometer;
        private final ParamValue range;
        private final ParamValue costPerKilometerBase;
        private final ParamValue variableCostsPerKilometerBase;
        private final ParamValue rangeBase;


        FuelType(ParamValue costPerKilometer, ParamValue costPerKilometerBase, ParamValue variableCostsPerKilometer, ParamValue variableCostsPerKilometerBase, ParamValue range, ParamValue rangeBase) {
            this.costPerKilometer = costPerKilometer;
            this.variableCostsPerKilometer = variableCostsPerKilometer;
            this.costPerKilometerBase = costPerKilometerBase;
            this.variableCostsPerKilometerBase = variableCostsPerKilometerBase;
            this.range = range;
            this.rangeBase = rangeBase;
        }

        public ParamValue getCostPerKilometer(SimulationType type) {
            if (SimulationType.SCENARIO.equals(type)) {
                return costPerKilometer;
            } else {
                return costPerKilometerBase;
            }
        }

        public ParamValue getRange(SimulationType type) {
            if (SimulationType.SCENARIO.equals(type)) {
                return range;
            } else {
                return rangeBase;
            }
        }

        public ParamValue getVariableCostsPerKilometer(SimulationType type) {
            if (SimulationType.SCENARIO.equals(type)) {
                return variableCostsPerKilometer;
            } else {
                return variableCostsPerKilometerBase;
            }
        }
    }

    /**
     * This enum represents all known car sizes in TAPAS
     */
    public enum EmissionClass {
        EURO_0, EURO_1, EURO_2, EURO_3, EURO_4, EURO_5, EURO_6
    }

    public enum CarSize {
        TRANSPORTER, SMALL, MEDIUM, LARGE, TRUCK75, TRUCK12, HEAVYDUTY, TRAILER, BUS, COACH
    }


}
