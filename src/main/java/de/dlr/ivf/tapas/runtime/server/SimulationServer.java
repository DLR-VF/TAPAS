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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
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
    private static String hostname;

    /**
     * contains the generated sha512 hash of the servers jar file
     */
    private static String hashcode;
    /**
     * this will block the {@link #main} method to finish before the shutdown procedure is fully executed
     */
    private static volatile boolean shuttingDown = false;
    /**
     * Prevent {@link RunWhenShuttingDown#run()} from being executed twice when shutting down a server from the GUI.
     * {\@link #killServer()} invokes the {@link RunWhenShuttingDown#run()} procedure, which, when it returns, lets the {@link #main(String[])} finish.
     * As {@link RunWhenShuttingDown} also being a ShutDownHook it will be invoked when {@link #main(String[])} finishes.
     */
    private static volatile boolean shutdown = false;
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

    /**
     * The constructor setup the remote server, binds the remote object and starts the update task
     *
     * @param simulationServerIPAddress IP address of the current network adapter
     * @param tapasNetworkDirectory     tapas network directory path
     * @throws SQLException           This exception is thrown if a connection to the database could not be established.
     * @throws ClassNotFoundException This exception is thrown if the driver for the database was not found.
     */
    private SimulationServer(InetAddress simulationServerIPAddress, File tapasNetworkDirectory) throws SQLException, IOException, ClassNotFoundException {

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
                SimulationServer.hostname + "' AND end_time IS NULL";
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
        SimulationServerUpdateTask stuTask = new SimulationServerUpdateTask();
        this.timer.schedule(stuTask, 10, 750);

        // insert SimulationServer JVM information into DB
        String processidentifier = ManagementFactory.getRuntimeMXBean().getName();
        int processid = Integer.parseInt(processidentifier.split("@")[0]); // e.g. PID@HOSTNAME -> PID
        String ip = "inet '" + simulationServerIPAddress.getHostAddress() + "'";
        String starttime = LocalDateTime.now().toString();
        query = "INSERT INTO " + parameterClass.getString(ParamString.DB_TABLE_PROCESSES) +
                " (identifier, server_ip, p_id, start_time, host, sim_key, sha512) VALUES ('" + processidentifier +
                "', " + ip + ", " + processid + ", '" + starttime + "', '" + SimulationServer.hostname +
                "', 'IDLE', '" + SimulationServer.hashcode + "')";

        this.dbConnector.execute(query, SimulationServer.this);

        query = "UPDATE " + parameterClass.getString(ParamString.DB_TABLE_SERVERS) +
                " SET server_boot_flag = false WHERE server_name = '" + SimulationServer.hostname + "'";
        this.dbConnector.execute(query, SimulationServer.this);
    }

    /**
     * This main method checks all parameters and then setup a remote server for TAPAS simulations
     *
     * @param args args[0]: tapas network directory path
     * @throws Exception this exception is thrown when the server couldn't be setup
     */
    public static void main(String[] args) throws Exception {

        SimulationServer.hostname = IPInfo.getHostname();

        System.out.println(sysoutPrefix + "Starting Tapas. Press Control-c to finish it.");

        Object[] parameters = TPS_Argument.checkArguments(args, PARAMETERS);

        File tapasNetworkDirectory = ((File) parameters[0]).getParentFile();
        SimulationServer simServer = new SimulationServer(IPInfo.getEthernetInetAddress(), tapasNetworkDirectory);

        //simServer.initRMIService();
        simServer.start();

        simServer.join();


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
        Runtime.getRuntime().addShutdownHook(new RunWhenShuttingDown());
        try {
            String sim_key = null;
            String iAddr = IPInfo.getEthernetInetAddress().getHostAddress();
            ResultSet rsGet, rs;
            boolean printMessage = true;

            //clean up broken households from simulations
            String query = "SELECT sim_key FROM " + this.getParameters().getString(ParamString.DB_TABLE_SIMULATIONS) +
                    " " + "WHERE sim_finished = false";
            rs = this.dbConnector.executeQuery(query, this);
            while (rs.next()) {
                sim_key = rs.getString("sim_key");
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

                query = "SELECT core.get_next_simulation() as sim_key";
                rsGet = this.dbConnector.executeQuery(query, this);

                if (rsGet.next()) sim_key = rsGet.getString("sim_key");

                rsGet.close();


                if (sim_key != null) {
                    printMessage = true;
                    System.out.println("Simulation to start: " + sim_key);
                    File file = this.runtimeFile;
//					query = "SELECT sim_file FROM " + this.getParameters().getString(ParamString.DB_TABLE_SIMULATIONS) + " WHERE sim_key= '"+sim_key+"'";
//					rs = this.dbConnector.executeQuery(query,this);
//					File file = null;
//
//					if (rs.next())
//						file = new File(tapasNetworkDirectory, rs.getString("sim_file"));
//					rs.close();
//
                    if (file != null) {
                        //insert the simkey into server_processes table
                        query = "UPDATE server_processes SET sim_key = '" + sim_key + "' WHERE host = '" +
                                SimulationServer.hostname + "'";
                        this.dbConnector.execute(query, this);
                        TPS_Main main = new TPS_Main(file, sim_key);
                        main.run(Runtime.getRuntime().availableProcessors());

                        if (main.getPersistenceManager().finish()) while (SimulationServer.shuttingDown) {
                            System.out.println(sysoutPrefix + "Waiting for Shutdown procedure to finish...");
                            Thread.sleep(1000);
                        }
                        main.getPersistenceManager().close();


                        TPS_Main.STATE.setFinished();
                    } else System.out.println("file is null");
                } else {
                    if (printMessage) {
                        System.out.println(sysoutPrefix + "Waiting for new Simulation");
                        //set sim_key in server_processes table to IDLE
                        query = "UPDATE server_processes SET sim_key = 'IDLE' WHERE host = '" +
                                SimulationServer.hostname + "'";
                        SimulationServer.this.dbConnector.execute(query, this);
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

        /**
         * Inserts the server if necessary and prepares the update statement.
         *
         * @throws SQLException This exception is thrown if there occurs an error inserting the server into the database
         */
        public SimulationServerUpdateTask() throws SQLException {

            TPS_ParameterClass parameterClass = SimulationServer.this.getParameters();
            this.sTable = parameterClass.getString(ParamString.DB_TABLE_SERVERS);

            this.usage = new CPUUsage();
            SimulationServer.this.checkAndReestablishCon(SimulationServer.this);

            this.iAddr = "inet '" + simulationServerIPAddress.getHostAddress() + "'";

            this.procs = Runtime.getRuntime().availableProcessors();

            // insert server into table if necessary
            String query = "SELECT * FROM " + sTable + " WHERE server_name = '" + SimulationServer.hostname + "'";
            ResultSet rs = SimulationServer.this.dbConnector.executeQuery(query, SimulationServer.this);
            if (!rs.next()) {
                query = "INSERT INTO " + sTable + " (server_ip, server_online, server_cores, server_name) VALUES(" +
                        this.iAddr + ", TRUE, " + this.procs + ", '" + SimulationServer.hostname + "')";

                rs.close();
                SimulationServer.this.dbConnector.execute(query, this);
            }

        }

        /*
         * (non-Javadoc)
         *
         * @see java.util.TimerTask#run()
         */
        @Override
        public void run() {
            try {
                ResultSet rset = dbConnector.executeQuery("SELECT * FROM " +
                        SimulationServer.this.getParameters().getString(ParamString.DB_TABLE_PROCESSES) +
                        " WHERE host = '" + hostname + "' AND end_time IS NULL", this);

                if (rset.next()) SimulationServer.shuttingDown = rset.getBoolean("shutdown");
                rset.close();

                if (!SimulationServer.shuttingDown && !SimulationServer.shutdown) {
                    String query = "SELECT server_name FROM " + this.sTable + " WHERE server_name = '" +
                            SimulationServer.hostname + "'";
                    ResultSet rs = SimulationServer.this.dbConnector.executeQuery(query, this);
                    if (rs != null) { //sometimes the connection times out. It will be restablished on the next try...
                        if (rs.next()) {
                            query = "UPDATE " + this.sTable + " SET server_online = TRUE, server_usage = " +
                                    usage.getCPUUsage() + ", server_ip = " + this.iAddr + " WHERE server_name = '" +
                                    SimulationServer.hostname + "'";
                        } else {
                            query = "INSERT INTO " + this.sTable +
                                    " (server_ip, server_online, server_cores, server_name) VALUES(" + this.iAddr +
                                    ", TRUE, " + this.procs + ", '" + SimulationServer.hostname + "')";
                        }
                        SimulationServer.this.dbConnector.execute(query, this);
                        rs.close();
                    }
                }
                if (SimulationServer.shuttingDown) {
                    this.cancel();
                    Thread shutdown = new RunWhenShuttingDown();
                    shutdown.start();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
                if (ex.getNextException() != null) {
                    ex.getNextException().printStackTrace();
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    //~ Inner Classes -----------------------------------------------------------------------------
    private class RunWhenShuttingDown extends Thread {

        public void run() {
            if (!SimulationServer.shutdown) {
                String query;
                boolean tapas_finished = false;
                timer.cancel(); //stop the update process
                System.out.println(sysoutPrefix + "Shutdown initiated...");

                //tell out loop to finish, will let the server process finish
                keepOn = false;

                //tell TPS_Main to finish
                TPS_Main.STATE.setRunning(false);

                try {
                    SimulationServer.this.dbConnector.getConnection(this);

                    for (int i = 0; i <
                            60; i++) { //quick fix to avoid endless looping in case TPS_Main is "hanging"; one could think of implementing a CountDownLatch
                        if (TPS_Main.waitForMe) {
                            System.out.println(sysoutPrefix + "Waiting for TAPAS to finish...");
                            Thread.sleep(1000);
                        } else {
                            tapas_finished = true;
                            break;
                        }
                    }
                    //Thread.sleep(2000); //this allows the timer to cancel before updating the server table.
                    //Otherwise it might happen that changes by DB-trigger functions get overwritten.


                    //update server process in the server_processes table. This will trigger cleanup procedures inside the DB
                    query = "UPDATE " + SimulationServer.this.getParameters().getString(
                            ParamString.DB_TABLE_PROCESSES) + " SET end_time = '" + LocalDateTime.now().toString() +
                            "', tapas_exit_ok = " + tapas_finished + " WHERE host = '" + SimulationServer.hostname +
                            "' AND end_time IS NULL";

                    int rowcount = SimulationServer.this.dbConnector.executeUpdate(query, this);
                    System.out.println(sysoutPrefix + "Updated " + rowcount + " rows from the server processes table.");
                    System.out.println(sysoutPrefix + "Shutdown finished!");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                SimulationServer.shutdown = true;
                SimulationServer.shuttingDown = false;
            }
        }
    }
}
