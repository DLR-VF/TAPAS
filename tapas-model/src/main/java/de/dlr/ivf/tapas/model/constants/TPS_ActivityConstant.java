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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


/**
 * This class represents the activity constants.
 */
@Builder
public class TPS_ActivityConstant {

    /**
     * Constant for the default activity
     */
    public static TPS_ActivityConstant DEFAULT_ACTIVITY;
    /**
     * dummy activity constant
     * This constant is used for calculating the travel times to the representatives of the traffic analysis zones
     */
    public static TPS_ActivityConstant DUMMY = TPS_ActivityConstant.builder()
            .id(-1)
            .internalConstant(new TPS_InternalConstant<>("null",-999, TPS_ActivityCodeType.ZBE))
            .internalConstant(new TPS_InternalConstant<>("null", -999,TPS_ActivityCodeType.VOT))
            .internalConstant(new TPS_InternalConstant<>("null", -999, TPS_ActivityCodeType.TAPAS))
            .internalConstant(new TPS_InternalConstant<>("null", -999, TPS_ActivityCodeType.MCT))
            .internalConstant(new TPS_InternalConstant<>("null", -999, TPS_ActivityCodeType.PRIORITY))
            .isFix(false)
            .isTrip(false)
            .attribute(null)
            .build();

    /**
     * maps ids from the db to {@link TPS_ActivityConstant} objects constructed from corresponding db data
     */
    private static final HashMap<Integer, TPS_ActivityConstant> ACTIVITY_CONSTANTS_MAP = new HashMap<>();
    /**
     * Special attribute of the constant
     */
    private TPS_ActivityConstantAttribute attribute;
    /**
     * This flag determines whether the location which is chosen to this kind of activity is fix. When it is true, e.g.
     * for work, then the location can only be chosen once, otherwise the location to this activity is chosen every time.
     */
    private final boolean isFix;
    /**
     * This flag determines whether the activity constant is a trip or a stay.
     */
    private final boolean isTrip;

    /**
     * id of the activity constant (in the db)
     */
    private final int id;
    /**
     * mapping enum TPS_ActivityCodeType to internal constant, i.e. its three properties (name, code, type)
     */
    @Singular
    private final Map<TPS_ActivityCodeType, TPS_InternalConstant<TPS_ActivityCodeType>> internalAttributes;

    @Singular
    private final Collection<TPS_InternalConstant<TPS_ActivityCodeType>> internalConstants;


    /**
     * @param type like MCT or TAPAS, see {@link TPS_ActivityCodeType}
     * @param code activityCode code from the db
     * @return activity code object for a given TPS_ActivityCodeType and code (int)
     */
    public static TPS_ActivityConstant getActivityCodeByTypeAndCode(TPS_ActivityCodeType type, int code) {
        for (TPS_ActivityConstant tac : ACTIVITY_CONSTANTS_MAP.values()) {
            if (tac.getCode(type) == code) {
                return tac;
            }
        }
        return null;
    }

    /**
     * Convenience method to obtain the activity code for a given {@link TPS_ActivityCodeType}
     *
     * @param type like MCT or TAPAS, see {@link TPS_ActivityCodeType}
     * @return the corresponding code as int
     */
    public int getCode(TPS_ActivityCodeType type) {
        return this.internalAttributes.get(type).getCode();
    }

    /**
     * @return id of the activity constant
     */
    public int getId() {
        return id;
    }

    /**
     * This method compares the stored special attribute with the given attributes. If any of the given attributes is
     * equal to the member attribute true is returned, false otherwise.
     *
     * @param attributes array of attributes to compare the member attribute with
     * @return true if the constant contains on of the given attributes
     */
    public boolean hasAttribute(TPS_ActivityConstantAttribute... attributes) {
        for (TPS_ActivityConstantAttribute attribute : attributes) {
            if (attribute.equals(this.attribute)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return true if the constant is fix, false otherwise
     */
    public boolean isFix() {
        return isFix;
    }

    /**
     * @return true if constant represents a trip, false otherwise
     */
    public boolean isTrip() {
        return isTrip;
    }


    /**
     * This enum provides all available attributes for an activity constant.
     */
    public enum TPS_ActivityConstantAttribute {
        DEFAULT, E_COMMERCE_OUT_OF_HOME, SCHOOL, UNIVERSITY, WORKING,
    }

    /**
     * This enum type provides all activity code types. They are separated into types for the mode choice, values of
     * time, a special type for the TAPAS application and the origin values of the ZBE.
     */
    public enum TPS_ActivityCodeType {
        /**
         * Mode Choice Tree code type<br>
         */
        MCT,
        /**
         * TAPAS code type
         */
        TAPAS,
        /**
         * Values Of Time code type
         */
        VOT,
        /**
         * ZBE code type
         */
        ZBE,
        /**
         * priority code
         */
        PRIORITY
    }
}
