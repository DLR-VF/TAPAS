/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.model.constants;

import lombok.Builder;
import lombok.Singular;

import java.util.*;

/**
 * This class represents the constants for the distances. They are all stored in one distribution to retrieve the best
 * fitting distance constant to each distant value.
 */
@Builder
public class TPS_Distance {

    /**
     * DISTANCE_MAP maps ids to TPS_Distance objects containing (name, code, type)
     */
    private static final HashMap<Integer, TPS_Distance> DISTANCE_MAP = new HashMap<>();
    /**
     * MAX_VALUES maps max values of distance to ids (=keys in DISTANCE_MAP)
     */
    private static final TreeMap<Integer, Integer> MAX_VALUES = new TreeMap<>();

    @Singular
    private final Collection<TPS_InternalConstant<TPS_DistanceCodeType>> internalDistanceCodes;

    /**
     * maximum value of a distance category
     */
    private final int max;
    /**
     * id corresponding to the db entry
     */
    private final int id;
    /**
     * mapping of distance code types to (name, code, type) triples
     * Example:
     * VOT -> (under 10k, 2, VOT)
     * MCT -> (under 7k, 7000, MCT)
     */
    private final EnumMap<TPS_DistanceCodeType, TPS_InternalConstant<TPS_DistanceCodeType>> map;

    /**
     * Empties the global static distance map and the max values map
     */
    public static void clearDistanceMap() {
        DISTANCE_MAP.clear();
        MAX_VALUES.clear();
    }

    /**
     * This method searches the distance value which is the next greater (or equal) than the given value. To this
     * constant the code is returned.
     *
     * @param type     distance type (enum)
     * @param distance determines the corresponding distance category, e.g. distance=2.3km corresponds to category
     *                 'under 5km'
     * @return code corresponding to the given enumeration type and the distance
     */
    public static int getCode(TPS_DistanceCodeType type, double distance) {
        return DISTANCE_MAP.get(MAX_VALUES.ceilingEntry((int) distance).getValue()).getCode(type);
    }

    /**
     * Add distance instance to the global static distance map
     * Additionally add (max_val, id)-key-value-pair to the MAX_VALUES map for better searchability of the DISTANCE_MAP
     */
    public void addDistanceToMap() {
        DISTANCE_MAP.put(this.id, this);
        MAX_VALUES.put(this.max, this.id);
    }

    /**
     * Returns the distance code for this object for a given distance code type
     *
     * @param type distance code type like MCT or VOT
     * @return returns the distance code
     */
    public int getCode(TPS_DistanceCodeType type) {
        return this.map.get(type).getCode();
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
