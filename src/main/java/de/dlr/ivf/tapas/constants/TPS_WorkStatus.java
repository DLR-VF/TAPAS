/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.constants;

/**
 * Constant for the work status of a person (group).
 */
public enum TPS_WorkStatus {

    /**
     * Enumeration of the working constants
     * WORKING means any PART_TIME or FULL_TIME
     */
    NON_RELEVANT, NON_WORKING, WORKING, PART_TIME, FULL_TIME;


    /**
     * This method checks whether the given double work status fits to the constant. If the constant is NON_RELEVANT then true is
     * returned. WORKING means any of PART_TIME or FULL_TIME (so the double value is greater than 0)
     *
     * @param working status as a double value 0 means non_working, 0.5 means part time, 1 means full time
     * @return true if the given double value fits with the constant, false otherwise
     */
    public boolean fits(double working) {
        switch (this) {
            case NON_RELEVANT:
                return true;
            case NON_WORKING:
                return working == 0;
            case WORKING:
                return working > 0;
            case PART_TIME:
                return working == 0.5;
            case FULL_TIME:
                return working == 1;
        }
        return false;
    }
}
