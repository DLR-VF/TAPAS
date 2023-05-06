/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.model.parameter;

import de.dlr.ivf.tapas.logger.LogHierarchy;
import de.dlr.ivf.tapas.logger.TPS_Logger;
import de.dlr.ivf.tapas.logger.HierarchyLogLevel;
import de.dlr.ivf.tapas.logger.SeverityLogLevel;
import de.dlr.ivf.tapas.model.Matrix;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
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
        if (!TPS_Logger.isLogging(SeverityLogLevel.WARN)) {
            return;
        }
        for (ParamFlag pf : ParamFlag.values()) {
            if (!this.isDefined(pf) && !this.getType(pf).equals(ParamType.OPTIONAL)) {
                TPS_Logger.log(SeverityLogLevel.WARN, pf + " not defined in the properties file");
            }
        }
        for (ParamValue pv : ParamValue.values()) {
            if (!this.isDefined(pv) && !this.getType(pv).equals(ParamType.OPTIONAL)) {
                TPS_Logger.log(SeverityLogLevel.WARN, pv + " not defined in the properties file");
            }
        }
        for (ParamString ps : ParamString.values()) {
            if (!this.isDefined(ps) && !this.getType(ps).equals(ParamType.OPTIONAL)) {
                TPS_Logger.log(SeverityLogLevel.WARN, ps + " not defined in the properties file");
            }
        }
        //TODO: we cannot check the matrices this way,because they need to be loaded somewhere first!
//        for (ParamMatrix pm : ParamMatrix.values()) {
//            if (!this.isDefined(pm) && !this.getType(pm).equals(ParamType.OPTIONAL)) {
//                TPS_Logger.log(SeverityLogLevel.WARN, pm + " not defined in the properties file");
//            }
//        }
//        for (ParamMatrixMap pmm : ParamMatrixMap.values()) {
//            if (!this.isDefined(pmm) && !this.getType(pmm).equals(ParamType.OPTIONAL)) {
//                TPS_Logger.log(SeverityLogLevel.WARN, pmm + " not defined in the properties file");
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
                        TPS_Logger.log(SeverityLogLevel.ERROR,
                                "Illegal " + "Parameter value: " + key + " -> " + value + " not in {true, false}");
                    }
                } else {
                    if (TPS_Logger.isLogging(SeverityLogLevel.WARN)) {
                        TPS_Logger.log(SeverityLogLevel.WARN,
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
                    if (TPS_Logger.isLogging(SeverityLogLevel.WARN)) {
                        TPS_Logger.log(SeverityLogLevel.WARN,
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
                    if (TPS_Logger.isLogging(SeverityLogLevel.WARN)) {
                        TPS_Logger.log(SeverityLogLevel.WARN, "Parameter " + "already defined: " + key + " -> " +
                                this.paramValueClass.getDoubleValue(pv));
                    }
                    consumed = true;
                }
            } catch (NumberFormatException e) {
                TPS_Logger.log(SeverityLogLevel.ERROR, "Illegal parameter " + "value: " + key, e);
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
            if (TPS_Logger.isLogging(SeverityLogLevel.DEBUG)) {
                TPS_Logger.log(SeverityLogLevel.DEBUG, "Unknown parameter defined in file: " + key);
            }
        }
        return consumed;
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
                    TPS_Logger.log(SeverityLogLevel.SEVERE,
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
                TPS_Logger.log(SeverityLogLevel.SEVERE, "Illegal parameter value: " + key, e);
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
            TPS_Logger.log(SeverityLogLevel.WARN, "Unknown parameter defined: " + key);
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

}
