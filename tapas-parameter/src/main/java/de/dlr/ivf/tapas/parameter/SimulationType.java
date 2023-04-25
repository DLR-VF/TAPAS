/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.parameter;


/**
 * The two types of simulations: basis and scenario case
 *
 * @author mark_ma
 */
public enum SimulationType {
    BASE(0), SCENARIO(1);
    /**
     * index of the simulation type. This index can be used to access values
     * in an array of size two.
     */
    private final int index;

    /**
     * Constructor sets the index.
     *
     * @param index
     */
    SimulationType(int index) {
        this.index = index;
    }

    /**
     * @return index value
     */
    public int getIndex() {
        return index;
    }
}
