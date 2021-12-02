/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.constants;

/**
 * Constant for the person groups which are subdivided by having children in the household.
 */
public enum TPS_HasChildCode {

    /**
     * Enumeration of the has child constants
     */
    NO, NON_RELEVANT, YES;


    /**
     * This method checks whether the given number of children fits to the constant. If the constant is NON_RELEVANT then true is
     * returned. In the cases NO and YES the amount has to be zero or greater than 0 to return true. Otherwise false is
     * returned.
     *
     * @param nrOfChildren number of children
     * @return true if the given nr of children fits with this constant, false otherwise
     */
    public boolean fits(int nrOfChildren) {
        boolean bool = false;
        switch (this) {
            case NON_RELEVANT:
                bool = true;
                break;
            case YES:
                bool = nrOfChildren > 0;
                break;
            case NO:
                bool = nrOfChildren == 0;
                break;
        }
        return bool;
    }
}
