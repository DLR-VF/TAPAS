/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.dlr.ivf.tapas.environment.gui.legacy;
import de.dlr.ivf.tapas.environment.gui.services.SimulationDataUpdateService;
import de.dlr.ivf.tapas.environment.gui.services.ServerDataUpdateService;
import de.dlr.ivf.tapas.environment.gui.legacy.util.MultilanguageSupport;
import de.dlr.ivf.tapas.environment.model.ServerData;
import de.dlr.ivf.tapas.environment.model.SimulationData;
import de.dlr.ivf.tapas.environment.model.SimulationStateLegacy;

import lombok.Builder;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * The {@link SimulationControl} represents the controller for the simulation
 * client. The controller has two active tasks and provides methods to change
 * the simulation entries in the database.<br>
 * <br>
 * The active tasks are:<br>
 * 1. {@link SimulationDataUpdateService}<br>
 * This task updates the simulation values in the memory of the client from the
 * database<br>
 * 2. {@link ServerDataUpdateService}<br>
 * This task updates the simulation server data values in the memory of the
 * client from the database<br>
 * <br>
 * Furthermore, the controller provides methods to add or remove simulations from the
 * database and to change the state of a simulation, e.g. it is possible the
 * stop or restart a simulation.
 *
 */

@Builder
public class SimulationControl {


    /**
     * Connection to the database
     */
    private Supplier<Connection> dbConnection;

    private final ServerDataUpdateService simulationServerDataUpdateService;

    private final SimulationDataUpdateService simulationDataUpdateTask;
    /**
     * Current timestamp of database
     */
    private Timestamp currentDatabaseTimestamp;

    private final GuiModel model;

    /**
     * All available servers
     */
    private final Map<String, ServerData> simulationServerDataMap;
    private final ExecutorService executorPool = Executors.newCachedThreadPool();

    /**
     * Map with all available simulations from the database
     */
    private final Map<String, SimulationData> simulationDataMap;

    /**
     * This method loads all values from the run properties file by the
     * parameter 'filename' and stores a simulation in the database. The
     * simulation is added to the internal map and the GUI via the
     * SimulationDataUpdateService.
     *
     * @param sim_key       key for the simulation in the database
     * @param filename      filename of the run properties file
     * @param addConfigToDB true if the config stored in filename should be stored in the
     *                      database. false if it is stored already.
     * @throws IOException  This exception is thrown if there occurred an error while
     *                      reading the run properties file
     * @throws SQLException This exception is thrown if there occurs an error while
     *                      inserting the simulation into the database
     */
    public void addSimulation(String sim_key, String filename, boolean addConfigToDB) throws IOException{
        String fullFileName = filename;
        // now read the parameters and store them into the db
        HashMap<String, String> parameters = new HashMap<>();
//        this.parameterFiles.push(new File(fullFileName));
//        while (!this.parameterFiles.empty()) {
//            loadParameters(this.parameterFiles.pop(), parameters);
//        }
        //fix the SUMO-dir by appending the simulation run!
        String paramVal = parameters.get("SUMO_DESTINATION_FOLDER");
        paramVal += "_" + sim_key;
        parameters.put("SUMO_DESTINATION_FOLDER", paramVal);

        Random generator;
        if (parameters.get("RANDOM_SEED_NUMBER") != null && Boolean.parseBoolean(
                parameters.get("FLAG_INFLUENCE_RANDOM_NUMBER"))) {
            generator = new Random(Long.parseLong(parameters.get("RANDOM_SEED_NUMBER")));
        } else {
            generator = new Random();
        }
        double randomSeed = generator.nextDouble(); // postgres needs a double
        // value as seed ranging
        // from 0 to 1

        String projectName = parameters.getOrDefault("PROJECT_NAME", "");
        String query = String.format("INSERT INTO simulations (sim_key, sim_file, sim_description) VALUES('%s', '%s', '%s')",
                sim_key, filename, projectName);

//        SimulationControl.this.dbConnection.execute(query, this);
//        if (addConfigToDB) {
//            this.getParameters().writeToDB(this.dbConnection.getConnection(this), sim_key, parameters);
//        }
    }

    /**
     * This method loads all values from the run properties file by the
     * parameter 'filename' and stores a simulation in the database. The
     * simulation is added to the internal map and the GUI via the
     * SimulationDataUpdateService.
     *
     * @param sim_key  key for the simulation in the database
     * @param filename filename of the run properties file
     * @throws IOException  This exception is thrown if there occurred an error while
     *                      reading the run properties file
     * @throws SQLException This exception is thrown if there occurs an error while
     *                      inserting the simulation into the database
     */
    public void addSimulation(String sim_key, String filename) throws IOException, SQLException {
        addSimulation(sim_key, filename, true);
    }

    /**
     * This method changes the state of the simulation corresponding to the
     * parameter sim_key.<br>
     * The changes are:<br>
     * <br>
     * INSERTED -> STARTED<br>
     * STOPPED -> STARTED<br>
     * <br>
     * STARTED -> STOPPED<br>
     * FINISHED -> Remove simulation completely from the database<br>
     * <br>
     * The changes are done via a SQl statement and the skipUpdate flag is set
     * to true after the statement was committed.
     *
     * @param sim_key key for the simulation in the database
     */
    public void changeSimulationDataState(String sim_key) {
        SimulationData simulation = this.simulationDataMap.get(sim_key);
        SimulationStateLegacy state = SimulationStateLegacy.getState(simulation);

        // synchronized (simulationDataMap) {
        String query;
        switch (state) { // the state represents the PAST state!
            case INSERTED:
                // make simulation ready and start it
                query = "select core.prepare_simulation_for_start('" + sim_key + "')";
//                SimulationControl.this.dbConnection.execute(query, this);
                break;
            case STOPPED:
                // start the sim
                query = "UPDATE simulations SET sim_ready = true, " +
                        "sim_started = true, sim_finished = false WHERE sim_key = '" + sim_key + "'";
//                SimulationControl.this.dbConnection.execute(query, this);
                break;
            case STARTED:
                // stop simulation
                query = "UPDATE simulations SET sim_ready = true, " +
                        "sim_started = false, sim_finished = false WHERE sim_key = '" + sim_key + "'";
//                SimulationControl.this.dbConnection.execute(query, this);
                break;
            case FINISHED:
                // remove simulation from database
                this.removeSimulation(sim_key, true);
                break;
        }
    }

    /**
     * This method opens a file dialog in which you can choose a run properties
     * file, which have a name like 'run_*.csv' where * can be any string.
     *
     * @param parent parent component for the dialog which is opened in this method
     * @return the chosen run properties file
     */
    public File[] chooseRunFile(Component parent) {
        JFileChooser fd = new JFileChooser();
        fd.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fd.setMultiSelectionEnabled(true);
        fd.setVisible(true);
        fd.setFileFilter(new FileFilter() {
            /*
             * (non-Javadoc)
             *
             * @see javax.swing.filechooser.FileFilter#accept(java.io.File)
             */
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || (f.getName().startsWith("run_") && f.getName().endsWith(".csv"));
            }

            /*
             * (non-Javadoc)
             *
             * @see javax.swing.filechooser.FileFilter#getDescription()
             */
            @Override
            public String getDescription() {
                return MultilanguageSupport.getString("CHOOSE_SIMULATION_DIALOG_DESCRIPTION");
            }
        });
//        fd.setCurrentDirectory(new File(this.props.get(ClientControlPropKey.LAST_SIMULATION_DIR)));
//        fd.setSelectedFile(new File(this.props.get(ClientControlPropKey.LAST_SIMULATION)));
        fd.setDialogTitle(MultilanguageSupport.getString("CHOOSE_SIMULATION_FILE_BUTTON"));
        int key = fd.showOpenDialog(parent);
        if (key == JFileChooser.APPROVE_OPTION) {
//            File selection = fd.getSelectedFiles()[0];
//            this.props.set(ClientControlPropKey.LAST_SIMULATION_DIR, selection.getParent());
//            this.props.set(ClientControlPropKey.LAST_SIMULATION, selection.getName());
            return fd.getSelectedFiles();
        }
        return null;
    }

    /**
     * This method stops the timer with all scheduled tasks and closes the
     * connection to the database.
     */
    public void close() {
//        if (timer != null) {
//            try {
//                this.timer.cancel();
//                Thread.sleep(100);// wait a bit, so no sql-query is fired any
//                // more to
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
//        try {
//            if (dbConnection != null) this.dbConnection.closeConnection(this);
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
    }

    /**
     * check if changes are made
     */
    public boolean configChanged() {
        return false;//this.props.isChanged();
    }


    /**
     * @return current database timestamp
     */
    public Timestamp getCurrentDatabaseTimestamp() {
        return currentDatabaseTimestamp;
    }


    /**
     * Retrieves the ServerHash-String from the internal servers map
     *
     * @param hostname the host from which the Hash is retrieved
     * @return - empty String if server is <strong>offline</strong> or no Hash is defined
     * - the hash string if server is <strong>online</strong> and the Hash has been set inside the database
     */
    public String getServerHash(String hostname) {

        if (simulationServerDataMap.containsKey(hostname)) {
            ServerData ssd = simulationServerDataMap.get(hostname);
            String hashstring = ssd.getHashString() == null || ssd.getHashString().equals("") ? MultilanguageSupport
                    .getString("HASH_MESSAGE_ERROR") : ssd.getHashString();

            return ssd.isOnline() ? hashstring : MultilanguageSupport.getString("HASH_MESSAGE_UNKNOWN");
        } else return MultilanguageSupport.getString("HASH_MESSAGE_ERROR");
    }


    /**
     * Remove a TAPAS SimulationServer from DB
     *
     * @param ip
     */
    public void removeServer(InetAddress ip) {
//        String query = "DELETE FROM " + this.getParameters().getString(ParamString.DB_TABLE_SERVERS) +
//                " WHERE server_ip = inet'" + ip.getHostAddress() + "'";
//        this.dbConnection.execute(query, this);
    }

    /**
     * This method completely removes a simulation from the database. The
     * changes take place in the internal map and the GUI when the
     * SimulationDataUpdateService works the next time.
     *
     * @param sim_key    key for the simulation in the database
     * @param withDialog flag if a "are you sure?" dialog should be presented
     */
    public void removeSimulation(String sim_key, boolean withDialog) {

        int option = withDialog ? JOptionPane.showConfirmDialog(null,
                "Do you really want to remove the simulation\n\n" + sim_key + "\n\ncompletely from the database?\n" +
                        "Note: This step can't be reverted!", "Remove Simulation from database",
                JOptionPane.YES_NO_OPTION) : JOptionPane.YES_OPTION;

        if (option == JOptionPane.YES_OPTION) {
            // temporary tables are removed via a database trigger
            // first delete this simulation from the process table to stop
            // servers!
//            this.dbConnection.execute("DELETE FROM " + this.getParameters().getString(ParamString.DB_TABLE_PROCESSES) +
//                    " WHERE sim_key='" + sim_key + "'", this);
//            this.dbConnection.execute(
//                    "DELETE FROM " + this.getParameters().getString(ParamString.DB_TABLE_SIMULATIONS) +
//                            " WHERE sim_key='" + sim_key + "'", this);
        }
    }

    /**
     * This method initiates a remote shutdown of a SimulationServer. This task runs asynchronously and is being handled as a CompletableFuture
     * chain by an ExecutorService.
     * Exceptions occurring inside the chain will be handled at the end
     *
     * @param hostname the host name of the remote computer where a SimulationServer needs to be shut down.
     */
    protected void shutServerDown(String hostname) {
        final ServerData ssd = this.simulationServerDataMap.get(hostname);
//        ssd.setServerState(ServerControlState.STOP);
//        this.gui.updateServerControl(this.simulationServerDataMap);
//        CompletableFuture.runAsync(() -> dbConnection.executeUpdate(
//                "UPDATE " + this.getParameters().getString(ParamString.DB_TABLE_PROCESSES) +
//                        " SET shutdown = true WHERE host = '" + hostname + "' AND end_time IS NULL", this),
//                executorPool).exceptionally(e -> {
//            Stream.of(e.getStackTrace()).forEach(System.out::println);
//            return null;
//        });
    }

    /**
     * Launches remotely a SimulationServer from the SimlationDaemon. This task runs asynchronously and is being handled as a CompletableFuture
     * chain by an ExecutorService.
     * Checked exceptions will be re-thrown as unchecked ones so that the "exceptionally" block will be executed
     *
     * @param hostname the host name of the remote computer where a SimulationServer needs to be started up.
     */
    protected void startServer(String hostname) {

//        CompletableFuture.runAsync(() -> dbConnection.executeUpdate(
//                "UPDATE " + this.getParameters().getString(ParamString.DB_TABLE_SERVERS) +
//                        " SET server_boot_flag = true WHERE server_name = '" + hostname + "'", this), executorPool)
//                         .exceptionally(e -> {
//                             Stream.of(e.getStackTrace()).forEach(System.out::println);
//                             return null;
//                         });
    }

    /**
     * This task updates the server control buttons and any additional information of the selected server from the servers table.
     *
     * @author sche_ai
     */
    private class ServerControlUpdateTask extends TimerTask {

        @Override
        public void run() {

//            SimulationControl.this.gui.updateServerControl(simulationServerDataMap);
        }


    }
}