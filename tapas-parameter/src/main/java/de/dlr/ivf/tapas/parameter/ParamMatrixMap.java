/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.parameter;

/**
 * This class provides all flag (boolean) enums which determine the name of
 * parameters available in the application
 * <p>
 * This enum provides constants in a map of matrix form for the application.
 * It is possible that on constant has two matrix maps depending on the type
 * of the constant. If it is simulation type dependent there exist one
 * matrix map for the base an one for the scenario case.
 */
public enum ParamMatrixMap {

    /**
     * matrixMap containing the access times for each pair of tvz for the pt
     * in the base/scenario
     */
    ARRIVAL_PT,

    /**
     * matrixMap containing the access times for each pair of tvz for the pt
     * in the base/scenario
     */
    ARRIVAL_BIKE,

    /**
     * matrixMap containing the access times for each pair of tvz for the pt
     * in the base/scenario
     */
    ARRIVAL_WALK,

    /**
     * matrixMap containing the access times for each pair of tvz for the pt
     * in the base/scenario
     */
    ARRIVAL_MIT,

    /**
     * average speed for simulated school busses depending on bbr region
     * code in the base/scenario
     */
    AVERAGE_SPEED_SCHOOLBUS,

    /**
     * matrixMap containing the leaving times for each pair of tvz for the
     * pt in the base/scenario
     */
    EGRESS_PT,

    /**
     * matrixMap containing the access times for each pair of tvz for the pt
     * in the base/scenario
     */
    EGRESS_BIKE,

    /**
     * matrixMap containing the access times for each pair of tvz for the pt
     * in the base/scenario
     */
    EGRESS_WALK,

    /**
     * matrixMap containing the access times for each pair of tvz for the pt
     * in the base/scenario
     */
    EGRESS_MIT,

    /**
     * matrixMap containing the access times for each pair of tvz for the pt
     * in the base/scenario
     */
    INTERCHANGES_PT,

    /**
     * matrixMap containing the entrainment TAZ for bike+pt rides
     */
    PTBIKE_ACCESS_TAZ,

    /**
     * matrixMap containing the disembark TAZ for bike+pt rides
     */
    PTBIKE_EGRESS_TAZ,

    /**
     * matrixMap containing the entrainment TAZ for car+pt rides
     */
    PTCAR_ACCESS_TAZ,

    /**
     * matrixMap containing the number of interchanges between two TAZ for pt+bike
     */
    PTBIKE_INTERCHANGES,

    /**
     * matrixMap containing the number of interchanges between two TAZ for pt+car
     */
    PTCAR_INTERCHANGES,

    /**
     * matrixMap containing the travel times for each pair of tvz in the
     * base/scenario Mode: car/passenger
     */
    TRAVEL_TIME_MIT,

    /**
     * matrixMap containing the travel times for each pair of tvz in the
     * base/scenario Mode: PT
     */
    TRAVEL_TIME_PT,

    /**
     * matrixMap containing the travel times for each pair of tvz in the
     * base/scenario Mode: Walk
     */
    TRAVEL_TIME_WALK,

    /**
     * matrixMap containing the travel times for each pair of tvz in the
     * base/scenario Mode: Bike
     */
    TRAVEL_TIME_BIKE
}


