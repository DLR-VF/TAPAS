/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.runtime.server;

import de.dlr.ivf.tapas.TPS_Main;
import de.dlr.ivf.tapas.persistence.db.TPS_DB_Connector;
import de.dlr.ivf.tapas.runtime.util.CPUUsage;
import de.dlr.ivf.tapas.runtime.util.DaemonControlProperties;
import de.dlr.ivf.tapas.runtime.util.DaemonControlProperties.DaemonControlPropKey;
import de.dlr.ivf.tapas.runtime.util.IPInfo;
import de.dlr.ivf.tapas.util.Checksum;
import de.dlr.ivf.tapas.util.Checksum.HashType;
import de.dlr.ivf.tapas.util.TPS_Argument;
import de.dlr.ivf.tapas.util.TPS_Argument.TPS_ArgumentType;
import de.dlr.ivf.tapas.util.parameters.ParamFlag;
import de.dlr.ivf.tapas.util.parameters.ParamString;
import de.dlr.ivf.tapas.util.parameters.TPS_ParameterClass;
import org.apache.commons.lang3.SystemUtils;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;

/**
 * The main method of this class is used to setup a TAPAS remote server. Each machine which is registered in the
 * database in the public.sim_servers table and where the server is running there can be remote simulations for tapas
 * started.
 *
 * @author mark_ma
 */
public class SimulationServer extends Thread {

    /**
     * Identifying String for the command line output
     */
    private final static String sysoutPrefix = "-- Server -- ";
    /**
     * Parameter array with all parameters for this main method.<br>
     * PARAMETERS[0] = tapas network directory path
     */
    private static final TPS_ArgumentType<?>[] PARAMETERS = {new TPS_ArgumentType<>("tapas network directory path",
            File.class)};

    /**
     * the hostname extracted by {@link IPInfo#getHostname()}
     */
    private String hostname;

    /**
     * contains the generated sha512 hash of the servers jar file
     */
    private static String hashcode;
    /**
     * this will block the {@link #main} method to finish before the shutdown procedure is fully executed
     */
    private boolean shuttingDown = false;
    /**
     * Prevent {@link RunWhenShuttingDown#run()} from being executed twice when shutting down a server from the GUI.
     * {\@link #killServer()} invokes the {@link RunWhenShuttingDown#run()} procedure, which, when it returns, lets the {@link #main(String[])} finish.
     * As {@link RunWhenShuttingDown} also being a ShutDownHook it will be invoked when {@link #main(String[])} finishes.
     */
    private boolean shutdown = false;
    /**
     * The reference to the connection manager, needed to reestablish the connection, if necessary
     */
    private final TPS_DB_Connector dbConnector;
    /**
     * IP Address of this {@link SimulationServer}
     */
    private final InetAddress simulationServerIPAddress;
    /**
     * runtime file including the user credentials
     */
    private final File runtimeFile;
    /**
     * Timer for the {@link SimulationServerUpdateTask} task
     */
    private final Timer timer;
    private volatile boolean keepOn = true;

    TPS_Main current_simulation_run = null;

    /**
     * The constructor setup the remote server, binds the remote object and starts the update task
     *
     * @param simulationServerIPAddress IP address of the current network adapter
     * @param tapasNetworkDirectory     tapas network directory path
     * @throws SQLException           This exception is thrown if a connection to the database could not be established.
     * @throws ClassNotFoundException This exception is thrown if the driver for the database was not found.
     */
    private SimulationServer(InetAddress simulationServerIPAddress, File tapasNetworkDirectory, String hostname) throws SQLException, IOException, ClassNotFoundException {
        this.hostname = hostname;

        File propFile = new File("daemon.properties");
        DaemonControlProperties prop = new DaemonControlProperties(propFile);
        String runtimeConf = prop.get(DaemonControlPropKey.LOGIN_CONFIG);
        if (runtimeConf == null || runtimeConf.length() == 0) {
            runtimeConf = "runtime.csv";
            prop.set(DaemonControlPropKey.LOGIN_CONFIG, runtimeConf);
            prop.updateFile();
        }

        TPS_ParameterClass parameterClass = new TPS_ParameterClass();
        this.runtimeFile = new File(new File(tapasNetworkDirectory, parameterClass.SIM_DIR), runtimeConf);
        parameterClass.loadRuntimeParameters(this.runtimeFile);
        parameterClass.setString(ParamString.FILE_WORKING_DIRECTORY, tapasNetworkDirectory.getPath());

        this.dbConnector = new TPS_DB_Connector(parameterClass);
        //check if there is a SimulationServer zombie process currently running
        String query = "SELECT * FROM " + parameterClass.getString(ParamString.DB_TABLE_PROCESSES) + " WHERE host = '" +
                hostname + "' AND end_time IS NULL";
        ResultSet rs = this.dbConnector.executeQuery(query, this);

        while (rs.next()) {
            String pid = rs.getString("p_id");
            System.out.println(sysoutPrefix + "Killing zombie process with PID " + pid);
            killProcess(pid);
        }
        rs.close();

        // initialize checksum on the TAPAS jar
        String debugString = System.getProperty("debug");

        if (debugString != null && debugString.equalsIgnoreCase("true")) {
            SimulationServer.hashcode = "DEBUG";
        } else {
            String path = this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
            try {
                String decodedPath = URLDecoder.decode(path, StandardCharsets.UTF_8);
                SimulationServer.hashcode = Checksum.generateFileChecksum(new File(decodedPath), HashType.SHA512);
            } catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
                SimulationServer.hashcode = "";
                e.printStackTrace();
            }
        }

        // Set ip address member
        this.simulationServerIPAddress = simulationServerIPAddress;

        // initialise timer and server table update task
        this.timer = new Timer();
        SimulationServerUpdateTask stuTask = new SimulationServerUpdateTask(this, this.dbConnector);
        this.timer.schedule(stuTask, 10, 750);

        // insert SimulationServer JVM information into DB
        String processidentifier = ManagementFactory.getRuntimeMXBean().getName();
        int processid = Integer.parseInt(processidentifier.split("@")[0]); // e.g. PID@HOSTNAME -> PID
        String ip = "inet '" + simulationServerIPAddress.getHostAddress() + "'";
        String starttime = LocalDateTime.now().toString();
        query = "INSERT INTO " + parameterClass.getString(ParamString.DB_TABLE_PROCESSES) +
                " (identifier, server_ip, p_id, start_time, host, sim_key, sha512) VALUES ('" + processidentifier +
                "', " + ip + ", " + processid + ", '" + starttime + "', '" + hostname +
                "', 'IDLE', '" + SimulationServer.hashcode + "')";

        this.dbConnector.execute(query, SimulationServer.this);

        query = "UPDATE " + parameterClass.getString(ParamString.DB_TABLE_SERVERS) +
                " SET server_boot_flag = false WHERE server_name = '" + hostname + "'";
        this.dbConnector.execute(query, SimulationServer.this);
    }

    /**
     * This main method checks all parameters and then setup a remote server for TAPAS simulations
     *
     * @param args args[0]: tapas network directory path
     * @throws Exception this exception is thrown when the server couldn't be setup
     */
    public static void main(String[] args) throws Exception {

        System.out.println(sysoutPrefix + "Starting Tapas. Press Control-c to finish it.");

        Object[] parameters = TPS_Argument.checkArguments(args, PARAMETERS);

        File tapasNetworkDirectory = ((File) parameters[0]).getParentFile();
        SimulationServer simServer = new SimulationServer(IPInfo.getEthernetInetAddress(), tapasNetworkDirectory, IPInfo.getHostname());

        //simServer.initRMIService();
        simServer.start();

        simServer.join();


    }

    public String getHostname(){
        return this.hostname;
    }

    private boolean checkAndReestablishCon(Object caller) throws SQLException {
        boolean returnVal = true;
        if (this.dbConnector.getConnection(caller).isClosed()) {
            returnVal = false;
            this.dbConnector.getConnection(caller);
        }
        return returnVal;
    }

    public TPS_ParameterClass getParameters() {
        return this.dbConnector.getParameters();
    }

    /**
     * Forcibly kills a process by its PID
     *
     * @param pid the process identification
     * @throws IOException if an I/O error occurs
     */
    private void killProcess(String pid) throws IOException {

        if (SystemUtils.IS_OS_WINDOWS) Runtime.getRuntime().exec("taskkill /F /PID " + pid);
        else Runtime.getRuntime().exec("kill -9 " + pid);

    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Thread#run()
     */
    public void run() {
        Runtime.getRuntime().addShutdownHook(new RunWhenShuttingDown(this.dbConnector, this));
        try {
            String iAddr = IPInfo.getEthernetInetAddress().getHostAddress();
            ResultSet rsGet, rs;
            boolean printMessage = true;

            //clean up broken households from simulations that are computed classically. The latter are those that do not have
            //an associated server in the "simulation_server" column
            String query = "SELECT * FROM " + this.getParameters().getString(ParamString.DB_TABLE_SIMULATIONS) +
                    " " + "WHERE sim_finished = false AND sim_ready = true AND sim_started = true AND simulation_server IS NULL";
            rs = this.dbConnector.executeQuery(query, this);
            while (rs.next()) {
                String sim_key = rs.getString("sim_key");
                query = "SELECT core.reset_unfinished_households('" + sim_key + "','" + iAddr + "')";
                rsGet = this.dbConnector.executeQuery(query, this);
                if (rsGet.next()) {
                    if (rsGet.getInt(1) > 0) System.out.println(
                            sysoutPrefix + "Cleaning up crashed simulation " + sim_key + ": Reset " + rsGet.getInt(1) +
                                    " households");
                }
                rsGet.close();
            }
            rs.close();

            while (keepOn) {

                Optional<TPS_Simulation> next_simulation = getNextSimulationToProcess(dbConnector, runtimeFile, hostname);

                if (next_simulation.isPresent()) {

                    TPS_Simulation simulation = next_simulation.get();

                    printMessage = true;

                    String simulation_key = simulation.getSimulationKey();
                    System.out.println("Simulation to start: " + simulation_key);

                    //insert the simkey into server_processes table
                    query = "UPDATE server_processes SET sim_key = '" + simulation_key + "' WHERE host = '" + hostname + "'";
                    this.dbConnector.execute(query, this);

                    this.current_simulation_run = new TPS_Main(simulation);

                    //starting a simulation blocks this thread
                    this.current_simulation_run.run(Runtime.getRuntime().availableProcessors());
                } else {

                    if (printMessage) {
                        System.out.println(sysoutPrefix + "Waiting for new Simulation");
                        //set sim_key in server_processes table to IDLE
                        query = "UPDATE server_processes SET sim_key = 'IDLE' WHERE host = '" + hostname + "'";
                        this.dbConnector.execute(query, this);
                        printMessage = false;
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * This task inserts the current simulation server into the database if necessary. Then it updates the corresponding
     * entry with the current CPU usage.
     *
     * @author mark_ma
     */
    private class SimulationServerUpdateTask extends TimerTask {

        /**
         * Instance to determine the CPU usage
         */
        private final CPUUsage usage;

        private final String iAddr;

        private final int procs;
        private final String sTable;

        private final SimulationServer server;
        private final TPS_DB_Connector dbConnector;

        /**
         * Inserts the server if necessary and prepares the update statement.
         *
         * @throws SQLException This exception is thrown if there occurs an error inserting the server into the database
         */
        public SimulationServerUpdateTask(SimulationServer server, TPS_DB_Connector dbConnector) throws SQLException {

            this.server = server;
            this.dbConnector = dbConnector;

            TPS_ParameterClass parameterClass = server.getParameters();
            this.sTable = parameterClass.getString(ParamString.DB_TABLE_SERVERS);

            this.usage = new CPUUsage();
            checkAndReestablishCon(server);

            this.iAddr = "inet '" + simulationServerIPAddress.getHostAddress() + "'";

            this.procs = Runtime.getRuntime().availableProcessors();

            // insert server into table if necessary
            String query = "SELECT * FROM " + sTable + " WHERE server_name = '" + server.getHostname() + "'";
            ResultSet rs = dbConnector.executeQuery(query, server);
            if (!rs.next()) {
                query = "INSERT INTO " + sTable + " (server_ip, server_online, server_cores, server_name) VALUES(" +
                        this.iAddr + ", TRUE, " + this.procs + ", '" + server.getHostname() + "')";

                rs.close();
                dbConnector.execute(query, this);
            }

        }

        /*
         * (non-Javadoc)
         *
         * @see java.util.TimerTask#run()
         */
        @Override
        public void run() {

                boolean external_shutdown = false;

                try(ResultSet rset = dbConnector.executeQuery("SELECT * FROM " +
                        server.getParameters().getString(ParamString.DB_TABLE_PROCESSES) +
                        " WHERE host = '" + hostname + "' AND end_time IS NULL", this))
                {
                    if (rset.next())
                        external_shutdown = rset.getBoolean("shutdown");


                }catch(Exception e){
                    e.printStackTrace();
                }

                if(!external_shutdown){
                    updateServerInDatabase();
                }else{
                    cancel(); //cancel the timer
                    initiateServerShutdown();
                }

        }

        private void initiateServerShutdown() {
            this.server.initiateShutdown();
        }

        private void updateServerInDatabase() {

            String hostname = server.getHostname();
            String query = "SELECT server_name FROM " + this.sTable + " WHERE server_name = '"+ hostname +"'";
            try(ResultSet rs = dbConnector.executeQuery(query, this)) {

                if (rs.next()) {
                    query = "UPDATE " + this.sTable + " SET server_online = TRUE, server_usage = " +
                            usage.getCPUUsage() + ", server_ip = " + this.iAddr + " WHERE server_name = '" +
                            hostname + "'";
                } else {
                    query = "INSERT INTO " + this.sTable +
                            " (server_ip, server_online, server_cores, server_name) VALUES(" + this.iAddr +
                            ", TRUE, " + this.procs + ", '" + hostname + "')";
                }
                dbConnector.execute(query, this);
            }catch(SQLException | IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void initiateShutdown() {
        this.keepOn = false;
        if(this.current_simulation_run != null){
            this.current_simulation_run.initShutdown();
        }
    }

    public boolean isManuallyShutDown(){
        return !this.keepOn;
    }

    //~ Inner Classes -----------------------------------------------------------------------------
    private class RunWhenShuttingDown extends Thread {
        private final TPS_DB_Connector dbConnector;
        private final SimulationServer server;

        public RunWhenShuttingDown(TPS_DB_Connector dbConnector, SimulationServer server) {
            this.dbConnector = dbConnector;
            this.server = server;
        }

        public void run() {
            //update server process in the server_processes table. This will trigger cleanup procedures inside the DB
            String query = "UPDATE " + server.getParameters().getString(
                    ParamString.DB_TABLE_PROCESSES) + " SET end_time = '" + LocalDateTime.now().toString() +
                    "', tapas_exit_ok = " + !server.isManuallyShutDown() + " WHERE host = '" + server.getHostname() +
                    "' AND end_time IS NULL";

            int rowcount = dbConnector.executeUpdate(query, this);
            System.out.println(sysoutPrefix + "Updated " + rowcount + " rows from the server processes table.");
            System.out.println(sysoutPrefix + "Shutdown finished!");

            query = "UPDATE " + server.getParameters().getString(ParamString.DB_TABLE_SERVERS) + " SET server_online = false WHERE server_name = '" + server.getHostname() + "'";
            dbConnector.execute(query, this);
        }
    }

    private Optional<TPS_Simulation> getNextSimulationToProcess(TPS_DB_Connector dbConnector, File runtime_file, String hostname){

        Connection connection = null;
        TPS_Simulation simulation = null;

        try {
            connection = dbConnector.getConnection(this);


            TPS_ParameterClass parameterClass = dbConnector.getParameters();

            File tmpFile = new File(runtime_file.getPath());
            while (!tmpFile.getPath().endsWith(
                    parameterClass.SIM_DIR.substring(0, parameterClass.SIM_DIR.length() - 1))) {
                tmpFile = tmpFile.getParentFile();
            }
            parameterClass.setString(ParamString.FILE_WORKING_DIRECTORY, tmpFile.getParent());

            String simulations_table_name = parameterClass.getString(ParamString.DB_TABLE_SIMULATIONS);

            //take manual control over the transaction
            connection.setAutoCommit(false);

            String lock = "LOCK TABLE " + simulations_table_name + " IN ACCESS EXCLUSIVE MODE;";

            connection.createStatement().execute(lock);

            String available_simulations = "SELECT * FROM " + simulations_table_name +
                    " WHERE sim_finished = false" +
                    " AND sim_ready = true" +
                    " AND sim_started = true" +
                    " AND (simulation_server IS NULL OR simulation_server = '')" +
                    " ORDER BY timestamp_insert LIMIT 1";

            ResultSet available_simulation = dbConnector.executeQuery(available_simulations, this);

            if (available_simulation.next()) {

                String sim_key = available_simulation.getString("sim_key");


                parameterClass.setString(ParamString.RUN_IDENTIFIER, sim_key);
                dbConnector.readRuntimeParametersFromDB(sim_key);

                if (parameterClass.isDefined(ParamFlag.FLAG_SEQUENTIAL_EXECUTION) &&
                        parameterClass.isTrue(ParamFlag.FLAG_SEQUENTIAL_EXECUTION)) { //insert the server that has dedicated itself

                    String update_host_query = "UPDATE " + simulations_table_name + " SET simulation_server = '" + hostname + "' WHERE sim_key = '" + sim_key + "'";
                    connection.createStatement().execute(update_host_query);
                }

                simulation = new TPS_Simulation(sim_key, dbConnector);
            }

            connection.commit();
            connection.setAutoCommit(true);

        } catch (SQLException throwables) {
            try {
                if(connection != null)
                    connection.rollback();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            throwables.printStackTrace();
        }

        return Optional.ofNullable(simulation);
    }

    public Optional<TPS_Main> getCurrentSimulationRun(){
        return Optional.ofNullable(this.current_simulation_run);
    }

}
