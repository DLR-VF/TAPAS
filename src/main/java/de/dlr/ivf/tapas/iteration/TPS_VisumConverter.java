/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.iteration;

import de.dlr.ivf.tapas.TPS_Main;
import de.dlr.ivf.tapas.log.LogHierarchy;
import de.dlr.ivf.tapas.log.TPS_Logger;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface.HierarchyLogLevel;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface.SeverenceLogLevel;
import de.dlr.ivf.tapas.mode.TPS_Mode;
import de.dlr.ivf.tapas.persistence.db.TPS_DB_IO;
import de.dlr.ivf.tapas.tools.persitence.db.TPS_BasicConnectionClass;
import de.dlr.ivf.tapas.util.Matrix;
import de.dlr.ivf.tapas.util.MatrixMap;
import de.dlr.ivf.tapas.util.parameters.*;

import java.io.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.StringTokenizer;

/**
 * This class provides all the necessary tools to communicate with Visum.
 * This includes converter routines, to load/store form the database to/from Visum
 * Generating additional information.
 *
 * @author hein_mh
 */
@LogHierarchy(hierarchyLogLevel = HierarchyLogLevel.CLIENT)
public class TPS_VisumConverter extends TPS_BasicConnectionClass {

    public TPS_ParameterClass parameterClass;
    /**
     * Parameter for the weight, if two matrices should be merged.
     */
    private double weight = 1;
    /**
     * Identifier for the iteration cycle
     */
    private int iteration = 1;
    /**
     * String array which holds the matrix names for comparison.
     */

    private String[] compareMatrix = null;
    /**
     * Parameter to determine, if the input is a pt matrix or iv matrix
     */
    private boolean isPT = false;
    /**
     * vector to store the intra taz-beeline factors
     */
    private double[] intraBeeLineFactor = null;
    /**
     * vector to store the intra taz-speeds
     */
    private double[] intraSpeed = null;
    /**
     * array for the traveltime in seconds
     */
    private double[][] travelTime = null;
    /**
     * array for the speed in m/s
     */
    private double[][] speed = null;
    /**
     * array for the access time in seconds
     */
    private double[][] accessTime = null;
    /**
     * array for the egress time in seconds
     */
    private double[][] egressTime = null;
    /**
     * array for the net-distances in m
     */
    private double[][] distance = null;
    /**
     * array for the beeline distances in meter
     */
    private double[][] beeline = null;
    /**
     * hashmap for id conversion
     */
    private final HashMap<Integer, Integer> idToIndex = new HashMap<>();
    /**
     * hashmap for id conversion
     */
    private final HashMap<Integer, Integer> indexToId = new HashMap<>();

    /**
     * standard constructor
     */
    public TPS_VisumConverter(TPS_ParameterClass parameterClass) {
        super(parameterClass);
    }

    /**
     * This constructor reads all parameters and tries to initialise all data which is available via the
     * TPS_PersistenceManager . If any Exception is thrown it is logged and the application stops.
     *
     * @param file   filename of the parameter file with all run information. This file leads to all other files with e.g.
     *               logging, database, etc. information.
     * @param simKey key of the simulation
     */
    public TPS_VisumConverter(TPS_ParameterClass parameterClass, String file, String simKey) {
        super(parameterClass, file);
        //TPS_Parameters.clear();
        if (simKey == null) simKey = TPS_Main.getDefaultSimKey();


        this.parameterClass.setString(ParamString.RUN_IDENTIFIER, simKey);
        this.parameterClass.setString(ParamString.CURRENCY, CURRENCY.EUR.name());

        try {
            //Thread.sleep(10000);
            File tmpFile = new File(file);
            while (!tmpFile.getPath().endsWith(
                    this.parameterClass.SIM_DIR.substring(0, this.parameterClass.SIM_DIR.length() - 1))) {
                tmpFile = tmpFile.getParentFile();
            }
            this.parameterClass.setString(ParamString.FILE_WORKING_DIRECTORY, tmpFile.getParent());

            //try to load parameters from db
            String query = "SELECT * FROM " + this.parameterClass.getString(
                    ParamString.DB_TABLE_SIMULATION_PARAMETERS) + " WHERE sim_key = '" + simKey + "'";
            ResultSet rs = dbCon.executeQuery(query, this);
            TPS_SumoConverter.readParametersFromDBMaintainLoginInfo(rs, this.parameterClass);
            rs.close();
            this.parameterClass.checkParameters();

        } catch (Exception e) {
            TPS_Logger.log(SeverenceLogLevel.FATAL, "Application shutdown: unhandable exception", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Internal method to store the min/max-values
     *
     * @param array
     */
    private void calcMinMaxValue(double[] array) {
        if (array.length != 3) {
            TPS_Logger.log(SeverenceLogLevel.ERROR, "Wrong array size: " + array.length);
            return;
        }
        array[1] = Math.min(array[1], array[0]);
        if (array[0] != 999999.0) array[2] = Math.max(array[2], array[0]);
    }

    /**
     * Method to check if a given intro info name exists
     *
     * @param name the info name to look for
     * @return true if it exists
     */
    public boolean checkIntraInfos(String name) {
        String tableName;
        boolean retrunVal = false;
        if (this.isPT) tableName = this.parameterClass.getString(ParamString.DB_TABLE_TAZ_INTRA_PT_INFOS);
        else tableName = this.parameterClass.getString(ParamString.DB_TABLE_TAZ_INTRA_MIT_INFOS);

        String query = "SELECT * FROM " + tableName + " WHERE info_name = '" + name + "'";
        try {
            ResultSet rs = dbCon.executeQuery(query, this);
            if (rs.next()) {
                retrunVal = true;
            }
            rs.close();
        } catch (SQLException e) {
            TPS_Logger.log(SeverenceLogLevel.ERROR, "SQL error!", e);
            return false;

        }
        return retrunVal;
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
            TPS_Logger.log(SeverenceLogLevel.ERROR, "SQL error! ", e);
            return false;
        }
        return returnVal;
    }

    /**
     * Internal Method to check for multiple mins.
     *
     * @param val   the value to test
     * @param array an array, which holds the array.length minimum values
     * @return the position, where the value was inserted or -1 if "val" is bigger than every value in "array"
     */

    private int checkMultiMin(double val, double[] array) {
        for (int i = 0; i < array.length; ++i) {
            if (val < array[i]) {
                insertMinAt(val, array, i);
                return i;
            }
        }
        return -1;
    }

    /**
     * This method deletes the intra info cells for the given name.
     *
     * @param name the name if the intra infos to delete
     */
    public void deleteIntraInfos(String name) {
        String tableName;

        if (this.isPT) tableName = this.parameterClass.getString(ParamString.DB_TABLE_TAZ_INTRA_PT_INFOS);
        else tableName = this.parameterClass.getString(ParamString.DB_TABLE_TAZ_INTRA_MIT_INFOS);

        String query = "DELETE FROM " + tableName + " WHERE info_name = '" + name + "'";
        dbCon.execute(query, this);
    }

    /**
     * This method deletes the matrix from the db
     *
     * @param matrixName the matrix to delete
     */
    public void deleteMatrix(String matrixName) {
        String query = "DELETE FROM " + this.parameterClass.getString(ParamString.DB_TABLE_MATRICES) +
                " WHERE \"matrix_name\" = '" + matrixName + "'";
        dbCon.execute(query, this);
    }

    /**
     * This method generates the intra-infos.
     * Workflow:
     * 1. Find n closest TVZs
     * 2. Store traveltime, distance, beeline and speed
     * 3. Calc top3-average for speed
     * 4. Calc top3-average for ratio distance/beeline
     * 5. Calv top3*weight average for traveltime and distance
     * 6. Store intra-infos in the vector for speed and distance/beeline
     * 7. Store traveltime and distance in diagonal of the according matrix
     */
    public void generateIntraCellInfos(double weight) {
        intraBeeLineFactor = new double[this.distance.length];
        intraSpeed = new double[this.distance.length];

        int i, j, insert;

        final int length = 3;

        double[] tt = new double[length], speed = new double[length], at = new double[length], et = new double[length], dist = new double[length], bl = new double[length];
        double intraBLF, intraV, intraAT, intraET, intraTT, intraDist;


        for (i = 0; i < this.distance.length; ++i) {
            //init the mins
            for (j = 0; j < length; ++j) {
                tt[j] = 1e100;
                speed[j] = 1e100;
                at[j] = 1e100;
                et[j] = 1e100;
                dist[j] = 1e100;
                bl[j] = 1e100;
            }
            //find the mins
            for (j = 0; j < this.distance[0].length; ++j) {
                if (i != j) { //diagonal is zero!
                    if (isPT) {
                        checkMultiMin(this.accessTime[i][j], at); // calc three shortest access times
                        checkMultiMin(this.egressTime[i][j], et); // calc three shortest egress times
                    }
                    insert = checkMultiMin(this.distance[i][j], dist);
                    if (insert >= 0) {
                        insertMinAt(this.travelTime[i][j], tt, insert);
                        insertMinAt(this.beeline[i][j], bl, insert);
                        if (!isPT) insertMinAt(this.speed[i][j], speed, insert);
                    }
                }
            }
            // init the avg
            intraBLF = 0;
            intraV = 0;
            intraAT = 0;
            intraET = 0;
            intraTT = 0;
            intraDist = 0;
            //build average
            for (j = 0; j < length; ++j) {
                intraTT += tt[j];
                intraDist += dist[j];
                if (isPT) {
                    intraAT += at[j];
                    intraET += et[j];
                } else {
                    intraBLF += dist[j] / bl[j];
                    intraV += speed[j];
                }
            }
            intraTT *= weight / length; //intraTT is half the average to the n closest cells
            intraDist *= weight / length; //intraDist is half the average to the n closest cells
            this.travelTime[i][i] = intraTT;
            this.distance[i][i] = intraDist;

            if (isPT) {
                // access/egress times are not influenced by the cell-size: take the average of the three lowest times
                this.accessTime[i][i] = intraAT / length;
                this.egressTime[i][i] = intraET / length;
                intraSpeed[i] = intraDist / intraTT; //average speed
            } else {
                intraBeeLineFactor[i] = intraBLF / length;
                intraSpeed[i] = intraV / length;
            }
        }
    }

    /**
     * @return the accessTime
     */
    public double[][] getAccessTime() {
        return accessTime;
    }

    /**
     * @param accessTime the accessTime to set
     */
    public void setAccessTime(double[][] accessTime) {
        this.accessTime = accessTime;
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
     * @return the egressTime
     */
    public double[][] getEgressTime() {
        return egressTime;
    }


    /**
     * @param egressTime the egressTime to set
     */
    public void setEgressTime(double[][] egressTime) {
        this.egressTime = egressTime;
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
     * @return the travelTime
     */
    public double[][] getTravelTime() {
        return travelTime;
    }

    /**
     * @param travelTime the travelTime to set
     */
    public void setTravelTime(double[][] travelTime) {
        this.travelTime = travelTime;
    }

    /**
     * Internal method to insert a value at a specific position in the array. All succedion values are shifted one to the left. In C I would use the shift command ;)
     *
     * @param val      the new value
     * @param array    the array to insert into
     * @param position the position
     */
    private void insertMinAt(double val, double[] array, int position) {
        //shift all other elements one back
        if (array.length - 1 - position >= 0) System.arraycopy(array, position, array, position + 1,
                array.length - 1 - position);
        //copy new value
        array[position] = val;

    }

    /**
     * Method to merge the matrices according to the weight
     */
    public void mergeMatrices() {
        if (this.compareMatrix != null) {
            //check input
            if ((this.compareMatrix.length != 2 && !isPT) || (this.compareMatrix.length != 4 && isPT)) return;

            double[][] comp = null;
            //time
            if (TPS_Logger.isLogging(SeverenceLogLevel.INFO)) {
                TPS_Logger.log(SeverenceLogLevel.INFO, "Merging times with: " + this.compareMatrix[0]);
            }
            comp = this.readMatrixFromDB(this.compareMatrix[0]);
            if (comp != null) mergeMatrixPair(comp, this.travelTime);
            //dist
            if (TPS_Logger.isLogging(SeverenceLogLevel.INFO)) {
                TPS_Logger.log(SeverenceLogLevel.INFO, "Merging distances with: " + this.compareMatrix[1]);
            }
            comp = this.readMatrixFromDB(this.compareMatrix[1]);
            if (comp != null) mergeMatrixPair(comp, this.distance);
            if (isPT) {
                //access  time
                if (TPS_Logger.isLogging(SeverenceLogLevel.INFO)) {
                    TPS_Logger.log(SeverenceLogLevel.INFO, "Merging access times with: " + this.compareMatrix[2]);
                }
                comp = this.readMatrixFromDB(this.compareMatrix[2]);
                if (comp != null) mergeMatrixPair(comp, this.accessTime);
                //egress time
                if (TPS_Logger.isLogging(SeverenceLogLevel.INFO)) {
                    TPS_Logger.log(SeverenceLogLevel.INFO, "Merging egress times with: " + this.compareMatrix[3]);
                }
                comp = this.readMatrixFromDB(this.compareMatrix[3]);
                if (comp != null) mergeMatrixPair(comp, this.egressTime);
            }
        }
    }

    /**
     * This method merges the two matrices. The result is stored in "newVals".
     * Every value of the matrix is updated by: val = ( old_val+weight*new_val)/(1+weight)
     *
     * @param oldVals matrix containing the old values
     * @param newVals matrix containing the new values and return object
     */
    public void mergeMatrixPair(double[][] oldVals, double[][] newVals) {
        int i, j;
        double oldVal, newVal;
        double error, maxError = 0, minError = 1e100, avgError = 0;
        final int size = this.travelTime.length;
        final double normalizationFactor = 1.0 / (1.0 + this.weight);
        for (i = 0; i < size; ++i) {
            for (j = 0; j < size; ++j) {
                if (i != j) { //do not update the diagonal! these values are calculated separately!
                    oldVal = oldVals[i][j];
                    newVal = newVals[i][j];
                    error = oldVal - newVal;
                    if (newVal > 0.0) {//ignore invalid cells (no trafic)
                        maxError = Math.max(maxError, error);
                        minError = Math.min(minError, error);
                        avgError += error;
                        newVals[i][j] = (oldVal + this.weight * newVal) * normalizationFactor;
                    }
                }
            }
        }
        avgError /= size * size;
        if (TPS_Logger.isLogging(SeverenceLogLevel.INFO)) {
            TPS_Logger.log(SeverenceLogLevel.INFO,
                    "max error: " + maxError + " minError: " + minError + " avg error: " + avgError);
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
            int fromTVZ, toTVZ, tvzCounter = 0;
            tvzCounter = this.idToIndex.size();
            String delimiter = ";";
            boolean oFormat = false;
            if (tvzCounter == 0) {
                in = new FileReader(fileName);
                input = new BufferedReader(in);
                if (TPS_Logger.isLogging(SeverenceLogLevel.INFO)) {
                    TPS_Logger.log(SeverenceLogLevel.INFO, "File opened: " + fileName);
                }
                //read tvzs
                if (TPS_Logger.isLogging(SeverenceLogLevel.INFO)) {
                    TPS_Logger.log(SeverenceLogLevel.INFO, "Analyzing TVZ-IDs: ");
                }

                while ((line = input.readLine()) != null) {
                    if (line.startsWith("$") || line.startsWith("*")) { // comment or visum file-format
                        if (line.startsWith("$O;D")) {
                            delimiter = "  "; //O-Format hast two spaces as delimiter
                            oFormat = true;
                        }
                        if (line.startsWith("$NAMES")) //end of o-File reached
                            break;
                        continue;
                    }
                    StringTokenizer tok = new StringTokenizer(line.trim(), delimiter);
                    //check format
                    if (!((tok.countTokens() == 7 && isPT && !oFormat) ||
                            (tok.countTokens() == 6 && !isPT && !oFormat) || (tok.countTokens() == 3 && oFormat)))
                        continue;

                    //get from
                    fromTVZ = Integer.parseInt(tok.nextToken().trim());

                    if (idToIndex.get(fromTVZ) == null) {//new tvz
                        idToIndex.put(fromTVZ, tvzCounter);
                        indexToId.put(tvzCounter, fromTVZ);
                        tvzCounter++;
                    }

                    //get to
                    toTVZ = Integer.parseInt(tok.nextToken().trim());
                    if (idToIndex.get(toTVZ) == null) {//new tvz
                        idToIndex.put(toTVZ, tvzCounter);
                        indexToId.put(tvzCounter, toTVZ);
                        tvzCounter++;
                    }
                }
                if (TPS_Logger.isLogging(SeverenceLogLevel.INFO)) {
                    TPS_Logger.log(SeverenceLogLevel.INFO, "Found " + tvzCounter + " TVZ-IDs");
                }
                input.close();
                in.close();
            }


            //prepare arrays
            travelTime = new double[tvzCounter][tvzCounter];
            speed = new double[tvzCounter][tvzCounter];
            accessTime = new double[tvzCounter][tvzCounter];
            egressTime = new double[tvzCounter][tvzCounter];
            distance = new double[tvzCounter][tvzCounter];
            beeline = new double[tvzCounter][tvzCounter];

            //new local variables
            double[] tt = new double[3], v = new double[3], at = new double[3], et = new double[3], dist = new double[3], bl = new double[3];
            int indexFrom, indexTo;
            //open input
            in = new FileReader(fileName);
            input = new BufferedReader(in);
            if (TPS_Logger.isLogging(SeverenceLogLevel.INFO)) {
                TPS_Logger.log(SeverenceLogLevel.INFO, "File opened: " + fileName);
            }
            tt[1] = v[1] = at[1] = et[1] = dist[1] = bl[1] = 99999;
            tt[2] = v[2] = at[1] = et[2] = dist[2] = bl[2] = 0;
            while ((line = input.readLine()) != null) {
                if (line.startsWith("$") || line.startsWith("*")) { // comment or visum file-format
                    if (line.startsWith("$O;D")) {
                        delimiter = "  "; //O-Format hast two spaces as delimiter
                        oFormat = true;
                        speed = null;
                        accessTime = null;
                        egressTime = null;
                        distance = null;
                        beeline = null;

                    }
                    if (line.startsWith("$NAMES")) //end of o-File reached
                        break;
                    continue;
                }
                StringTokenizer tok = new StringTokenizer(line.trim(), delimiter);
                //check format
                if (!((tok.countTokens() == 7 && isPT && !oFormat) || (tok.countTokens() == 6 && !isPT && !oFormat) ||
                        (tok.countTokens() == 3 && oFormat))) continue;

                //get from
                fromTVZ = Integer.parseInt(tok.nextToken().trim());
                indexFrom = idToIndex.get(fromTVZ);

                //get to
                toTVZ = Integer.parseInt(tok.nextToken().trim());
                indexTo = idToIndex.get(toTVZ);
                if (isPT) {
                    //get acc t
                    at[0] = Double.parseDouble(tok.nextToken().trim());
                    // get bl
                    bl[0] = Double.parseDouble(tok.nextToken().trim());
                    //get egr t
                    et[0] = Double.parseDouble(tok.nextToken().trim());
                    //get travel dist
                    dist[0] = Double.parseDouble(tok.nextToken().trim());
                    //get tt
                    tt[0] = Double.parseDouble(tok.nextToken().trim());

                    //incl check
                    setValue(travelTime, indexFrom, indexTo, tt[0] != 999999.0 ? tt[0] * 60.0 : -1, true);
                    setValue(accessTime, indexFrom, indexTo, at[0] != 999999.0 ? at[0] * 60.0 : -1, true);
                    setValue(egressTime, indexFrom, indexTo, et[0] != 999999.0 ? et[0] * 60.0 : -1, true);
                    setValue(distance, indexFrom, indexTo, dist[0] != 999999.0 ? dist[0] * 1000.0 : 999999, true);
                    setValue(beeline, indexFrom, indexTo, bl[0] != 999999.0 ? bl[0] * 1000.0 : 999999, true);
                    if (indexFrom != indexTo) {
                        calcMinMaxValue(tt);
                        calcMinMaxValue(at);
                        calcMinMaxValue(et);
                        calcMinMaxValue(dist);
                        calcMinMaxValue(bl);
                    }
                } else {
                    //get tt
                    tt[0] = Double.parseDouble(tok.nextToken().trim());
                    setValue(travelTime, indexFrom, indexTo, tt[0] != 999999.0 ? tt[0] * 60.0 : -1, true);
                    if (!oFormat) {
                        v[0] = Double.parseDouble(tok.nextToken().trim());
                        dist[0] = Double.parseDouble(tok.nextToken().trim());
                        bl[0] = Double.parseDouble(tok.nextToken().trim());
                        setValue(speed, indexFrom, indexTo, v[0] != 999999.0 ? v[0] * 60.0 : -1, true);
                        setValue(distance, indexFrom, indexTo, dist[0] != 999999.0 ? dist[0] * 1000.0 : 999999, true);
                        setValue(beeline, indexFrom, indexTo, bl[0] != 999999.0 ? bl[0] * 1000.0 : 999999, true);
                    }

                    if (indexFrom != indexTo) {
                        calcMinMaxValue(tt);
                        if (!oFormat) {
                            calcMinMaxValue(v);
                            calcMinMaxValue(dist);
                            calcMinMaxValue(bl);
                        }
                    }
                }
            }
            if (TPS_Logger.isLogging(SeverenceLogLevel.INFO)) {
                TPS_Logger.log(SeverenceLogLevel.INFO, "travelTime	min/max: " + tt[1] + " " + tt[2]);
                if (isPT) {
                    TPS_Logger.log(SeverenceLogLevel.INFO, "accessTime	min/max: " + at[1] + " " + at[2]);
                    TPS_Logger.log(SeverenceLogLevel.INFO, "egressTime	min/max: " + et[1] + " " + et[2]);
                } else {
                    TPS_Logger.log(SeverenceLogLevel.INFO, "speed		min/max: " + v[1] + " " + v[2]);
                }
                TPS_Logger.log(SeverenceLogLevel.INFO, "distance	min/max: " + dist[1] + " " + dist[2]);
                TPS_Logger.log(SeverenceLogLevel.INFO, "Beeline		min/max: " + bl[1] + " " + bl[2]);
            }
        } finally {
            try {
                if (input != null) input.close();
                if (in != null) in.close();
            }//try
            catch (IOException ex) {
                TPS_Logger.log(SeverenceLogLevel.ERROR, " Could not close : " + fileName);
                throw new IOException(ex);
            }//catch
        }//finally
    }

    public void readIDMap() throws SQLException {
        String query = "";
        ResultSet rs = null;
        int taz, externalId;
        try {
            query = "SELECT taz_id, taz_num_id FROM " + this.parameterClass.getString(ParamString.DB_TABLE_TAZ);
            rs = dbCon.executeQuery(query, this);
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
            if (TPS_Logger.isLogging(SeverenceLogLevel.INFO)) {
                TPS_Logger.log(SeverenceLogLevel.INFO, "Found " + this.idToIndex.size() + " TVZ-IDs");
            }
        } catch (SQLException e) {
            TPS_Logger.log(SeverenceLogLevel.ERROR, "SQL error! Query: " + query, e);
            throw new SQLException("SQL error! Query: " + query, e);
        }
    }

    private void readMatrix(ParamString matrixName, ParamMatrixMap matrix, SimulationType simType, int sIndex) {
        String query = "";
        ResultSet rs = null;
        try {
            query = "SELECT \"matrixMap_num\", \"matrixMap_matrixNames\",  \"matrixMap_distribution\"  FROM " +
                    this.parameterClass.getString(ParamString.DB_TABLE_MATRIXMAPS) + " WHERE \"matrixMap_name\"='" +
                    this.parameterClass.getString(matrixName) + "'";
            rs = dbCon.executeQuery(query, this);

            if (rs.next()) {
                // get number of matrices to load
                int numOfMatrices = rs.getInt("matrixMap_num");
                // get matrix names
                String[] matrix_names = TPS_DB_IO.extractStringArray(rs, "matrixMap_matrixNames");
                // get distribution
                double[] distribution = TPS_DB_IO.extractDoubleArray(rs, "matrixMap_distribution");
                rs.close();
                // check sizes
                if (numOfMatrices != matrix_names.length || numOfMatrices != distribution.length) {
                    TPS_Logger.log(SeverenceLogLevel.FATAL,
                            "Couldn't load matrixmap " + this.parameterClass.getString(matrixName) +
                                    " from database. Different array sizes (num, matrices, distribution): " +
                                    numOfMatrices + " " + matrix_names.length + " " + distribution.length +
                                    " SQL query: " + query);
                }

                // init matrix map
                Matrix[] matrices = new Matrix[numOfMatrices];

                // load matrix map
                for (int i = 0; i < numOfMatrices; ++i) {
                    query = "SELECT matrix_values FROM " + this.parameterClass.getString(
                            ParamString.DB_TABLE_MATRICES) + " WHERE matrix_name='" + matrix_names[i] + "'";
                    rs = dbCon.executeQuery(query, this);
                    if (rs.next()) {
                        int[] iArray = TPS_DB_IO.extractIntArray(rs, "matrix_values");
                        int len = (int) Math.sqrt(iArray.length);
                        matrices[i] = new Matrix(len, len, sIndex);
                        for (int index = 0; index < iArray.length; index++) {
                            matrices[i].setRawValue(index, iArray[index]);
                        }
                        rs.close();
                    } else {
                        TPS_Logger.log(SeverenceLogLevel.FATAL,
                                "Couldn't load matrix " + matrix_names[i] + " form matrix map" +
                                        this.parameterClass.getString(matrixName) + ": No such matrix. SQL Query: " +
                                        query);
                        return;
                    }
                }

                // set collected data
                if (simType != null) this.parameterClass.paramMatrixMapClass.setMatrixMap(matrix, distribution,
                        matrices, simType);
                else this.parameterClass.paramMatrixMapClass.setMatrixMap(matrix, distribution, matrices);
            }
        } catch (SQLException e) {
            TPS_Logger.log(SeverenceLogLevel.FATAL,
                    "Couldn't load matrixmap " + this.parameterClass.getString(matrixName) +
                            " from database: No such entry. SQL Query: " + query, e);
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
        String query = "SELECT * FROM " + this.parameterClass.getString(ParamString.DB_TABLE_MATRICES) +
                " WHERE \"matrix_name\" = '" + matrixName + "'";
        try {
            ResultSet rs = dbCon.executeQuery(query, this);
            if (rs.next()) {
                Object array = rs.getArray("matrix_values").getArray();
                if (array instanceof Integer[]) {
                    //parse data to memory model
                    Integer[] matrixVal = (Integer[]) array;
                    int size = (int) Math.sqrt(matrixVal.length);
                    if (size != this.travelTime.length) {
                        TPS_Logger.log(SeverenceLogLevel.ERROR,
                                "Matrix " + matrixName + " has a different size: " + size + " Expected: " +
                                        this.travelTime.length);
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
                TPS_Logger.log(SeverenceLogLevel.ERROR, "Matrix " + matrixName + " does not exist!");
            }
            rs.close();
        } catch (SQLException e) {
            TPS_Logger.log(SeverenceLogLevel.ERROR, "SQL Error: " + query, e);
        }
        return returnVal;
    }

    /**
     * Sets the array of matrixnames for comparison.
     * Order for IV:
     * travelTime
     * distance
     * <p>
     * Order for PT:
     * travelTime
     * distance
     * accessTime
     * egressTime
     *
     * @param compareMatrix The array of matrix names in the db.
     */
    public void setCompareMatrix(String[] compareMatrix) {
        this.compareMatrix = compareMatrix;
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
     * Internal method to insert data in (symmetric) matrices
     *
     * @param array     Reference to the array to store
     * @param x         First coordinate
     * @param y         Second coordinate
     * @param val       The value to store
     * @param symmetric Is the Matrix symmetric? Yes: store array[y][x], too.
     */
    private void setValue(double[][] array, int x, int y, double val, boolean symmetric) {
        array[x][y] = val;
        if (x < y && symmetric) array[y][x] = val;
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
     * this method stores the intra infos.
     *
     * @param name name for the entry.
     * @return true if successful
     */
    public boolean storeIntraInfos(String name) {
        String tableName;
        if (this.isPT) tableName = this.parameterClass.getString(ParamString.DB_TABLE_TAZ_INTRA_PT_INFOS);
        else tableName = this.parameterClass.getString(ParamString.DB_TABLE_TAZ_INTRA_MIT_INFOS);
        String query;
        if (checkIntraInfos(name)) {
            //update
            if (TPS_Logger.isLogging(SeverenceLogLevel.INFO)) {
                TPS_Logger.log(SeverenceLogLevel.INFO,
                        "Updating data for entry: " + name + " in table " + tableName + ". ");
            }
            for (int i = 0; i < this.intraSpeed.length; ++i) {
                if (this.isPT) query = "UPDATE " + tableName + " SET average_speed_pt =" + this.intraSpeed[i] +
                        " WHERE info_taz_id = " + (i + 1) + " AND info_name = '" + name + "'";
                else query = "UPDATE " + tableName + " SET average_speed_mit =" + this.intraSpeed[i] +
                        ",beeline_factor_mit = " + this.intraBeeLineFactor[i] + " WHERE info_taz_id = " + (i + 1) +
                        " AND info_name = '" + name + "'";
                dbCon.execute(query, this);
            }

        } else {
            if (TPS_Logger.isLogging(SeverenceLogLevel.INFO)) {
                TPS_Logger.log(SeverenceLogLevel.INFO,
                        "Inserting data for entry: " + name + " in table " + tableName + ". ");
            }
            for (int i = 0; i < this.intraSpeed.length; ++i) {
                if (this.isPT)
                    query = "INSERT INTO " + tableName + " (info_taz_id, average_speed_pt, info_name) VALUES (" +
                            (i + 1) + "," + this.intraSpeed[i] + ",'" + name + "')";
                else query = "INSERT INTO " + tableName +
                        " (info_taz_id, beeline_factor_mit, average_speed_mit, info_name ) VALUES (" + (i + 1) + "," +
                        this.intraBeeLineFactor[i] + "," + this.intraSpeed[i] + ",'" + name + "')";
                dbCon.execute(query, this);
            }
        }
        if (TPS_Logger.isLogging(SeverenceLogLevel.INFO)) {
            TPS_Logger.log(SeverenceLogLevel.INFO, " Successful!");
        }
        return true;
    }

    /**
     * This method exports the trips to VISUM OD-Matrices
     *
     * @param outputPath the path to store the OD-matrix. usually Trip-Exports
     */
    public void writeODMatrices(String outputPath) throws SQLException, IOException {
        this.readMatrix(ParamString.DB_NAME_MATRIX_TT_MIT, ParamMatrixMap.TRAVEL_TIME_MIT, SimulationType.SCENARIO, 0);
        MatrixMap timeDistribution = this.parameterClass.paramMatrixMapClass.getMatrixMap(
                ParamMatrixMap.TRAVEL_TIME_MIT, SimulationType.SCENARIO);

        int[] slices = new int[timeDistribution.matrices.length];
        for (int j = 0; j < slices.length; ++j) {
            slices[j] = timeDistribution.matrices[j].value / 60; //seconds to minutes
        }
        String query = "";
        try {
            ResultSet rs = null;
            query = "SELECT sim_finished FROM " + this.parameterClass.getString(ParamString.DB_TABLE_SIMULATIONS) +
                    " WHERE sim_key = '" + this.parameterClass.getString(ParamString.RUN_IDENTIFIER) + "'";
            rs = dbCon.executeQuery(query, this);

            // check if sim exists
            if (!rs.next()) {
                rs.close();
                return;
            }
            // check if sim is finished
            if (!rs.getBoolean("sim_finished")) {
                rs.close();
                return;
            }

            double sampleFactor = 1.0 / this.parameterClass.paramValueClass.getDoubleValue(
                    ParamValue.DB_HH_SAMPLE_SIZE);
            int numTAZ = this.indexToId.size();
            int numModes = TPS_Mode.MODE_TYPE_ARRAY.length;
            int thisTime, lastTime = 0;
            double[][] ODMatrix = new double[numTAZ][numTAZ];
            int i, j, k;
            String filename;

            File outputDir = new File(
                    outputPath + this.parameterClass.getString(ParamString.DB_TABLE_TRIPS) + "_iter_" +
                            this.parameterClass.paramValueClass.getIntValue(ParamValue.ITERATION));
            if (!outputDir.exists()) {
                if (!outputDir.mkdir()) {
                    TPS_Logger.log(SeverenceLogLevel.FATAL,
                            "Exception during VISUM export! cannot create output dir: " + outputDir.getAbsolutePath());
                    return;
                }
            }
            //TODO is this linkedlist necessary? look at unused/VisumJob
            LinkedList<String> tapasMatricesWithPaths = new LinkedList<>();
            for (int slice : slices) {
                thisTime = slice;

                // get all trips for this mode in this timeslice
                for (i = 0; i < numModes; ++i) {
                    // init ODmatrix
                    for (j = 0; j < numTAZ; ++j) {
                        for (k = 0; k < numTAZ; ++k) {
                            ODMatrix[j][k] = 0;
                        }
                    }

                    query = "SELECT taz_id_start, taz_id_end FROM " + this.parameterClass.getString(
                            ParamString.DB_TABLE_TRIPS) + " WHERE mode=" + i + " AND start_time_min<" + thisTime +
                            " AND start_time_min>=" + lastTime;
                    rs = dbCon.executeQuery(query, this);
                    while (rs.next()) {
                        j = rs.getInt("taz_id_start") - 1;
                        k = rs.getInt("taz_id_end") - 1;
                        ODMatrix[j][k] += sampleFactor;
                    }
                    rs.close();
                    // create filename
                    filename = "Mode_" + i + "_" + thisTime + ".mtx";
                    File outPutFile = new File(outputDir, filename);
                    tapasMatricesWithPaths.add(outPutFile.getPath());
                    // open file
                    FileWriter writer = new FileWriter(outPutFile);
                    if (TPS_Logger.isLogging(SeverenceLogLevel.INFO)) {
                        TPS_Logger.log(SeverenceLogLevel.INFO,
                                "Writing O/D matrices for mode " + i + ". Timeslot from " + (lastTime / 60) + "h to " +
                                        (thisTime / 60) + "h to file: " + outPutFile.getAbsolutePath());
                    }
                    // header
                    writer.append("$O;D3\n");
                    writer.append("* Von  Bis\n");
                    // append time
                    writer.append(lastTime / 60 + ".");
                    if (lastTime % 60 < 10) writer.append("0");
                    writer.append(lastTime % 60 + "\t" + thisTime / 60 + ".");
                    if (thisTime % 60 < 10) writer.append("0");
                    writer.append(thisTime % 60 + "\n");
                    // scale factor
                    writer.append("* Faktor\n1.0\n");
                    // writer.append(Double.toString(sampleFactor)+"\n");
                    writer.append("*\n");
                    writer.append("* DLR\n");
                    writer.append("* ");
                    Calendar now = Calendar.getInstance();

                    writer.append(now.get(Calendar.DAY_OF_MONTH) + ".");
                    if (now.get(Calendar.MONTH) < 8) {
                        writer.append("0");
                    }
                    writer.append(Integer.toString(now.get(Calendar.MONTH) + 1) + now.get(Calendar.YEAR) + "\n");
                    // data
                    int from, to;
                    for (j = 0; j < numTAZ; ++j) {
                        from = this.indexToId.get(j);
                        for (k = 0; k < numTAZ; ++k) {
                            to = this.indexToId.get(k);
                            writer.append(" " + from + "  " + to + " " + (int) ODMatrix[j][k] + ".000\n");
                        }
                    }
                    // close file
                    writer.close();
                }
                // update for next loop
                lastTime = thisTime;
            }

        } catch (SQLException e) {
            TPS_Logger.log(SeverenceLogLevel.FATAL, "Exception during VISUM export! Error reading SQL! Query:" + query,
                    e);
            throw new SQLException("Exception during VISUM export! Error reading SQL! Query:" + query, e);
        } catch (IOException e) {
            TPS_Logger.log(SeverenceLogLevel.FATAL, "Exception during VISUM export! Error writing cvs:", e);
            throw new IOException("Exception during VISUM export! Error reading SQL! Query:" + query, e);
        }
    }
}
