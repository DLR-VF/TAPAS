/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.model.constants;

import lombok.Getter;

/**
 * This class represents the constants for the distances. They are all stored in one distribution to retrieve the best
 * fitting distance constant to each distant value.
 */
public class TPS_Distance {

    /**
     * maximum value of a distance category
     */
    private final int max;
    /**
     * id corresponding to the db entry
     */
    private final int id;

    /**
     * -- GETTER --
     *  This method searches the distance value which is the next greater (or equal) than the given value. To this
     *  constant the code is returned.
     *
     * @return code corresponding to the given enumeration type and the distance
     */
    @Getter
    private final int code;

    public TPS_Distance(int id, int distanceThreshold, int code){
        this.id = id;
        this.max = distanceThreshold;
        this.code = code;
    }


    /**
     * There exist two different distributions for the distances. One for the mode choice and one for the values of
     * time.
     */
    public enum TPS_DistanceCodeType {
        /**
         * Distance distribution for the mode choice tree
         */
        MCT,
        /**
         * Distance distribution for the values of time
         */
        VOT
    }


}
