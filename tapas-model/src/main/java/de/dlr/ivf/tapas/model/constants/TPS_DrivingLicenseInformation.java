/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.model.constants;

/**
 * This class represents the information about driving licenses of a person. One person can have more than one driving
 * license information. But there are restrictions about their combination.
 * <p>
 * There exist three types of information:
 * a) no driving license (not combinable)
 * b) no driving license information available (not combinable)
 * c) driving license (e.g. motorbike, car, truck) (combinable)
 */
public enum TPS_DrivingLicenseInformation {
    UNKNOWN(-1), // ordinal: 0
    CAR(1), //ordinal: 1
    NO_DRIVING_LICENSE(2); //ordinal: 2

    /**
     * code of the driving license information from the database
     */
    private int code;

    /**
     * @param code of the driving license information from the database
     */
    TPS_DrivingLicenseInformation(int code) {
        this.code = code;
    }

    /**
     * @param code corresponding to an TPS_DrivingLicenseInformation enum
     * @return driving license information
     */
    public static TPS_DrivingLicenseInformation getEnum(int code) {
        for (TPS_DrivingLicenseInformation s : TPS_DrivingLicenseInformation.values()) {
            if (s.code == code) return s;
        }
        return TPS_DrivingLicenseInformation.UNKNOWN;
    }

    /**
     * @return code of TPS_DrivingLicenseInformation
     */
    public int getCode() {
        return code;
    }

    /**
     * @param code code to set
     */
    public void setCode(int code) {
        this.code = code;
    }

    /**
     * @return true if there is information about the driving license, i.e. in either case of CAR or NO_DRIVING_LICENSE
     */
    public boolean hasDrivingLicenseInformation() {
        return !UNKNOWN.equals(this);
    }
}

