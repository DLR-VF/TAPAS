/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.util.parameters;

/**
 * This class provides all value (Number, int, double etc.) enums which determine the name of
 * parameters available in the application
 *
 * @author radk_an
 */

public enum ParamValue {

    /**
     * average distance to the next pt stop in meters
     */
    AVERAGE_DISTANCE_PT_STOP,
    // Soll das 0d da hin? -> Ja, da es dann als double und nicht als
    // integer interpretiert wird

    /**
     * level for automatic vehicle to activate valet parking
     */
    AUTOMATIC_VEHICLE_LEVEL,

    /**
     * level for automatic vehicle to activate valet parking
     */
    AUTOMATIC_VALET_PARKING,

    /**
     * global probability for vehicles for being automated (when selected, GLOBAL_AUTOMATION_LEVEL is set as automation level)
     */
    GLOBAL_AUTOMATION_PROBABILITY,

    /**
     * the automation level to set
     */
    GLOBAL_AUTOMATION_LEVEL,

    /**
     * constant access time for automatic parking vehicles
     */
    AUTOMATIC_PARKING_ACCESS,

    /**
     * constant egress time for automatic parking vehicles
     */
    AUTOMATIC_PARKING_EGRESS,

    /**
     * ramp up time for automatic vehicle time perception
     */
    AUTOMATIC_VEHICLE_RAMP_UP_TIME,

    /**
     * time perception modificator for automatic vehicles after ramp up for far travels.
     * Reference: cars VOT
     */
    AUTOMATIC_VEHICLE_TIME_MOD_FAR,

    /**
     * time perception modificator for automatic vehicles after ramp up for near travels.
     * Reference: cars VOT
     */

    AUTOMATIC_VEHICLE_TIME_MOD_NEAR,
    /**
     * Threshold between far and near
     */
    AUTOMATIC_VEHICLE_TIME_MOD_THRESHOLD,

    /**
     * factor to transform the bike availability
     */
    AVAILABILITY_FACTOR_BIKE,
    /**
     * factor to transform the bike availability
     */
    AVAILABILITY_FACTOR_CARSHARING,
    /**
     * factor to transform beeline distances into distances on the street
     * net by bike
     */
    BEELINE_FACTOR_BIKE,

    /**
     * factor to transform beeline distances into distances on the street
     * net by foot
     */
    BEELINE_FACTOR_FOOT,

    /**
     * factor to transform beeline distances into distances on the street
     * net by car
     */
    BEELINE_FACTOR_MIT,

    /**
     * factor to transform beeline distances into distances on the street
     * net by train
     */
    BEELINE_FACTOR_PT,

    /**
     * costs of using the bike in the scenario in € / km
     */
    BIKE_COST_PER_KM,
    /* other modes */
    /**
     * costs of using the pt in the base scenario in € / km
     */
    BIKE_COST_PER_KM_BASE,

    /**
     * Theshold for the time of the previous stay in seconds, when the
     * special cfn-behaviour should be used
     */
    CFN_SPECIAL_THRESHOLD,

    /**
     * Time in seconds added to the access time MIV for carsharing, because of longer walking trips
     * Constant for now!
     */
    CARSHARING_ACCESS_ADDON,
    /**
     * The household sample size in the range of [0, 1]
     */
    DB_HH_SAMPLE_SIZE,

    /**
     * Property key for the port the database listens to connections
     */
    DB_PORT,

    /**
     * default activity type
     */
    DEFAULT_ACT_CODE_ZBE,

    /**
     * default score for block level times
     */

    DEFAULT_BLOCK_SCORE,

    /**
     * default value of time in € when no suitable entry can be found in the
     * file provided
     */
    DEFAULT_VOT,

    /**
     * default value for school bus access time in seconds
     */
    DEFAULT_SCHOOL_BUS_ACCESS,

    /**
     * default value for school bus egres time in seconds
     */
    DEFAULT_SCHOOL_BUS_EGRESS,

    /**
     * default value for school bus wait time in seconds
     */
    DEFAULT_SCHOOL_BUS_WAIT,

    /**
     * cost factor for PT travel time
     */
    PT_TT_FACTOR,

    /**
     * cost factor for PT access time
     */
    PT_ACCESS_FACTOR,

    /**
     * cost factor for PT egress time
     */
    PT_EGRESS_FACTOR,

    /**
     * minimum travel time for PT in seconds
     */
    PT_MINIMUM_TT,

    /**
     * delta factor for acceptable earlier start
     */
    DELTA_START_EARLIER,

    /**
     * delta factor for acceptable later start
     */
    DELTA_START_LATER,

    /**
     * financial budget check: calibration parameter for the EVA1-function;
     * E-parameter for the approximation of the curve in relation to the
     * resistance asymptote - Anschmiegen der Kurve an die
     * Widerstandsasymptote (unten)
     */
    FINANCE_BUDGET_E,

    /**
     * financial budget check: calibration parameter for the EVA1-function;
     * F-Parameter for the reluctance to leave the initial niveau
     */
    FINANCE_BUDGET_F,

    /**
     * financial budget check: calibration parameter for the EVA1-function;
     * turning point of the curve
     */
    FINANCE_BUDGET_WP,

    /**
     * Value for correcting the weight of location choice according to a gamma-function
     */
    GAMMA_LOCATION_WEIGHT,

    /**
     * actual iteration number
     */
    ITERATION,

    /**
     * modification of the location choice (reduction of the distance in
     * case of budget constraints in percent of the original cfn4 value);
     * This parameter affects ALL CFN4-values for THIS simulation to
     * simulate smaler or wider locattoin range
     */
    LOC_CHOICE_MOD_CFN4,

    LOC_CAPACITY_FACTOR,

    /**
     * the maximum system speed for selecting locations in reach. Unit: m/s
     */
    MAX_SYSTEM_SPEED,
    /**
     * maximum acceptable difference between the calculated travel time
     * expenditures and the scheduled travel times in the diary in percent
     * (not corresponding to the budget check!)
     */
    MAX_TIME_DIFFERENCE,

    /**
     * maximum number of iterations for the construction of day plans per
     * person
     */
    MAX_TRIES_PERSON,

    /**
     * maximum number of iterations for the construction of a day plan per
     * person per scheme
     */
    MAX_TRIES_SCHEME,


    /**
     * maximum number of iterations for finding locations: the system speed
     * for reaching locations is constantly increased from
     * MAX_SYSTEM_SPEED/MAX_TRIES_LOCATION_SELECTION top MAX_SYSTEM_SPEED
     */
    MAX_TRIES_LOCATION_SELECTION,

    /**
     * Value which determines, how often visum should be triggered. Must be
     * zero if MAX_SUMO_ITERATION>0
     */
    MAX_VISUM_ITERATION,
    /**
     * Value which determines, how often sumo should be triggered. Must be
     * zero if MAX_VISUM_ITERATION>0
     */
    MAX_SUMO_ITERATION,

    MAX_WALK_DIST,
    /**
     * minimum distance between locations
     */
    MIN_DIST,

    /**
     * factor to transform the bike availability
     */
    MIN_AGE_CARSHARING,

    /*
     * Parameter declaration for the MNL-Models, both the linear and the
     * non-linear version
     */

    /**
     * fuel related cost per kilometer for pc in € in the scenario; average
     * value taking into account both fuel price and consumption
     */
    MIT_GASOLINE_COST_PER_KM,

    /**
     * fuel related cost per kilometer for pc in € in the base scenario;
     * average value taking into account both fuel price and consumption
     */
    MIT_GASOLINE_COST_PER_KM_BASE,

    /**
     * fuel related cost per kilometer for pc in € in the scenario; average
     * value taking into account both fuel price and consumption
     * for autonomous gaosline vehicles
     */
    MIT_GASOLINE_AUTOMATED_COST_PER_KM,

    /**
     * fuel related cost per kilometer for pc in € in the scenario; average
     * value taking into account both fuel price and consumption
     */
    MIT_DIESEL_COST_PER_KM,

    /**
     * fuel related cost per kilometer for pc in € in the base scenario;
     * average value taking into account both fuel price and consumption
     */
    MIT_DIESEL_COST_PER_KM_BASE,

    /**
     * fuel related cost per kilometer for pc in € in the scenario; average
     * value taking into account both fuel price and consumption
     * for autonomous diesel vehicles
     */
    MIT_DIESEL_AUTOMATED_COST_PER_KM,

    /**
     * fuel related cost per kilometer for pc in € in the scenario; average
     * value taking into account both fuel price and consumption
     */
    MIT_PLUGIN_COST_PER_KM,

    /**
     * fuel related cost per kilometer for pc in € in the base scenario;
     * average value taking into account both fuel price and consumption
     */
    MIT_PLUGIN_COST_PER_KM_BASE,

    /**
     * fuel related cost per kilometer for pc in € in the base scenario;
     * average value taking into account both fuel price and consumption
     * for autonomous plugin hybrid vehicles
     */
    MIT_PLUGIN_AUTOMATED_COST_PER_KM,

    /**
     * fuel related cost per kilometer for pc in € in the scenario; average
     * value taking into account both fuel price and consumption
     */
    MIT_ELECTRO_COST_PER_KM,

    /**
     * fuel related cost per kilometer for pc in € in the base scenario;
     * average value taking into account both fuel price and consumption
     */
    MIT_ELECTRO_COST_PER_KM_BASE,

    /**
     * fuel related cost per kilometer for pc in € in the base scenario;
     * average value taking into account both fuel price and consumption
     * for autonomous electric vehicles
     */
    MIT_ELECTRO_AUTOMATED_COST_PER_KM,

    /**
     * fuel related cost per kilometer for pc in € in the scenario; average
     * value taking into account both fuel price and consumption
     */
    MIT_FUELCELL_COST_PER_KM,

    /**
     * fuel related cost per kilometer for pc in € in the base scenario;
     * average value taking into account both fuel price and consumption
     */
    MIT_FUELCELL_COST_PER_KM_BASE,

    /**
     * fuel related cost per kilometer for pc in € in the scenario; average
     * value taking into account both fuel price and consumption
     */
    MIT_GAS_COST_PER_KM,

    /**
     * fuel related cost per kilometer for pc in € in the base scenario;
     * average value taking into account both fuel price and consumption
     */
    MIT_GAS_COST_PER_KM_BASE,

    /**
     * Sother variable costs for pc usage per kilometer in € in the
     * scenario; average value
     */
    MIT_VARIABLE_COST_PER_KM,

    /**
     * other variable costs for pc usage per kilometer in € in the base
     * scenario; average value
     */
    MIT_VARIABLE_COST_PER_KM_BASE,


    /**
     * surcharge per kilometer in € for using the pc in the scenario caused
     * by the abolishment of the commuter tax refund; calculated as ontop of
     * the fuel prices
     */
    MIT_FUEL_COST_PER_KM_COMMUTE,

    /**
     * surcharge per kilometer in € for using the pc in the base scenario
     * caused by the abolishment of the commuter tax refund; calculated as
     * ontop of the fuel prices
     */
    MIT_FUEL_COST_PER_KM_COMMUTE_BASE,

    /**
     * income class threshold: Incomes equal or higher this parameter are
     * affected by the MIT_FUEL_COST_PER_KM_COMMUTE values
     */

    MIT_INCOME_CLASS_COMMUTE,


    /**
     * Range for conventional fuel (Diesel, Gas, Petrol)
     */
    MIT_RANGE_CONVENTIONAL,

    /**
     * Range for plugin fuel (Hybrid)
     */
    MIT_RANGE_PLUGIN,

    /**
     * Range for emobiles
     */
    MIT_RANGE_EMOBILE,

    /**
     * overall time budget check: calibration parameter for the
     * EVA1-function; E-parameter for the approximation of the curve in
     * relation to the resistance asymptote - Anschmiegen der Kurve an die
     * Widerstandsasymptote (unten)
     */
    OVERALL_TIME_E,

    /**
     * overall time budget check: calibration parameter for the
     * EVA1-function; F-Parameter for the reluctance to leave the initial
     * niveau
     */
    OVERALL_TIME_F,

    /**
     * parking fee for tvz with category 1 (default) in the scenario; fee
     * per hour in €
     */
    PARKING_FEE_CAT_1,
    /*
     * parking fees for different categories; implemented as cost per
     * duration of stay; areas with different fees within one simulation run
     * can be avaluated by specifing different prices for the categories;
     * category 1-3 both for base and scenario
     */
    /**
     * parking fee for tvz with category 1 (default) in the base scenario;
     * fee per hour in €
     */
    PARKING_FEE_CAT_1_BASE,
    /**
     * parking fee for tvz with category 2 in the base scenario; fee per
     * hour in €
     */
    PARKING_FEE_CAT_2,

    /**
     * parking fee for tvz with category 2 in the base scenario; fee per
     * hour in €
     */
    PARKING_FEE_CAT_2_BASE,
    /**
     * parking fee for tvz with category 3 in the base scenario; fee per
     * hour in €
     */
    PARKING_FEE_CAT_3,
    /**
     * parking fee for tvz with category 3 in the base scenario; fee per
     * hour in €
     */
    PARKING_FEE_CAT_3_BASE,

    /**
     * costs of beeing a passenger in the scenario in € / km
     */
    PASS_COST_PER_KM,
    /* other modes */
    /**
     * costs of beeing a passenger in the base scenario in € / km
     */
    PASS_COST_PER_KM_BASE,

    /**
     * probability of using a car from the household
     */
    PASS_PROBABILITY_HOUSEHOLD_CAR,

    /**
     * probability that a random car is restricted
     */
    PASS_PROBABILITY_RESTRICTED,

    /**
     * add on costs for pnr in € (pt-ticket)
     */
    PNR_COST_PER_TRIP,

    /**
     * add on costs for pnr in € parking-fee
     */
    PNR_COST_PER_HOUR,

    /**
     * costs of using the pt in the scenario in € / Kilometer
     */
    PT_COST_PER_KM,

    /**
     * costs of using the pt when carrying a bike in the scenario in € / Kilometer
     */
    PTBIKE_COST_PER_KM,

    /* other modes */
    /**
     * costs of using the pt in the base scenario in € / Kilometer
     */
    PT_COST_PER_KM_BASE,

    /**
     * costs of using the pt when carrying a bike in the base scenario in € / Kilometer
     */
    PTBIKE_COST_PER_KM_BASE,

    /**
     * Is a cost reduction factor for the public transport and other shared mobility modes like robotaxi, car sharing
     * and (autonomous) ride pooling
     * Double value should be between 0 and 1.
     * May be used together with the LOW_INCOME_THRESHOLD .
     * Note: The costs of the shared mobility modes should be MULTIPLIED by SHARED_MODE_COST_REDUCTION factor.
     * Default: 1 (i.e. no reduction at all)
     */
    SHARED_MODE_COST_REDUCTION,

    /**
     * Low income threshold
     * Depending on the utility function this is either a threshold for the total income of a household
     * or a threshold for the equivalence income (see
     * {@link de.dlr.ivf.tapas.person.TPS_Household}.getHouseholdEquivalenceIncome)
     * Households below the threshold are considered to have a low income and may benefit of scenario measures
     * like public transport cost reduction etc.
     * Default=0, i.e. there is no consideration of low income households
     */
    LOW_INCOME_THRESHOLD,

    /**
     * Random seed in the case the random number generator should be fixed
     */
    RANDOM_SEED_NUMBER,

    /**
     * number of years to rejuvenate a retiree when applicable
     */
    REJUVENATE_BY_NB_YEARS,

    /**
     * age when a retiree is rejuvenated
     */
    REJUVENATE_AGE,

    /**
     * Factor for an acceptable longer duration
     */
    RELATIVE_DURATION_LONGER,

    /**
     * Factor for an acceptable shorter duration
     */
    RELATIVE_DURATION_SHORTER,

    /**
     * costs of using a ride pooling service in the scenario in € / Kilometer
     */
    RIDE_POOLING_COST_PER_KM,

    /**
     * This parameter defines the duration of one time slot in the scheme in
     * seconds
     */
    SEC_TIME_SLOT,

    /**
     * Constant parameter for the ballancing of trip starts
     */
    SCALE_SHIFT,
    /**
     * Constant for the ballancing of trip durations
     */
    SCALE_STRETCH,

    /**
     * costs of using the taxi in the scenario in € / Kilometer
     */
    TAXI_COST_PER_KM,
    /**
     * costs of using the taxi in the base scenario in € / Kilometer
     */
    TAXI_COST_PER_KM_BASE,

    /**
     * time budget check: calibration parameter for the EVA1-function;
     * E-parameter for the approximation of the curve in relation to the
     * resistance asymptote - Anschmiegen der Kurve an die
     * Widerstandsasymptote (unten)
     */
    TIME_BUDGET_E,
    /**
     * time budget check: calibration parameter for the EVA1-function;
     * F-Parameter for the reluctance to leave the initial niveau
     */
    TIME_BUDGET_F,

    /*
     * End of the model and scenario definitions
     */

    /**
     * time budget check: calibration parameter for the EVA1-function;
     * turning point of the curve
     */
    TIME_BUDGET_WP,

    /**
     * toll fee for tvz with category 1 (default) in the scenario in €
     */
    TOLL_CAT_1,

    /*
     * toll fees for the different categories; implemented as codon toll;
     * per default only charged when entering the toll ring; extra flag for
     * charging when leaving available; areas with different fees within one
     * simulation run can be evaluated by specifying different prices for the
     * categories; category 1-3 both for base and scenario caution: as the
     * model used was estimated on prices in €; the prices here have to be
     * in € as well;
     */
    /**
     * toll fee for tvz with category 1 (default) in the base scenario in €
     */
    TOLL_CAT_1_BASE,

    /**
     * toll fee for tvz with category 2 in the scenario in €
     */
    TOLL_CAT_2,

    /**
     * toll fee for tvz with category 2 in the base scenario in €
     */
    TOLL_CAT_2_BASE,

    /**
     * toll fee for tvz with category 3 in the scenario in €
     */
    TOLL_CAT_3,

    /**
     * toll fee for tvz with category 3 in the base scenario in €
     */
    TOLL_CAT_3_BASE,

    /**
     * costs of using the the train in the scenario in € / Kilometer;
     * category train contains "other" as ferry, plane, train, ...
     */
    CAR_SHARING_COST_PER_KM,

    /**
     * costs of using the the train in the base scenario in € / Kilometer;
     * category train contains "other" as ferry, plane, train, ...
     */
    CAR_SHARING_COST_PER_KM_BASE,

    /**
     * average speed on bike in m/s
     */
    VELOCITY_BIKE,

    /**
     * Average speed for cars
     */
    VELOCITY_CAR,

    /**
     * average speed on foot in m/s
     */
    VELOCITY_FOOT,

    /**
     * Average speed for public transport
     */
    VELOCITY_TRAIN,
    /**
     * costs of walking in the scenario in € / Kilometer
     */
    WALK_COST_PER_KM,
    /* other modes */
    /**
     * costs of walking in the base scenario in € / Kilometer
     */
    WALK_COST_PER_KM_BASE,

    /**
     * This parameter controls the occupancy of the locations. The
     * attractivity of a location is calcuklated by a inverted exponential
     * function. If this parameter is 1 it is a standart function. The
     * higher the function decreases faster to zero. This leads to a better
     * uniform distribution of the locations' occupancy.
     */
    WEIGHT_OCCUPANCY,

    /**
     * weight of telework suitability when selecting a scheme (increasing in
     * case of suitablility)
     */
    WEIGHT_WORKING_AT_HOME,

    /**
     * weight of trip chains when selecting a scheme (increasing the weight
     * in the absence of trip chaines)
     */
    WEIGHT_WORKING_CHAINS,

    /**
     * The mode constant for using public transport in conjunction with a bike
     */
    PTBIKE_MODE_CONSTANT,

    /**
     * The mode constant for using public transport in conjunction with a passenger car
     */
    PTCAR_MODE_CONSTANT,

    /**
     * Factor for scaling PTBIKE combination
     */
    PTBIKE_MODE_PROB_FACTOR,

    /**
     * The mode constant for using public transport in conjunction with a passenger car
     */
    PTCAR_MODE_PROB_FACTOR,

    /**
     * The µm parameter for logsum to scale the logsum
     */
    LOGSUM_WEIGHT_MU,

    /**
     * The µ parameter for logsum to adjust the influence of the mode choice to the location choice
     */
    LOGSUM_INTERACT_MU,

    /**
     * The calibration factor education for logsum
     */
    LOGSUM_CALIB_EDUCATION,
    /**
     * The calibration factor STUDENT for logsum
     */
    LOGSUM_CALIB_STUDENT,
    /**
     * The calibration factor work for logsum
     */
    LOGSUM_CALIB_WORK,
    /**
     * The calibration factor shopping for logsum
     */
    LOGSUM_CALIB_SHOP,

    /**
     * The calibration factor private matters for logsum
     */
    LOGSUM_CALIB_ERRANT,

    /**
     * The calibration factor free time for logsum
     */

    LOGSUM_CALIB_FREETIME,
    /**
     * The calibration factor free time at/near home for logsum
     */
    LOGSUM_CALIB_FREETIME_HOME,

    /**
     * The calibration factor misc for logsum
     */
    LOGSUM_CALIB_MISC

}



