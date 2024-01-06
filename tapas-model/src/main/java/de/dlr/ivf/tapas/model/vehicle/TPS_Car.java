/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.model.vehicle;

import de.dlr.ivf.tapas.logger.legacy.LogHierarchy;
import de.dlr.ivf.tapas.logger.legacy.HierarchyLogLevel;

@LogHierarchy(hierarchyLogLevel = HierarchyLogLevel.THREAD)
public interface TPS_Car{


    /**
     * @return the automation
     */
    int getAutomationLevel();

    double getRangeLeft();


    /**
     * This method converts the internal kba number to a size
     *
     * @return the car size
     */
    CarSize getCarSize();

    /**
     * Method to get the cost per kilometre, depending on fueltype
     */
    double costPerKilometer();

    /**
     * @return the engineClass
     */
    EmissionClass getEmissionClass();

    /**
     * Method to get the fixed costs of this car
     *
     * @return the actual fix costs
     */
    double getFixCosts();

    /**
     * Method to get the fuel type of this car
     *
     * @return the reference of the fueltype, which is a singelton
     */
    FuelType getFuelType();

    /**
     * GEts the id of this car
     *
     * @return the car id
     */
    int getId();

    FuelTypeName getFuelTypeName();

    /**
     * Method to get the kba-number of this car
     *
     * @return the kba number
     */
    int getKBANumber();

    /**
     * Method to get the cost per kilometre, depending on fueltype
     */

    double variableCostPerKilometer();

    int id();

    FuelType fuelType();

    /**
     * @return the restricted
     */
    boolean isRestricted();
}
