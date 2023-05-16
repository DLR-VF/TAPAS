/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.model.vehicle;

import de.dlr.ivf.tapas.logger.LogHierarchy;
import de.dlr.ivf.tapas.logger.HierarchyLogLevel;
import de.dlr.ivf.tapas.model.mode.ModeUtils;
import de.dlr.ivf.tapas.model.parameter.SimulationType;
import de.dlr.ivf.tapas.model.plan.TPS_Plan;
import de.dlr.ivf.tapas.model.scheme.TPS_TourPart;
import lombok.Builder;

import java.util.List;


/**
 * Class for different car types#
 * Size Attributes: Large, Medium, Small, Transporter
 * Fuel Attributes: Benzine, Diesel, Plugin (hybrid), EMobil (Range limited)
 * Internal attributes: available, costPerKilometer, variableCostsPerKilometer, range
 *
 * @author hein_mh
 */
@Builder
@LogHierarchy(hierarchyLogLevel = HierarchyLogLevel.THREAD)
public class TPS_Car implements Vehicle {

    /**N
     * this fuel type
     */
    private FuelTypeName type;
    /**
     * this car size
     */
    private final int kbaNo;

    private long entry_time = 0;
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

    private final boolean restricted;
    /**
     * Level of automation for this car
     */
    private final int automationLevel;

    /**
     * fix costs, eg. insurance, tax, parking-zone in Euro
     */
    private double fixCosts = 0.0;
    private double cost_per_kilometer;

    private final FuelType fuelType;
    private double rangeLeft;


//    /**
//     * Method to select a car from the household for the tourpart
//     *
//     * @param plan     The plan which is processed.
//     * @param tourpart The tourpart for this query
//     * @return null if no car is a available, otherwise a car, which can be used.
//     */
//    public static TPS_Car selectCar(TPS_Plan plan, TPS_TourPart tourpart) {
//
//        //check if a car is already attached to this tourpart
//        if (tourpart.isCarUsed()) {
//            return tourpart.getCar();
//        }
//
//        //check for available cars
//        List<TPS_Car> availableCars = plan.getPerson().getHousehold().getAvailableCars(
//                tourpart.getOriginalSchemePartStart(), tourpart.getOriginalSchemePartEnd());
//
//        //select a car if available
//        if (availableCars.size() > 0) {
//            //TODO: make a choice here!
//            for (TPS_Car availableCar : availableCars) {
//                if (!availableCar.isRestricted()) // first take unrestricted cars
//                    return availableCar;
//            }
//            return availableCars.get(0);
//        } else {
//            return null;
//        }
//    }

    /**
     * @return the automation
     */
    public int getAutomationLevel() {
        return automationLevel;
    }


    /**
     * This method converts the internal kba number to a size
     *
     * @return the car size
     */
    public CarSize getCarSize() {
        return ModeUtils.getCarSize(this.kbaNo);
    }

    /**
     * Method to get the cost per kilometre, depending on fueltype
     */
    @Override
    public double costPerKilometer() {

        return fuelType.getFuelCostPerKm() * fixCosts;
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
     * Method to get the fuel type of this car
     *
     * @return the reference of the fueltype, which is a singelton
     */
    public FuelType getFuelType() {
        return this.fuelType;
    }

    /**
     * GEts the id of this car
     *
     * @return the car id
     */
    public int getId() {
        return id;
    }

    public FuelTypeName getFuelTypeName(){
        return this.fuelType.getFuelType();
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
     * Method to get the cost per kilometre, depending on fueltype
     */
    @Override
    public double variableCostPerKilometer() {
        return this.fuelType.getVariableCostPerKm() * ModeUtils.getKBAVariableCostPerKilometerFactor(this.kbaNo);
    }

    @Override
    public int id() {
        return this.id;
    }

    @Override
    public FuelType fuelType() {
        return this.fuelType;
    }

    /**
     * @return the restricted
     */
    public boolean isRestricted() {
        return restricted;
    }
}
