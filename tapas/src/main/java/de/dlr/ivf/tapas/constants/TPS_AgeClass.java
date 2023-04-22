/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.constants;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;

/**
 * This class represents the age classes constants. There exist two layers of distributions. The first layer should include a
 * continuous amount of intervals e.g. [10,15][16,18] and so on. The second layer consists of one constant, the NON-RELEVANT
 * constant. It includes all ages from 0 to infinite. When there exists no appropriate age class in the first layer, this
 * constant is used as the default age class.
 */
public class TPS_AgeClass {

    /**
     * Static constant which represents all ages [0, infinite[
     */
    private static TPS_AgeClass NON_RELEVANT = null;
    /**
     * maps ids from the db to {@link TPS_AgeClass} objects constructed from corresponding db data
     */
    private static final HashMap<Integer, TPS_AgeClass> AGE_CLASS_MAP = new HashMap<>();
    /**
     * Included maximum vale
     */
    private final int max;

    public int getMax() {
        return max;
    }

    public int getMin() {
        return min;
    }

    /**
     * Included minimum value
     */
    private final int min;
    /**
     * id obtained from the db
     */
    private final int id;
    /**
     * mapping of the {@link TPS_AgeCodeType} to the internal representation
     */
    private final EnumMap<TPS_AgeCodeType, TPS_InternalConstant<TPS_AgeCodeType>> map;

    /**
     * Basic constructor for a TPS_AgeClass
     *
     * @param id         corresponding id from the db
     * @param attributes attributes have to have a length of 3*n where each 3-segment is of the form (name, code,
     *                   type) like ("under 40", "8", "STBA")
     */
    public TPS_AgeClass(int id, String[] attributes, int min, int max) {
        this.min = min;
        this.max = max;
        this.id = id;
        this.map = new EnumMap<>(TPS_AgeCodeType.class);
        if (attributes.length % 3 != 0) {
            throw new RuntimeException("TPS_AgeClass need n*3 attributes n*(name, code, type): " + attributes.length);
        }
        TPS_InternalConstant<TPS_AgeCodeType> tic;

        for (int i = 0; i < attributes.length; i += 3) {
            tic = new TPS_InternalConstant<>(attributes[i], Integer.parseInt(attributes[i + 1]),
                    TPS_AgeCodeType.valueOf(attributes[i + 2]));
            this.map.put(tic.getType(), tic);
        }

        for (TPS_AgeCodeType type : TPS_AgeCodeType.values()) {
            if (!this.map.containsKey(type)) {
                throw new RuntimeException("Age code for " + type.name() + " for type " + type.name() + " not defined");
            }
        }

        if (this.max == Integer.MAX_VALUE && this.min == 0) {
            NON_RELEVANT = this;
        }
    }

    /**
     * Empties the global static age class map
     */
    public static void clearAgeClassMap() {
        AGE_CLASS_MAP.clear();
    }

    /**
     * This method searches the corresponding age class to the given age., When there exists no appropriate age class the
     * default age class NON_RELEVANT is returned.
     *
     * @param age should be a non-negative integer
     * @return corresponding age class
     */
    public static TPS_AgeClass getAgeClass(int age) {
        for (TPS_AgeClass tac : AGE_CLASS_MAP.values()) {
            if (tac.fits(age) && !TPS_AgeClass.NON_RELEVANT.equals(tac)) return tac;
        }
        return NON_RELEVANT;
    }

    /**
     * @param type age code type
     * @param code code of the internal constant
     * @return constants corresponding to the internal representation of this code
     */
    public static ArrayList<TPS_AgeClass> getConstants(TPS_AgeCodeType type, int code) {
        ArrayList<TPS_AgeClass> list = new ArrayList<>();
        for (TPS_AgeClass tac : AGE_CLASS_MAP.values()) {
            if (tac.getCode(type) == code) list.add(tac);
        }
        return list;
    }

    /**
     * Adds age class object to static collection of all age classes constants.
     * The objects are accessed by their ids from the db.
     */
    public void addAgeClassToMap() {
        AGE_CLASS_MAP.put(this.id, this);
    }

    /**
     * @param age should be a non-negative integer
     * @return true if the given age is inside the interval [minimum, maximum], false otherwise
     */
    public boolean fits(int age) {
        return this.min <= age && age <= this.max;
    }

    /**
     * Convenience method to obtain the age class code for a given {@link TPS_AgeCodeType}
     *
     * @param type like STBA or PersGroup, see {@link TPS_AgeCodeType}
     * @return the corresponding code as int
     */
    public int getCode(TPS_AgeCodeType type) {
        return this.map.get(type).getCode();
    }

    /**
     * There exist two types of age classes. The official distribution from the FSO and the modified one for the TAPAS
     * person groups.
     */
    public enum TPS_AgeCodeType {
        /**
         * Special distribution used in TAPAS for the person groups
         */
        PersGroup,
        /**
         * Age class distribution<br>
         * source: Federal Statistical Office [FSO = STBA (german: 'Statistisches Bundesamt')]
         */
        STBA
    }

}
