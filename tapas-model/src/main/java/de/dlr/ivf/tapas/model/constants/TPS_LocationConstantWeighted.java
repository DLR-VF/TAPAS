/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.model.constants;

/**
 * NOTE: This class is a stub. It is in an early stage. Development may go on in the future!
 * This class represents the constants for the location codes with a weight for the capacity.
 * This is necessary because at some locations more than one activity can be performed but the capacity is shared.
 *
 * @author hein_mh
 */
public class TPS_LocationConstantWeighted {
    private final double weight;

    protected TPS_LocationConstantWeighted(int id, String[] attributes) {
        weight = 1.0;
    }

    protected TPS_LocationConstantWeighted(int id, String[] attributes, double weight) {
        this.weight = weight;
    }

    public double getWeight() {
        return this.weight;
    }

}
