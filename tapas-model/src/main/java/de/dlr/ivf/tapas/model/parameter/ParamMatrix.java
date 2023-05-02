/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.model.parameter;

/**
 * This class provides all matrix enums which determine the name of
 * parameters available in the application
 * <p>
 * This enum provides constants in matrix form for the application. It is
 * possible that on constant has two matrices depending on the type of the
 * constant. If it is simulation type dependent there exist one matrix for
 * the base an one for the scenario case.
 */
public enum ParamMatrix {

    /**
     * matrix containing the distances on the street net for each pair of
     * tvz
     */
    DISTANCES_STREET,
    /**
     * matrix containing the distances on the street and park net for each pair of
     * tvz
     */
    DISTANCES_WALK,
    /**
     * matrix containing the distances on the street and park net for each pair of
     * tvz
     */
    DISTANCES_BIKE,
    /**
     * matrix containing the distances on the train net for each pair of
     * tvz
     */
    DISTANCES_PT,
    /**
     * matrix containing the distances of the beelines
     * tvz
     */
    DISTANCES_BL
}


