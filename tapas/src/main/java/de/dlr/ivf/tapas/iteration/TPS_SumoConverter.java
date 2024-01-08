/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.iteration;

import de.dlr.ivf.api.io.util.SqlArrayUtils;
import de.dlr.ivf.tapas.TPS_Main;
import de.dlr.ivf.tapas.logger.legacy.LogHierarchy;
import de.dlr.ivf.tapas.logger.legacy.TPS_Logger;
import de.dlr.ivf.tapas.logger.legacy.HierarchyLogLevel;
import de.dlr.ivf.tapas.logger.legacy.SeverityLogLevel;
import de.dlr.ivf.tapas.misc.Helpers;
import de.dlr.ivf.tapas.persistence.db.TPS_DB_Connector;
import de.dlr.ivf.tapas.model.Matrix;
import de.dlr.ivf.tapas.model.MatrixMap;
import de.dlr.ivf.tapas.parameter.CURRENCY;
import de.dlr.ivf.tapas.model.parameter.ParamString;
import de.dlr.ivf.tapas.model.parameter.ParamValue;
import de.dlr.ivf.tapas.model.parameter.TPS_ParameterClass;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;

/**
 * This class provides all the necessary tools to communicate with Sumo.
 * This includes converter routines, to load/store form the database to/from Sumo
 * Generating additional information.
 *
 * @author hein_mh
 */
@LogHierarchy(hierarchyLogLevel = HierarchyLogLevel.CLIENT)
public class TPS_SumoConverter {

    /**
     * Parameter for the weight, if two matrices should be merged.
     */
    double weight = 1;
    /**
     * Identifier for the iteration cycle
     */
    int iteration = 1;
    /**
     * Parameter to determine, if the input is a pt matrix or iv matrix
     */
    boolean isPT = false;
    /**
     * extracted reference to the dbManager
     */
    private final TPS_DB_Connector dbManager;
    /**
     * array for the net-distances in m
     */
    private double[][] distance = null;
    /**
     * time distribution for the matrices
     */
    private double[] distribution = null;
    /**
     * matrix array for the travel times
     */
    private Matrix[] matrixArray = null;
    /**
     * matrix array for the counters
     */
    private int[][][] counter = null;
    /**
     * hashmap for id conversion
     */
    private final HashMap<Integer, Integer> idToIndex = new HashMap<>();
    /**
     * hashmap for id conversion
     */
    private final HashMap<Integer, Integer> indexToId = new HashMap<>();

    /**
     * Constructor, if this is called externally with already loaded parameters.
     *
     * @param con reference to the TPS_DB_Connector to use
     */

    public TPS_SumoConverter(TPS_DB_Connector con) {
        this.dbManager = con;
    }

    /**
     * This constructor reads all parameters and tries to initialise all data which is available via the
     * TPS_PersistenceManager . If any Exception is thrown it is logged and the application stops.
     *
     * @param file   filename of the parameter file with all run information. This file leads to all other files with e.g.
     *               logging, database, etc. information.
     * @param simKey key of the simulation
     */
    public TPS_SumoConverter(File file, String simKey) {

        //TPS_Parameters.clear();
        if (simKey == null) simKey = TPS_Main.getDefaultSimKey();


        TPS_ParameterClass parameterClass = new TPS_ParameterClass();
        parameterClass.setString(ParamString.RUN_IDENTIFIER, simKey);
        parameterClass.setString(ParamString.CURRENCY, CURRENCY.EUR.name());

        try {
            //Thread.sleep(10000);
            parameterClass.loadRuntimeParameters(file);
            File tmpFile = new File(file.getPath());
            while (!tmpFile.getPath().endsWith(
                    parameterClass.SIM_DIR.substring(0, parameterClass.SIM_DIR.length() - 1))) {
                tmpFile = tmpFile.getParentFile();
            }
            parameterClass.setString(ParamString.FILE_WORKING_DIRECTORY, tmpFile.getParent());

            //ty to load parameters from db
            this.dbManager = new TPS_DB_Connector(parameterClass);
            String query = "SELECT * FROM " + parameterClass.getString(ParamString.DB_TABLE_SIMULATION_PARAMETERS) +
                    " WHERE sim_key = '" + simKey + "'";
            ResultSet rs = dbManager.executeQuery(query, this);
            readParametersFromDBMaintainLoginInfo(rs, parameterClass);
            rs.close();
            parameterClass.checkParameters();

        } catch (Exception e) {
            TPS_Logger.log(SeverityLogLevel.FATAL, "Application shutdown: unhandable exception", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * The constructor builds a File from the given filename and calls TPS_SumoConverter(File, String).
     *
     * @param filename filename of the run properties file
     * @param sim_key  key of the simulation
     */
    public TPS_SumoConverter(String filename, String sim_key) {
        this(new File(filename), sim_key);
    }

    /**
     * This method is used to read all parameters but should keep the login information because we need elevated rights to write to db later!
     *
     * @param rs             the ResultSet which contains the simulation parameters
     * @param parameterClass parameter container to access the enum values
     * @throws SQLException The exception could be thrown  while reading form the DB
     */
    public static void readParametersFromDBMaintainLoginInfo(ResultSet rs, TPS_ParameterClass parameterClass) throws SQLException {
        String login = null, password = null;
        //read old login
        if (parameterClass.isDefined(ParamString.DB_PASSWORD)) {
            password = parameterClass.getString(ParamString.DB_PASSWORD);
        }

        if (parameterClass.isDefined(ParamString.DB_USER)) {
            login = parameterClass.getString(ParamString.DB_USER);
        }
        //read parameters
        parameterClass.readRuntimeParametersFromDB(rs);
        //write old login
        if (password != null) {
            parameterClass.setString(ParamString.DB_PASSWORD, password);
        }
        if (login != null) {
            parameterClass.setString(ParamString.DB_USER, login);
        }
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
        String query = "SELECT * FROM " + this.dbManager.getParameters().getString(ParamString.DB_SCHEMA_CORE) +
                this.dbManager.getParameters().getString(ParamString.DB_TABLE_MATRICES) + " WHERE \"matrix_name\" = '" +
                matrixName + "'";
        try {
            ResultSet rs = this.dbManager.executeQuery(query, this);
            if (rs.next()) {
                returnVal = true;
            }
            rs.close();
        } catch (SQLException e) {
            TPS_Logger.log(SeverityLogLevel.ERROR, "SQL error! ", e);
            return false;
        }
        return returnVal;
    }

    /**
     * This method deletes the matrix from the db
     *
     * @param matrixName the matrix to delete
     */
    public void deleteMatrix(String matrixName) {
        String query = "DELETE FROM " + this.dbManager.getParameters().getString(ParamString.DB_TABLE_MATRICES) +
                " WHERE \"matrix_name\" = '" + matrixName + "'";
        this.dbManager.execute(query, this);

    }

    /**
     * @return the counter
     */
    public int[][][] getCounter() {
        return counter;
    }

    /**
     * @param counter the counter to set
     */
    public void setCounter(int[][][] counter) {
        this.counter = counter;
    }

    /**
     * @return the distance
     */
    public double[][] getDistance() {
        return distance;
    }

    /**
     * @param distance the distance to set
     */
    public void setDistance(double[][] distance) {
        this.distance = distance;
    }

    /**
     * @return the distribution
     */
    public double[] getDistribution() {
        return distribution;
    }

    /**
     * @param distribution the distribution to set
     */
    public void setDistribution(double[] distribution) {
        this.distribution = distribution;
    }

    /**
     * @return the iteration
     */
    public int getIteration() {
        return iteration;
    }

    /**
     * @param iteration the iteration to set
     */
    public void setIteration(int iteration) {
        this.iteration = iteration;
    }

    /**
     * @return the matrixArray
     */
    public Matrix[] getMatrixArray() {
        return matrixArray;
    }

    /**
     * @param matrixArray the matrixArray to set
     */
    public void setMatrixArray(Matrix[] matrixArray) {
        this.matrixArray = matrixArray;
    }

    /**
     * this method returns the Index of the time slices according to the distribution
     *
     * @param startTimeMin the start time in minutes after midnight
     * @return the index of the time slice or -1 if nothing is found
     */
    private int getTimeSliceIndex(int startTimeMin) {
        if (this.distribution == null) return -1;

        double time = startTimeMin / 60.0;

        //correct time
        while (time < 0.0) time += 24.0;
        while (time > 24.0) time -= 24.0;

        for (int i = 0; i < this.distribution.length; ++i) {
            if (this.distribution[i] >= time) return i;
        }
        return -1;
    }

    /**
     * Method to merge the matrices according to the weight
     */
    public void mergeMatrices() {
    }

    /**
     * This method merges the two matrices. The result is stored in "newVals".
     * Every value of the matrix is updated by: val = ( old_val+weight*new_val)/(1+weight)
     *
     * @param oldVals matrix containing the old values
     * @param index   index of the internal matrix containing the new values and return object
     */
    public void mergeMatrixPair(Matrix oldVals, int index) {
        int i, j;
        double oldVal, newVal;
        double error, maxError = 0, minError = 1e100, avgError = 0;
        final int size = this.matrixArray[index].getNumberOfColums();
        final double normalizationFactor = 1.0 / (1.0 + this.weight);
        for (i = 0; i < size; ++i) {
            for (j = 0; j < size; ++j) {
                oldVal = oldVals.getValue(i, j);
                newVal = this.matrixArray[index].getValue(i, j);
                error = oldVal - newVal;
                if (newVal > 0.0) {//ignore invalid cells (no trafic)
                    maxError = Math.max(maxError, error);
                    minError = Math.min(minError, error);
                    avgError += error;
                    newVal = (oldVal + this.weight * newVal) * normalizationFactor;
                    this.matrixArray[index].setValue(i, j, newVal);
                }
            }
        }
        avgError /= size * size;
        if (TPS_Logger.isLogging(SeverityLogLevel.INFO)) {
            TPS_Logger.log(SeverityLogLevel.INFO,
                    "max error: " + maxError + " minError: " + minError + " avg error: " + avgError);
        }
    }

    public void prepareNextSimulationIteration(String key) {
        //read some Param-Values
        String tabeleTrips = this.dbManager.readParameter(key, "DB_TABLE_TRIPS", this);
        String schemaTemp = this.dbManager.readParameter(key, "DB_SCHEMA_TEMP", this);
        String simulations = this.dbManager.readParameter(key, "DB_TABLE_SIMULATIONS", this);
        String simulationParams = this.dbManager.readParameter(key, "DB_TABLE_SIMULATION_PARAMETERS", this);
        String iteration = this.dbManager.readParameter(key, "ITERATION", this);

        //delete old trips
        String query = "DELETE FROM " + tabeleTrips + "_" + key;
        this.dbManager.executeQuery(query, this);
        //empty locations
        query = "UPDATE " + schemaTemp + "locations_" + key + " set loc_occupancy=0";
        this.dbManager.executeQuery(query, this);
        //reset households
        query = "UPDATE " + schemaTemp + "households_" + key +
                " set hh_started =false, hh_finished = false, server_ip=NULL";
        this.dbManager.executeQuery(query, this);
        //set sim-params
        query = "UPDATE " + simulations +
                " set sim_finished =false, sim_progress=0 timestamp_finished= null WHERE sim_key='" + key + "'";
        this.dbManager.executeQuery(query, this);
        //update iteration
        int iter = Integer.parseInt(iteration) + 1;
        query = "UPDATE " + simulationParams + " set param_value='" + iter + "' where sim_key='" + key +
                "' and param_key = 'ITERATION'";
        this.dbManager.executeQuery(query, this);
    }

    public void readAllParis(String tablename) {
        String query = "";
        try {
            query = "select taz_id_start, taz_id_end, interval_end, travel_time_sec, distance_real from " + tablename;
            ResultSet rs = this.dbManager.executeQuery(query, this);
            int fromTVZ, toTVZ, distributionIndex;
            double tt, dist;
            while (rs.next()) {
                distributionIndex = this.getTimeSliceIndex(rs.getInt("interval_end") / 60);
                fromTVZ = this.idToIndex.get(rs.getInt("taz_id_start"));
                toTVZ = this.idToIndex.get(rs.getInt("taz_id_end"));
                tt = rs.getDouble("travel_time_sec");
                dist = rs.getDouble("distance_real");
                this.distance[fromTVZ][toTVZ] = dist;
                this.matrixArray[distributionIndex].setValue(fromTVZ, toTVZ, tt);
            }
            rs.close();
        } catch (SQLException e) {
            TPS_Logger.log(SeverityLogLevel.ERROR, "SQL error in query: " + query, e);
        }
    }

    /**
     * This Method reads the csv-file and stores the information in the according matrices.
     * The file has the format:
     * <p>
     * IV:
     * fromID;toID;travelTime;Speed;Distance;Beeline
     * <p>
     * PT:
     * fromID;toID;accessTime;Beeline;egressTime;Distance;TravelTime
     *
     * @param fileName The csv-file.
     */

    public void readCSVFile(String fileName) throws IOException {
        FileReader in = null;
        BufferedReader input = null;
        String line = "";
        try {
            int fromTVZ, toTVZ, tvzCounter = 0, startTime, mode, distributionIndex;
            double time, actDistance;
            boolean hasDistance;
            tvzCounter = this.idToIndex.size();
            //prepare arrays
            this.distance = new double[tvzCounter][tvzCounter];

            //open input
            in = new FileReader(fileName);
            input = new BufferedReader(in);
            line = input.readLine();//header
            if (TPS_Logger.isLogging(SeverityLogLevel.INFO)) {
                TPS_Logger.log(SeverityLogLevel.INFO, "File opened: " + fileName + " header: " + line);
            }

            String actToken;
            while ((line = input.readLine()) != null) {
                if (line.startsWith("$")) // comment
                    continue;
                StringTokenizer tok = new StringTokenizer(line, ",");
                //check format
                //check format
                if (tok.countTokens() == 10) hasDistance = true;
                else if (tok.countTokens() == 9) hasDistance = false;
                else continue;
                actToken = tok.nextToken(); //p_id
                actToken = tok.nextToken(); //hh_id
                actToken = tok.nextToken(); //start time
                startTime = Integer.parseInt(actToken.trim());
                distributionIndex = this.getTimeSliceIndex(startTime);
                actToken = tok.nextToken(); //start taz
                fromTVZ = Integer.parseInt(actToken.trim()) - 1;
                actToken = tok.nextToken(); //end taz
                toTVZ = Integer.parseInt(actToken.trim()) - 1;
                actToken = tok.nextToken(); //start block
                actToken = tok.nextToken(); //end block
                actToken = tok.nextToken(); //mode
                mode = Integer.parseInt(actToken.trim());
                if (distributionIndex >= 0 && mode == 2) {
                    actToken = tok.nextToken(); //travel time
                    time = Double.parseDouble(actToken.trim());
                    //inc time
                    this.matrixArray[distributionIndex].setValue(fromTVZ, toTVZ,
                            this.matrixArray[distributionIndex].getValue(fromTVZ, toTVZ) + time);
                    //inc counter for this cell
                    this.counter[distributionIndex][fromTVZ][toTVZ]++;
                    this.counter[counter.length - 1][fromTVZ][toTVZ]++;
                    if (hasDistance) {
                        actToken = tok.nextToken(); //distance
                        actDistance = Double.parseDouble(actToken.trim());
                        //inc distance
                        this.distance[fromTVZ][toTVZ] += actDistance;
                    }
                }
            }
            //calc average

            for (int i = 0; i < this.distribution.length; ++i) {
                for (int j = 0; j < tvzCounter; ++j) {
                    for (int k = 0; k < tvzCounter; ++k) {
                        //only valid distances
                        if (i == 0 && this.counter[counter.length - 1][j][k] > 0) {
                            this.distance[j][k] /= this.counter[counter.length - 1][j][k];
                        }
                        int counterVal = this.counter[i][j][k];
                        //only valid traveltimes
                        if (counterVal > 0) {
                            double val = this.matrixArray[i].getValue(j, k);
                            this.matrixArray[i].setValue(j, k, val / counterVal);
                        }
                    }
                }
            }
        } finally {
            try {
                if (input != null) input.close();
                if (in != null) in.close();
            }//try
            catch (IOException ex) {
                TPS_Logger.log(SeverityLogLevel.ERROR, " Could not close : " + fileName);
                throw new IOException(ex);
            }//catch
        }//finally
    }

    public void readIDMap() throws SQLException {
        String query = "";
        ResultSet rs = null;
        int taz, externalId;
        try {
            query = "SELECT taz_id, taz_num_id FROM " + this.dbManager.getParameters().getString(
                    ParamString.DB_TABLE_TAZ);
            rs = this.dbManager.executeQuery(query, this);
            while (rs.next()) {
                taz = rs.getInt("taz_id") - 1;
                externalId = rs.getInt("taz_num_id");
                if (externalId > 0) {
                    if (idToIndex.get(externalId) == null) {//new tvz
                        idToIndex.put(externalId, taz);
                        indexToId.put(taz, externalId);
                    }
                }
            }
            rs.close();
            if (TPS_Logger.isLogging(SeverityLogLevel.INFO)) {
                TPS_Logger.log(SeverityLogLevel.INFO, "Found " + this.idToIndex.size() + " TVZ-IDs");
            }

        } catch (SQLException e) {
            TPS_Logger.log(SeverityLogLevel.ERROR, "SQL error! Query: " + query, e);
            throw new SQLException("SQL error! Query: " + query, e);
        }
    }

    /**
     * This method reads the square matrix from the db and returns a 2D-Array
     *
     * @param matrixName The matrix name in the db
     * @return the 2D-double array of the square matrix
     */
    public double[][] readMatrixFromDB(String matrixName) {
        double[][] returnVal = null;
        //load the data from the db
        String query = "SELECT * FROM " + this.dbManager.getParameters().getString(ParamString.DB_TABLE_MATRICES) +
                " WHERE \"matrix_name\" = '" + matrixName + "'";
        try {
            ResultSet rs = this.dbManager.executeQuery(query, this);
            if (rs.next()) {
                Object array = rs.getArray("matrix_values").getArray();
                if (array instanceof Integer[]) {
                    //parse data to memory model
                    Integer[] matrixVal = (Integer[]) array;
                    int size = (int) Math.sqrt(matrixVal.length);
                    if (size != this.distance.length) {
                        TPS_Logger.log(SeverityLogLevel.ERROR,
                                "Matrix " + matrixName + " has a different size: " + size + " Expected: " +
                                        this.distance.length);
                        return null;
                    }
                    returnVal = new double[size][size];
                    int k = 0;
                    for (int i = 0; i < size; ++i) {
                        for (int j = 0; j < size; ++j, ++k) {
                            returnVal[i][j] = (double) matrixVal[k];
                        }
                    }
                }
            } else {
                TPS_Logger.log(SeverityLogLevel.ERROR, "Matrix " + matrixName + " does not exist!");
            }
            rs.close();
        } catch (SQLException e) {
            TPS_Logger.log(SeverityLogLevel.ERROR, "SQL Error: " + query, e);
        }
        return returnVal;
    }

    public MatrixMap readMatrixMap(ParamString matrixName, List<String> matrixNames) {
        String query = "";
        ResultSet rs = null;
        try {
            query = "SELECT \"matrixmap_num\", \"matrixmap_matrixnames\",  \"matrixmap_distribution\"  FROM " +
                    this.dbManager.getParameters().getString(ParamString.DB_TABLE_MATRIXMAPS) +
                    " WHERE \"matrixmap_name\"='" + this.dbManager.getParameters().getString(matrixName) + "'";
            rs = this.dbManager.executeQuery(query, this);

            if (rs.next()) {
                // get number of matrices to load
                int numOfMatrices = rs.getInt("matrixmap_num");
                // get matrix names
                String[] matrix_names = SqlArrayUtils.extractStringArray(rs.getArray("matrixmap_matrixnames"));
                // get distribution
                double[] thisDistribution = SqlArrayUtils.extractDoubleArray(rs, "matrixmap_distribution");
                rs.close();
                // check sizes
                if (numOfMatrices != matrix_names.length) {
                    TPS_Logger.log(SeverityLogLevel.FATAL,
                            "Couldn't load matrixmap " + this.dbManager.getParameters().getString(matrixName) +
                                    " from database. Different array sizes (num, matrices, distribution): " +
                                    numOfMatrices + " " + matrix_names.length + " " + distribution.length +
                                    " SQL query: " + query);
                }

                // init matrix map
                Matrix[] matrices = new Matrix[numOfMatrices];

                // load matrix map
                for (int i = 0; i < numOfMatrices; ++i) {
                    matrixNames.add(matrix_names[i]);
                    query = "SELECT matrix_values FROM " + this.dbManager.getParameters().getString(
                            ParamString.DB_TABLE_MATRICES) + " WHERE matrix_name='" + matrix_names[i] + "'";
                    rs = this.dbManager.executeQuery(query, this);
                    if (rs.next()) {
                        int[] iArray = SqlArrayUtils.extractIntArray(rs, "matrix_values");
                        int len = (int) Math.sqrt(iArray.length);
                        matrices[i] = new Matrix(len, len);
                        for (int index = 0; index < iArray.length; index++) {
                            matrices[i].setRawValue(index, iArray[index]);
                        }
                        rs.close();
                    } else {
                        TPS_Logger.log(SeverityLogLevel.FATAL,
                                "Couldn't load matrix " + matrix_names[i] + " form matrix map" +
                                        this.dbManager.getParameters().getString(matrixName) +
                                        ": No such matrix. SQL Query: " + query);
                        return null;
                    }
                }
                return new MatrixMap(thisDistribution, matrices);
            }
        } catch (SQLException e) {
            TPS_Logger.log(SeverityLogLevel.FATAL,
                    "Couldn't load matrixmap " + this.dbManager.getParameters().getString(matrixName) +
                            " from database: No such entry. SQL Query: " + query, e);
        }
        return null;
    }

    /**
     * Setter if the input is pt or iv
     *
     * @param isPT the flag
     */
    public void setPT(boolean isPT) {
        this.isPT = isPT;
    }

    /**
     * Sets the weight for merging with the old matrix.
     * val = (old_val + weight*new_val)/(1+weight)
     *
     * @param weight the weight of the new values
     */
    public void setWeight(double weight) {
        this.weight = weight;
    }

    /**
     * This method stores the given matrix with the given key in the db
     *
     * @param matrixName    the key for this matrix
     * @param mat           the Matrix to store
     * @param decimalPlaces the number of decimal placed, curently only 0 is supported!
     */
    public void storeInDB(String matrixName, Matrix mat, int decimalPlaces) {
        if (decimalPlaces != 0) {
            if (TPS_Logger.isLogging(SeverityLogLevel.WARN)) {
                TPS_Logger.log(SeverityLogLevel.WARN,
                        "Decimal places are currently incompatible with the db (integers not doubles). Setting Decimal Places to 0!");
            }
            decimalPlaces = 0;
        }
        //load the data from the db
        String query = "SELECT * FROM " + this.dbManager.getParameters().getString(ParamString.DB_TABLE_MATRICES) +
                " WHERE \"matrix_name\" = '" + matrixName + "'";
        if (TPS_Logger.isLogging(SeverityLogLevel.INFO)) {
            TPS_Logger.log(SeverityLogLevel.INFO, "Preparing data for entry: " + matrixName + " in table " +
                    this.dbManager.getParameters().getString(ParamString.DB_TABLE_MATRICES));
        }
        if (checkMatrixName(matrixName)) {
            //update!
            query = "UPDATE " + this.dbManager.getParameters().getString(ParamString.DB_TABLE_MATRICES) +
                    " SET matrix_values = ";
            query += Helpers.matrixToSQLArray(mat, decimalPlaces) + " WHERE \"matrix_name\" = '" + matrixName + "'";
            if (TPS_Logger.isLogging(SeverityLogLevel.INFO)) {
                TPS_Logger.log(SeverityLogLevel.INFO, "Updating data for entry: " + matrixName + " in table " +
                        this.dbManager.getParameters().getString(ParamString.DB_TABLE_MATRICES) + ".");
            }
        } else {
            query = "INSERT INTO " + this.dbManager.getParameters().getString(ParamString.DB_TABLE_MATRICES) +
                    " (matrix_name, matrix_values) VALUES ('" + matrixName + "', ";
            query += Helpers.matrixToSQLArray(mat, decimalPlaces) + ")";
            if (TPS_Logger.isLogging(SeverityLogLevel.INFO)) {
                TPS_Logger.log(SeverityLogLevel.INFO, "Inserting data for entry: " + matrixName + " in table " +
                        this.dbManager.getParameters().getString(ParamString.DB_TABLE_MATRICES) + ".");
            }
        }
        this.dbManager.execute(query, this);
        if (TPS_Logger.isLogging(SeverityLogLevel.INFO)) {
            TPS_Logger.log(SeverityLogLevel.INFO, "Successful!");
        }

    }

    /**
     * Method to store the matrixMap of this set of matrices inthe db
     *
     * @param matrixMapName
     * @param matrixNames
     * @return
     */
    public void storeMatrixMap(ParamString matrixMapName, List<String> matrixNames) {
        String query = "";
        ResultSet rs = null;
        try {
            query = "SELECT \"matrixMap_num\", \"matrixMap_matrixNames\",  \"matrixMap_distribution\"  FROM " +
                    this.dbManager.getParameters().getString(ParamString.DB_TABLE_MATRIXMAPS) +
                    " WHERE \"matrixMap_name\"='" + this.dbManager.getParameters().getString(matrixMapName) + "'";
            rs = this.dbManager.executeQuery(query, this);

            if (rs.next()) {
                //update
                query = "";
            } else {
                //insert
                query = "";
            }
            this.dbManager.execute(query, this);
        } catch (SQLException e) {
            TPS_Logger.log(SeverityLogLevel.FATAL,
                    "Couldn't load matrixmap " + this.dbManager.getParameters().getString(matrixMapName) +
                            " from database: No such entry. SQL Query: " + query, e);
        }
    }

    /**
     * This method exports the trips to Sumo TripFile
     *
     * @param outputPath the path to store the OD-matrix. usually Trip-Exports
     */
    public void writeTripFile(String outputPath) throws SQLException {
        String query = "";
        try {
            query = "SELECT core.simple_export_trip_table('" + this.dbManager.getParameters().getString(
                    ParamString.DB_REGION) + "_trips_" + this.dbManager.getParameters().getString(
                    ParamString.RUN_IDENTIFIER) + "_IT_" + this.dbManager.getParameters().getIntValue(
                    ParamValue.ITERATION) + "')";

            ResultSet rs = dbManager.executeQuery(query, this);
            rs.close();

        } catch (SQLException e) {
            TPS_Logger.log(SeverityLogLevel.FATAL, "Exception during Sumo export! Error reading SQL! Query:" + query,
                    e);
            throw new SQLException("Exception during Sumo export! Error reading SQL! Query:" + query, e);
        }
    }
}
