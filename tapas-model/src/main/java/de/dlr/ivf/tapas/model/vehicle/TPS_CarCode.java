/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.model.vehicle;

/**
 * Constant for the person groups which are subdivided by the ownership of cars.
 */
public enum TPS_CarCode {

    /**
     * Enumeration of the car constants
     */
    NO(2), NON_RELEVANT(0), YES(1);

    /**
     * car enumeration constant code
     */
    public int code;

    /**
     * @param code of the car
     */
    TPS_CarCode(int code) {
        this.code = code;
    }

    public static TPS_CarCode getEnum(int code) {
        for (TPS_CarCode cc : TPS_CarCode.values()) {
            if (cc.code == code) return cc;
        }
        return TPS_CarCode.NON_RELEVANT;
    }

    /**
     * This method checks whether the given amount of cars fits to the constant. If the constant is NON_RELEVANT then true is
     * returned. In the cases NO and YES the amount has to be zero or greater than 0 to return true. False is otherwise
     * returned.
     *
     * @param amountOfCars number of cars
     * @return true if the given amount of cars fits with this constant, false otherwise
     */
    public boolean fits(int amountOfCars) {
        boolean bool = false;
        switch (this) {
            case NON_RELEVANT:
                bool = true;
                break;
            case YES:
                bool = amountOfCars > 0;
                break;
            case NO:
                bool = amountOfCars == 0;
                break;
        }
        return bool;
    }
}
