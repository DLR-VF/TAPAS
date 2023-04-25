/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.parameter;

/**
 * This class provides all string enums which determine the name of
 * parameters available in the application
 *
 * @author radk_an
 */

public enum ParamString {
    /**
     * complete class name to use for data import; relevant for choice
     * between file or db source
     */
    CLASS_DATA_SCOURCE_ORIGIN,

    /**
     * This is the current currency used in the application
     */
    CURRENCY,

    /**
     * The project name for this run.
     */
    PROJECT_NAME,

    /**
     * Property key for the database to connect to
     */
    DB_DBNAME,
    /**
     * Property key for the driver which is used to connect to the database
     */
    DB_DRIVER,
    /**
     * Property key for the database server
     */
    DB_HOST,
    /**
     * Region of the simulation, e.g. berlin, hamburg
     */
    DB_REGION,
    /**
     * Name of the database table of the blocks
     */
    DB_NAME_BLOCK_NEXT_PT_STOP,
    /**
     * Name of the database table of the blocks
     */
    DB_NAME_BLOCK_SCORES,

    /**
     * Name of the database table of the model parameters for the logit
     * model
     */
    DB_NAME_MODEL_PARAMETERS,

    /**
     * Name for the table holding the PT intra cell info in the scenario
     * case.
     */
    DB_NAME_TAZ_INTRA_PT_INFOS,
    /**
     * Name for the table holding the MIT intra cell info in the scenario
     * case.
     */
    DB_NAME_TAZ_INTRA_MIT_INFOS,
    /**
     * Name for the table holding the PT intra cell info in the base case.
     */
    DB_NAME_TAZ_INTRA_PT_INFOS_BASE,
    /**
     * Name for the table holding the MIT intra cell info in the base case.
     */
    DB_NAME_TAZ_INTRA_MIT_INFOS_BASE,

    /**
     * Name of the fees and tolls to use from the fees and tolls table
     */
    DB_NAME_FEES_TOLLS,

    /**
     * Name of the access time for public transport table
     */
    DB_NAME_MATRIX_ACCESS_PT,

    /**
     * Name of the access time for public transport table for base scenario
     */
    DB_NAME_MATRIX_ACCESS_PT_BASE,

    /**
     * Name of the access time for bike table
     */
    DB_NAME_MATRIX_ACCESS_BIKE,

    /**
     * Name of the access time for bike table for base scenario
     */
    DB_NAME_MATRIX_ACCESS_BIKE_BASE,

    /**
     * Name of the access time for car table
     */
    DB_NAME_MATRIX_ACCESS_MIT,

    /**
     * Name of the access time for car table for base scenario
     */
    DB_NAME_MATRIX_ACCESS_MIT_BASE,

    /**
     * Name of the access time for walk table
     */
    DB_NAME_MATRIX_ACCESS_WALK,

    /**
     * Name of the access time for walk table for base scenario
     */
    DB_NAME_MATRIX_ACCESS_WALK_BASE,

    /**
     * Name of the miv distance table
     */
    DB_NAME_MATRIX_DISTANCES_STREET,

    /**
     * Name of the miv distance table
     */
    DB_NAME_MATRIX_DISTANCES_PT,

    /**
     * Name of the miv distance table
     */
    DB_NAME_MATRIX_DISTANCES_WALK,

    /**
     * Name of the miv distance table
     */
    DB_NAME_MATRIX_DISTANCES_BIKE,

    /**
     * Name of the egress time for public transport table
     */
    DB_NAME_MATRIX_INTERCHANGE_PT,

    /**
     * Name of the table that holds the TAZ ID at which pt is embarked for pt+bike trips
     */
    DB_NAME_PTBIKE_ACCESS_TAZ,

    /*
     * Name of the table that holds the TAZ ID at which pt is left for pt+bike trips
     */
    DB_NAME_PTBIKE_EGRESS_TAZ,

    /**
     * Name of the table that holds the TAZ ID at which pt is embarked for pt+car trips
     */
    DB_NAME_PTCAR_ACCESS_TAZ,

    /*
     * Name of the table that holds the number of interchanges between TAZ when using pt+bike
     */
    DB_NAME_PTBIKE_INTERCHANGES,

    /**
     * Name of the table that holds the number of interchanges between TAZ when using pt+car
     */
    DB_NAME_PTCAR_INTERCHANGES,


    /**
     * Name of the egress time for public transport table
     */
    DB_NAME_MATRIX_INTERCHANGE_PT_BASE,

    /**
     * Name of the egress time for public transport table
     */
    DB_NAME_MATRIX_EGRESS_PT,

    /**
     * Name of the egress time for public transport table for base scenario
     */
    DB_NAME_MATRIX_EGRESS_PT_BASE,

    /**
     * Name of the egress time for bike table
     */
    DB_NAME_MATRIX_EGRESS_BIKE,

    /**
     * Name of the egress time for bike table for base scenario
     */
    DB_NAME_MATRIX_EGRESS_BIKE_BASE,

    /**
     * Name of the egress time for car table
     */
    DB_NAME_MATRIX_EGRESS_MIT,

    /**
     * Name of the egress time for car table for base scenario
     */
    DB_NAME_MATRIX_EGRESS_MIT_BASE,

    /**
     * Name of the egress time for walk table
     */
    DB_NAME_MATRIX_EGRESS_WALK,

    /**
     * Name of the egress time for walk table for base scenario
     */
    DB_NAME_MATRIX_EGRESS_WALK_BASE,
    /**
     * Name of the travel time table for WALK
     */
    DB_NAME_MATRIX_TT_WALK,

    /**
     * Name of the travel time table for WALK -base scenatrio
     */
    DB_NAME_MATRIX_TT_WALK_BASE,

    /**
     * Name of the travel time table for BIKE
     */
    DB_NAME_MATRIX_TT_BIKE,

    /**
     * Name of the travel time table for BIKE -base scenatrio
     */
    DB_NAME_MATRIX_TT_BIKE_BASE,

    /**
     * Name of the travel time individual transport table
     */
    DB_NAME_MATRIX_TT_MIT,

    /**
     * Name of the travel time individual transport table for base scenario
     */
    DB_NAME_MATRIX_TT_MIT_BASE,

    /**
     * Name of the travel time public transport table
     */
    DB_NAME_MATRIX_TT_PT,

    /**
     * Name of the travel time public transport table for base scenario
     */
    DB_NAME_MATRIX_TT_PT_BASE,

    /**
     * Name of the mode choice tree to use from the mode choice tree table
     */
    DB_NAME_MCT,

    /**
     * Name of the database table of the scheme class distribution
     */
    DB_NAME_SCHEME_CLASS_DISTRIBUTION,

    /**
     * Name of the database table of the taz infos (average speeds for pt
     * and mit and belline factor for mit)
     */
    DB_NAME_TAZ_SCORES,

    /**
     * Name of the values of time to use from the values of time table
     */
    DB_NAME_VOT,

    /**
     * Property key for the user's password for the db
     */
    DB_PASSWORD,

    /**
     * This constant stores the name of the core schema of the database
     */
    DB_SCHEMA_CORE,

    /**
     * This constant stores the name of the temporary schema of the database
     */
    DB_SCHEMA_TEMP,

    /**
     * Name for the additional traffic table used in SUMO.
     */
    DB_TABLE_ADDITIONAL_TRAFFIC,
    /**
     * Name of the database table of the blocks
     */
    DB_TABLE_BLOCK,

    /**
     * Name of the database table of the blocks
     */
    DB_TABLE_BLOCK_NEXT_PT_STOP,

    /**
     * Name of the database table of the blocks
     */
    DB_TABLE_BLOCK_SCORES,
    /**
     * Name of the database table of the cfn4 values indexed by the
     * settlement type of the current taz and the current episode code
     */
    DB_TABLE_CFN4,
    /**
     * Name of the database table of the cfn4 indexed values. These are the
     * special cfn4 values, e.g. the default or the values for non-working
     * and short stays
     */
    DB_TABLE_CFN4_IND,
    /**
     * Name of the database table of the cfnx values
     */
    DB_TABLE_CFNX,

    /**
     * Table names which holds the codes for constant activity
     */
    DB_TABLE_CONSTANT_ACTIVITY,
    /**
     * Table names which holds the codes for constant activity to location
     * mapping
     */
    DB_TABLE_CONSTANT_ACTIVITY_2_LOCATION,
    /**
     * Table names which holds the codes for constant age class
     */
    DB_TABLE_CONSTANT_AGE,
    /**
     * Table names which holds the codes for constant car class
     */
    DB_TABLE_CONSTANT_CARS,
    /**
     * Table names which holds the codes for constant distance class
     */
    DB_TABLE_CONSTANT_DISTANCE,
    /**
     * Table names which holds the codes for constant driving license
     */
    DB_TABLE_CONSTANT_DRIVING_LICENSE_INFORMATION,
    /**
     * Table names which holds the codes for constant income class
     */
    DB_TABLE_CONSTANT_INCOME,
    /**
     * Table names which holds the codes for constant household type
     */
    DB_TABLE_CONSTANT_HOUSEHOLD,
    /**
     * Table names which holds the codes for constant location type
     */
    DB_TABLE_CONSTANT_LOCATION,
    /**
     * Table names which holds the codes for constant mode
     */

    DB_TABLE_CONSTANT_MODE,
    /**
     * Table names which holds the codes for constant persongroup
     */

    DB_TABLE_CONSTANT_PERSON,
    /**
     * Table names which holds the codes for constant settlement (region
     * code)
     */

    DB_TABLE_CONSTANT_SETTLEMENT,
    /**
     * Table names which holds the codes for constant sex
     */
    DB_TABLE_CONSTANT_SEX,

    /**
     * Table name of the global episodes table
     */
    DB_TABLE_EPISODE,

    /**
     * Database table name of the fees and tolls for the traffic analysis
     * zones
     */
    DB_TABLE_TAZ_FEES_TOLLS,

    /**
     * Database table name of the household table
     */
    DB_TABLE_HOUSEHOLD,

    /**
     * Database table name of the household table
     */
    DB_TABLE_HOUSEHOLD_TMP,

    /**
     * Database table name of the location table
     */
    DB_TABLE_LOCATION,

    /**
     * Database table name of the location table
     */
    DB_TABLE_LOCATION_TMP,

    /**
     * Database table name of the matrices (distances, constant travel
     * times)
     */
    DB_TABLE_MATRICES,

    /**
     * Database table name of the matrixmaps (variable travel times)
     */
    DB_TABLE_MATRIXMAPS,

    /**
     * Database table name of all mode choice trees
     */
    DB_TABLE_MCT,

    /**
     * Database table name of the person table
     */
    DB_TABLE_PERSON,

    /**
     * Database table name of the cars table
     */
    DB_TABLE_CARS,
    /**
     * Database table name of the scheme table
     */
    DB_TABLE_SCHEME,

    /**
     * Database table name of the scheme class table
     */
    DB_TABLE_SCHEME_CLASS,

    /**
     * Database table name of the scheme class distribution table
     */
    DB_TABLE_SCHEME_CLASS_DISTRIBUTION,

    /**
     * Database table name of the traffic analysis zone table
     */
    DB_TABLE_TAZ,

    /**
     * Name of the database table of the taz infos (average speeds for pt
     * and mit and belline factor for mit)
     */
    DB_TABLE_TAZ_INTRA_PT_INFOS,

    /**
     * Name of the database table of the taz infos (average speeds for pt
     * and mit and belline factor for mit)
     */
    DB_TABLE_TAZ_INTRA_MIT_INFOS,

    /**
     * Name of the database table of the taz infos (average speeds for pt
     * and mit and belline factor for mit)
     */
    DB_TABLE_TAZ_SCORES,

    /**
     * Database table name for the trips
     */
    DB_TABLE_TRIPS,

    /**
     * Database table containing taz representatives for travel time estimation
     */
    DB_TABLE_REPRESENTATIVES,

    /**
     * Database names for the additional traffic, semicolon separated array of ids
     */
    DB_NAME_ADDITIONAL_TRAFFIC,
    /**
     * Database table name of the values of time
     */
    DB_TABLE_VOT,

    /**
     * Property key for the type of database, e.g. postgresql
     */
    DB_TYPE,

    /**
     * Property key for the user which is registered in database
     */
    DB_USER,

    /**
     * name of the file containing the visum version
     */
    FILE_VISUM_VERSION,

    /**
     * name of the file containing the database properties
     */
    FILE_DATABASE_PROPERTIES,

    /**
     * name of the file containing the logging properties
     */
    FILE_LOGGING_PROPERTIES,

    /**
     * name of the file containing the parameter properties
     */
    FILE_PARAMETER_PROPERTIES, FILE_PARENT_PROPERTIES,

    /**
     * name of the working directory
     */
    FILE_WORKING_DIRECTORY,


    /**
     * Mask for logging: All values below this level are not logged
     */
    HIERARCHY_LOG_LEVEL_MASK,

    /**
     * String qualifying the Sorting algorithm for the household members
     */
    HOUSEHOLD_MEMBERSORTING,

    /**
     * Level for class logging
     */
    LOG_CLASS,
    /**
     * Level for all logging
     */

    LOG_LEVEL_ALL,
    /**
     * Level for debugging
     */
    LOG_LEVEL_DEBUG,
    /**
     * Level for errors
     */
    LOG_LEVEL_ERROR,

    /**
     * Level for fatal errors
     */
    LOG_LEVEL_FATAL,

    /**
     * Level for fine logging
     */
    LOG_LEVEL_FINE,

    /**
     * Level for finer logging
     */
    LOG_LEVEL_FINER,
    /**
     * Level for finest logging
     */
    LOG_LEVEL_FINEST,
    /**
     * Level for info
     */
    LOG_LEVEL_INFO,
    /**
     * Level for switching off
     */
    LOG_LEVEL_OFF,
    /**
     * Level for severe errors
     */
    LOG_LEVEL_SEVERE,
    /**
     * Level for warnings
     */
    LOG_LEVEL_WARN,
    /**
     * absolute general path to input files
     */
    PATH_ABS_DB,
    /**
     * absolute general path to input files
     */
    PATH_ABS_INPUT,
    /**
     * absolute general path to output files
     */
    PATH_ABS_OUTPUT,
    /**
     * absolute path to the directory of the properties files. All of them
     * are in the same directory.
     */
    PATH_ABS_PROPERTIES,
    /**
     * The unique id string for this run
     */
    RUN_IDENTIFIER,
    /**
     * The mask for the severe log level
     */
    SEVERENCE_LOG_LEVEL_MASK,
    /**
     * class name of the utility function
     */
    UTILITY_FUNCTION_CLASS,
    /**
     * class name of the utility function
     */
    UTILITY_FUNCTION_KEY,
    /**
     * The key-name for the activity to location mapping e.g. MID2008_Y2030, default value is 'default'
     * used in conjunction with the DB_TABLE_CONSTANT_ACTIVITY_2_LOCATION table
     */
    DB_ACTIVITY_2_LOCATION_KEY,
    /**
     * The key-name for the additional traffic e.g. 'MID2008_Y2030', default value is 'default'
     * used in conjunction with the DB_TABLE_ADDITIONAL_TRAFFIC table
     */
    DB_ADDITIONAL_TRAFFIC_KEY,
    /**
     * The key-name for the population e.g. 'MID2008_Y2030', default value is 'default'
     * used in conjunction with the DB_TABLE_HOUSEHOLD and DB_TABLE_PERSON
     */
    DB_HOUSEHOLD_AND_PERSON_KEY,
    /**
     * The key-name for the locations e.g. 'MID2008_Y2030', default value is 'default'
     * used in conjunction with the DB_TABLE_LOCATION table
     */
    DB_LOCATION_KEY,
    /**
     * The key-name for the episodes table e.g. 'MID2008_Y2030', default value is 'default'
     * used in conjunction with the DB_TABLE_EPISODE table
     */
    DB_EPISODE_KEY,
    /**
     * The key-name for the schemes table e.g. 'MID17', default value is 'default'
     * used in conjunction with the DB_TABLE_SCHEME table
     */
    DB_SCHEME_KEY,
    /**
     * The key-name for the scheme class distribution table table e.g. 'MID17', default value is 'default'
     * used in conjunction with the DB_TABLE_SCHEME_CLASS_DISTRIBUTION table and DB_NAME_SCHEME_CLASS_DISTRIBUTION name
     */
    DB_SCHEME_CLASS_DISTRIBUTION_KEY,
    /**
     * The key-name for the scheme classes table e.g. 'MID17', default value is 'default'
     * used in conjunction with the DB_TABLE_SCHEME_CLASS table
     */
    DB_SCHEME_CLASS_KEY,
    /**
     * The key-name for the car fleet e.g. MID2008_Y2030
     */
    DB_CAR_FLEET_KEY,
    /**
     * The key-name for the person group division e.g. TAPAS or VISEVA, default is TAPAS
     */
    DB_PERSON_GROUP_KEY,
    /**
     * The key-name for the activity based search radius table e.g. default
     */
    DB_ACTIVITY_CNF_KEY,
    /**
     * The key-name for the activity based search radius table e.g. default
     */
    DB_ACTIVITY_POTENTIAL_KEY,
    /**
     * The key-name for the region based capacity limiting table e.g. default
     */
    DB_REGION_CNF_KEY,
    /**
     * The table name for simulations.
     */
    DB_TABLE_SIMULATIONS,
    /**
     * The table name for the simulation parameters.
     */
    DB_TABLE_SIMULATION_PARAMETERS,
    /**
     * The table name for the servers.
     */
    DB_TABLE_SERVERS,
    /**
     * the table of the actual jobs processed by the servers.
     */
    DB_TABLE_PROCESSES,
    /**
     * the table of the results of a simulation
     */
    DB_TABLE_CALIBRATION_RESULTS,
    /**
     * The ip ot host name of the coumputer running visum
     */
    VISUM_HOST,

    /**
     * The modes SUMO should use for simulation
     */
    SUMO_MODES,
    /**
     * The folder for the net template to use
     */
    SUMO_TEMPLATE_FOLDER,
    /**
     * Folder to put all the net data. Can be reused or separated to save space or not.
     */
    SUMO_DESTINATION_FOLDER,

    /**
     * Table name to put the OD-output. Should be convertet to <region>_sumo_od_<key>_<iteration>.
     */
    DB_TABLE_SUMO_OD_OUTPUT,
    /**
     * Table name to put the trip-output. Should be <region>_sumo_trip_<key>_<iteration>..
     */
    DB_TABLE_SUMO_TRIP_OUTPUT,
    /**
     * Table name to get the status for the sumo-processes.
     */
    DB_TABLE_SUMO_STATUS,
    /**
     * The class name for the location selection model
     */
    LOCATION_SELECT_MODEL_CLASS,
    /**
     * The class name for the location choice set generator
     */
    LOCATION_CHOICE_SET_CLASS,

    DB_TABLE_EKT, DB_NAME_EKT
}

