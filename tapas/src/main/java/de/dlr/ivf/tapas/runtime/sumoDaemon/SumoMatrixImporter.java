/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.runtime.sumoDaemon;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.dlr.ivf.api.io.util.SqlArrayUtils;
import de.dlr.ivf.tapas.misc.Helpers;
import de.dlr.ivf.api.io.JdbcConnectionProvider;
import de.dlr.ivf.tapas.iteration.TPS_SumoConverter;
import de.dlr.ivf.tapas.logger.TPS_Logger;
import de.dlr.ivf.tapas.logger.SeverityLogLevel;
import de.dlr.ivf.tapas.model.Matrix;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;


/**
 * Class to read the SUMO traveltime and distance matrix and store them in TAPAS format
 *
 * @author hein_mh
 */
public class SumoMatrixImporter {

    private final SumoMatrixImporterConfig config;
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
     */
    public SumoMatrixImporter(SumoMatrixImporterConfig config) {
        this.config = config;
    }

    /**
     * @param args
     */
    public static void main(String[] args) throws IOException {
        Path configFile = Paths.get(args[0]);
        if (!Files.isRegularFile(configFile))
            throw new IllegalArgumentException("The provided argument is not a file.");

        SumoMatrixImporterConfig config = new ObjectMapper().readValue(configFile.toFile(), SumoMatrixImporterConfig.class);

        SumoMatrixImporter worker = new SumoMatrixImporter(config);
                //new SumoMatrixImporter("T:\\Simulationen\\runtime_athene_admin.csv",
                //"T:\\Simulationen\\DB_Produktiv\\Berlin\\Urmo Digital\\db_Run_full_new.csv");

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

            String tableNameODEntries = schema + "." + tableODEntries;
            String tableNameRelations = schema + "." + relationEntries;
            double[] tt, dist;

        Supplier<Connection> connectionSupplier = () -> JdbcConnectionProvider.newJdbcConnectionProvider().get(config.getConnectionDetails());

        try (Connection connection = connectionSupplier.get()){

            int id = 0;

            //read min and max taz values
            query = "SELECT taz_num_id from " + config.getTazTable().getUri() + " order by taz_num_id asc";

            try(PreparedStatement st = connection.prepareStatement(query);
                ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    this.tazMap.put(rs.getInt("taz_num_id"), id);
                    id++;
                }
            }catch (SQLException e){
                e.printStackTrace();
            }


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

                try(PreparedStatement st = connection.prepareStatement(query);
                    ResultSet rs = st.executeQuery()){
                    while (rs.next()) {
                        from = this.tazMap.get(rs.getInt("taz_id_start"));
                        to = this.tazMap.get(rs.getInt("taz_id_end"));
                        tt = SqlArrayUtils.extractDoubleArray(rs, "travel_time_sec");
                        dist = SqlArrayUtils.extractDoubleArray(rs, "distance_real");
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
                }catch (SQLException e){
                    e.printStackTrace();
                }
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

//todo revise this
//            TPS_Geometrics.calcTop3(valuesTT);
//            TPS_Geometrics.calcTop3(valuesDist);

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

        Supplier<Connection> connectionSupplier = () -> JdbcConnectionProvider.newJdbcConnectionProvider().get(config.getConnectionDetails());
        //load the data from the db
        String query = "SELECT * FROM " + config.getMatricesTable().getUri() + " WHERE \"matrix_name\" = '" +
                matrixName + "'";
        if (TPS_Logger.isLogging(SeverityLogLevel.INFO)) {
            TPS_Logger.log(SeverityLogLevel.INFO, "Preparing data for entry: " + matrixName + " in table " +
                    config.getMatricesTable().getUri());
        }
        if (converter.checkMatrixName(matrixName)) {
            //update!
            query = "UPDATE " + config.getMatricesTable().getUri() + " SET matrix_values = ";
            query += Helpers.matrixToSQLArray(matrix, 0) + " WHERE \"matrix_name\" = '" + matrixName + "'";
            if (TPS_Logger.isLogging(SeverityLogLevel.INFO)) {
                TPS_Logger.log(SeverityLogLevel.INFO, "Updating data for entry: " + matrixName + " in table " +
                        config.getMatricesTable().getUri() + ".");
            }
        } else {
            query = "INSERT INTO " + config.getMatricesTable().getUri() +
                    " (matrix_name, matrix_values) VALUES ('" + matrixName + "', ";
            query += Helpers.matrixToSQLArray(matrix, 0) + ")";
            if (TPS_Logger.isLogging(SeverityLogLevel.INFO)) {
                TPS_Logger.log(SeverityLogLevel.INFO, "Inserting data for entry: " + matrixName + " in table " +
                        config.getMatricesTable().getUri() + ".");
            }
        }
        writeStringToFile(query, "C:\\temp\\sumo-" + matrixName + ".sql");
        try(Connection connection = connectionSupplier.get();
            Statement st = connection.createStatement()){
            st.execute(query);
        }catch (SQLException e){
            e.printStackTrace();
        }
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
