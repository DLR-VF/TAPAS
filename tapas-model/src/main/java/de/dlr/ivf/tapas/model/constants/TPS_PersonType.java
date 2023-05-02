/*
 * Copyright (c) 2021 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.model.constants;

/**
 * General person types as enum constants.
 */
public enum TPS_PersonType {
    ADULT, NON_WORKING_ADULT, WORKING_ADULT, CHILD, PUPIL, RETIREE, STUDENT, TRAINEE;

    public static TPS_PersonType getPersonTypeByStatus(int status) {
        switch (status) {
            case 1:
                return CHILD;
            case 2:
                return PUPIL;
            case 3:
                return STUDENT;
            case 4:
                return RETIREE;
            case 5:
                return NON_WORKING_ADULT;
            case 6:
                return WORKING_ADULT;
            case 7:
                return WORKING_ADULT;
            case 8:
                return TRAINEE;
            case 9:
                return NON_WORKING_ADULT;
        }
        return null;
    }
}
