/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.model.constants;

import java.util.HashMap;
import java.util.TreeMap;

/**
 * This class represents the income codes. There is also an ascending distribution of the values stored, to find the fitting
 * code for any income value.
 */
public class TPS_Income {

    /**
     * INCOME_MAP maps id to income objects
     */
    private static final HashMap<Integer, TPS_Income> INCOME_MAP = new HashMap<>();
    /**
     * MAX_VALUES maps max values of income to ids (=keys in INCOME_MAP)
     */
    private static final TreeMap<Integer, Integer> MAX_VALUES = new TreeMap<>();
    /**
     * name/description of the income category
     */
    public String name;
    /**
     * maximum value of a distance category
     */
    private final int max;
    /**
     * id corresponding to the db entry
     */
    private final int id;
    /**
     * code of the income category
     */
    private final int code;


    /**
     * Basic constructor of the income constant object
     *
     * @param id   corresponding to the db entry
     * @param name description of the income category
     * @param code of the income category
     * @param max  maximum value of a distance category
     */
    public TPS_Income(int id, String name, int code, int max) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.max = max;
    }

    /**
     * Empties the global static income map and the max values map
     */
    public static void clearIncomeMap() {
        INCOME_MAP.clear();
        MAX_VALUES.clear();
    }

    /**
     * This method searches the income value which is the next greater (or equal) than the given value. To this
     * constant the code is returned.
     *
     * @param income determines the corresponding income category, e.g. income=1320 corresponds to category 'under 1500'
     * @return fitting income code
     */
    public static int getCode(double income) {
        return INCOME_MAP.get(MAX_VALUES.ceilingEntry((int) income).getValue()).getCode();
    }

    /**
     * Associates an id with a TPS_Income object.
     * Additionally maps a max_value to an id to conveniently return a TPS_Income object or its code
     */
    public void addToIncomeMap() {
        INCOME_MAP.put(id, this);
        MAX_VALUES.put(max, id);
    }

    /**
     * @return income code
     */
    public int getCode() {
        return this.code;
    }

}
