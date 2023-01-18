/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.util.parameters;

import com.csvreader.CsvReader;
import de.dlr.ivf.tapas.log.LogHierarchy;
import de.dlr.ivf.tapas.log.TPS_Logger;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface.HierarchyLogLevel;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface.SeverenceLogLevel;
import de.dlr.ivf.tapas.util.Matrix;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;


/**
 * This class provides all constants which are available in the application
 */
@LogHierarchy(hierarchyLogLevel = HierarchyLogLevel.CLIENT)
public class TPS_ParameterClass {


    /**
     * Name of the binary folder
     */
    public String BIN_DIR;
    /**
     * Name of the Input folder
     */
    public String INPUT_DIR;
    /**
     * Name of the output folder
     */
    public String OUTPUT_DIR;
    /**
     * Name of the binary folder
     */
    public String SIM_DIR;
    /**
     * Name of the log folder
     */
    public String LOG_DIR = "Log/";

    public ParamFlagClass paramFlagClass;
    public ParamValueClass paramValueClass;
    public ParamMatrixClass paramMatrixClass;
    public ParamMatrixMapClass paramMatrixMapClass;
    public ParamStringClass paramStringClass;
    /**
     * Stack to store the parents of the parameter files. Needed to descent in
     * the correct order.
     */
    private final Stack<File> parameterFiles;

    /**
     * Constructor of TPS_ParameterClass
     * calls the inner classes  ParamFlagClass, ParamValueClass,
     * ParamMatrixClass,
     * ParamMatrixMapClass, ParamStringClass
     * which are container classes for the corresponding
     * parameter enums TPS_Param*
     */
    public TPS_ParameterClass() {
        this.BIN_DIR = "Program/";
        this.INPUT_DIR = "Inputfiles/";
        this.OUTPUT_DIR = "Outputfiles/";
        this.SIM_DIR = "Simulations/";
        this.LOG_DIR = "Log/";
        this.parameterFiles = new Stack<>();
        this.paramFlagClass = new ParamFlagClass();
        this.paramValueClass = new ParamValueClass();
        this.paramMatrixClass = new ParamMatrixClass();
        this.paramMatrixMapClass = new ParamMatrixMapClass();
        this.paramStringClass = new ParamStringClass();
    }

    /**
     * Method to check uninitialised parameters. Uninitialised parameters are
     * reported via the logging function.
     */
    public void checkParameters() {
        if (!TPS_Logger.isLogging(SeverenceLogLevel.WARN)) {
            return;
        }
        for (ParamFlag pf : ParamFlag.values()) {
            if (!this.isDefined(pf) && !this.getType(pf).equals(ParamType.OPTIONAL)) {
                TPS_Logger.log(SeverenceLogLevel.WARN, pf + " not defined in the properties file");
            }
        }
        for (ParamValue pv : ParamValue.values()) {
            if (!this.isDefined(pv) && !this.getType(pv).equals(ParamType.OPTIONAL)) {
                TPS_Logger.log(SeverenceLogLevel.WARN, pv + " not defined in the properties file");
            }
        }
        for (ParamString ps : ParamString.values()) {
            if (!this.isDefined(ps) && !this.getType(ps).equals(ParamType.OPTIONAL)) {
                TPS_Logger.log(SeverenceLogLevel.WARN, ps + " not defined in the properties file");
            }
        }
        //TODO: we cannot check the matrices this way,because they need to be loaded somewhere first!
//        for (ParamMatrix pm : ParamMatrix.values()) {
//            if (!this.isDefined(pm) && !this.getType(pm).equals(ParamType.OPTIONAL)) {
//                TPS_Logger.log(SeverenceLogLevel.WARN, pm + " not defined in the properties file");
//            }
//        }
//        for (ParamMatrixMap pmm : ParamMatrixMap.values()) {
//            if (!this.isDefined(pmm) && !this.getType(pmm).equals(ParamType.OPTIONAL)) {
//                TPS_Logger.log(SeverenceLogLevel.WARN, pmm + " not defined in the properties file");
//            }
//        }
    }

    /**
     * Checks if the path is in the correct format and corrects it if necessary.
     *
     * @param param the ParamString to check.
     */
    private void checkPath(ParamString param) {
        if (this.isDefined(param)) {
            String tmp = replaceSeparator(this.getString(param));
            tmp = tmp.startsWith("/") ? tmp.substring(1) : tmp;
            tmp = tmp.endsWith("/") ? tmp : tmp + "/";
            this.setString(param, tmp);
        }
    }

    /**
     * Checks if the given ParamString has the correct db-schema format, which
     * means, it should end with a dot.
     *
     * @param param The parameter to check
     */
    private void checkSchema(ParamString param) {
        if (this.isDefined(param)) {
            String tmp = this.getString(param);
            tmp = tmp.endsWith(".") ? tmp : tmp + ".";
            this.setString(param, tmp);
        }
    }

    /**
     * Clears all attached parameters
     */
    public void clear() {
        for (ParamMatrix matrix : ParamMatrix.values()) {
            this.clear(matrix);
        }
        for (ParamMatrixMap matrixmap : ParamMatrixMap.values()) {
            this.clear(matrixmap);
        }
        for (ParamString string : ParamString.values()) {
            this.clear(string);
        }
        for (ParamFlag flag : ParamFlag.values()) {
            this.clear(flag);
        }
        for (ParamValue value : ParamValue.values()) {
            this.clear(value);
        }
    }

    /**
     * Clears the value of a parameter
     *
     * @param <E> parameter enum
     * @return
     * @throws RuntimeException if is not of type "ParamFlag,
     *                          ParamString, ParamValue,
     *                          ParamMatrix or ParamMatrixMap
     */
    public <E extends Enum<?>> void clear(E param) {
        if (param instanceof ParamFlag) this.paramFlagClass.clear((ParamFlag) param);
        else if (param instanceof ParamString) this.paramStringClass.clear((ParamString) param);
        else if (param instanceof ParamValue) this.paramValueClass.clear((ParamValue) param);
        else if (param instanceof ParamMatrix) this.paramMatrixClass.clear((ParamMatrix) param);
        else if (param instanceof ParamMatrixMap) this.paramMatrixMapClass.clear((ParamMatrixMap) param);
        else {
            throw new RuntimeException(
                    "Parameter is not of type ParamFlag, ParamString," + " ParamValue, ParamMatrix or " +
                            "ParamMatrixMap");
        }
    }

    /**
     * This method processes exactly one parameter and its given value. It
     * returns true if the parameter is successfully consumed
     *
     * @param currentPath the path for other files, leave null if the
     *                    parameters are
     *                    read from the database
     * @param key         the parameter name for the value
     * @param value       the parameter value
     * @param flagSet     set with all instances of {@link ParamFlag}, which
     *                    are not defined yet
     * @param stringSet   set with all instances of {@link ParamString},
     *                    which are not defined yet
     * @param valueSet    set with all instances of {@link ParamValue},
     *                    which
     *                    are not defined yet
     * @return true, if successful
     * @throws FileNotFoundException This exception is thrown if the fiel is
     *                               not found
     * @throws IOException           This exception is thrown if there occurs
     *                               a read error
     */
    private boolean consumeParameter(File currentPath, String key, String value, EnumSet<ParamFlag> flagSet, EnumSet<ParamString> stringSet, EnumSet<ParamValue> valueSet) {
        boolean consumed = false;
        if (!consumed) {
            try {
                ParamFlag pf = ParamFlag.valueOf(key);
                if (flagSet.contains(pf)) {
                    if (value.equalsIgnoreCase("true")) {
                        this.setFlag(pf, true);
                        flagSet.remove(pf);
                    }
                    if (value.equalsIgnoreCase("false")) {
                        this.setFlag(pf, false);
                        flagSet.remove(pf);
                    } else {
                        TPS_Logger.log(SeverenceLogLevel.ERROR,
                                "Illegal " + "Parameter value: " + key + " -> " + value + " not in {true, false}");
                    }
                } else {
                    if (TPS_Logger.isLogging(SeverenceLogLevel.WARN)) {
                        TPS_Logger.log(SeverenceLogLevel.WARN,
                                "Parameter " + "already defined: " + key + " -> " + this.paramFlagClass.isTrue(pf));
                    }
                    consumed = true;
                }
                consumed = true;
            } catch (IllegalArgumentException e) {
                /*
                 * ParamFlag pf = ParamFlag.valueOf(key.toString()); this line
                 * throws the exception, i.e. that there exists no enum constant
                 * with this name
                 */
            }
        }
        if (!consumed) {
            try {
                ParamString ps = ParamString.valueOf(key);
                if (stringSet.contains(ps) || ParamString.FILE_PARENT_PROPERTIES.equals(ps)) {
                    this.setString(ps, value);
                    stringSet.remove(ps);
                    consumed = true;

                    switch (ps) {
                        case FILE_DATABASE_PROPERTIES:
                        case FILE_LOGGING_PROPERTIES:
                        case FILE_PARAMETER_PROPERTIES:
                        case FILE_PARENT_PROPERTIES:
                            if (currentPath != null) {
                                String propertiesFileName = this.getString(ps);
                                File thisPath = new File(currentPath.getAbsolutePath());
                                while (propertiesFileName.startsWith("./")) {
                                    thisPath = thisPath.getParentFile();
                                    propertiesFileName = propertiesFileName.substring(2);
                                }
                                this.parameterFiles.push(new File(thisPath, propertiesFileName));
                            }
                            break;
                        default:
                            // everything ok
                    }
                } else {
                    if (TPS_Logger.isLogging(SeverenceLogLevel.WARN)) {
                        TPS_Logger.log(SeverenceLogLevel.WARN,
                                "Parameter " + "already defined: " + key + " -> " + this.getString(ps));
                    }
                    consumed = true;
                }
            } catch (IllegalArgumentException e) {
                /*
                 * ParamString ps = ParamString.valueOf(key.toString()); this
                 * line throws the exception, i.e. that there exists no enum
                 * constant with this name
                 */
            }
        }
        if (!consumed) {
            try {
                ParamValue pv = ParamValue.valueOf(key);
                if (valueSet.contains(pv)) {
                    this.paramValueClass.setValue(pv, Double.parseDouble(value));
                    valueSet.remove(pv);
                    consumed = true;
                } else {
                    if (TPS_Logger.isLogging(SeverenceLogLevel.WARN)) {
                        TPS_Logger.log(SeverenceLogLevel.WARN, "Parameter " + "already defined: " + key + " -> " +
                                this.paramValueClass.getDoubleValue(pv));
                    }
                    consumed = true;
                }
            } catch (NumberFormatException e) {
                TPS_Logger.log(SeverenceLogLevel.ERROR, "Illegal parameter " + "value: " + key, e);
                consumed = true;
            } catch (IllegalArgumentException e) {
                /*
                 * ParamValue pv = ParamValue.valueOf(key.toString()); this line
                 * throws the exception, i.e. that there exists no enum constant
                 * with this name
                 */
            }
        }
        if (!consumed) {
            if (TPS_Logger.isLogging(SeverenceLogLevel.DEBUG)) {
                TPS_Logger.log(SeverenceLogLevel.DEBUG, "Unknown parameter defined in file: " + key);
            }
        }
        return consumed;
    }

    /**
     * Deletes all given parameters in the table "simulation_parameters" for
     * which the simulation-identifier is sim_key.
     */
    public void deleteInDB(Connection con, String sim_key, Map<String, String> params) throws SQLException {
        PreparedStatement prepareStatement = null;
        try {
            prepareStatement = con.prepareStatement(
                    "DELETE FROM " + "simulation_parameters WHERE sim_key=? AND param_key=?");
            prepareStatement.setString(1, sim_key);
            for (String key : params.keySet()) {
                prepareStatement.setString(2, key);
                prepareStatement.addBatch();
            }
        } finally {
            if (prepareStatement != null) prepareStatement.executeBatch();
            prepareStatement.close();
            if (!con.getAutoCommit()) con.commit();
        }
    }

    /**
     * Method to generate some temporary parameters for this run. It generates
     * tablenames and the Filesystem directories for this run.
     */
    public void generateTemporaryParameters() {
        checkSchema(ParamString.DB_SCHEMA_CORE);
        checkSchema(ParamString.DB_SCHEMA_TEMP);

        if (this.isDefined(ParamString.RUN_IDENTIFIER)) {
            this.paramStringClass.add(ParamString.DB_TABLE_TRIPS, "_" + this.getString(ParamString.RUN_IDENTIFIER));
            if (this.isDefined(ParamString.DB_SCHEMA_TEMP)) {
                this.setString(ParamString.DB_TABLE_LOCATION_TMP,
                        this.getString(ParamString.DB_SCHEMA_TEMP) + "locations_" +
                                this.getString(ParamString.RUN_IDENTIFIER));
                this.setString(ParamString.DB_TABLE_HOUSEHOLD_TMP,
                        this.getString(ParamString.DB_SCHEMA_TEMP) + "households_" +
                                this.getString(ParamString.RUN_IDENTIFIER));
            }
        }

        for (ParamString param : ParamString.values()) {
            if (this.isDefined(param) && param.name().startsWith("DB_TABLE_")) {
                switch (param) {
                    case DB_TABLE_LOCATION_TMP:
                    case DB_TABLE_HOUSEHOLD_TMP:
                    case DB_TABLE_TRIPS:
                    case DB_TABLE_SIMULATIONS:
                    case DB_TABLE_SIMULATION_PARAMETERS:
                    case DB_TABLE_SERVERS:
                    case DB_TABLE_PROCESSES:
                    case DB_TABLE_SUMO_STATUS:
                        break;
                    default:
                        this.paramStringClass.add(param, ParamString.DB_SCHEMA_CORE, "");
                }
            }
        }

        Set<ParamString> set = new HashSet<>();
        set.add(ParamString.PATH_ABS_INPUT);
        set.add(ParamString.PATH_ABS_OUTPUT);
        set.add(ParamString.PATH_ABS_PROPERTIES);
        for (ParamString param : set) {
            if (this.isDefined(param)) checkPath(param);
        }
        set.clear();

    }

    public double getDoubleValue(ParamValue param) {
        return this.paramValueClass.getDoubleValue(param);
    }

    public int getIntValue(ParamValue param) {
        return this.paramValueClass.getIntValue(param);
    }

    public long getLongValue(ParamValue param) {
        return this.paramValueClass.getLongValue(param);
    }

    public Matrix getMatrix(ParamMatrix param) {
        return this.paramMatrixClass.getMatrix(param);
    }

    /**
     * Method to return the set simulation type for this run
     *
     * @return The SimulationType
     */
    public SimulationType getSimulationType() {
        return this.paramFlagClass.isTrue(ParamFlag.FLAG_RUN_SZENARIO) ? SimulationType.SCENARIO : SimulationType.BASE;
    }

    public String getString(ParamString param) {
        return this.paramStringClass.getString(param);
    }

    /*
     * helper getter functions for parameter enums
     * it would be too clunky and less readable if the sub-containers (paramStringClass, paramValueClass etc.)
     * are called all the time
     */
    public ParamType getType(ParamString param) {
        return this.paramStringClass.getType(param);
    }

    public ParamType getType(ParamValue param) {
        return this.paramValueClass.getType(param);
    }

    public ParamType getType(ParamFlag param) {
        return this.paramFlagClass.getType(param);
    }

    public ParamType getType(ParamMatrix param) {
        return this.paramMatrixClass.getType(param);
    }

    public ParamType getType(ParamMatrixMap param) {
        return this.paramMatrixMapClass.getType(param);
    }

    /**
     * here follow helper function which are basically calls into the
     * Param{Flag|Matrix|MatrixMap|Value|String}Class
     */

    public boolean isDefined(ParamFlag param) {
        return this.paramFlagClass.isDefined(param);
    }

    public boolean isDefined(ParamString param) {
        return this.paramStringClass.isDefined(param);
    }

    public boolean isDefined(ParamValue param) {
        return this.paramValueClass.isDefined(param);
    }

    public boolean isDefined(ParamMatrix param) {
        return this.paramMatrixClass.isDefined(param);
    }

    public boolean isDefined(ParamMatrixMap param) {
        return this.paramMatrixMapClass.isDefined(param);
    }

    public boolean isFalse(ParamFlag param) {
        return this.paramFlagClass.isFalse(param);
    }

    public boolean isTrue(ParamFlag param) {
        return this.paramFlagClass.isTrue(param);
    }

    public boolean getFlag(ParamFlag param) {
        return this.paramFlagClass.getFlag(param);
    }

    /**
     * This method opens the file and stores all key value pairs in a property
     * instance. If the file is in csv format the key is in the first column and
     * the value in the second one.<br>
     * In the second step the value all values are set to enum constants of type
     * ParamString, ParamFlag and ParamValue. If the constant is found and the
     * parameter has a wrong format an error is logged (e.g. you try to define
     * an instance of ParamValue with "true"). If no constant was found to set
     * the value a warning is logged.
     *
     * @param currentPath    current path of the properties file
     * @param propertiesFile file name of the current properties file; file can be in java properties or in standard
     *                       csv format
     * @param flagSet        set with all instances of ParamFlag, which are not defined yet
     * @param stringSet      set with all instances of ParamString, which are not defined yet
     * @param valueSet       set with all instances of ParamValue, which are not defined
     *                       yet
     * @throws FileNotFoundException if the file is not found
     * @throws IOException           if there occurs a read error
     */
    private void load(File currentPath, File propertiesFile, EnumSet<ParamFlag> flagSet, EnumSet<ParamString> stringSet, EnumSet<ParamValue> valueSet) throws FileNotFoundException, IOException {
        boolean consumed;
        String key;
        String value;
        int counter = 0;
        CsvReader reader = new CsvReader(new FileReader(propertiesFile));
        reader.readHeaders();
        while (reader.readRecord()) {
            key = reader.get(0);
            value = reader.get(1);
            consumed = consumeParameter(currentPath, key, value, flagSet, stringSet, valueSet);
            if (consumed) {
                counter++;
            }
        }
        reader.close();
        if (TPS_Logger.isLogging(SeverenceLogLevel.INFO)) {
            TPS_Logger.log(SeverenceLogLevel.INFO,
                    "Load file: " + propertiesFile.getPath() + " with " + counter + " key-value pairs");
        }
    }

    /**
     * This method reads all properties for the Application and stores them in
     * the constants of type ParamString , ParamFlag and ParamValue. If one
     * constant is not defined in the properties files a warning is logged. If
     * an exception occurs during reading the application stops and logs a fatal
     * message.
     *
     * @param runPropertiesFile file name of the properties file with the run
     *                          parameters
     * @throws IOException if there occurs an error while reading from a file
     */
    public void loadRuntimeParameters(File runPropertiesFile) throws IOException {
        EnumSet<ParamFlag> flagSet = EnumSet.allOf(ParamFlag.class);
        EnumSet<ParamString> stringSet = EnumSet.allOf(ParamString.class);
        EnumSet<ParamValue> valueSet = EnumSet.allOf(ParamValue.class);

        this.setString(ParamString.PATH_ABS_PROPERTIES, runPropertiesFile.getParent());
        stringSet.remove(ParamString.PATH_ABS_PROPERTIES);

        File absPath = runPropertiesFile;
        while (!SIM_DIR.startsWith(absPath.getName())) {
            absPath = absPath.getParentFile();
        }
        absPath = absPath.getParentFile();
        File absInput = new File(absPath, INPUT_DIR);
        this.setString(ParamString.PATH_ABS_INPUT, absInput.getPath());
        this.setString(ParamString.PATH_ABS_OUTPUT, this.getString(ParamString.PATH_ABS_PROPERTIES));
        stringSet.remove(ParamString.PATH_ABS_INPUT);
        stringSet.remove(ParamString.PATH_ABS_OUTPUT);

        this.parameterFiles.push(runPropertiesFile);

        File actFile = null;
        while (!this.parameterFiles.empty()) {
            actFile = this.parameterFiles.pop();
            load(actFile.getParentFile(), actFile, flagSet, stringSet, valueSet);
        }

        generateTemporaryParameters();

        if (this.isDefined(ParamString.LOG_CLASS)) TPS_Logger.setLoggingClass(this.getString(ParamString.LOG_CLASS),
                this);
    }

    /**
     * Method to load a single parameter file. Parent files and other files are
     * ignored.
     *
     * @param propertiesFile The one and only file to load
     * @throws FileNotFoundException
     * @throws IOException
     */
    public void loadSingleParameterFile(File propertiesFile) throws FileNotFoundException, IOException {
        EnumSet<ParamFlag> flagSet = EnumSet.allOf(ParamFlag.class);
        EnumSet<ParamString> stringSet = EnumSet.allOf(ParamString.class);
        EnumSet<ParamValue> valueSet = EnumSet.allOf(ParamValue.class);

        this.parameterFiles.push(propertiesFile);

        File actFile = null;
        while (!this.parameterFiles.empty()) {
            actFile = this.parameterFiles.pop();
            load(actFile.getParentFile(), actFile, flagSet, stringSet, valueSet);
        }

    }

    /**
     * Prints all parameters
     */
    public void print() {
        for (ParamFlag pf : EnumSet.allOf(ParamFlag.class)) {
            if (this.isDefined(pf)) System.out.println(this.toString(pf));
        }
        for (ParamValue pv : EnumSet.allOf(ParamValue.class)) {
            if (this.isDefined(pv)) System.out.println(this.toString(pv));
        }
        for (ParamString ps : EnumSet.allOf(ParamString.class)) {
            if (this.isDefined(ps)) System.out.println(this.toString(ps));
        }
        for (ParamMatrix pm : EnumSet.allOf(ParamMatrix.class)) {
            if (this.isDefined(pm)) System.out.println(this.toString(pm));
        }
        for (ParamMatrixMap pmm : EnumSet.allOf(ParamMatrixMap.class)) {
            if (this.isDefined(pmm)) System.out.println(this.toString(pmm));
        }
    }

    /**
     * This method reads all properties for the Application and stores them in
     * the constants of type ParamString , ParamFlag and ParamValue. If one
     * constant is not defined in the properties files a warning is logged. If
     * an exception occurs during reading the application stops and logs a fatal
     * message.
     *
     * @param rs the SQL-result set to look for parameters
     * @throws SQLException This exception is thrown if there occurs an error
     *                      while
     *                      reading from a file
     */
    public void readRuntimeParametersFromDB(ResultSet rs) throws SQLException {
        if (rs.next()) {

            EnumSet<ParamFlag> flagSet = EnumSet.allOf(ParamFlag.class);
            EnumSet<ParamString> stringSet = EnumSet.allOf(ParamString.class);
            EnumSet<ParamValue> valueSet = EnumSet.allOf(ParamValue.class);

            // not needed!
            stringSet.remove(ParamString.PATH_ABS_PROPERTIES);
            stringSet.remove(ParamString.PATH_ABS_INPUT);
            stringSet.remove(ParamString.PATH_ABS_OUTPUT);

            String key = "";
            String value = "";
            String sim_key = "sim key not found";
            int counter = 0;
            // the first rs.next() was called to determine, if any results are
            // present
            sim_key = rs.getString(1);
            do {
                key = rs.getString(2);
                value = rs.getString(3);
                if (updateParameter(key, value, flagSet, stringSet, valueSet)) {
                    counter++;
                }
            } while (rs.next());

            if (TPS_Logger.isLogging(SeverenceLogLevel.INFO)) {
                TPS_Logger.log(SeverenceLogLevel.INFO,
                        "Load db parameters " + "for " + sim_key + " with " + counter + " key-value pairs");
            }

            generateTemporaryParameters();

            this.setString(ParamString.FILE_DATABASE_PROPERTIES, "loadedFromDB");
            this.setString(ParamString.FILE_LOGGING_PROPERTIES, "loadedFromDB");
            this.setString(ParamString.FILE_PARAMETER_PROPERTIES, "loadedFromDB");
            if (this.isDefined(ParamString.LOG_CLASS)) {
                TPS_Logger.setLoggingClass(this.getString(ParamString.LOG_CLASS), this);
            }
        }
    }

    /**
     * This method replaces double slashes and double backslashes with single
     * one in the given string.
     *
     * @param string The string to chsnge
     * @return The result without double slashes
     */
    private String replaceSeparator(String string) {
        if (string != null) {
            string = string.replace('\\', '/');
            string = string.replace("//", "/");
        }
        return string;
    }

    /**
     * Method to revert the temporary parameters for this run.
     */
    public void revertTemporaryParameters() {

        if (this.getString(ParamString.DB_TABLE_TRIPS).endsWith("_" + this.getString(ParamString.RUN_IDENTIFIER)))
            this.setString(ParamString.DB_TABLE_TRIPS, this.getString(ParamString.DB_TABLE_TRIPS)
                                                           .replace("_" + this.getString(ParamString.RUN_IDENTIFIER),
                                                                   ""));

        this.clear(ParamString.DB_TABLE_LOCATION_TMP);
        this.clear(ParamString.DB_TABLE_HOUSEHOLD_TMP);

        for (ParamString param : ParamString.values()) {
            if (this.isDefined(param) && param.name().startsWith("DB_TABLE_")) {
                switch (param) {
                    case DB_TABLE_LOCATION_TMP:
                    case DB_TABLE_HOUSEHOLD_TMP:
                    case DB_TABLE_TRIPS:
                    case DB_TABLE_SIMULATIONS:
                    case DB_TABLE_SIMULATION_PARAMETERS:
                    case DB_TABLE_SERVERS:
                    case DB_TABLE_PROCESSES:
                        break;
                    default:
                        if (this.getString(param).startsWith(this.getString(ParamString.DB_SCHEMA_CORE))) {
                            this.setString(param,
                                    this.getString(param).replace(this.getString(ParamString.DB_SCHEMA_CORE), ""));
                        }
                }
            }
        }
    }

    public void setFlag(ParamFlag param, boolean flag) {
        this.paramFlagClass.setFlag(param, flag);
    }

    public void setMatrix(ParamMatrix param, Matrix matrix) {
        this.paramMatrixClass.setMatrix(param, matrix);
    }

    public void setMatrix(ParamMatrix param, Matrix matrix, SimulationType simType) {
        this.paramMatrixClass.setMatrix(param, matrix, simType);
    }

    public void setString(ParamString param, String str) {
        this.paramStringClass.setString(param, str);
    }

    /**
     * This method sets the value for the given key string with the given value.
     * The value is casted to the correct format, if possible
     *
     * @param key   The name of the parameter
     * @param value The value of the parameter
     * @return
     */
    public boolean setValue(String key, String value) {
        boolean consumed = false;
        if (!consumed) {
            try {
                ParamFlag pf = ParamFlag.valueOf(key);
                if (value.equalsIgnoreCase("true")) {
                    this.setFlag(pf, true);
                } else if (value.equalsIgnoreCase("false")) {
                    this.setFlag(pf, false);
                } else {
                    TPS_Logger.log(SeverenceLogLevel.SEVERE,
                            "Illegal Parameter value: " + key + " -> " + value + " not in {true, false}");
                }
                consumed = true;
            } catch (IllegalArgumentException e) {
                /*
                 * ParamFlag pf = ParamFlag.valueOf(key.toString()); this line
                 * throws the exception, i.e. that there exists no enum constant
                 * with this name
                 */
            }
        }
        if (!consumed) {
            try {
                ParamString ps = ParamString.valueOf(key);
                this.setString(ps, value);
                consumed = true;
            } catch (IllegalArgumentException e) {
                //todo
                /*
                 * ParamString ps = ParamString.valueOf(key.toString()); this
                 * line throws the exception, i.e. that there exists no enum
                 * constant with this name
                 */
            }
        }
        if (!consumed) {
            try {
                ParamValue pv = ParamValue.valueOf(key);
                this.paramValueClass.setValue(pv, Double.parseDouble(value));
                consumed = true;
            } catch (NumberFormatException e) {
                TPS_Logger.log(SeverenceLogLevel.SEVERE, "Illegal parameter value: " + key, e);
                consumed = true;
            } catch (IllegalArgumentException e) {
                /*
                 * ParamValue pv = ParamValue.valueOf(key.toString()); this line
                 * throws the exception, i.e. that there exists no enum constant
                 * with this name
                 */
            }
        }
        if (!consumed) {
            TPS_Logger.log(SeverenceLogLevel.WARN, "Unknown parameter defined: " + key);
        }
        return consumed;
    }


    /*
      helper setter functions for parameter enums
      it would be too clunky and less readable if the sub-containers (paramStringClass, paramValueClass etc.)
      are called all the time
     */

    public void setValue(ParamValue param, int val) {
        this.paramValueClass.setValue(param, val);
    }

    public void setValue(ParamValue param, double val) {
        this.paramValueClass.setValue(param, val);
    }

    public void setValue(ParamValue param, float val) {
        this.paramValueClass.setValue(param, val);
    }

    public void setValue(ParamValue param, long val) {
        this.paramValueClass.setValue(param, val);
    }

    public void setValue(ParamValue param, Number val) {
        this.paramValueClass.setValue(param, val);
    }

    /**
     * Internal write method for the Enum subclasses ParamMatrix, ParamString,
     * ParamFlag and ParamValue.
     *
     * @param enum0
     * @return printed enum
     */
    private <E extends Enum<?>> String toString(E enum0) {
        StringBuilder sb = new StringBuilder();
        sb.append(enum0.getClass().getSimpleName() + "." + enum0.name() + "[");
        for (Field field : enum0.getClass().getDeclaredFields()) {
            if (field.getName().contains("$")) {
                continue;
            }
            try {
                field.setAccessible(true);
                Object obj = field.get(enum0);
                String fieldValue = null;
                if (obj instanceof Enum<?>) {
                    if (obj instanceof ParamType) {
                        ParamType type = (ParamType) obj;
                        fieldValue = null == type ? "null" : type.name();
                    } else {
                        continue;
                    }
                } else {
                    fieldValue = obj == null ? "null" : obj.toString();
                }
                sb.append(field.getName() + "=" + fieldValue + ", ");
            } catch (SecurityException e) {
                sb.append(field.getName() + "=<<SecurityException>>, ");
            } catch (IllegalArgumentException e) {
                sb.append(field.getName() + "=<<IllegalArgumentException>>, ");
            } catch (IllegalAccessException e) {
                sb.append(field.getName() + "=<<IllegalAccessException>>, ");
            }
        }
        sb.setLength(sb.length() - 2);
        sb.append("]");
        return sb.toString();
    }

    /**
     * Updates all given parameters in the table "simulation_parameters" for
     * which the simulation-identifier is sim_key.
     */
    public void updateInDB(Connection con, String sim_key, Map<String, String> params) throws SQLException {
        this.deleteInDB(con, sim_key, params);
        this.writeToDB(con, sim_key, params);
    }

    /**
     * This method processes exactly one parameter and its given value. It
     * returns true if the parameter is successfully consumed
     *
     * @param key       the parameter name for the value
     * @param value     the parameter value
     * @param flagSet   set with all instances of {@link ParamFlag}, which
     *                  are not defined yet
     * @param stringSet set with all instances of {@link ParamString}, which
     *                  are not defined yet
     * @param valueSet  set with all instances of {@link ParamValue}, which
     *                  are not defined yet
     * @return true, if successful
     */
    private boolean updateParameter(String key, String value, EnumSet<ParamFlag> flagSet, EnumSet<ParamString> stringSet, EnumSet<ParamValue> valueSet) {
        boolean consumed = false;
        if (!consumed) {
            try {
                ParamFlag pf = ParamFlag.valueOf(key);
                if (flagSet.contains(pf)) {
                    if (value.equalsIgnoreCase("true")) {
                        this.setFlag(pf, true);
                        flagSet.remove(pf);
                    } else if (value.equalsIgnoreCase("false")) {
                        this.setFlag(pf, false);
                        flagSet.remove(pf);
                    } else {
                        TPS_Logger.log(SeverenceLogLevel.SEVERE,
                                "Illegal Parameter value: " + key + " -> " + value + " not in {true, false}");
                    }
                } else {
                    TPS_Logger.log(SeverenceLogLevel.SEVERE,
                            "Parameter already defined: " + key + " -> " + this.paramFlagClass.isTrue(pf));
                }
                consumed = true;
            } catch (IllegalArgumentException e) {
                /*
                 * ParamFlag pf = ParamFlag.valueOf(key.toString()); this line
                 * throws the exception, i.e. that there exists no enum constant
                 * with this name
                 */
            }
        }
        if (!consumed) {
            try {
                ParamString ps = ParamString.valueOf(key);
                if (stringSet.contains(ps) || ParamString.FILE_PARENT_PROPERTIES.equals(ps)) {
                    this.setString(ps, value);
                    stringSet.remove(ps);
                    consumed = true;

                    switch (ps) {
                        case FILE_DATABASE_PROPERTIES:
                        case FILE_LOGGING_PROPERTIES:
                        case FILE_PARAMETER_PROPERTIES:
                        case FILE_PARENT_PROPERTIES:
                            break;
                        default:
                            // everything ok
                    }
                } else {
                    TPS_Logger.log(SeverenceLogLevel.SEVERE,
                            "Parameter already defined: " + key + " -> " + this.getString(ps));
                }
            } catch (IllegalArgumentException e) {
                /*
                 * ParamString ps = ParamString.valueOf(key.toString()); this
                 * line throws the exception, i.e. that there exists no enum
                 * constant with this name
                 */
            }
        }
        if (!consumed) {
            try {
                ParamValue pv = ParamValue.valueOf(key);
                if (valueSet.contains(pv)) {
                    this.paramValueClass.setValue(pv, Double.parseDouble(value));
                    valueSet.remove(pv);
                    consumed = true;
                } else {
                    TPS_Logger.log(SeverenceLogLevel.SEVERE,
                            "Parameter already defined: " + key + " -> " + this.paramValueClass.getDoubleValue(pv));
                }
            } catch (NumberFormatException e) {
                TPS_Logger.log(SeverenceLogLevel.SEVERE, "Illegal parameter value: " + key, e);
                consumed = true;
            } catch (IllegalArgumentException e) {
                /*
                 * ParamValue pv = ParamValue.valueOf(key.toString()); this line
                 * throws the exception, i.e. that there exists no enum constant
                 * with this name
                 */
            }
        }
        if (!consumed) {
            if (TPS_Logger.isLogging(SeverenceLogLevel.WARN)) {
                TPS_Logger.log(SeverenceLogLevel.WARN, "Unknown parameter defined in file: " + key);
            }
        }
        return consumed;
    }

    /**
     * Inserts the given params into the table "simulation_parameters". The
     * identifier for the simulation in this table will be the given sim_key.
     */
    public void writeToDB(Connection con, String sim_key, Map<String, String> params) throws SQLException {
        PreparedStatement prepareStatement = null;
        try {
            prepareStatement = con.prepareStatement(
                    "INSERT INTO " + "simulation_parameters (" + "sim_key, " + "param_key, " + "param_value) " +
                            "VALUES(?, ?, ?)");
            prepareStatement.setString(1, sim_key);

            for (String key : params.keySet()) {
                prepareStatement.setString(2, key);
                prepareStatement.setString(3, params.get(key));

                prepareStatement.addBatch();
            }
        } finally {
            if (prepareStatement != null) prepareStatement.executeBatch();
            prepareStatement.close();
            if (!con.getAutoCommit()) con.commit();
        }
    }


}

