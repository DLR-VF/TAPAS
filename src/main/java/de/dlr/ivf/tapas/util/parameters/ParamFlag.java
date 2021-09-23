/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.util.parameters;

/**
 * This class provides all flag (boolean) enums which determine the name of
 * parameters available in the application
 */

public enum ParamFlag {
    /**
     * Flag, whether a check of budget constraints should be done for the
     * diary plans
     */
    FLAG_CHECK_BUDGET_CONSTRAINTS,
    /**
     * Flag which indicated, that all temporary tables should be deleted
     * after a complete run. Must be false, if iterations are requested!
     */
    FLAG_DELETE_TEMPORARY_TABLES,
    /**
     * Flag which indicated, that all temporary visum files should be
     * deleted after an iteration.
     */
    FLAG_DELETE_TEMPORARY_VISUM_FILES,
    /**
     * Flag, determines whether the choice of the random numbers should be
     * fixed to a provided seed
     */
    FLAG_INFLUENCE_RANDOM_NUMBER,
    /**
     * Flag, whether the diagonal line of the travel time and distance
     * matrices contains values (inner tvz information)
     */
    FLAG_INTRA_INFOS_MATRIX,
    /**
     * Determines, whether location dependent costs such as parking fees
     * should be considered when calculating the location choice; in
     * contrary to TPS_SCHEME_RUN_SZENARIO ignoring the information for mode
     * choice purposes
     */
    FLAG_LOCATION_POCKET_COSTS,
    /**
     * Flag if all households should be prefetched. This takes some time during initialization
     * and a lot of memory, but its worth it for full samples!
     */
    FLAG_PREFETCH_ALL_HOUSEHOLDS,
    /**
     * Flag, whether an artificial rejuvenation of retirees should occur
     * (possibly leads to the selection of diaries in the group of the
     * younger elderly )
     */
    FLAG_REJUVENATE_RETIREE,
    /**
     * Flag, whether a toll zone should ban restricted cars completely
     */
    FLAG_RESTRICT_TOLL_ZONE,
    /**
     * Flag, whether the calculation of a scenario in comparison to a base
     * situation should be conducted; true leads to the calculation of a
     * mode and location choice where differences in costs in comparison to
     * the base case are taken into account
     */
    FLAG_RUN_SZENARIO,
    /**
     * Boolean; results in special weighting for diaries suitable for
     * telework
     */
    FLAG_SCHEMES_MANIPULATE_BY_WORK_AT_HOME,
    /**
     * Boolean; results in special weighting for diaries containing trip
     * chains on the way to work or back home
     */
    FLAG_SCHEMES_MANIPULATE_BY_WORKINGCHAINS,
    /**
     * Boolean, determines whether the probabilities for the choice of a
     * diary plan should be manipulated when selecting a scheme
     */
    FLAG_SCHEMES_MANIPULATE_SELECTION_PROBS,
    /**
     * FOR FUTURE USE: Boolean, determines whether the location choice
     * should be differentiated by type of person (children, persons without
     * a car, others)
     */
    FLAG_SELECT_LOCATIONS_DIFF_PERSON_GROUP,
    /**
     * Boolean, determines whether the location choice for shopping
     * should be differentiated by type of motive
     */
    FLAG_USE_SHOPPING_MOTIVES,
    /**
     * determines whether the choice set for shopping locations
     * should be filtered by time/distance constraints.
     * Default behaviour is "true"
     */
    FLAG_FILTER_SHOPPING_CHOICE_SET,
    /**
     * Defines if the location weights should be considered or not
     */
    FLAG_UPDATE_LOCATION_WEIGHTS,
    /**
     * Flag, whether beneath the tvz level another level (block) should be
     * considered
     */
    FLAG_USE_BLOCK_LEVEL,
    /**
     * Determines, whether the input file containing the persons contains
     * the attribute driving license and whether this should be used
     */
    FLAG_USE_DRIVING_LICENCE,
    /**
     * Flag, whether toll should be charged when leaving the toll area
     */
    FLAG_USE_EXIT_MAUT,
    /**
     * Flag, whether tolls and fees should be charged to the passengers as well
     */
    FLAG_CHARGE_PASSENGERS_WITH_EVERYTHING,
    /**
     * Determines, whether the location choice for work, school and
     * university locations should occur only once ion the base setting,
     * thus ignoring cost changes for this case
     */
    FLAG_USE_FIXED_LOCS_ON_BASE,
    /**
     * Flag, whether the existence of a school bus should be assumed in case
     * there is no regular public transport connection available
     */
    FLAG_USE_SCHOOLBUS,
    /**
     * Flag, whether train should be used as CarSharing-Mode
     */
    FLAG_USE_CARSHARING,
    /**
     * Flag, whether car sharing may be used by all
     */
    FLAG_USE_ROBOTAXI,
    /**
     * Flag, whether train should be used as CarSharing-Mode
     */
    FLAG_USE_INTERMODAL,
    /**
     * Flag, whether taxi should be enabled
     */
    FLAG_USE_TAXI,
    /**
     * Flag, if the PT costs are fix or distance dependent
     */
    FLAG_FIX_PT_COSTS,
    /**
     * Flag, whether taxi should be enabled
     */
    FLAG_USE_LOCATION_GROUPS,
    /**
     * Flag, whether the "group" column is used for indicating the person groups (true) or attributes like sex, age, cars, children, status (false)
     * default false
     */
    FLAG_USE_GROUP_COLUMN_FOR_PERSON_GROUPS,

    /**
     * Flag whether automated ride pooling services are used, default false
     */
    FLAG_USE_AUTOMATED_RIDE_POOLING
}

