package de.dlr.ivf.tapas.util.parameters;

import org.apache.commons.lang3.tuple.MutablePair;

import java.util.EnumMap;

public class ParamStringClass {
    private EnumMap<ParamString, MutablePair<ParamType, String>> paramStrings;

    ParamStringClass() {
        this.paramStrings = new EnumMap<>(ParamString.class);

        this.paramStrings.put(ParamString.CLASS_DATA_SCOURCE_ORIGIN, new MutablePair<>(ParamType.RUN, null));
        this.paramStrings.put(ParamString.CURRENCY, new MutablePair<>(ParamType.DEFAULT, CURRENCY.EUR.name()));
        this.paramStrings.put(ParamString.PROJECT_NAME, new MutablePair<>(ParamType.DEFAULT, ""));
        this.paramStrings.put(ParamString.DB_DBNAME, new MutablePair<>(ParamType.DB, null));
        this.paramStrings.put(ParamString.DB_DRIVER, new MutablePair<>(ParamType.DB, null));
        this.paramStrings.put(ParamString.DB_HOST, new MutablePair<>(ParamType.DB, null));
        this.paramStrings.put(ParamString.DB_REGION, new MutablePair<>(ParamType.DB, null));
        this.paramStrings.put(ParamString.DB_NAME_BLOCK_NEXT_PT_STOP, new MutablePair<>(ParamType.DB, null));
        this.paramStrings.put(ParamString.DB_NAME_BLOCK_SCORES, new MutablePair<>(ParamType.DB, null));
        this.paramStrings.put(ParamString.DB_NAME_MODEL_PARAMETERS, new MutablePair<>(ParamType.DB, null));
        this.paramStrings.put(ParamString.DB_NAME_TAZ_INTRA_PT_INFOS, new MutablePair<>(ParamType.DB, null));
        this.paramStrings.put(ParamString.DB_NAME_TAZ_INTRA_MIT_INFOS, new MutablePair<>(ParamType.DB, null));
        this.paramStrings.put(ParamString.DB_NAME_TAZ_INTRA_PT_INFOS_BASE, new MutablePair<>(ParamType.DB, null));
        this.paramStrings.put(ParamString.DB_NAME_TAZ_INTRA_MIT_INFOS_BASE, new MutablePair<>(ParamType.DB, null));
        this.paramStrings.put(ParamString.DB_NAME_FEES_TOLLS, new MutablePair<>(ParamType.DB, null));
        this.paramStrings.put(ParamString.DB_NAME_MATRIX_ACCESS_PT, new MutablePair<>(ParamType.OPTIONAL, null));
        this.paramStrings.put(ParamString.DB_NAME_MATRIX_ACCESS_PT_BASE, new MutablePair<>(ParamType.OPTIONAL, null));
        this.paramStrings.put(ParamString.DB_NAME_MATRIX_ACCESS_BIKE, new MutablePair<>(ParamType.OPTIONAL, null));
        this.paramStrings.put(ParamString.DB_NAME_MATRIX_ACCESS_BIKE_BASE, new MutablePair<>(ParamType.OPTIONAL, null));
        this.paramStrings.put(ParamString.DB_NAME_MATRIX_ACCESS_MIT, new MutablePair<>(ParamType.OPTIONAL, null));
        this.paramStrings.put(ParamString.DB_NAME_MATRIX_ACCESS_MIT_BASE, new MutablePair<>(ParamType.OPTIONAL, null));
        this.paramStrings.put(ParamString.DB_NAME_MATRIX_ACCESS_WALK, new MutablePair<>(ParamType.OPTIONAL, null));
        this.paramStrings.put(ParamString.DB_NAME_MATRIX_ACCESS_WALK_BASE, new MutablePair<>(ParamType.OPTIONAL, null));
        this.paramStrings.put(ParamString.DB_NAME_MATRIX_DISTANCES_STREET, new MutablePair<>(ParamType.DB, null));
        this.paramStrings.put(ParamString.DB_NAME_MATRIX_DISTANCES_PT, new MutablePair<>(ParamType.OPTIONAL, null));
        this.paramStrings.put(ParamString.DB_NAME_MATRIX_DISTANCES_WALK, new MutablePair<>(ParamType.OPTIONAL, null));
        this.paramStrings.put(ParamString.DB_NAME_MATRIX_DISTANCES_BIKE, new MutablePair<>(ParamType.OPTIONAL, null));
        this.paramStrings.put(ParamString.DB_NAME_MATRIX_INTERCHANGE_PT, new MutablePair<>(ParamType.OPTIONAL, null));
        this.paramStrings.put(ParamString.DB_NAME_PTBIKE_ACCESS_TAZ, new MutablePair<>(ParamType.OPTIONAL, null));
        this.paramStrings.put(ParamString.DB_NAME_PTBIKE_EGRESS_TAZ, new MutablePair<>(ParamType.OPTIONAL, null));
        this.paramStrings.put(ParamString.DB_NAME_PTCAR_ACCESS_TAZ, new MutablePair<>(ParamType.OPTIONAL, null));
        this.paramStrings.put(ParamString.DB_NAME_PTBIKE_INTERCHANGES, new MutablePair<>(ParamType.OPTIONAL, null));
        this.paramStrings.put(ParamString.DB_NAME_PTCAR_INTERCHANGES, new MutablePair<>(ParamType.OPTIONAL, null));
        this.paramStrings.put(ParamString.DB_NAME_MATRIX_INTERCHANGE_PT_BASE,
                new MutablePair<>(ParamType.OPTIONAL, null));
        this.paramStrings.put(ParamString.DB_NAME_MATRIX_EGRESS_PT, new MutablePair<>(ParamType.OPTIONAL, null));
        this.paramStrings.put(ParamString.DB_NAME_MATRIX_EGRESS_PT_BASE, new MutablePair<>(ParamType.OPTIONAL, null));
        this.paramStrings.put(ParamString.DB_NAME_MATRIX_EGRESS_BIKE, new MutablePair<>(ParamType.OPTIONAL, null));
        this.paramStrings.put(ParamString.DB_NAME_MATRIX_EGRESS_BIKE_BASE, new MutablePair<>(ParamType.OPTIONAL, null));
        this.paramStrings.put(ParamString.DB_NAME_MATRIX_EGRESS_MIT, new MutablePair<>(ParamType.OPTIONAL, null));
        this.paramStrings.put(ParamString.DB_NAME_MATRIX_EGRESS_MIT_BASE, new MutablePair<>(ParamType.OPTIONAL, null));
        this.paramStrings.put(ParamString.DB_NAME_MATRIX_EGRESS_WALK, new MutablePair<>(ParamType.OPTIONAL, null));
        this.paramStrings.put(ParamString.DB_NAME_MATRIX_EGRESS_WALK_BASE, new MutablePair<>(ParamType.OPTIONAL, null));
        this.paramStrings.put(ParamString.DB_NAME_MATRIX_TT_WALK, new MutablePair<>(ParamType.DB, null));
        this.paramStrings.put(ParamString.DB_NAME_MATRIX_TT_WALK_BASE, new MutablePair<>(ParamType.DB, null));
        this.paramStrings.put(ParamString.DB_NAME_MATRIX_TT_BIKE, new MutablePair<>(ParamType.DB, null));
        this.paramStrings.put(ParamString.DB_NAME_MATRIX_TT_BIKE_BASE, new MutablePair<>(ParamType.DB, null));
        this.paramStrings.put(ParamString.DB_NAME_MATRIX_TT_MIT, new MutablePair<>(ParamType.DB, null));
        this.paramStrings.put(ParamString.DB_NAME_MATRIX_TT_MIT_BASE, new MutablePair<>(ParamType.DB, null));
        this.paramStrings.put(ParamString.DB_NAME_MATRIX_TT_PT, new MutablePair<>(ParamType.DB, null));
        this.paramStrings.put(ParamString.DB_NAME_MATRIX_TT_PT_BASE, new MutablePair<>(ParamType.DB, null));
        this.paramStrings.put(ParamString.DB_NAME_MCT, new MutablePair<>(ParamType.DB, null));
        this.paramStrings.put(ParamString.DB_NAME_SCHEME_CLASS_DISTRIBUTION, new MutablePair<>(ParamType.DB, null));
        this.paramStrings.put(ParamString.DB_NAME_TAZ_SCORES, new MutablePair<>(ParamType.DB, null));
        this.paramStrings.put(ParamString.DB_NAME_VOT, new MutablePair<>(ParamType.DB, null));
        this.paramStrings.put(ParamString.DB_PASSWORD, new MutablePair<>(ParamType.DB, null));
        this.paramStrings.put(ParamString.DB_SCHEMA_CORE, new MutablePair<>(ParamType.DB, null));
        this.paramStrings.put(ParamString.DB_SCHEMA_TEMP, new MutablePair<>(ParamType.DB, null));
        this.paramStrings.put(ParamString.DB_TABLE_ADDITIONAL_TRAFFIC, new MutablePair<>(ParamType.DB, ""));
        this.paramStrings.put(ParamString.DB_TABLE_BLOCK, new MutablePair<>(ParamType.DB, null));
        this.paramStrings.put(ParamString.DB_TABLE_BLOCK_NEXT_PT_STOP, new MutablePair<>(ParamType.DB, null));
        this.paramStrings.put(ParamString.DB_TABLE_BLOCK_SCORES, new MutablePair<>(ParamType.DB, null));
        this.paramStrings.put(ParamString.DB_TABLE_CFN4, new MutablePair<>(ParamType.DB, null));
        this.paramStrings.put(ParamString.DB_TABLE_CFN4_IND, new MutablePair<>(ParamType.DB, null));
        this.paramStrings.put(ParamString.DB_TABLE_CFNX, new MutablePair<>(ParamType.DB, null));
        this.paramStrings.put(ParamString.DB_TABLE_CONSTANT_ACTIVITY, new MutablePair<>(ParamType.DB, null));
        this.paramStrings.put(ParamString.DB_TABLE_CONSTANT_ACTIVITY_2_LOCATION, new MutablePair<>(ParamType.DB, null));
        this.paramStrings.put(ParamString.DB_TABLE_CONSTANT_AGE, new MutablePair<>(ParamType.DB, null));
        this.paramStrings.put(ParamString.DB_TABLE_CONSTANT_CARS, new MutablePair<>(ParamType.DB, null));
        this.paramStrings.put(ParamString.DB_TABLE_CONSTANT_DISTANCE, new MutablePair<>(ParamType.DB, null));
        this.paramStrings.put(ParamString.DB_TABLE_CONSTANT_DRIVING_LICENSE_INFORMATION,
                new MutablePair<>(ParamType.DB, null));
        this.paramStrings.put(ParamString.DB_TABLE_CONSTANT_INCOME, new MutablePair<>(ParamType.DB, null));
        this.paramStrings.put(ParamString.DB_TABLE_CONSTANT_HOUSEHOLD, new MutablePair<>(ParamType.DB, null));
        this.paramStrings.put(ParamString.DB_TABLE_CONSTANT_LOCATION, new MutablePair<>(ParamType.DB, null));
        this.paramStrings.put(ParamString.DB_TABLE_CONSTANT_MODE, new MutablePair<>(ParamType.DB, null));
        this.paramStrings.put(ParamString.DB_TABLE_CONSTANT_PERSON, new MutablePair<>(ParamType.DB, null));
        this.paramStrings.put(ParamString.DB_TABLE_CONSTANT_SETTLEMENT, new MutablePair<>(ParamType.DB, null));
        this.paramStrings.put(ParamString.DB_TABLE_CONSTANT_SEX, new MutablePair<>(ParamType.DB, null));
        this.paramStrings.put(ParamString.DB_TABLE_EPISODE, new MutablePair<>(ParamType.DB, null));
        this.paramStrings.put(ParamString.DB_TABLE_TAZ_FEES_TOLLS, new MutablePair<>(ParamType.DB, null));
        this.paramStrings.put(ParamString.DB_TABLE_HOUSEHOLD, new MutablePair<>(ParamType.DB, null));
        this.paramStrings.put(ParamString.DB_TABLE_HOUSEHOLD_TMP, new MutablePair<>(ParamType.TMP, null));
        this.paramStrings.put(ParamString.DB_TABLE_LOCATION, new MutablePair<>(ParamType.DB, null));
        this.paramStrings.put(ParamString.DB_TABLE_LOCATION_TMP, new MutablePair<>(ParamType.TMP, null));
        this.paramStrings.put(ParamString.DB_TABLE_MATRICES, new MutablePair<>(ParamType.DB, null));
        this.paramStrings.put(ParamString.DB_TABLE_MATRIXMAPS, new MutablePair<>(ParamType.DB, null));
        this.paramStrings.put(ParamString.DB_TABLE_MCT, new MutablePair<>(ParamType.DB, null));
        this.paramStrings.put(ParamString.DB_TABLE_PERSON, new MutablePair<>(ParamType.DB, null));
        this.paramStrings.put(ParamString.DB_TABLE_CARS, new MutablePair<>(ParamType.DB, null));
        this.paramStrings.put(ParamString.DB_TABLE_SCHEME, new MutablePair<>(ParamType.DB, null));
        this.paramStrings.put(ParamString.DB_TABLE_SCHEME_CLASS, new MutablePair<>(ParamType.DB, null));
        this.paramStrings.put(ParamString.DB_TABLE_SCHEME_CLASS_DISTRIBUTION, new MutablePair<>(ParamType.DB, null));
        this.paramStrings.put(ParamString.DB_TABLE_TAZ, new MutablePair<>(ParamType.DB, null));
        this.paramStrings.put(ParamString.DB_TABLE_TAZ_INTRA_PT_INFOS, new MutablePair<>(ParamType.DB, null));
        this.paramStrings.put(ParamString.DB_TABLE_TAZ_INTRA_MIT_INFOS, new MutablePair<>(ParamType.DB, null));
        this.paramStrings.put(ParamString.DB_TABLE_TAZ_SCORES, new MutablePair<>(ParamType.DB, null));
        this.paramStrings.put(ParamString.DB_TABLE_TRIPS, new MutablePair<>(ParamType.DB, null));
        this.paramStrings.put(ParamString.DB_TABLE_REPRESENTATIVES, new MutablePair<>(ParamType.DB, null));
        this.paramStrings.put(ParamString.DB_NAME_ADDITIONAL_TRAFFIC, new MutablePair<>(ParamType.DB, null));
        this.paramStrings.put(ParamString.DB_TABLE_VOT, new MutablePair<>(ParamType.DB, null));
        this.paramStrings.put(ParamString.DB_TYPE, new MutablePair<>(ParamType.DB, null));
        this.paramStrings.put(ParamString.DB_USER, new MutablePair<>(ParamType.DB, null));
        this.paramStrings.put(ParamString.FILE_VISUM_VERSION, new MutablePair<>(ParamType.OPTIONAL, null));
        this.paramStrings.put(ParamString.FILE_DATABASE_PROPERTIES, new MutablePair<>(ParamType.EXEC, null));
        this.paramStrings.put(ParamString.FILE_LOGGING_PROPERTIES, new MutablePair<>(ParamType.EXEC, null));
        this.paramStrings.put(ParamString.FILE_PARAMETER_PROPERTIES, new MutablePair<>(ParamType.EXEC, null));
        this.paramStrings.put(ParamString.FILE_PARENT_PROPERTIES, new MutablePair<>(ParamType.TMP, null));
        this.paramStrings.put(ParamString.FILE_WORKING_DIRECTORY, new MutablePair<>(ParamType.EXEC, null));
        this.paramStrings.put(ParamString.HIERARCHY_LOG_LEVEL_MASK, new MutablePair<>(ParamType.LOG, null));
        this.paramStrings.put(ParamString.HOUSEHOLD_MEMBERSORTING, new MutablePair<>(ParamType.OPTIONAL, null));
        this.paramStrings.put(ParamString.LOG_CLASS, new MutablePair<>(ParamType.LOG, null));
        this.paramStrings.put(ParamString.LOG_LEVEL_ALL, new MutablePair<>(ParamType.LOG, null));
        this.paramStrings.put(ParamString.LOG_LEVEL_DEBUG, new MutablePair<>(ParamType.LOG, null));
        this.paramStrings.put(ParamString.LOG_LEVEL_ERROR, new MutablePair<>(ParamType.LOG, null));
        this.paramStrings.put(ParamString.LOG_LEVEL_FATAL, new MutablePair<>(ParamType.LOG, null));
        this.paramStrings.put(ParamString.LOG_LEVEL_FINE, new MutablePair<>(ParamType.LOG, null));
        this.paramStrings.put(ParamString.LOG_LEVEL_FINER, new MutablePair<>(ParamType.LOG, null));
        this.paramStrings.put(ParamString.LOG_LEVEL_FINEST, new MutablePair<>(ParamType.LOG, null));
        this.paramStrings.put(ParamString.LOG_LEVEL_INFO, new MutablePair<>(ParamType.LOG, null));
        this.paramStrings.put(ParamString.LOG_LEVEL_OFF, new MutablePair<>(ParamType.LOG, null));
        this.paramStrings.put(ParamString.LOG_LEVEL_SEVERE, new MutablePair<>(ParamType.LOG, null));
        this.paramStrings.put(ParamString.LOG_LEVEL_WARN, new MutablePair<>(ParamType.LOG, null));
        this.paramStrings.put(ParamString.PATH_ABS_DB, new MutablePair<>(ParamType.DB, null));
        this.paramStrings.put(ParamString.PATH_ABS_INPUT, new MutablePair<>(ParamType.TMP, null));
        this.paramStrings.put(ParamString.PATH_ABS_OUTPUT, new MutablePair<>(ParamType.TMP, null));
        this.paramStrings.put(ParamString.PATH_ABS_PROPERTIES, new MutablePair<>(ParamType.EXEC, null));
        this.paramStrings.put(ParamString.RUN_IDENTIFIER, new MutablePair<>(ParamType.TMP, null));
        this.paramStrings.put(ParamString.SEVERENCE_LOG_LEVEL_MASK, new MutablePair<>(ParamType.LOG, null));
        this.paramStrings.put(ParamString.UTILITY_FUNCTION_CLASS, new MutablePair<>(ParamType.RUN, null));
        this.paramStrings.put(ParamString.UTILITY_FUNCTION_KEY, new MutablePair<>(ParamType.RUN, "default"));
        this.paramStrings.put(ParamString.DB_HOUSEHOLD_AND_PERSON_KEY, new MutablePair<>(ParamType.DB, null));
        this.paramStrings.put(ParamString.DB_CAR_FLEET_KEY, new MutablePair<>(ParamType.DB, null));
        this.paramStrings.put(ParamString.DB_ACTIVITY_CNF_KEY, new MutablePair<>(ParamType.DB, "default"));
        this.paramStrings.put(ParamString.DB_REGION_CNF_KEY, new MutablePair<>(ParamType.DB, "default"));
        this.paramStrings.put(ParamString.DB_TABLE_SIMULATIONS, new MutablePair<>(ParamType.DB, "simulations"));
        this.paramStrings.put(ParamString.DB_TABLE_SIMULATION_PARAMETERS,
                new MutablePair<>(ParamType.DB, "simulation_parameters"));
        this.paramStrings.put(ParamString.DB_TABLE_SERVERS, new MutablePair<>(ParamType.DB, "servers"));
        this.paramStrings.put(ParamString.DB_TABLE_PROCESSES, new MutablePair<>(ParamType.DB, "server_processes"));
        this.paramStrings.put(ParamString.DB_TABLE_CALIBRATION_RESULTS,
                new MutablePair<>(ParamType.DB, "calibration_results"));
        this.paramStrings.put(ParamString.VISUM_HOST, new MutablePair<>(ParamType.RUN, "129.247.221.161"));
        this.paramStrings.put(ParamString.SUMO_MODES, new MutablePair<>(ParamType.RUN, "2;4"));
        this.paramStrings.put(ParamString.SUMO_TEMPLATE_FOLDER, new MutablePair<>(ParamType.RUN, "BASE2010"));
        this.paramStrings.put(ParamString.SUMO_DESTINATION_FOLDER, new MutablePair<>(ParamType.RUN, "GENERIC"));
        this.paramStrings.put(ParamString.DB_TABLE_SUMO_OD_OUTPUT, new MutablePair<>(ParamType.TMP, null));
        this.paramStrings.put(ParamString.DB_TABLE_SUMO_TRIP_OUTPUT, new MutablePair<>(ParamType.TMP, null));
        this.paramStrings.put(ParamString.DB_TABLE_SUMO_STATUS, new MutablePair<>(ParamType.DB, null));
        this.paramStrings.put(ParamString.LOCATION_SELECT_MODEL_CLASS, new MutablePair<>(ParamType.RUN, null));
        this.paramStrings.put(ParamString.LOCATION_CHOICE_SET_CLASS, new MutablePair<>(ParamType.RUN, null));
        this.paramStrings.put(ParamString.DB_TABLE_EKT, new MutablePair<>(ParamType.DB, null));
        this.paramStrings.put(ParamString.DB_NAME_EKT, new MutablePair<>(ParamType.DB, null));
    }


    /**
     * @param param string parameter enum
     * @return constant string
     * @throws RuntimeException if the constant was not defined
     */
    public String getString(ParamString param) throws RuntimeException {
        if (!this.isDefined(param)) {
            throw new RuntimeException("Enum-String is not defined: " + param);
        }
        return this.paramStrings.get(param).getRight();
    }

    /**
     * Sets the string value
     *
     * @param param  string parameter enum
     * @param string string value to be set to the parameter
     */
    public void setString(ParamString param, String string) {
        this.paramStrings.get(param).setRight(string);
    }

    /**
     * Append a given string to this String
     *
     * @param param    string parameter enum
     * @param appendix the String to append to this one
     */
    public void add(ParamString param, String appendix) {
        if (isDefined(param)) {
            String currentString = this.getString(param);
            this.setString(param, currentString + appendix);
        }
    }

    /**
     * Adds a prefix and suffix to the existing String
     *
     * @param param  string parameter enum
     * @param prefix The prefix to add
     * @param suffix The suffix to add
     */
    public void add(ParamString param, String prefix, String suffix) {
        if (isDefined(param)) {
            String currentString = this.getString(param);
            this.setString(param, prefix + currentString + suffix);
        }
    }

    /**
     * Appends the value of a ParamString object to this one
     *
     * @param param    string parameter enum
     * @param appendix The ParamString to add
     */
    public void add(ParamString param, ParamString appendix) {
        if (this.isDefined(appendix)) {
            this.add(param, this.getString(appendix));
        }
    }

    /**
     * Adds a prefix and suffix to the existing String
     *
     * @param param  string parameter enum as a ParamString
     * @param prefix The prefix to add
     * @param suffix The suffix to add
     */
    public void add(ParamString param, ParamString prefix, ParamString suffix) {
        if (this.isDefined(suffix)) {
            this.add(param, prefix, this.getString(suffix));
        }
    }

    /**
     * Adds a prefix and suffix to the existing String
     *
     * @param param  string parameter enum as a ParamString
     * @param prefix The prefix to add as a ParamString
     * @param suffix The suffix to add as a string
     */
    public void add(ParamString param, ParamString prefix, String suffix) {
        if (this.isDefined(prefix)) {
            this.add(param, this.getString(prefix), suffix);
        }
    }

    /**
     * @param param string parameter enum
     * @return parameter type
     */
    public ParamType getType(ParamString param) {
        return this.paramStrings.get(param).getLeft();
    }

    /**
     * @param param string parameter enum
     * @return true if the string is defined (not null), false otherwise
     */
    public boolean isDefined(ParamString param) {
        return this.paramStrings.get(param).getRight() != null && this.paramStrings.get(param).getRight().length() > 0;
    }

    /**
     * clears the attached String value to the default value
     */
    public void clear(ParamString param) {
        this.setString(param, new ParamStringClass().getString(param));
    }

    /**
     * TODO ugly hack maybe improve
     *
     * @param param string parameter enum
     * @return the default string value of the enum
     */
    public String getPreset(ParamString param) {
        try {
            return new ParamStringClass().getString(param);
        } catch (RuntimeException e) {
            return null;
        }
    }
}
