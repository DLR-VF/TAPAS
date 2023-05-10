/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.model.constants;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.util.*;

/**
 * This class represents the constants for the location codes.
 */
@Builder
@Getter
public class TPS_LocationConstant {

    /**
     * This constant represents the location code for the home of a person/household
     */
    public static TPS_LocationConstant HOME = TPS_LocationConstant.builder()
            .id(-1)
            .internalConstant(new TPS_InternalConstant<>("null", -1, TPS_LocationCodeType.GENERAL))
            .internalConstant(new TPS_InternalConstant<>("null", -1, TPS_LocationCodeType.TAPAS))
            .build();
    /**
     * maps ids from the db to TPS_LocationConstant objects constructed from corresponding db data
     */
    private static final HashMap<Integer, TPS_LocationConstant> locationConstantMappings = new HashMap<>();
    /**
     * maps a location code type (like TAPAS o GENERAL) to an internal constant and its fields (name, code and
     * the same location code)
     */
    private final Map<TPS_LocationCodeType, TPS_InternalConstant<TPS_LocationCodeType>> internalConstantMappings;

    @Singular
    private final Collection<TPS_InternalConstant<TPS_LocationCodeType>> internalConstants;

    /**
     * id from the db
     */
    private final int id;


    /**
     * Empties the location constant map
     */
    public static void clearLocationConstantMap() {
        locationConstantMappings.clear();
    }

    /**
     * @param type like GENERAL or TAPAS, see TPS_LocationCodeType
     * @param code location constant code from the db
     * @return location constant object for a given TPS_LocationCodeType and code (int)
     */
    public static TPS_LocationConstant getLocationCodeByTypeAndCode(TPS_LocationCodeType type, int code) {
        for (TPS_LocationConstant tac : locationConstantMappings.values()) {
            if (tac.getCode(type) == code) {
                return tac;
            }
        }
        return null;
    }

    /**
     * Add to static collection of all location constants.
     * The objects are accessed by their ids from the db.
     */
    public void addLocationCodeToMap() {
        locationConstantMappings.put(id, this);
    }

    /**
     * @param type location code enum type (e.g. TAPAS or GENERAL)
     * @return code corresponding to the location code enum type
     */
    public int getCode(TPS_LocationCodeType type) {
        return internalConstantMappings.get(type).getCode();
    }

    /**
     * @return id of the location constant
     */
    public int getId() {
        return id;
    }


    /**
     * There exist two types of location codes. The general location codes and special modified codes for tapas.
     */
    public enum TPS_LocationCodeType {
        /**
         * General location codes<br>
         */
        GENERAL,
        /**
         * Modified location codes for TAPAS
         */
        TAPAS
    }

}
