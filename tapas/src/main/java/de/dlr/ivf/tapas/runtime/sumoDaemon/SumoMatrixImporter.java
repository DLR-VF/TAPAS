/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.runtime.sumoDaemon;

import de.dlr.ivf.tapas.iteration.TPS_SumoConverter;
import de.dlr.ivf.tapas.logger.TPS_Logger;
import de.dlr.ivf.tapas.logger.SeverityLogLevel;
import de.dlr.ivf.tapas.persistence.db.TPS_DB_IO;
import de.dlr.ivf.tapas.tools.persitence.db.TPS_BasicConnectionClass;
import de.dlr.ivf.tapas.model.Matrix;
import de.dlr.ivf.tapas.tools.TPS_Geometrics;
import de.dlr.ivf.tapas.parameter.ParamString;

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

        SumoMatrixImporter worker = new SumoMatrixImporter("T:\\Simulationen\\runtime_athene_admin.csv",
                "T:\\Simulationen\\DB_Produktiv\\Berlin\\Urmo Digital\\db_Run_full_new.csv");

        worker.readSumoValues("temp", "sumo_od_entry_2021y_06m_04d_11h_04m_34s_855ms_car", "sumo_od_2021y_06m_04d_11h_04m_34s_855ms_car",86400);
        worker.writeMatrix(worker.travelTime, "SUMO_1223_MIV_TT_T0_20211005");
        worker.writeMatrix(worker.distance, "SUMO_1223_DIST_T0_20211005");


    }

    /**
     * Method to read the values from a given table. It determines the taz-structure, initializes the output matrices and fills them with values.
     *
     * @param schema the schema where sumo stores the values , usualy "temp"
     * @param tableODEntries  the tablename to read
     */
    public void readSumoValues(String schema, String tableODEntries, String relationEntries, double intervalEnd) {
        String query = "";
        try {
            String tableNameODEntries = schema + "." + tableODEntries;
            String tableNameRelations = schema + "." + relationEntries;
            double[] tt, dist;
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
            int chunks = 100;
            for( int i=0; i< chunks; i++){
                query = "SELECT taz_id_start, taz_id_end, travel_time_sec, distance_real " +
                        "FROM " + tableNameODEntries + " AS od " +
                        "JOIN " + tableNameRelations + " AS rel on od.entry_id = rel.entry_id " +
                        "WHERE interval_end = " + intervalEnd + " AND od.entry_id%" + chunks + " = " + i;

                int from, to;
                rs = dbCon.executeQuery(query, this);
                while (rs.next()) {
                    from = this.tazMap.get(rs.getInt("taz_id_start"));
                    to = this.tazMap.get(rs.getInt("taz_id_end"));
                    tt = TPS_DB_IO.extractDoubleArray(rs, "travel_time_sec");
                    dist = TPS_DB_IO.extractDoubleArray(rs, "distance_real");
                    if(tt != null  && tt.length == 3 && dist != null && dist.length == 3) {
                        travelTime.setValue(from, to, Math.round(tt[2]));
                        distance.setValue(from, to, Math.round(dist[2]));
                    }
                    else{
                        System.err.println(
                                this.getClass().getCanonicalName() + " readSumoValues: unexpected db entry: F: "
                                        + from + " T: "+to +" TT: "+tt+" dist: " + dist);
                    }
                }
                rs.close();
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
        if (TPS_Logger.isLogging(SeverityLogLevel.INFO)) {
            TPS_Logger.log(SeverityLogLevel.INFO, "Preparing data for entry: " + matrixName + " in table " +
                    this.parameterClass.getString(ParamString.DB_TABLE_MATRICES));
        }
        if (converter.checkMatrixName(matrixName)) {
            //update!
            query = "UPDATE " + this.parameterClass.getString(ParamString.DB_SCHEMA_CORE) +
                    this.parameterClass.getString(ParamString.DB_TABLE_MATRICES) + " SET matrix_values = ";
            query += TPS_DB_IO.matrixToSQLArray(matrix, 0) + " WHERE \"matrix_name\" = '" + matrixName + "'";
            if (TPS_Logger.isLogging(SeverityLogLevel.INFO)) {
                TPS_Logger.log(SeverityLogLevel.INFO, "Updating data for entry: " + matrixName + " in table " +
                        this.parameterClass.getString(ParamString.DB_TABLE_MATRICES) + ".");
            }
        } else {
            query = "INSERT INTO " + this.parameterClass.getString(ParamString.DB_SCHEMA_CORE) +
                    this.parameterClass.getString(ParamString.DB_TABLE_MATRICES) +
                    " (matrix_name, matrix_values) VALUES ('" + matrixName + "', ";
            query += TPS_DB_IO.matrixToSQLArray(matrix, 0) + ")";
            if (TPS_Logger.isLogging(SeverityLogLevel.INFO)) {
                TPS_Logger.log(SeverityLogLevel.INFO, "Inserting data for entry: " + matrixName + " in table " +
                        this.parameterClass.getString(ParamString.DB_TABLE_MATRICES) + ".");
            }
        }
        writeStringToFile(query, "C:\\temp\\sumo-" + matrixName + ".sql");
        dbCon.execute(query, this);
        if (TPS_Logger.isLogging(SeverityLogLevel.INFO)) {
            TPS_Logger.log(SeverityLogLevel.INFO, "Successful!");
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
