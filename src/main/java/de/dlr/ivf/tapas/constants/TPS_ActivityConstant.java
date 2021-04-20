/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.constants;

import java.util.EnumMap;
import java.util.HashMap;


/**
 * This class represents the activity constants.
 */
public class TPS_ActivityConstant {

    /**
     * Constant for the default activity
     */
    public static TPS_ActivityConstant DEFAULT_ACTIVITY;
    /**
     * dummy activity constant
     * This constant is used for calculating the travel times to the representatives of the traffic analysis zones
     */
    public static TPS_ActivityConstant DUMMY = new TPS_ActivityConstant(-1,
            new String[]{"null", "-999", "ZBE", "null", "-999", "VOT", "null", "-999", "TAPAS", "null", "-999", "MCT", "null", "-999", "PRIORITY"},
            false, false, null);
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
    private final EnumMap<TPS_ActivityCodeType, TPS_InternalConstant<TPS_ActivityCodeType>> map;

    /**
     * Basic constructor for a TPS_ActivityConstant
     *
     * @param id         corresponding id from the db
     * @param attributes attributes have to have a length of 3*n where each 3-segment is of the form (name, code,
     *                   type) like ("SCHOOL", "410", "MCT")
     * @param isTrip     True if the activity constant is a trip or a stay
     * @param isFix      True if the location to this activity is fixed, i.e. is set once, e.g. SCHOOL, WORK etc.
     * @param attr       special attribute, see{@link TPS_ActivityConstantAttribute}
     */
    public TPS_ActivityConstant(int id, String[] attributes, boolean isTrip, boolean isFix, String attr) {
        if ((attributes.length) % 3 != 0) {
            throw new RuntimeException(
                    "ActivityConstant need n*3 attributes n*(name, code, type): " + attributes.length);
        }
        this.id = id;
        this.map = new EnumMap<>(TPS_ActivityCodeType.class);
        TPS_InternalConstant<TPS_ActivityCodeType> iac;
        for (int i = 0; i < attributes.length; i += 3) {
            iac = new TPS_InternalConstant<>(attributes[i], Integer.parseInt(attributes[i + 1]),
                    TPS_ActivityCodeType.valueOf(attributes[i + 2]));
            this.map.put(iac.getType(), iac);
        }

        for (TPS_ActivityCodeType type : TPS_ActivityCodeType.values()) {
            if (!this.map.containsKey(type)) {
                throw new RuntimeException(
                        "Activity code for " + this.getId() + " for type " + type.name() + " not " + "defined");
            }
        }
        //set isTrip(bool), isFix(bool) and attribute(TPS_ActivityCodeAttribute)
        this.isTrip = isTrip;
        this.isFix = isFix;
        if (!(attr == null || attr.equals("null"))) {
            this.attribute = TPS_ActivityConstantAttribute.valueOf(attr);
            if (this.hasAttribute(TPS_ActivityConstantAttribute.DEFAULT)) {
                DEFAULT_ACTIVITY = this;
            }
        }

    }

    /**
     * Empties the global static activity constant map
     */
    public static void clearActivityConstantMap() {
        ACTIVITY_CONSTANTS_MAP.clear();
    }

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
     * Add to static collection of all activity code constants.
     * The objects are accessed by their ids from the db.
     */
    public void addActivityConstantToMap() {
        ACTIVITY_CONSTANTS_MAP.put(id, this);
    }

    /**
     * Convenience method to obtain the activity code for a given {@link TPS_ActivityCodeType}
     *
     * @param type like MCT or TAPAS, see {@link TPS_ActivityCodeType}
     * @return the corresponding code as int
     */
    public int getCode(TPS_ActivityCodeType type) {
        return this.map.get(type).getCode();
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
