/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.model.parameter;

import org.apache.commons.lang3.tuple.MutablePair;

import java.util.EnumMap;

public class ParamValueClass {
    private final EnumMap<ParamValue, MutablePair<ParamType, Number>> paramValues;

    ParamValueClass() {
        this.paramValues = new EnumMap<>(ParamValue.class);
        this.paramValues.put(ParamValue.AVERAGE_DISTANCE_PT_STOP, new MutablePair<>(ParamType.TMP, 0d));
        this.paramValues.put(ParamValue.AUTOMATIC_VEHICLE_LEVEL,
                new MutablePair<>(ParamType.OPTIONAL, (Integer.MAX_VALUE - 1)));
        this.paramValues.put(ParamValue.AUTOMATIC_VEHICLE_MIN_DRIVER_AGE,
                new MutablePair<>(ParamType.OPTIONAL, 999));

        this.paramValues.put(ParamValue.AUTOMATIC_VALET_PARKING,
                new MutablePair<>(ParamType.OPTIONAL, (Integer.MAX_VALUE - 1)));
        this.paramValues.put(ParamValue.GLOBAL_AUTOMATION_PROBABILITY, new MutablePair<>(ParamType.OPTIONAL, -1));
        this.paramValues.put(ParamValue.GLOBAL_AUTOMATION_LEVEL,
                new MutablePair<>(ParamType.OPTIONAL, (Integer.MAX_VALUE - 1)));
        this.paramValues.put(ParamValue.AUTOMATIC_PARKING_ACCESS, new MutablePair<>(ParamType.OPTIONAL, 60));
        this.paramValues.put(ParamValue.AUTOMATIC_PARKING_EGRESS, new MutablePair<>(ParamType.OPTIONAL, 60));
        this.paramValues.put(ParamValue.AUTOMATIC_VEHICLE_RAMP_UP_TIME, new MutablePair<>(ParamType.OPTIONAL, 300));
        this.paramValues.put(ParamValue.AUTOMATIC_VEHICLE_TIME_MOD_FAR, new MutablePair<>(ParamType.OPTIONAL, 1.0));
        this.paramValues.put(ParamValue.AUTOMATIC_VEHICLE_TIME_MOD_NEAR, new MutablePair<>(ParamType.OPTIONAL, 1.0));
        this.paramValues.put(ParamValue.AUTOMATIC_VEHICLE_TIME_MOD_THRESHOLD,
                new MutablePair<>(ParamType.OPTIONAL, 3000.0));
        this.paramValues.put(ParamValue.AVAILABILITY_FACTOR_BIKE, new MutablePair<>(ParamType.OPTIONAL, 1.0));
        this.paramValues.put(ParamValue.AVAILABILITY_FACTOR_CARSHARING, new MutablePair<>(ParamType.OPTIONAL, 1.0));
        this.paramValues.put(ParamValue.BEELINE_FACTOR_BIKE, new MutablePair<>(ParamType.DEFAULT, null));
        this.paramValues.put(ParamValue.BEELINE_FACTOR_FOOT, new MutablePair<>(ParamType.DEFAULT, null));
        this.paramValues.put(ParamValue.BEELINE_FACTOR_MIT, new MutablePair<>(ParamType.DEFAULT, null));
        this.paramValues.put(ParamValue.BEELINE_FACTOR_PT, new MutablePair<>(ParamType.DEFAULT, null));
        this.paramValues.put(ParamValue.BIKE_COST_PER_KM, new MutablePair<>(ParamType.RUN, 0d));
        this.paramValues.put(ParamValue.BIKE_COST_PER_KM_BASE, new MutablePair<>(ParamType.RUN, 0d));
        this.paramValues.put(ParamValue.CFN_SPECIAL_THRESHOLD, new MutablePair<>(ParamType.DEFAULT, 900));
        this.paramValues.put(ParamValue.CARSHARING_ACCESS_ADDON, new MutablePair<>(ParamType.OPTIONAL, 180));
        this.paramValues.put(ParamValue.DB_HH_SAMPLE_SIZE, new MutablePair<>(ParamType.DB, null));
        this.paramValues.put(ParamValue.DB_PORT, new MutablePair<>(ParamType.DB, null));
        this.paramValues.put(ParamValue.DEFAULT_ACT_CODE_ZBE, new MutablePair<>(ParamType.DEFAULT, null));
        this.paramValues.put(ParamValue.DEFAULT_BLOCK_SCORE, new MutablePair<>(ParamType.DEFAULT, 3));
        this.paramValues.put(ParamValue.DEFAULT_VOT, new MutablePair<>(ParamType.DEFAULT, null));
        this.paramValues.put(ParamValue.DEFAULT_SCHOOL_BUS_ACCESS, new MutablePair<>(ParamType.DEFAULT, 300));
        this.paramValues.put(ParamValue.DEFAULT_SCHOOL_BUS_EGRESS, new MutablePair<>(ParamType.DEFAULT, 300));
        this.paramValues.put(ParamValue.DEFAULT_SCHOOL_BUS_WAIT, new MutablePair<>(ParamType.DEFAULT, 600));
        this.paramValues.put(ParamValue.PT_TT_FACTOR, new MutablePair<>(ParamType.DEFAULT, 0.02));
        this.paramValues.put(ParamValue.PT_ACCESS_FACTOR, new MutablePair<>(ParamType.DEFAULT, 0.01));
        this.paramValues.put(ParamValue.PT_EGRESS_FACTOR, new MutablePair<>(ParamType.DEFAULT, 0.01));
        this.paramValues.put(ParamValue.PT_MINIMUM_TT, new MutablePair<>(ParamType.DEFAULT, 120));
        this.paramValues.put(ParamValue.DELTA_START_EARLIER, new MutablePair<>(ParamType.DEFAULT, null));
        this.paramValues.put(ParamValue.DELTA_START_LATER, new MutablePair<>(ParamType.DEFAULT, null));
        this.paramValues.put(ParamValue.FINANCE_BUDGET_E, new MutablePair<>(ParamType.DEFAULT, null));
        this.paramValues.put(ParamValue.FINANCE_BUDGET_F, new MutablePair<>(ParamType.DEFAULT, null));
        this.paramValues.put(ParamValue.FINANCE_BUDGET_WP, new MutablePair<>(ParamType.DEFAULT, null));
        this.paramValues.put(ParamValue.GAMMA_LOCATION_WEIGHT, new MutablePair<>(ParamType.OPTIONAL, null));
        this.paramValues.put(ParamValue.ITERATION, new MutablePair<>(ParamType.RUN, 0));
        this.paramValues.put(ParamValue.LOC_CHOICE_MOD_CFN4, new MutablePair<>(ParamType.DEFAULT, null));
        this.paramValues.put(ParamValue.LOC_CAPACITY_FACTOR, new MutablePair<>(ParamType.DEFAULT, 1.0));
        this.paramValues.put(ParamValue.MAX_SYSTEM_SPEED, new MutablePair<>(ParamType.DEFAULT, 25));
        this.paramValues.put(ParamValue.MAX_TIME_DIFFERENCE, new MutablePair<>(ParamType.DEFAULT, null));
        this.paramValues.put(ParamValue.MAX_TRIES_PERSON, new MutablePair<>(ParamType.DEFAULT, null));
        this.paramValues.put(ParamValue.MAX_TRIES_SCHEME, new MutablePair<>(ParamType.DEFAULT, null));
        this.paramValues.put(ParamValue.MAX_TRIES_LOCATION_SELECTION, new MutablePair<>(ParamType.DEFAULT, 6));
        this.paramValues.put(ParamValue.MAX_VISUM_ITERATION, new MutablePair<>(ParamType.DEFAULT, 0));
        this.paramValues.put(ParamValue.MAX_SUMO_ITERATION, new MutablePair<>(ParamType.DEFAULT, 0));
        this.paramValues.put(ParamValue.MAX_WALK_DIST, new MutablePair<>(ParamType.DEFAULT, 7500));
        this.paramValues.put(ParamValue.MIN_DIST, new MutablePair<>(ParamType.DEFAULT, null));
        this.paramValues.put(ParamValue.MIN_AGE_CARSHARING, new MutablePair<>(ParamType.OPTIONAL, 20.0));
        this.paramValues.put(ParamValue.MIT_GASOLINE_COST_PER_KM, new MutablePair<>(ParamType.RUN, null));
        this.paramValues.put(ParamValue.MIT_GASOLINE_AUTOMATED_COST_PER_KM, new MutablePair<>(ParamType.RUN, null));
        this.paramValues.put(ParamValue.MIT_GASOLINE_COST_PER_KM_BASE, new MutablePair<>(ParamType.RUN, null));
        this.paramValues.put(ParamValue.MIT_DIESEL_COST_PER_KM, new MutablePair<>(ParamType.RUN, null));
        this.paramValues.put(ParamValue.MIT_DIESEL_AUTOMATED_COST_PER_KM, new MutablePair<>(ParamType.RUN, null));
        this.paramValues.put(ParamValue.MIT_DIESEL_COST_PER_KM_BASE, new MutablePair<>(ParamType.RUN, null));
        this.paramValues.put(ParamValue.MIT_PLUGIN_COST_PER_KM, new MutablePair<>(ParamType.RUN, null));
        this.paramValues.put(ParamValue.MIT_PLUGIN_AUTOMATED_COST_PER_KM, new MutablePair<>(ParamType.RUN, null));
        this.paramValues.put(ParamValue.MIT_PLUGIN_COST_PER_KM_BASE, new MutablePair<>(ParamType.RUN, null));
        this.paramValues.put(ParamValue.MIT_ELECTRO_COST_PER_KM, new MutablePair<>(ParamType.RUN, null));
        this.paramValues.put(ParamValue.MIT_ELECTRO_AUTOMATED_COST_PER_KM, new MutablePair<>(ParamType.RUN, null));
        this.paramValues.put(ParamValue.MIT_ELECTRO_COST_PER_KM_BASE, new MutablePair<>(ParamType.RUN, null));
        this.paramValues.put(ParamValue.MIT_FUELCELL_COST_PER_KM, new MutablePair<>(ParamType.RUN, null));
        this.paramValues.put(ParamValue.MIT_FUELCELL_COST_PER_KM_BASE, new MutablePair<>(ParamType.RUN, null));
        this.paramValues.put(ParamValue.MIT_GAS_COST_PER_KM, new MutablePair<>(ParamType.RUN, null));
        this.paramValues.put(ParamValue.MIT_GAS_COST_PER_KM_BASE, new MutablePair<>(ParamType.RUN, null));
        this.paramValues.put(ParamValue.MIT_VARIABLE_COST_PER_KM, new MutablePair<>(ParamType.RUN, null));
        this.paramValues.put(ParamValue.MIT_VARIABLE_COST_PER_KM_BASE, new MutablePair<>(ParamType.RUN, null));
        this.paramValues.put(ParamValue.MIT_FUEL_COST_PER_KM_COMMUTE, new MutablePair<>(ParamType.RUN, null));
        this.paramValues.put(ParamValue.MIT_FUEL_COST_PER_KM_COMMUTE_BASE, new MutablePair<>(ParamType.RUN, null));
        this.paramValues.put(ParamValue.MIT_INCOME_CLASS_COMMUTE, new MutablePair<>(ParamType.RUN, 5));
        this.paramValues.put(ParamValue.MIT_RANGE_CONVENTIONAL, new MutablePair<>(ParamType.RUN, null));
        this.paramValues.put(ParamValue.MIT_RANGE_PLUGIN, new MutablePair<>(ParamType.RUN, null));
        this.paramValues.put(ParamValue.MIT_RANGE_EMOBILE, new MutablePair<>(ParamType.RUN, null));
        this.paramValues.put(ParamValue.OVERALL_TIME_E, new MutablePair<>(ParamType.DEFAULT, 7.0));
        this.paramValues.put(ParamValue.OVERALL_TIME_F, new MutablePair<>(ParamType.DEFAULT, 10.0));
        this.paramValues.put(ParamValue.PARKING_FEE_CAT_1, new MutablePair<>(ParamType.RUN, null));
        this.paramValues.put(ParamValue.PARKING_FEE_CAT_1_BASE, new MutablePair<>(ParamType.RUN, null));
        this.paramValues.put(ParamValue.PARKING_FEE_CAT_2, new MutablePair<>(ParamType.RUN, null));
        this.paramValues.put(ParamValue.PARKING_FEE_CAT_2_BASE, new MutablePair<>(ParamType.RUN, null));
        this.paramValues.put(ParamValue.PARKING_FEE_CAT_3, new MutablePair<>(ParamType.RUN, null));
        this.paramValues.put(ParamValue.PARKING_FEE_CAT_3_BASE, new MutablePair<>(ParamType.RUN, null));
        this.paramValues.put(ParamValue.PASS_COST_PER_KM, new MutablePair<>(ParamType.RUN, 0d));
        this.paramValues.put(ParamValue.PASS_COST_PER_KM_BASE, new MutablePair<>(ParamType.RUN, 0d));
        this.paramValues.put(ParamValue.PASS_PROBABILITY_HOUSEHOLD_CAR, new MutablePair<>(ParamType.OPTIONAL, 1d));
        this.paramValues.put(ParamValue.PASS_PROBABILITY_RESTRICTED, new MutablePair<>(ParamType.OPTIONAL, 0d));
        this.paramValues.put(ParamValue.PNR_COST_PER_TRIP, new MutablePair<>(ParamType.RUN, 0d));
        this.paramValues.put(ParamValue.PNR_COST_PER_HOUR, new MutablePair<>(ParamType.RUN, 0d));
        this.paramValues.put(ParamValue.PT_COST_PER_KM, new MutablePair<>(ParamType.RUN, null));
        this.paramValues.put(ParamValue.PTBIKE_COST_PER_KM, new MutablePair<>(ParamType.OPTIONAL, 0d));
        this.paramValues.put(ParamValue.PT_COST_PER_KM_BASE, new MutablePair<>(ParamType.RUN, null));
        this.paramValues.put(ParamValue.PTBIKE_COST_PER_KM_BASE, new MutablePair<>(ParamType.OPTIONAL, 0d));
        this.paramValues.put(ParamValue.SHARED_MODE_COST_REDUCTION, new MutablePair<>(ParamType.OPTIONAL, 1d));
        this.paramValues.put(ParamValue.LOW_INCOME_THRESHOLD, new MutablePair<>(ParamType.OPTIONAL, 0d));
        this.paramValues.put(ParamValue.RANDOM_SEED_NUMBER, new MutablePair<>(ParamType.DEFAULT, null));
        this.paramValues.put(ParamValue.REJUVENATE_BY_NB_YEARS, new MutablePair<>(ParamType.DEFAULT, null));
        this.paramValues.put(ParamValue.REJUVENATE_AGE, new MutablePair<>(ParamType.DEFAULT, 75));
        this.paramValues.put(ParamValue.RELATIVE_DURATION_LONGER, new MutablePair<>(ParamType.DEFAULT, null));
        this.paramValues.put(ParamValue.RELATIVE_DURATION_SHORTER, new MutablePair<>(ParamType.DEFAULT, null));
        this.paramValues.put(ParamValue.SEC_TIME_SLOT, new MutablePair<>(ParamType.DEFAULT, 300));
        this.paramValues.put(ParamValue.SCALE_SHIFT, new MutablePair<>(ParamType.DEFAULT, null));
        this.paramValues.put(ParamValue.SCALE_STRETCH, new MutablePair<>(ParamType.DEFAULT, null));
        this.paramValues.put(ParamValue.TAXI_COST_PER_KM, new MutablePair<>(ParamType.RUN, null));
        this.paramValues.put(ParamValue.TAXI_COST_PER_KM_BASE, new MutablePair<>(ParamType.RUN, null));
        this.paramValues.put(ParamValue.TIME_BUDGET_E, new MutablePair<>(ParamType.DEFAULT, null));
        this.paramValues.put(ParamValue.TIME_BUDGET_F, new MutablePair<>(ParamType.DEFAULT, null));
        this.paramValues.put(ParamValue.TIME_BUDGET_WP, new MutablePair<>(ParamType.DEFAULT, null));
        this.paramValues.put(ParamValue.TOLL_CAT_1, new MutablePair<>(ParamType.RUN, null));
        this.paramValues.put(ParamValue.TOLL_CAT_1_BASE, new MutablePair<>(ParamType.RUN, null));
        this.paramValues.put(ParamValue.TOLL_CAT_2, new MutablePair<>(ParamType.RUN, null));
        this.paramValues.put(ParamValue.TOLL_CAT_2_BASE, new MutablePair<>(ParamType.RUN, null));
        this.paramValues.put(ParamValue.TOLL_CAT_3, new MutablePair<>(ParamType.RUN, null));
        this.paramValues.put(ParamValue.TOLL_CAT_3_BASE, new MutablePair<>(ParamType.RUN, null));
        this.paramValues.put(ParamValue.CAR_SHARING_COST_PER_KM, new MutablePair<>(ParamType.RUN, 0.64));
        this.paramValues.put(ParamValue.CAR_SHARING_COST_PER_KM_BASE, new MutablePair<>(ParamType.RUN, 0.64));
        this.paramValues.put(ParamValue.RIDE_POOLING_COST_PER_KM, new MutablePair<>(ParamType.RUN, null));
        this.paramValues.put(ParamValue.VELOCITY_BIKE, new MutablePair<>(ParamType.DEFAULT, null));
        this.paramValues.put(ParamValue.VELOCITY_CAR, new MutablePair<>(ParamType.DEFAULT, null));
        this.paramValues.put(ParamValue.VELOCITY_FOOT, new MutablePair<>(ParamType.DEFAULT, null));
        this.paramValues.put(ParamValue.VELOCITY_TRAIN, new MutablePair<>(ParamType.DEFAULT, null));
        this.paramValues.put(ParamValue.WALK_COST_PER_KM, new MutablePair<>(ParamType.RUN, 0d));
        this.paramValues.put(ParamValue.WALK_COST_PER_KM_BASE, new MutablePair<>(ParamType.RUN, 0d));
        this.paramValues.put(ParamValue.WEIGHT_OCCUPANCY, new MutablePair<>(ParamType.DEFAULT, null));
        this.paramValues.put(ParamValue.WEIGHT_WORKING_AT_HOME, new MutablePair<>(ParamType.DEFAULT, null));
        this.paramValues.put(ParamValue.WEIGHT_WORKING_CHAINS, new MutablePair<>(ParamType.DEFAULT, null));
        this.paramValues.put(ParamValue.PTBIKE_MODE_CONSTANT, new MutablePair<>(ParamType.OPTIONAL, 0d));
        this.paramValues.put(ParamValue.PTCAR_MODE_CONSTANT, new MutablePair<>(ParamType.OPTIONAL, 0d));
        this.paramValues.put(ParamValue.PTBIKE_MODE_PROB_FACTOR, new MutablePair<>(ParamType.OPTIONAL, 1.));
        this.paramValues.put(ParamValue.PTCAR_MODE_PROB_FACTOR, new MutablePair<>(ParamType.OPTIONAL, 1.));
        this.paramValues.put(ParamValue.LOGSUM_WEIGHT_MU, new MutablePair<>(ParamType.OPTIONAL, 1.));
        this.paramValues.put(ParamValue.LOGSUM_INTERACT_MU, new MutablePair<>(ParamType.OPTIONAL, 1.));
        this.paramValues.put(ParamValue.LOGSUM_CALIB_EDUCATION, new MutablePair<>(ParamType.OPTIONAL, 1.));
        this.paramValues.put(ParamValue.LOGSUM_CALIB_STUDENT, new MutablePair<>(ParamType.OPTIONAL, 1.));
        this.paramValues.put(ParamValue.LOGSUM_CALIB_WORK, new MutablePair<>(ParamType.OPTIONAL, 1.));
        this.paramValues.put(ParamValue.LOGSUM_CALIB_SHOP, new MutablePair<>(ParamType.OPTIONAL, 1.));
        this.paramValues.put(ParamValue.LOGSUM_CALIB_ERRANT, new MutablePair<>(ParamType.OPTIONAL, 1.));
        this.paramValues.put(ParamValue.LOGSUM_CALIB_FREETIME_HOME, new MutablePair<>(ParamType.OPTIONAL, 1.));
        this.paramValues.put(ParamValue.LOGSUM_CALIB_FREETIME, new MutablePair<>(ParamType.OPTIONAL, 1.));
        this.paramValues.put(ParamValue.LOGSUM_CALIB_MISC, new MutablePair<>(ParamType.OPTIONAL, 1.));
        this.paramValues.put(ParamValue.SIMULATION_END_TIME, new MutablePair<>(ParamType.OPTIONAL, 1440));
        this.paramValues.put(ParamValue.CAR_SHARING_CHECKOUT_PENALTY, new MutablePair<>(ParamType.OPTIONAL,0));
        this.paramValues.put(ParamValue.NUM_WORKERS, new MutablePair<>(ParamType.RUN, 1));

    }

    /**
     * clears the attached value to the default value
     *
     * @param param value parameter enum
     */
    public void clear(ParamValue param) {
        this.setValue(param, new ParamValueClass().getValue(param));
    }

    /**
     * @param param parameter enum
     * @return value as double
     * @throws RuntimeException if the constant was not defined
     */
    public double getDoubleValue(ParamValue param) {
        if (!this.isDefined(param)) throw new RuntimeException("Enum-Value is not defined: " + param);
        return this.paramValues.get(param).getRight().doubleValue();
    }

    /**
     * @param param parameter enum
     * @return value as float
     * @throws RuntimeException if the constant was not defined
     */
    public float getFloatValue(ParamValue param) {
        if (!this.isDefined(param)) throw new RuntimeException("Enum-Value is not defined: " + param);
        return this.paramValues.get(param).getRight().floatValue();
    }

    /**
     * @param param parameter enum
     * @return value as int, values after decimal point are skipped
     * @throws RuntimeException if the constant was not defined
     */
    public int getIntValue(ParamValue param) {
        if (!this.isDefined(param)) throw new RuntimeException("Enum-Value is not defined: " + param);
        return this.paramValues.get(param).getRight().intValue();
    }

    /**
     * @param param parameter enum
     * @return value as long, values after decimal point are skipped
     * @throws RuntimeException if the constant was not defined
     */
    public long getLongValue(ParamValue param) {
        if (!this.isDefined(param)) throw new RuntimeException("Enum-Value is not defined: " + param);
        return this.paramValues.get(param).getRight().longValue();
    }

    /**
     * TODO ugly hack maybe improve
     *
     * @param param number parameter enum
     * @return the default number value of the enum
     */
    public Number getPreset(ParamValue param) {
        try {
            return new ParamValueClass().getValue(param);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * @param param parameter enum
     * @return parameter type
     */
    public ParamType getType(ParamValue param) {
        return this.paramValues.get(param).getLeft();
    }

    /**
     * @param param parameter enum
     * @return value as the general Number type
     * @throws RuntimeException if the constant was not defined
     */
    public Number getValue(ParamValue param) throws RuntimeException {
        if (!this.isDefined(param)) throw new RuntimeException("Enum-Value is not defined: " + param);
        return this.paramValues.get(param).getRight();
    }

    /**
     * @param param value/Number parameter enum
     * @return true if defined, false otherwise
     */
    public boolean isDefined(ParamValue param) {
        return this.paramValues.get(param).getRight() != null;
    }

    /**
     * Sets the value
     *
     * @param param value/Number parameter enum
     * @param value as a double type
     */
    public void setValue(ParamValue param, double value) {
        this.paramValues.get(param).setRight(value);
    }

    /**
     * Sets the value
     *
     * @param param value/Number parameter enum
     * @param value as a float
     */
    public void setValue(ParamValue param, float value) {
        this.paramValues.get(param).setRight(value);
    }

    /**
     * Sets the value
     *
     * @param param value/Number parameter enum
     * @param value as a Number type
     */
    public void setValue(ParamValue param, Number value) {
        this.paramValues.get(param).setRight(value);
    }

    /**
     * Sets the value
     *
     * @param param value/Number parameter enum
     * @param value as an int
     */
    public void setValue(ParamValue param, int value) {
        this.paramValues.get(param).setRight(value);
    }

    /**
     * Sets the value
     *
     * @param param value/Number parameter enum
     * @param value as a long
     */
    public void setValue(ParamValue param, long value) {
        this.paramValues.get(param).setRight(value);
    }
}
