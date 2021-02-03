/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.runtime.client;

import com.csvreader.CsvReader;
import de.dlr.ivf.tapas.persistence.db.TPS_DB_Connector;
import de.dlr.ivf.tapas.persistence.db.TPS_DB_IOManager;
import de.dlr.ivf.tapas.persistence.db.TPS_DB_IOManager.Behaviour;
import de.dlr.ivf.tapas.runtime.server.SimulationData;
import de.dlr.ivf.tapas.runtime.server.SimulationData.TPS_SimulationState;
import de.dlr.ivf.tapas.runtime.server.SimulationServerData;
import de.dlr.ivf.tapas.runtime.util.ClientControlProperties;
import de.dlr.ivf.tapas.runtime.util.ClientControlProperties.ClientControlPropKey;
import de.dlr.ivf.tapas.runtime.util.MultilanguageSupport;
import de.dlr.ivf.tapas.runtime.util.ServerControlState;
import de.dlr.ivf.tapas.tools.persitence.db.TPS_BasicConnectionClass;
import de.dlr.ivf.tapas.util.TPS_Argument;
import de.dlr.ivf.tapas.util.TPS_Argument.TPS_ArgumentType;
import de.dlr.ivf.tapas.util.parameters.ParamString;
import de.dlr.ivf.tapas.util.parameters.TPS_ParameterClass;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Timer;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

/**
 * The {@link SimulationControl} represents the controller for the simulation
 * client. The controller has three active tasks and provides methods to change
 * the simulation entries in the database.<br>
 * <br>
 * The active tasks are:<br>
 * 1. {@link SimulationDataUpdateTask}<br>
 * This task updates the simulation values in the memory of the client from the
 * database<br>
 * 2. {@link SimulationServerDataUpdateTask}<br>
 * This task updates the simulation server data values in the memory of the
 * client from the database<br>
 * 3. {@link TimeUpdateTask}<br>
 * This task retrieves the current timestamp of the database<br>
 * <br>
 * Furthermore the client provides methods to add or remove simulations from the
 * database and to change the state of a simulation, e.g. it is possible the
 * stop or restart a simulation.
 *
 * @author mark_ma
 */
public class SimulationControl {

    public static final String BUILD_NUMBER = ".*";
    /**
     * Parameter array with all parameters for this main method.<br>
     * PARAMETERS[0] = tapas network directory path
     */
    private static final TPS_ArgumentType<?>[] PARAMETERS = {new TPS_ArgumentType<>("tapas network directory path",
            File.class)};
    @SuppressWarnings("unused")
    private static SimulationControl control;
    /**
     * Stack to store the parents of the parameter files. Needed to descent in
     * the correct order.
     */
    private final Stack<File> parameterFiles = new Stack<>();
    /**
     * Connection to the database
     */
    private TPS_DB_Connector dbConnection;
    /**
     * Current timestamp of database
     */
    private Timestamp currentDatabaseTimestamp;
    /**
     * the client graphical user interface
     */
    private final SimulationMonitor gui;
    /**
     * All available servers
     */
    private final Map<String, SimulationServerData> simulationServerDataMap;
    private final ExecutorService executorpool = Executors.newCachedThreadPool();
    /**
     * This instance can test if a SimulationServer is online
     */
    @SuppressWarnings("unused")
    private final ServerOnlineTester serverOnlineTester = new ServerOnlineTester();
    /**
     * Map with all available simulations from the database
     */
    private final Map<String, SimulationData> simulationDataMap;
    /**
     * This flag indicates if one update step of the SimulationData in
     * SimulationDataUpdateTask has to be skipped
     */
    private boolean skipUpdate;
    /**
     * Complete local path to the TAPAS directory
     */
    private final File tapasNetworkDirectory;
    /**
     * Timer for all internal tasks
     */
    private final Timer timer;
    /**
     * The Client control properties
     */
    private ClientControlProperties props;
    private String version;
    private String builddate;
    private String buildnumber;

    /**
     * The constructor consists of five parts:<br>
     * <br>
     * 1. initialising member variables<br>
     * <br>
     * 2. loading runtime file<br>
     * The constructor loads the 'runtime.csv' file from the given parameter
     * 'tapasNetworkDirectory'. The file can look like this:<br>
     * <br>
     * name,value,comment<br>
     * FILE_PARENT_PROPERTIES,db_connect_1_achilles.csv,<br>
     * FILE_PARENT_PROPERTIES,db_user_0_admin.csv,<br>
     * FILE_PARENT_PROPERTIES,db_schema_table_0.csv,<br>
     * <br>
     * 3. connecting to database<br>
     * With the information of the runtime file there is a connection to the
     * database built.<br>
     * <br>
     * 4. setting up the GUI<br>
     * <br>
     * 5. initialising the timer and all tasks
     *
     * @param tapasNetworkDirectory absolute path to the tapas network directory
     */
    public SimulationControl(File tapasNetworkDirectory) {


        //System.out.println(System.getProperties().getProperty("sun.rmi.transport.proxy.connectTimeout"));
        // Part 1: initialising member variables
        this.tapasNetworkDirectory = tapasNetworkDirectory;
        this.simulationServerDataMap = new HashMap<>();
        this.simulationDataMap = new HashMap<>();

        File propFile = new File("client.properties");
        this.props = new ClientControlProperties(propFile);

        Properties versionprops = new Properties();
        try {
            InputStream is = getClass().getResourceAsStream("/buildnumber.properties");
            if (is != null) {
                versionprops.load(is);
                builddate = versionprops.getProperty("builddate", "NA");
                buildnumber = versionprops.getProperty("buildnumber", "NA");
                version = versionprops.getProperty("version", "NA");
            } else {
                builddate = "NA";
                buildnumber = "NA";
                version = "NA";
            }
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        // Part 2: loading runtime file
        TPS_ParameterClass parameterClass = new TPS_ParameterClass();
        File runtimeFile;
        if (this.props.get(ClientControlPropKey.LOGIN_CONFIG) == null || this.props.get(
                ClientControlPropKey.LOGIN_CONFIG).isEmpty()) {
            runtimeFile = TPS_BasicConnectionClass.getRuntimeFile("client.properties");
            this.props = new ClientControlProperties(propFile);
        } else {
            runtimeFile = new File(this.props.get(ClientControlPropKey.LOGIN_CONFIG));
        }

        try {
            parameterClass.loadRuntimeParameters(runtimeFile);
        } catch (Exception e) {
            System.err.println("Exception thrown during reading runtime file: " + runtimeFile.getAbsolutePath());
            e.printStackTrace();
        }

        // Part 3: connecting to database

        try {
            this.dbConnection = new TPS_DB_Connector(parameterClass);
        } catch (Exception e) {
            System.err.println("Exception thrown during establishing database connection!");
            e.printStackTrace();
        }

        // Part 4: setting up the GUI
        this.gui = new SimulationMonitor(this);

        // Part 5: initialising the timer and all tasks
        this.timer = new Timer("Server & Simulation Update Timer");
        this.timer.schedule(new TimeUpdateTask(), 0, 250);
        // this.simulationDataUpdateTask =new SimulationDataUpdateTask();
        this.timer.schedule(new SimulationDataUpdateTask(), 75, 500);
        this.timer.schedule(new SimulationServerDataUpdateTask(), 25, 1000);
        this.timer.schedule(new ServerControlUpdateTask(), 0, 250);
        //this.timer.schedule(new ProgressUpdateTask(), 250, 250);
    }

    /**
     * This main method checks all parameters and then setup a client with a
     * graphical user interface for TAPAS simulations
     *
     * @param args args[0]: tapas network directory path
     */
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Unable to set specific look and feel: " + UIManager.getSystemLookAndFeelClassName());
            e.printStackTrace();
        }

        Object[] parameters = null;
        try {
            parameters = TPS_Argument.checkArguments(args, PARAMETERS);
        } catch (Exception e) {
        }

        File file = null;
        File propFile = new File("client.properties");
        ClientControlProperties prop = new ClientControlProperties(propFile);

        if (parameters != null && parameters.length > 0) {
            file = ((File) parameters[0]).getParentFile();
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                prop.set(ClientControlPropKey.TAPAS_DIR_WIN, file.getPath());
            } else {
                prop.set(ClientControlPropKey.TAPAS_DIR_LINUX, file.getPath());
            }
            prop.updateFile();
        }
        // Constructs the client
        control = new SimulationControl(file);
    }

    /**
     * This method loads all values from the run properties file by the
     * parameter 'filename' and stores a simulation in the database. The
     * simulation is added to the internal map and the GUI via the
     * SimulationDataUpdateTask.
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
    public void addSimulation(String sim_key, String filename, boolean addConfigToDB) throws IOException, SQLException {
//        filename = filename.replace('\\', '/').substring(tapasNetworkDirectory.getAbsolutePath().length());

        String fullFileName;

//        fullFileName = tapasNetworkDirectory.getAbsolutePath() + filename;
        fullFileName = filename;
        // now read the parameters and store them into the db
        HashMap<String, String> parameters = new HashMap<>();
        this.parameterFiles.push(new File(fullFileName));
        while (!this.parameterFiles.empty()) {
            loadParameters(this.parameterFiles.pop(), parameters);
        }
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


        String query = ("INSERT INTO simulations (sim_key, sim_file, sim_description) VALUES('" +
                sim_key + "', '', '" + filename + "')");

        SimulationControl.this.dbConnection.execute(query, this);
        if (addConfigToDB) {
            this.getParameters().writeToDB(this.dbConnection.getConnection(this), sim_key, parameters);
        }
    }

    /**
     * This method loads all values from the run properties file by the
     * parameter 'filename' and stores a simulation in the database. The
     * simulation is added to the internal map and the GUI via the
     * SimulationDataUpdateTask.
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
        TPS_SimulationState state = TPS_SimulationState.getState(simulation);

        // synchronized (simulationDataMap) {
        String query;
        switch (state) { // the state represents the PAST state!
            case INSERTED:
                // make simulation ready and start it
                query = "select core.prepare_simulation_for_start('" + sim_key + "')";
                SimulationControl.this.dbConnection.execute(query, this);
                break;
            case STOPPED:
                // start the sim
                query = "UPDATE simulations SET sim_ready = true, " +
                        "sim_started = true, sim_finished = false WHERE sim_key = '" + sim_key + "'";
                SimulationControl.this.dbConnection.execute(query, this);
                break;
            case STARTED:
                // stop simulation
                query = "UPDATE simulations SET sim_ready = true, " +
                        "sim_started = false, sim_finished = false WHERE sim_key = '" + sim_key + "'";
                SimulationControl.this.dbConnection.execute(query, this);
                break;
            case FINISHED:
                // remove simulation from database
                this.removeSimulation(sim_key, true);
                break;
        }
        this.skipUpdate = true;
        // }
    }

    public boolean checkConnection() throws SQLException {
        return !this.dbConnection.getConnection(this).isClosed();
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
        fd.setCurrentDirectory(new File(this.props.get(ClientControlPropKey.LAST_SIMULATION_DIR)));
        fd.setSelectedFile(new File(this.props.get(ClientControlPropKey.LAST_SIMULATION)));
        fd.setDialogTitle(MultilanguageSupport.getString("CHOOSE_SIMULATION_FILE_BUTTON"));
        int key = fd.showOpenDialog(parent);
        if (key == JFileChooser.APPROVE_OPTION) {
            File selection = fd.getSelectedFiles()[0];
            this.props.set(ClientControlPropKey.LAST_SIMULATION_DIR, selection.getParent());
            this.props.set(ClientControlPropKey.LAST_SIMULATION, selection.getName());
            return fd.getSelectedFiles();
        }
        return null;
    }

    /**
     * This method stops the timer with all scheduled tasks and closes the
     * connection to the database.
     */
    public void close() {
        if (timer != null) {
            try {
                this.timer.cancel();
                Thread.sleep(100);// wait a bit, so no sql-query is fired any
                // more to
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        try {
            if (dbConnection != null) this.dbConnection.closeConnection(this);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * check if changes are made
     */
    public boolean configChanged() {
        return this.props.isChanged();
    }

    protected String getBuilddate() {
        return builddate;
    }

    protected String getBuildnumber() {
        return buildnumber;
    }

    public TPS_DB_Connector getConnection() {
        return this.dbConnection;
    }

    /**
     * @return current database timestamp
     */
    public Timestamp getCurrentDatabaseTimestamp() {
        return currentDatabaseTimestamp;
    }

    /**
     * @return parameter class reference
     */
    public TPS_ParameterClass getParameters() {
        return this.dbConnection.getParameters();
    }

    protected ClientControlProperties getProperties() {
        return this.props;
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
            SimulationServerData ssd = simulationServerDataMap.get(hostname);
            String hashstring = ssd.getHashString() == null || ssd.getHashString().equals("") ? MultilanguageSupport
                    .getString("HASH_MESSAGE_ERROR") : ssd.getHashString();

            return ssd.isOnline() ? hashstring : MultilanguageSupport.getString("HASH_MESSAGE_UNKNOWN");
        } else return MultilanguageSupport.getString("HASH_MESSAGE_ERROR");
    }

    protected String getVersion() {
        return version;
    }

    /**
     * This method loads all given parameter entries and stores them in a
     * hashmap: (key,value). This hashmap is used to put the parameters into the
     * database.
     */
    private int loadParameters(File name, HashMap<String, String> parameters) throws IOException {
        String key;
        String value;
        String parent;
        int counter = 0;
        CsvReader reader = new CsvReader(new FileReader(name.getAbsolutePath()));
        reader.readHeaders();
        while (reader.readRecord()) {
            key = reader.get(0);
            value = reader.get(1);

            if (key.equals("FILE_PARENT_PROPERTIES") || key.equals("FILE_FILE_PROPERTIES") || key.equals(
                    "FILE_LOGGING_PROPERTIES") || key.equals("FILE_PARAMETER_PROPERTIES") || key.equals(
                    "FILE_DATABASE_PROPERTIES")) {
                parent = name.getParent();
                while (value.startsWith("./")) {
                    value = value.substring(2);
                    parent = new File(parent).getParent();
                }
                this.parameterFiles.push(new File(parent, value));
            } else {
                if (!parameters.containsKey(key)) {
                    counter++;
                    parameters.put(key, value); // this does not overwrites old
                    // values!
                }
            }
        }
        reader.close();

        return counter;
    }

    /**
     * Remove a TAPAS SimulationServer from DB
     *
     * @param ip
     */
    public void removeServer(InetAddress ip) {
        String query = "DELETE FROM " + this.getParameters().getString(ParamString.DB_TABLE_SERVERS) +
                " WHERE server_ip = inet'" + ip.getHostAddress() + "'";
        this.dbConnection.execute(query, this);
    }

    /**
     * This method completely removes a simulation from the database. The
     * changes take place in the internal map and the GUI when the
     * SimulationDataUpdateTask works the next time.
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
            this.dbConnection.execute("DELETE FROM " + this.getParameters().getString(ParamString.DB_TABLE_PROCESSES) +
                    " WHERE sim_key='" + sim_key + "'", this);
            this.dbConnection.execute(
                    "DELETE FROM " + this.getParameters().getString(ParamString.DB_TABLE_SIMULATIONS) +
                            " WHERE sim_key='" + sim_key + "'", this);
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
        final SimulationServerData ssd = this.simulationServerDataMap.get(hostname);
        ssd.setServerState(ServerControlState.STOP);
        this.gui.updateServerControl(this.simulationServerDataMap);
        CompletableFuture.runAsync(() -> dbConnection.executeUpdate(
                "UPDATE " + this.getParameters().getString(ParamString.DB_TABLE_PROCESSES) +
                        " SET shutdown = true WHERE host = '" + hostname + "' AND end_time IS NULL", this),
                executorpool).exceptionally(e -> {
            Stream.of(e.getStackTrace()).forEach(System.out::println);
            return null;
        });
    }

    /**
     * Launches remotely a SimulationServer from the SimlationDaemon. This task runs asynchronously and is being handled as a CompletableFuture
     * chain by an ExecutorService.
     * Checked exceptions will be re-thrown as unchecked ones so that the "exceptionally" block will be executed
     *
     * @param hostname the host name of the remote computer where a SimulationServer needs to be started up.
     * @throws RemoteException
     */
    protected void startServer(String hostname) {

        CompletableFuture.runAsync(() -> dbConnection.executeUpdate(
                "UPDATE " + this.getParameters().getString(ParamString.DB_TABLE_SERVERS) +
                        " SET server_boot_flag = true WHERE server_name = '" + hostname + "'", this), executorpool)
                         .exceptionally(e -> {
                             Stream.of(e.getStackTrace()).forEach(System.out::println);
                             return null;
                         });
    }

    /**
     * writes all properties to the property file
     */
    public void updateProperties() {
        this.props.updateFile();
    }

    /**
     * This class provides a mechanism to test whether a SimulationServer is
     * online or not.
     *
     * @author mark_ma
     */
    private class ServerOnlineTester {

        /**
         * Collection of all SimulationServer IP Addresses which have to be
         * tested if they are available. The IP Addresses are added in this
         * collection when their last timestamp in the database is too old, so
         * that it is possible that they are not available already.
         */
        private final Collection<InetAddress> simulationServerIPAddressCollection = new HashSet<>();

        /**
         * This Thread tests if a SimulationServer is online. If the server is
         * available the thread does nothing. If the server is not online the
         * Thread resets the entry in the database to the corresponding
         * simulation process.
         *
         * @author mark_ma
         */
        @SuppressWarnings("unused")
        private class SimulationServerOnlineTestThread extends Thread {

            /**
             * IP Address of the SimulationServer to test
             */
            private final InetAddress simulationServerIPAddress;

            /**
             * Constructs the thread an sets the SimulationServer IP Address
             *
             * @param simulationServerIPAddress IP Address of the SimulationServer to test
             */
            public SimulationServerOnlineTestThread(InetAddress simulationServerIPAddress) {
                this.simulationServerIPAddress = simulationServerIPAddress;
            }

            /**
             * @return true if the server was successfully reseted, false
             * otherwise
             */
            private boolean resetDatabaseEntryOfSimulationServer() {
                try {
                    // Reset statement for the simulation server entry in the
                    // database
                    int rows = SimulationControl.this.dbConnection.executeUpdate("UPDATE " +
                            SimulationControl.this.dbConnection.getParameters()
                                                               .getString(ParamString.DB_TABLE_SERVERS) +
                            " SET server_usage = 0, server_online = false WHERE server_ip = inet '" +
                            this.simulationServerIPAddress.getHostAddress() + "'", SimulationControl.this);
                    return rows > 0;
                } finally {
                    // remove the entry with this IP Address from the collection
                    // of SimulationServers to test
                    synchronized (simulationServerIPAddressCollection) {
                        simulationServerIPAddressCollection.remove(simulationServerIPAddress);
                    }
                }
            }

            /*
             * (non-Javadoc)
             *
             * @see java.lang.Thread#run()
             */
            @Override
            public void run() {

            }
        }

        /**
         * This method starts one instance of
         * {@link SimulationServerOnlineTestThread} to test the given IP
         * Address, if there exists no instance of
         * {@link SimulationServerOnlineTestThread} which already does this
         * test. The test itself needs a few seconds but this method doesn't
         * block. It starts a new Thread which does the test. If an IP Address
         * is added, while it is tested this command is ignored.
         *
         * @param simulationServerIPAddress
         * @throws SQLException
         */
//		public void test(InetAddress simulationServerIPAddress)
//				throws SQLException {
//			synchronized (simulationServerIPAddressCollection) {
//				if (simulationServerIPAddressCollection
//						.contains(simulationServerIPAddress)) {
//					return;
//				} else {
//					simulationServerIPAddressCollection
//							.add(simulationServerIPAddress);
//					new SimulationServerOnlineTestThread(
//							simulationServerIPAddress).start();
//				}
//			}
//		}
    }

    /**
     * This task updates the server control buttons and any additional information of the selected server from the servers table.
     *
     * @author sche_ai
     */
    private class ServerControlUpdateTask extends TimerTask {

        @Override
        public void run() {

            SimulationControl.this.gui.updateServerControl(simulationServerDataMap);
        }


    }

    /**
     * This task updates the SimulationServerData of all SimulationServer.
     *
     * @author mark_ma
     */
    private class SimulationServerDataUpdateTask extends TimerTask {

        public SimulationServerDataUpdateTask() {

            // opens the connection to pass the connectiontest
            try {
                dbConnection.getConnection(this);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        /*
         * (non-Javadoc)
         *
         * @see java.util.TimerTask#run()
         */
        @Override
        public void run() {
            // Select all available servers from the database
            SimulationServerData data;
            if (!SimulationControl.this.dbConnection.checkConnection(this)) return;

            String query = "SELECT * FROM " + SimulationControl.this.dbConnection.getParameters().getString(
                    ParamString.DB_TABLE_SERVERS) + " ORDER BY server_ip";

            try (ResultSet rs = SimulationControl.this.dbConnection.executeQuery(query, this)) {

                while (rs.next()) {

                    // receive or create SimulationServerData from database ResultSet
                    String hostname = rs.getString("server_name");
                    if (SimulationControl.this.simulationServerDataMap.containsKey(hostname)) {
                        data = SimulationControl.this.simulationServerDataMap.get(hostname);
                        data.update(rs);
                    } else {
                        data = new SimulationServerData(rs);
                        SimulationControl.this.simulationServerDataMap.put(hostname, data);
                    }

                    SimulationControl.this.gui.updateServerData(data);
                }

            } catch (UnknownHostException | SQLException e) {
                e.printStackTrace();
            }

            query = "SELECT * FROM " + SimulationControl.this.dbConnection.getParameters().getString(
                    ParamString.DB_TABLE_PROCESSES) + " WHERE end_time IS NULL";
            try (ResultSet rs = SimulationControl.this.dbConnection.executeQuery(query, this)) {

                while (rs.next()) {
                    String hostname = rs.getString("host");
                    if (SimulationControl.this.simulationServerDataMap.containsKey(hostname)) {

                        data = SimulationControl.this.simulationServerDataMap.get(hostname);
                        data.setServerProcessInfo(rs);
                        data.setServerState(
                                rs.getBoolean("shutdown") ? ServerControlState.STOP : ServerControlState.BOOT);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * This task updates the SimulationData of all simulations from the
     * database.
     *
     * @author mark_ma
     */
    private class SimulationDataUpdateTask extends TimerTask {

        public SimulationDataUpdateTask() {
            // opens the connection to pass the connectiontest
            try {
                dbConnection.getConnection(this);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        /*
         * (non-Javadoc)
         *
         * @see java.util.TimerTask#run()
         */
        @Override
        public void run() {
            // One update step has to be skipped if there were made changes in
            // the GUI to prevent these changes to be
            // overwritten.
            synchronized (this) {
                if (SimulationControl.this.skipUpdate) {
                    SimulationControl.this.skipUpdate = false;
                    return;
                }
            }

            synchronized (SimulationControl.this.simulationDataMap) {
                String sim_key;
                SimulationData simulationData;
                Set<String> simKeyCollection = new HashSet<>(simulationDataMap.keySet());
                try {
                    if (!SimulationControl.this.dbConnection.checkConnection(this)) return;
                    // retrieve all simulation information from the database
                    ResultSet rs = SimulationControl.this.dbConnection.executeQuery("SELECT * FROM " +
                            SimulationControl.this.dbConnection.getParameters()
                                                               .getString(ParamString.DB_TABLE_SIMULATIONS) +
                            " ORDER BY timestamp_insert", this);
                    while (rs.next()) {
                        // update or create all SimulationData
                        sim_key = rs.getString("sim_key");
                        if (!simKeyCollection.remove(sim_key)) {
                            simulationData = new SimulationData(rs);
                            SimulationControl.this.simulationDataMap.put(simulationData.getKey(), simulationData);
                        } else {
                            simulationData = SimulationControl.this.simulationDataMap.get(sim_key);
                            simulationData.update(rs);
                        }

                        SimulationControl.this.gui.updateSimulationData(simulationData);

                    }
                    rs.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // remove all simulation entries from the GUI which are not in
                // the database
                for (String simKeyCollectionEntry : simKeyCollection) {
                    SimulationData simulation = simulationDataMap.remove(simKeyCollectionEntry);
                    gui.removeSimulation(simulation);
                }
            }
        }
    }

    /**
     * This task retrieves the current database time
     *
     * @author mark_ma
     */
    private class TimeUpdateTask extends TimerTask {

        /*
         * (non-Javadoc)
         *
         * @see java.util.TimerTask#run()
         */
        @Override
        public void run() {
            try {
                ResultSet rs = SimulationControl.this.dbConnection.executeQuery("SELECT now() as ts", this);
                if (rs.next()) {
                    SimulationControl.this.currentDatabaseTimestamp = rs.getTimestamp("ts");
                }
                rs.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}