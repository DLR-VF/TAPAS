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
class TPS_LocationConstant {

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
