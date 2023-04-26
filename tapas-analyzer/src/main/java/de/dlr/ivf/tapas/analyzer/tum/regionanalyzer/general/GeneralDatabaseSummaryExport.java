/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.analyzer.tum.regionanalyzer.general;

import de.dlr.ivf.tapas.analyzer.inputfileconverter.TapasTrip;
import de.dlr.ivf.tapas.analyzer.tum.constants.CategoryCombination;
import de.dlr.ivf.tapas.analyzer.tum.constants.TuMEnums.*;
import de.dlr.ivf.tapas.analyzer.tum.databaseConnector.DBTripReader;
import de.dlr.ivf.tapas.analyzer.tum.regionanalyzer.Analyzer;
import de.dlr.ivf.tapas.persistence.db.TPS_DB_Connector;
import de.dlr.ivf.tapas.tools.persitence.db.TPS_BasicConnectionClass;
import de.dlr.ivf.tapas.parameter.TPS_ParameterClass;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class GeneralDatabaseSummaryExport {

    private final TPS_DB_Connector dbCon;
    private final Analyzer analyzer;
    private final String source;

    private boolean update = false;

    /**
     * @param analyzer This {@link Analyzer} must have the following analyzers:
     *                 <code>[{@link ModeAnalyzer}, {@link PersonGroupAnalyzer}, {@link TripIntentionAnalyzer}, {@link DefaultDistanceCategoryAnalyzer}]</code>
     *                 . The order is important. The {@link PersonGroupAnalyzer} must
     *                 have person statistics.
     * @param source
     * @throws SQLException             when the connection to the database fails.
     * @throws IOException              if the loginInfo is not found
     * @throws ClassNotFoundException   if the {@link TPS_DB_Connector} could not login.
     * @throws IllegalArgumentException if any of the mentioned
     */
    public GeneralDatabaseSummaryExport(Analyzer analyzer, String source) throws IOException, ClassNotFoundException {
        TPS_ParameterClass parameterClass = new TPS_ParameterClass();
        parameterClass.loadRuntimeParameters(TPS_BasicConnectionClass.getRuntimeFile());
        dbCon = new TPS_DB_Connector(parameterClass);
        this.analyzer = analyzer;
        this.source = source;

        Categories[] cats = analyzer.getCategories();

        // check if analyzer is valid for this
        if (cats.length != 4) {
            throw new IllegalArgumentException("Not the right number of Analyzers");
        }
        if (cats[0] != Categories.Mode) {
            throw new IllegalArgumentException("Analyzer[0] is not Mode");
        }
        if (cats[1] != Categories.PersonGroup) {
            throw new IllegalArgumentException("Analyzer[1] is not PersonGroup");
        }
        if (cats[2] != Categories.TripIntention) {
            throw new IllegalArgumentException("Analyzer[2] is not TripIntention");
        }
        if (cats[3] != Categories.DistanceCategoryDefault) {
            throw new IllegalArgumentException("Analyzer[3] is not DistanceCategoryDefault");
        }

        if (!analyzer.hasPersonGroupStatistics()) {
            throw new IllegalArgumentException("Analyzer has no PersonGroup statistics");
        }

        try {
            PreparedStatement ps = dbCon.getConnection(this).prepareStatement(
                    "SELECT * FROM calibration_results WHERE sim_key = ?");
            ps.setString(1, source);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                System.err.println("Simkey for export exists and will be overwritten.");
                update = true;
            }
            rs.close();
        } catch (SQLException e) {
            // should never happen
            e.printStackTrace();
        }
    }

    /**
     * @param args
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws SQLException
     */
    public static void main(String[] args) throws ClassNotFoundException, IOException, SQLException {
        TPS_ParameterClass parameterClass = new TPS_ParameterClass();
        parameterClass.loadRuntimeParameters(TPS_BasicConnectionClass.getRuntimeFile());
        TPS_DB_Connector dbCon = new TPS_DB_Connector(parameterClass);

        ModeAnalyzer mo = new ModeAnalyzer();
        TripIntentionAnalyzer ti = new TripIntentionAnalyzer();
        DefaultDistanceCategoryAnalyzer dc = new DefaultDistanceCategoryAnalyzer();
        PersonGroupAnalyzer pg = new PersonGroupAnalyzer(mo, ti, dc);

        Analyzer analyzer = new Analyzer(mo, pg, ti, dc);

        String simkey = "2013y_09m_10d_11h_43m_03s_321ms";

        DBTripReader tripReader = new DBTripReader(simkey, null, null, null, dbCon);

        System.out.println("Starting the statistics");
        while (tripReader.getIterator().hasNext()) {
            TapasTrip tt = tripReader.getIterator().next();
            analyzer.addTrip(tt);
        }
        System.out.println("Adding finished.");
        tripReader.close();
        GeneralDatabaseSummaryExport exporter = new GeneralDatabaseSummaryExport(analyzer, simkey);

        if (exporter.writeSummary()) {
            System.out.println("Database export successful");
        }

    }

    @SuppressWarnings("rawtypes")
    private void fillBatch(PreparedStatement ps) {

        Categories[] cats = {Categories.Mode, Categories.PersonGroup, Categories.TripIntention, Categories.DistanceCategoryDefault};
        Enum[] instances = {Mode.BIKE, PersonGroup.PG_1, TripIntention.TRIP_31, DistanceCategoryDefault.CAT_1};

        ArrayList<CategoryCombination> combinations = CategoryCombination.listAllCombinations(cats);

        for (CategoryCombination cc : combinations) {

            instances[0] = cc.getCategories()[0];// mode
            instances[1] = cc.getCategories()[1];// person group
            instances[2] = cc.getCategories()[2];// trip intention
            instances[3] = cc.getCategories()[3];// distance category

            long cntTrips = analyzer.getTripCnt(instances);
            double dist = analyzer.getDistance(instances);
            double time = analyzer.getDuration(instances);
            int cntPersons = analyzer.getCntPersons(instances);

            try {
                ps.setInt(1, cntPersons);
                ps.setLong(2, cntTrips);
                ps.setDouble(3, dist);
                ps.setDouble(4, time);
                ps.setString(5, source);
                ps.setInt(6, instances[1].ordinal());
                ps.setInt(7, instances[3].ordinal());
                ps.setInt(8, instances[0].ordinal());
                ps.setInt(9, instances[2].ordinal());
                ps.addBatch();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * Inserts or updates the summary in the database.
     *
     * @return <code>false</code> if the database access failed or the region
     * was differentiated.
     */
    public boolean writeSummary() {

        try {
            Connection con = dbCon.getConnection(this);
            con.setAutoCommit(false);
            PreparedStatement updateStatement;
            if (update) {
                updateStatement = con.prepareStatement(
                        "UPDATE calibration_results " + "SET cnt_persons = ?," + "cnt_trips = ?, " + "avg_dist = ?, " +
                                "avg_time = ? " + "WHERE sim_key = ? AND " + "person_group = ? AND " +
                                "distance_category = ? AND " + "trip_mode = ? AND trip_intention = ?");

            } else {
                updateStatement = con.prepareStatement(
                        "INSERT INTO calibration_results " + "(cnt_persons, cnt_trips, avg_dist, avg_time, sim_key, " +
                                "person_group, distance_category, trip_mode, trip_intention) " +
                                "VALUES (?,?,?,?,?,?,?,?,?)");
            }

            fillBatch(updateStatement);

            /* int[] result = */
            updateStatement.executeBatch();
            con.commit();
            updateStatement.close();
            con.setAutoCommit(true);
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Error when executing query.");
            e = e.getNextException();
            e.printStackTrace();
            return false;
        }

        return true;

    }

}
