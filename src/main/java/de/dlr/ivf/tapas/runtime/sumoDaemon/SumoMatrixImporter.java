/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.runtime.sumoDaemon;

import de.dlr.ivf.tapas.iteration.TPS_SumoConverter;
import de.dlr.ivf.tapas.log.TPS_Logger;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface.SeverenceLogLevel;
import de.dlr.ivf.tapas.persistence.db.TPS_DB_IO;
import de.dlr.ivf.tapas.tools.persitence.db.TPS_BasicConnectionClass;
import de.dlr.ivf.tapas.util.Matrix;
import de.dlr.ivf.tapas.util.TPS_Geometrics;
import de.dlr.ivf.tapas.util.parameters.ParamString;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;


/**
 * Class to read the SUMO traveltime and distance matrix and store them in TAPAS format
 *
 * @author hein_mh
 */
public class SumoMatrixImporter extends TPS_BasicConnectionClass {

    /**
     * A reference to the existing converter, which provides several helper functions
     */
    TPS_SumoConverter converter;
    /**
     * the matrices
     */
    Matrix travelTime = null, distance = null;
    Map<Integer, Integer> tazMap = new HashMap<>();
    /**
     * Constructor for this class.
     *
     * @param loginInfo Filename containing the db-login
     */
    public SumoMatrixImporter(String loginInfo, String configInfo) {
        super(loginInfo);
        converter = new TPS_SumoConverter(this.dbCon);
        try {
            this.parameterClass.loadSingleParameterFile(new File(configInfo));
            //reload the config to ensure we have the admin passwords!
            this.parameterClass.loadSingleParameterFile(new File(loginInfo));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param args
     */
    public static void main(String[] args) {

        SumoMatrixImporter worker = new SumoMatrixImporter("T:\\Simulationen\\runtime_perseus.csv",
                "T:\\Simulationen\\Berlin\\DB_Test\\full_test_perseus\\db_Run_full_new.csv");

        worker.readSumoValues("temp", "sumo_od_empty_net");
        worker.writeMatrix(worker.travelTime, "SUMO_1193_MIV_TT_T0_20151026");
        worker.writeMatrix(worker.distance, "SUMO_1193_dist_T0_20151026");


    }

    /**
     * Method to read the values from a given table. It determines the taz-structure, initializes the output matrices and fills them with values.
     *
     * @param schema the schema where sumo stores the values , usualy "temp"
     * @param table  the tablename to read
     */
    public void readSumoValues(String schema, String table) {
        String query = "";
        try {
            String tableName = schema + "." + table;
            double tt, dist;
            ResultSet rs;
            //read min and max taz values

            query = "SELECT taz_num_id from " + this.parameterClass.getString(ParamString.DB_SCHEMA_CORE) +
                    this.parameterClass.getString(ParamString.DB_TABLE_TAZ) + " order by taz_num_id asc";
            rs = dbCon.executeQuery(query, this);
            int id = 0;
            while (rs.next()) {
                this.tazMap.put(rs.getInt("taz_num_id"), id);
                id++;
            }
            rs.close();


            int numOfTAZ = id;
            //init the matrices
            travelTime = new Matrix(numOfTAZ, numOfTAZ);
            distance = new Matrix(numOfTAZ, numOfTAZ);

            //get the values
            query = "SELECT taz_id_start, taz_id_end, travel_time_sec, distance_real from " + tableName;

            int from, to;
            rs = dbCon.executeQuery(query, this);
            while (rs.next()) {
                from = this.tazMap.get(rs.getInt("taz_id_start"));
                to = this.tazMap.get(rs.getInt("taz_id_end"));
                tt = rs.getDouble("travel_time_sec");
                dist = rs.getDouble("distance_real");

                travelTime.setValue(from, to, Math.round(tt));
                distance.setValue(from, to, Math.round(dist));
            }

            double[][] valuesTT = new double[numOfTAZ][numOfTAZ];
            double[][] valuesDist = new double[numOfTAZ][numOfTAZ];
            int i, j;
            for (i = 0; i < numOfTAZ; ++i) {
                for (j = 0; j < numOfTAZ; ++j) {
                    valuesTT[i][j] = travelTime.getValue(i, j);
                    valuesDist[i][j] = distance.getValue(i, j);

                }
            }


            TPS_Geometrics.calcTop3(valuesTT);
            TPS_Geometrics.calcTop3(valuesDist);

            //fill in diagonal
            for (i = 0; i < numOfTAZ; ++i) {
                for (j = 0; j < numOfTAZ; ++j) {
                    if (i == j) {
                        travelTime.setValue(i, j, valuesTT[i][j]);
                        distance.setValue(i, j, valuesDist[i][j]);
                    }
                }
            }


        } catch (SQLException e) {
            System.err.println(
                    this.getClass().getCanonicalName() + " readSumoValues: SQL-Error during statement: " + query);
            e.printStackTrace();
        }

    }

    public void writeMatrix(Matrix matrix, String matrixName) {
        //load the data from the db
        String query = "SELECT * FROM " + this.parameterClass.getString(ParamString.DB_SCHEMA_CORE) +
                this.parameterClass.getString(ParamString.DB_TABLE_MATRICES) + " WHERE \"matrix_name\" = '" +
                matrixName + "'";
        if (TPS_Logger.isLogging(SeverenceLogLevel.INFO)) {
            TPS_Logger.log(SeverenceLogLevel.INFO, "Preparing data for entry: " + matrixName + " in table " +
                    this.parameterClass.getString(ParamString.DB_TABLE_MATRICES));
        }
        if (converter.checkMatrixName(matrixName)) {
            //update!
            query = "UPDATE " + this.parameterClass.getString(ParamString.DB_SCHEMA_CORE) +
                    this.parameterClass.getString(ParamString.DB_TABLE_MATRICES) + " SET matrix_values = ";
            query += TPS_DB_IO.matrixToSQLArray(matrix, 0) + " WHERE \"matrix_name\" = '" + matrixName + "'";
            if (TPS_Logger.isLogging(SeverenceLogLevel.INFO)) {
                TPS_Logger.log(SeverenceLogLevel.INFO, "Updating data for entry: " + matrixName + " in table " +
                        this.parameterClass.getString(ParamString.DB_TABLE_MATRICES) + ".");
            }
        } else {
            query = "INSERT INTO " + this.parameterClass.getString(ParamString.DB_SCHEMA_CORE) +
                    this.parameterClass.getString(ParamString.DB_TABLE_MATRICES) +
                    " (matrix_name, matrix_values) VALUES ('" + matrixName + "', ";
            query += TPS_DB_IO.matrixToSQLArray(matrix, 0) + ")";
            if (TPS_Logger.isLogging(SeverenceLogLevel.INFO)) {
                TPS_Logger.log(SeverenceLogLevel.INFO, "Inserting data for entry: " + matrixName + " in table " +
                        this.parameterClass.getString(ParamString.DB_TABLE_MATRICES) + ".");
            }
        }
        writeStringToFile(query, "C:\\temp\\sumo-" + matrixName + ".sql");
        dbCon.execute(query, this);
        if (TPS_Logger.isLogging(SeverenceLogLevel.INFO)) {
            TPS_Logger.log(SeverenceLogLevel.INFO, "Successful!");
        }
    }

    public void writeStringToFile(String input, String file) {
        try {
            FileWriter writer = new FileWriter(file);
            writer.append(input);
            writer.flush();
            writer.close();

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


}
