/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.constants;

/**
 * Sex constant.
 */
public enum TPS_Sex {

    /**
     * All possible values for the sex.
     */

    NON_RELEVANT(0), //resolves to ordinal 0
    MALE(1), //resolves to ordinal 1
    FEMALE(2), //resolves to ordinal 2
    UNKNOWN(3); //resolves to ordinal 3

    public int code;

    TPS_Sex(int code) {
        this.code = code;
    }

    public static TPS_Sex getEnum(int code) {
        for (TPS_Sex s : TPS_Sex.values()) {
            if (s.code == code) return s;
        }
        return TPS_Sex.UNKNOWN;
    }

    public int getCode() {
        return code;
    }

    /**
     * This method checks whether the sex of a person fits to the constant. If the constant is NON_RELEVANT then true is
     * returned. In the other cases it has to be equal to the constant.
     *
     * @param sex gender attribute of the person
     * @return bool value of current TPS_Sex enum and sex of person
     */
    public boolean fits(TPS_Sex sex) {
        switch (this) {
            case NON_RELEVANT:
                return true;
            case MALE:
                return MALE.equals(sex);
            case FEMALE:
                return FEMALE.equals(sex);
            case UNKNOWN:
                return UNKNOWN.equals(sex);
        }
        return false;
    }
}
