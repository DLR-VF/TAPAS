/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.tools.persitence.db;

import de.dlr.ivf.tapas.log.TPS_Logger;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface.SeverenceLogLevel;
import de.dlr.ivf.tapas.persistence.db.TPS_DB_Connector;
import de.dlr.ivf.tapas.runtime.util.ClientControlProperties;
import de.dlr.ivf.tapas.runtime.util.ClientControlProperties.ClientControlPropKey;
import de.dlr.ivf.tapas.util.parameters.ParamString;
import de.dlr.ivf.tapas.util.parameters.TPS_ParameterClass;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;

public class TPS_BasicConnectionClass {

    private static final String basePropertyFile = "base.properties";

    protected TPS_DB_Connector dbCon = null;

    protected TPS_ParameterClass parameterClass;

    /**
     * Standard constructor, which enables the connection to the DB
     */
    public TPS_BasicConnectionClass() {
        this.parameterClass = new TPS_ParameterClass();
        try {
            this.parameterClass.loadRuntimeParameters(TPS_BasicConnectionClass.getRuntimeFile());
            dbCon = TPS_DB_Connector.fromParameterClass(this.parameterClass);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public TPS_BasicConnectionClass(TPS_ParameterClass parameterClass) {
        this.parameterClass = parameterClass;
        try {
            this.parameterClass.loadRuntimeParameters(TPS_BasicConnectionClass.getRuntimeFile());
            dbCon = TPS_DB_Connector.fromParameterClass(this.parameterClass);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Standard constructor, which enables the connection to the DB
     */
    public TPS_BasicConnectionClass(String loginFile) {
        this.parameterClass = new TPS_ParameterClass();
        try {
            this.parameterClass.loadRuntimeParameters(new File(loginFile));
            dbCon = TPS_DB_Connector.fromParameterClass(this.parameterClass);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Standard constructor, which enables the connection to the DB
     */
    public TPS_BasicConnectionClass(TPS_ParameterClass parameterClass, String loginFile) {
        this.parameterClass = parameterClass;
        try {
            this.parameterClass.loadRuntimeParameters(new File(loginFile));
            dbCon = TPS_DB_Connector.fromParameterClass(this.parameterClass);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Helper to convert a 1D-list into a 2D-matrix
     *
     * @param matrixVal input. length must be a square value.
     * @return a nxn-matrix containing the values of the input.
     */

    public static double[][] array1Dto2D(Integer[] matrixVal) {
        int size = (int) Math.sqrt(matrixVal.length);
        double[][] values = new double[size][size];
        int i, j, k;
        for (i = 0, k = 0; i < size; ++i) {
            for (j = 0; j < size; ++j) {
                values[i][j] = matrixVal[k];
                k++;
            }
        }
        return values;
    }

    /**
     * Helper to convert a 1D-list into a 2D-matrix
     *
     * @param matrixVal input. length must be a square value.
     * @return a nxn-matrix containing the values of the input.
     */

    public static double[][] array1Dto2D(int[] matrixVal) {
        int size = (int) Math.sqrt(matrixVal.length);
        double[][] values = new double[size][size];
        int i, j, k;
        for (i = 0, k = 0; i < size; ++i) {
            for (j = 0; j < size; ++j) {
                values[i][j] = matrixVal[k];
                k++;
            }
        }
        return values;
    }

    public static File getRuntimeFile(boolean forceNewFile) {
        return TPS_BasicConnectionClass.getRuntimeFile(basePropertyFile, forceNewFile);
    }

    public static File getRuntimeFile(String propertyFile, boolean forceNewFile) {
        String loginFile, programPath;

        File propFile = new File(propertyFile);
        //File tapasNetworkDirectory;
        final File runtimeFile;
        ClientControlProperties props = new ClientControlProperties(propFile);

        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            programPath = props.get(ClientControlPropKey.TAPAS_DIR_WIN);
        } else {
            programPath = props.get(ClientControlPropKey.TAPAS_DIR_LINUX);
        }

        if (props.get(ClientControlPropKey.LOGIN_CONFIG).length() > 0 && programPath.length() > 0 && !forceNewFile) {
            loginFile = props.get(ClientControlPropKey.LOGIN_CONFIG);
            //tapasNetworkDirectory = new File(programPath);
            runtimeFile = new File(loginFile);

        } else {
            JFileChooser choose = new JFileChooser();
            choose.setDialogTitle("Select login info file");
            int val = choose.showOpenDialog(choose); // choose as a parameter is necessary to put the dialog in the front
            String baseDirectory;
            File tmpFile;
            // did they click on open
            if (val == JFileChooser.APPROVE_OPTION) {
                tmpFile = choose.getSelectedFile();
                loginFile = tmpFile.getAbsolutePath();
                props.set(ClientControlPropKey.LOGIN_CONFIG, loginFile);
                if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                    baseDirectory = loginFile.substring(0, loginFile.indexOf(":" + File.separator) + 2);
                    props.set(ClientControlPropKey.TAPAS_DIR_WIN, baseDirectory);
                } else {
                    baseDirectory = loginFile.substring(0, loginFile.indexOf(File.separator, 1));
                    props.set(ClientControlPropKey.TAPAS_DIR_LINUX, tmpFile.getParent());
                }
                props.set(ClientControlPropKey.LOGIN_CONFIG, tmpFile.getAbsolutePath());
                props.updateFile();
                runtimeFile = new File(loginFile);
            } else {
                runtimeFile = null;
            }
        }
        return runtimeFile;
    }

    public static File getRuntimeFile() {
        return TPS_BasicConnectionClass.getRuntimeFile(basePropertyFile, false);

    }

    public static File getRuntimeFile(String propertyFile) {
        return TPS_BasicConnectionClass.getRuntimeFile(propertyFile, false);
    }

    /**
     * Helper to convert a 1D-list into a 2D-matrix
     *
     * @param matrixVal input. length must be a square value.
     * @return a nxn-matrix containing the values of the input.
     */

    public static int[][] intArray1Dto2D(int[] matrixVal) {
        int size = (int) Math.sqrt(matrixVal.length);
        int[][] values = new int[size][size];
        int i, j, k;
        for (i = 0, k = 0; i < size; ++i) {
            for (j = 0; j < size; ++j) {
                values[i][j] = matrixVal[k];
                k++;
            }
        }
        return values;
    }

    /**
     * Internal method to convert matrixelements to a sql-parsable array
     *
     * @param array         the array
     * @param decimalPlaces number of decimal places for the string
     * @return
     */
    public static String matrixToSQLArray(double[][] array, int decimalPlaces) {
        StringBuilder buffer;
        StringBuilder totalBuffer = new StringBuilder("'{");
        for (int j = 0; j < array.length; ++j) {
            buffer = new StringBuilder();
            for (int k = 0; k < array[0].length; ++k) {
                buffer.append(new BigDecimal(array[j][k]).setScale(decimalPlaces, RoundingMode.HALF_UP)).append(",");
            }
            if (j < array.length - 1) totalBuffer.append(buffer);
            else totalBuffer.append(buffer.substring(0, buffer.length() - 1)).append("}'");
        }
        return totalBuffer.toString();
    }

    /**
     * Method to convert matrixelements to a sql-parsable array by using a
     * StringWriter in place of String
     *
     * @param array         the array
     * @param decimalPlaces number of decimal places for the 'string'
     * @return
     */
    public static StringWriter matrixToStringWriterSQL(double[][] array, int decimalPlaces) {
        StringWriter buffer;
        StringWriter totalBuffer = new StringWriter();
        totalBuffer.append("'{");
        for (int j = 0; j < array.length; ++j) {
            buffer = new StringWriter();
            for (int k = 0; k < array[0].length; ++k) {

                buffer.append(new BigDecimal(array[j][k]).setScale(decimalPlaces, RoundingMode.HALF_UP) + ",");
            }
            if (j < array.length - 1) totalBuffer.append(buffer.getBuffer());
            else {
                int last = buffer.getBuffer().length();
                totalBuffer.append(buffer.getBuffer(), 0, last - 1);
                totalBuffer.append("}'");
            }
        }

        return totalBuffer;
    }

    /**
     * This method checks if the given matrix already exists
     *
     * @param matrixName the name of the matrix
     * @return true if
     */
    public boolean checkMatrixName(String matrixName) {
        boolean returnVal = false;
        //load the data from the db
        String query = "SELECT * FROM " + this.parameterClass.getString(ParamString.DB_TABLE_MATRICES) +
                " WHERE \"matrix_name\" = '" + matrixName + "'";
        try {
            ResultSet rs = dbCon.executeQuery(query, this);
            if (rs.next()) {
                returnVal = true;
            }
            rs.close();
        } catch (SQLException e) {
            System.err.println(
                    this.getClass().getCanonicalName() + " checkMatrixName: SQL-Error during statement: " + query);
            e.printStackTrace();
        }

        return returnVal;
    }

    protected void finalize() {
        this.dbCon.closeConnections();
    }

    /**
     * Returns the parameter class reference
     *
     * @return parameter class reference
     */
    public TPS_ParameterClass getParameters() {
        return this.parameterClass;
    }

    /**
     * Method to get the parameter value for a given simulation key and parameter key.
     *
     * @param simKey   the Simulation to look for
     * @param paramKey The parameter key from TPS_Parameter
     * @return The stored parameter value or null if parameter or simulation does not exist.
     */
    public String readParameter(String simKey, String paramKey) {
        String query = "";
        String returnVal = null;
        try {
            query = "SELECT param_value FROM simulation_parameters WHERE sim_key= '" + simKey + "' AND param_key ='" +
                    paramKey + "'";
            ResultSet rs = dbCon.executeQuery(query, this);

            if (rs.next()) {
                returnVal = rs.getString("param_value");
            }
            rs.close();


        } catch (SQLException e) {
            System.err.println(
                    this.getClass().getCanonicalName() + " readParameters: SQL-Error during statement: " + query);
            e.printStackTrace();
        }
        return returnVal;
    }

    /**
     * This method stores the given matrix with the given key in the db
     *
     * @param matrixName    the key for this matrix
     * @param matrix        the matrix
     * @param decimalPlaces the number of decimal places
     */
    public void storeInDB(String matrixName, double[][] matrix, int decimalPlaces) {
        if (decimalPlaces != 0) {
            if (TPS_Logger.isLogging(SeverenceLogLevel.WARN)) {
                TPS_Logger.log(SeverenceLogLevel.WARN,
                        "Decimal places are currently incompatible with the db (integers not doubles). Setting Decimal Places to 0!");
            }
            decimalPlaces = 0;
        }
        //load the data from the db
        String query = "SELECT * FROM " + this.parameterClass.getString(ParamString.DB_TABLE_MATRICES) +
                " WHERE \"matrix_name\" = '" + matrixName + "'";

        if (TPS_Logger.isLogging(SeverenceLogLevel.INFO)) {
            TPS_Logger.log(SeverenceLogLevel.INFO, "Preparing data for entry: " + matrixName + " in table " +
                    this.parameterClass.getString(ParamString.DB_TABLE_MATRICES));
        }
        if (checkMatrixName(matrixName)) {
            //update!
            query = "UPDATE " + this.parameterClass.getString(ParamString.DB_TABLE_MATRICES) + " SET matrix_values = ";
            query += matrixToSQLArray(matrix, decimalPlaces) + " WHERE \"matrix_name\" = '" + matrixName + "'";
            if (TPS_Logger.isLogging(SeverenceLogLevel.INFO)) {
                TPS_Logger.log(SeverenceLogLevel.INFO, "Updating data for entry: " + matrixName + " in table " +
                        this.parameterClass.getString(ParamString.DB_TABLE_MATRICES) + ".");
            }
        } else {
            query = "INSERT INTO " + this.parameterClass.getString(ParamString.DB_TABLE_MATRICES) +
                    " (matrix_name, matrix_values) VALUES ('" + matrixName + "', ";
            query += matrixToSQLArray(matrix, decimalPlaces) + ")";
            if (TPS_Logger.isLogging(SeverenceLogLevel.INFO)) {
                TPS_Logger.log(SeverenceLogLevel.INFO, "Inserting data for entry: " + matrixName + " in table " +
                        this.parameterClass.getString(ParamString.DB_TABLE_MATRICES) + ".");
            }
        }
        dbCon.execute(query, this);
        if (TPS_Logger.isLogging(SeverenceLogLevel.INFO)) {
            TPS_Logger.log(SeverenceLogLevel.INFO, "Successful!");
        }
    }
}
