/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.analyzer.tum.results;

import de.dlr.ivf.tapas.analyzer.tum.constants.TuMEnums.DistanceCategory;
import de.dlr.ivf.tapas.analyzer.tum.databaseConnector.DBTripReader;
import de.dlr.ivf.tapas.analyzer.tum.regionanalyzer.RegionAnalyzer;
import de.dlr.ivf.tapas.constants.TPS_SettlementSystem.TPS_SettlementSystemType;
import de.dlr.ivf.tapas.persistence.db.TPS_DB_Connector;
import de.dlr.ivf.tapas.util.parameters.TPS_ParameterClass;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import java.io.File;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * This RegionAnalyzer starts a standard analysis of the given simkey as a background task.<br>
 * You may override the {@link StandaloneRegionAnalyzer#done()} method to have a callback.
 *
 * @author boec_pa
 */
public class StandaloneRegionAnalyzer extends SwingWorker<Integer, Object> {

    public static final int SUCCESS = 0;
    public static final int FAILED = 1;

    private DBTripReader tripReader;
    private String source;
    private final LinkedList<String> simulations = new LinkedList<>();
    private String simulation;
    private final TPS_DB_Connector connection;


    /**
     * @param simulation   the simulation to be analyzed.
     * @param acceptedTAZs may be <code>null</code>. All trips not starting or ending in these TAZs will be ignored.
     * @param connection   the database connection to be used.
     * @throws ClassNotFoundException
     * @throws IOException            when some parameter loading does not work.
     * @throws SQLException           when some database connection does not work.
     */
    public StandaloneRegionAnalyzer(String simulation, Set<Integer> acceptedTAZs, TPS_DB_Connector connection) {

        this(new String[]{simulation}, acceptedTAZs, connection);
        source = simulation;
    }

    public StandaloneRegionAnalyzer(String[] simulation, Set<Integer> acceptedTAZs, TPS_DB_Connector connection) {
        this.simulations.addAll(Arrays.asList(simulation));
        this.connection = connection;
    }

    /**
     * For testing purposes only!
     *
     * @throws IOException
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException, ExecutionException {
        String loginInfo = "T:\\Simulationen\\runtime_perseus.csv";
        TPS_ParameterClass parameterClass = new TPS_ParameterClass();
        parameterClass.loadRuntimeParameters(new File(loginInfo));
        TPS_DB_Connector dbCon = new TPS_DB_Connector(parameterClass);

        HashSet<Integer> acceptedTAZs = null;
        String sim = "2013y_03m_18d_11h_43m_32s_420ms";
        System.out.println("Creating and starting export for " + sim + " with tazFilter " + acceptedTAZs);
        StandaloneRegionAnalyzer ra = new StandaloneRegionAnalyzer(sim, acceptedTAZs, dbCon) {
            @Override
            protected void done() {
                super.done();
                System.out.println("Done in worker");
            }
        };
        ra.execute();
        ra.get();
    }

    @Override
    protected Integer doInBackground() throws SQLException, ClassNotFoundException, IOException, BadLocationException {

        while (!simulations.isEmpty()) {

            simulation = simulations.poll();
            PreparedStatement getParamStatement = connection.getConnection(this).prepareStatement(
                    "SELECT param_value FROM simulation_parameters WHERE sim_key = ? AND param_key = ?");

            getParamStatement.setString(1, simulation);
            getParamStatement.setString(2, "DB_HOUSEHOLD_AND_PERSON_KEY");

            String hhkey;
            String schema;
            String region;

            ResultSet rs = getParamStatement.executeQuery();
            if (rs.next()) {
                hhkey = rs.getString("param_value");
            } else {
                throw new IllegalStateException(
                        "Could not find household key in simulation_parameters for " + simulation);
            }
            rs.close();

            getParamStatement.setString(2, "DB_SCHEMA_CORE");
            rs = getParamStatement.executeQuery();
            if (rs.next()) {
                schema = rs.getString("param_value");
                // hack: delete dot
                schema = schema.substring(0, schema.length() - 1);
            } else {
                throw new IllegalStateException("Could not find scheme in simulation_parameters for " + simulation);
            }
            rs.close();

            getParamStatement.setString(2, "DB_REGION");
            rs = getParamStatement.executeQuery();
            if (rs.next()) {
                region = rs.getString("param_value");
            } else {
                throw new IllegalStateException("Could not find region in simulation_parameters for " + simulation);
            }
            rs.close();
            getParamStatement.close();

            tripReader = new DBTripReader(simulation, hhkey, schema, region, TPS_SettlementSystemType.FORDCP, null,
                    connection, null);


            boolean[] dcFilter = new boolean[DistanceCategory.values().length];
            Arrays.fill(dcFilter, true);

            RegionAnalyzer regionAnalyzer = new RegionAnalyzer(dcFilter, false);
            if (tripReader.getIterator().hasNext()) {

                if (isCancelled()) {
                    tripReader.close();
                    regionAnalyzer.cancelFinish();
                    return FAILED;
                }

                regionAnalyzer.init(null, null, false);

                while (tripReader.getIterator().hasNext()) {

                    setProgress(tripReader.getProgress());

                    regionAnalyzer.prepare(simulation, tripReader.getIterator().next(), connection.getParameters());
                }

                try {
                    //TODO summary export fails with connection timeout?
                    DatabaseSummaryExport databaseSummaryExport = new DatabaseSummaryExport(regionAnalyzer);
                    databaseSummaryExport.writeSummary();
                } catch (ClassNotFoundException | IOException e) {
                    System.err.println("Summary Export failed!");
                    e.printStackTrace();
                    return FAILED;
                }

            }

            tripReader.close();

        }
        return SUCCESS;
    }

    @Override
    protected void done() {

        setProgress(100);
        System.out.println("Worker is done!");

    }

    public String getSimulation() {
        return this.simulation;

    }

    public String getSource() {
        return source;
    }

}
