/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas;

import de.dlr.ivf.tapas.log.LogHierarchy;
import de.dlr.ivf.tapas.log.TPS_Logger;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface.HierarchyLogLevel;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface.SeverenceLogLevel;
import de.dlr.ivf.tapas.mode.TPS_Mode;
import de.dlr.ivf.tapas.mode.TPS_Mode.ModeType;
import de.dlr.ivf.tapas.persistence.TPS_PersistenceManager;
import de.dlr.ivf.tapas.persistence.db.TPS_DB_Connector;
import de.dlr.ivf.tapas.persistence.db.TPS_DB_IOManager;
import de.dlr.ivf.tapas.runtime.client.SimulationControl;
import de.dlr.ivf.tapas.runtime.server.*;
import de.dlr.ivf.tapas.runtime.util.ClientControlProperties;
import de.dlr.ivf.tapas.runtime.util.IPInfo;
import de.dlr.ivf.tapas.util.TPS_Argument;
import de.dlr.ivf.tapas.util.TPS_Argument.TPS_ArgumentType;
import de.dlr.ivf.tapas.util.parameters.ParamFlag;
import de.dlr.ivf.tapas.util.parameters.ParamString;
import de.dlr.ivf.tapas.util.parameters.ParamValue;
import de.dlr.ivf.tapas.util.parameters.TPS_ParameterClass;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;

/**
 * This class is the main entry point into a TAPAS simulation. It provides a main method which can start in different
 * modes indicated by the {@link RunType} of the simulation.
 *
 * @author mark_ma
 */
@LogHierarchy(hierarchyLogLevel = HierarchyLogLevel.CLIENT)
public class TPS_Main {

    public static Map<Integer, Integer[]> actStats = new HashMap<>();
    /**
     * state of the simulation
     */
    public static TPS_State STATE = new TPS_State();
    public static boolean waitForMe = false;
    /// Flag for DEBUG-Modus
    private static final boolean DEBUG = false;
    /**
     * persistence manager of the whole simulation run
     */
    private TPS_PersistenceManager PM = null;
    /**
     * container which holds all parameter values
     */
    private TPS_ParameterClass parameterClass = null;
    private TPS_DB_Connector dbConnector;

    private TPS_Simulator simulator;

    private boolean external_shutdown_received = false;

    /**
     * This constructor reads all parameters and tries to initialise all data which is available via the
     * TPS_PersistenceManager . If any Exception is thrown it is logged and the application stops.
     *
     * @param paramFile   filename of the parameter paramFile with all run information. This paramFile leads to all other files with
     *               e.g.
     *               logging, database, etc. information.
     * @param simKey key of the simulation
     */
    public TPS_Main(File paramFile, File credentialsFile, String simKey) {
        this.parameterClass = new TPS_ParameterClass();

        if (simKey == null) simKey = TPS_Main.getDefaultSimKey();
        try {
            this.parameterClass.setString(ParamString.RUN_IDENTIFIER, simKey);
            this.parameterClass.loadSingleParameterFile(paramFile);
            this.parameterClass.loadSingleParameterFile(credentialsFile);

            File tmpFile = new File(paramFile.getPath());
            while (tmpFile.getParentFile()!=null){
                tmpFile = tmpFile.getParentFile();
            }
            this.parameterClass.setString(ParamString.FILE_WORKING_DIRECTORY, tmpFile.getPath());
            //fix the SUMO-dir by appending the simulation run!
            String paramVal = this.parameterClass.paramStringClass.getString(ParamString.SUMO_DESTINATION_FOLDER);
            paramVal += "_" + simKey;
            this.parameterClass.paramStringClass.setString(ParamString.SUMO_DESTINATION_FOLDER, paramVal);

            Random generator;
            if (this.parameterClass.paramValueClass.isDefined((ParamValue.RANDOM_SEED_NUMBER) ) &&
                    this.parameterClass.paramFlagClass.isTrue(ParamFlag.FLAG_INFLUENCE_RANDOM_NUMBER)) {
                generator = new Random(this.parameterClass.paramValueClass.getLongValue(ParamValue.RANDOM_SEED_NUMBER));
            } else {
                generator = new Random();
            }
            double randomSeed = generator.nextDouble(); // postgres needs a double
            this.parameterClass.paramValueClass.setValue(ParamValue.RANDOM_SEED_NUMBER,randomSeed);
            this.parameterClass.checkParameters();

            try {
                TPS_DB_Connector dbConnector = new TPS_DB_Connector(parameterClass);
                parameterClass.writeAllToDB(dbConnector.getConnection(this), simKey);
                dbConnector.closeConnection(this);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            //now that we stored everything in the DB, we discard a lot and read it back
            this.readConfigFromDB(simKey, credentialsFile);

//            this.parameterClass.setString(ParamString.DB_HOST,dbConnector.getParameters().getString(ParamString.DB_HOST));
//
//            String query = "SELECT * FROM " +
//                    this.parameterClass.getString(ParamString.DB_TABLE_SIMULATION_PARAMETERS) + " WHERE sim_key = '" +
//                    simKey + "'";
//            ResultSet rs = dbConnector.executeQuery(query, this);
//            this.parameterClass.readRuntimeParametersFromDB(rs);
//            rs.close();

            TPS_Logger.log(SeverenceLogLevel.INFO,
                    "Starting iteration: " + this.parameterClass.paramValueClass.getIntValue(ParamValue.ITERATION));
            //  this.parameterClass.checkParameters();

            this.PM = initAndGetPersistenceManager(this.parameterClass);
        } catch (Exception e) {
            TPS_Logger.log(SeverenceLogLevel.FATAL, "Application shutdown: unhandable exception", e);
            throw new RuntimeException(e);
        }
    }


    public void readConfigFromDB(String simKey, File credentialsFile){
        try {
            //now that we stored everything in the DB, we read it back
            this.parameterClass = new TPS_ParameterClass();
            this.parameterClass.loadSingleParameterFile(credentialsFile);
            this.parameterClass.loadSingleParameterFile(credentialsFile);
            TPS_DB_Connector dbConnector = new TPS_DB_Connector(parameterClass);
            String login = null, password = null;
            //read old login
            if (parameterClass.isDefined(ParamString.DB_PASSWORD)) {
                password = parameterClass.getString(ParamString.DB_PASSWORD);
            }

            if (parameterClass.isDefined(ParamString.DB_USER)) {
                login = parameterClass.getString(ParamString.DB_USER);
            }

            String query = "SELECT * FROM " + this.parameterClass.getString(ParamString.DB_TABLE_SIMULATION_PARAMETERS) +
                    " WHERE sim_key = '" + simKey + "'";
            ResultSet rs = dbConnector.executeQuery(query, this);
            this.parameterClass.readRuntimeParametersFromDB(rs);
            rs.close();

            //write old login
            if (password != null) {
                parameterClass.setString(ParamString.DB_PASSWORD, password);
            }
            if (login != null) {
                parameterClass.setString(ParamString.DB_USER, login);
            }
            dbConnector.closeConnection(this);
        } catch (Exception e) {
            TPS_Logger.log(SeverenceLogLevel.FATAL, "Application shutdown: unhandable exception", e);
            throw new RuntimeException(e);
        }
    }

    public TPS_Main(TPS_Simulation simulation) {

        TPS_InitializedSimulation sim_to_run = simulation.initialize();
        this.parameterClass = sim_to_run.getParameters();
        this.PM = initAndGetPersistenceManager(this.parameterClass);
        this.dbConnector = simulation.getDbConnector();
    }

    /**
     * The constructor builds a File from the given filename and calls TPS_Main(File, String).
     *
     * @param parameterFilename filename of the run properties file
     * @param credentialsFilename filename of the db credentials file
     * @param sim_key  key of the simulation
     */
    public TPS_Main(String parameterFilename, String credentialsFilename, String sim_key) {
        this(new File(parameterFilename), new File(credentialsFilename), sim_key);
    }

    /**
     * sim_key
     *
     * @return default simulation key generated by the current timestamp
     */
    public static String getDefaultSimKey() {
        Calendar c = Calendar.getInstance();
        NumberFormat f00 = new DecimalFormat("00");
        NumberFormat f000 = new DecimalFormat("000");
        return c.get(Calendar.YEAR) + "y_" + f00.format(c.get(Calendar.MONTH) + 1) + "m_" + f00.format(
                c.get(Calendar.DAY_OF_MONTH)) + "d_" + f00.format(c.get(Calendar.HOUR_OF_DAY)) + "h_" + f00.format(
                c.get(Calendar.MINUTE)) + "m_" + f00.format(c.get(Calendar.SECOND)) + "s_" + f000.format(
                c.get(Calendar.MILLISECOND)) + "ms";
    }

    /**
     * Main method initialises a TPS_Main and starts the simulation via the run method.
     *
     * @param args Arguments with a R are necessary, these with an O are optional <br>
     *             R args[0] absolute filename of the run configuration file<br>
     *             O args[1] constant for run type [ALL, SIMULATE] <br>
     *             O args[2] simulation key or "new"<br>
     *             O args[3] amount of parallel threads<br>
     */
    public static void main(String[] args) {
        // List of parameters
        List<TPS_ArgumentType<?>> list = new ArrayList<>(5);
        list.add(new TPS_ArgumentType<>("absolute run configuration filename", File.class));
        list.add(new TPS_ArgumentType<>("DB credentials filename", File.class));
        list.add(new TPS_ArgumentType<>("simulation key", String.class, getDefaultSimKey()));
        list.add(new TPS_ArgumentType<>("amount of parallel threads", Integer.class,
                Runtime.getRuntime().availableProcessors()));

        // check parameters
        Object[] parameters = TPS_Argument.checkArguments(args, list);
        File configParam = (File) parameters[0];
        File loginParam = (File) parameters[1];
        String sim_key = (String) parameters[2];
        if(sim_key ==null || sim_key.equalsIgnoreCase("new"))
            sim_key = getDefaultSimKey();
        int numOfThreads = (Integer) parameters[3];

        if(numOfThreads <=0) //if it is negative or zero assume, it i9t a modifier to the max core count
            numOfThreads = Math.max(1,Runtime.getRuntime().availableProcessors()-numOfThreads);
        // initialise and start
        TPS_Main main = new TPS_Main(configParam, loginParam, sim_key);

        main.init();
        main.run(numOfThreads);
        main.finish();
        main.PM.close();

        STATE.setFinished();
    }

    public void insertANewSimulation(String simFile){
        File file = null;
        File propFile = new File("client.properties");
        ClientControlProperties prop = new ClientControlProperties(propFile);


        file = (new File (simFile)).getParentFile();
        // Constructs the client
        SimulationControl control = new SimulationControl(file);
    }

    /**
     * This method drops the temporary tables from the database if necessary.
     */
    public void finish() {
        if (PM instanceof TPS_DB_IOManager) {
            try {
                TPS_DB_IOManager dbManager = (TPS_DB_IOManager) PM;
                dbManager.dropTemporaryTables();
            } catch (Exception e) {
                TPS_Logger.log(SeverenceLogLevel.FATAL, "Application shutdown: unhandable exception", e);
                throw new RuntimeException(e);
            }
        }
    }

    public void initShutdown(){
        this.external_shutdown_received = true;
        TPS_Logger.log(getClass(),SeverenceLogLevel.INFO,"Server shutdown initiated, shutting down simulator...");
        this.simulator.shutdown();
        while(simulator.isRunningSimulation()){
            TPS_Logger.log(getClass(),SeverenceLogLevel.INFO,"Waiting for workers to finish remaining work...");

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * @return persistence manager of the whole simulation run
     */
    public TPS_PersistenceManager getPersistenceManager() {
        return PM;
    }

    /**
     * This method initialises all temporary tables in the database if necessary.
     * This method sets the
     */
    public void init() {
        if (PM instanceof TPS_DB_IOManager) {
            TPS_DB_IOManager dbManager = (TPS_DB_IOManager) PM;
            String projectName = this.parameterClass.paramStringClass.getString(ParamString.PROJECT_NAME);
            String sim_key = this.parameterClass.paramStringClass.getString(ParamString.RUN_IDENTIFIER);


            String hostName = "local"; //default
            try {
                hostName = IPInfo.getHostname();
            } catch (IOException e) {
                e.printStackTrace(); //should never happen or computer has no network, which is BAD!
            }
            String query;

            if(sim_key !=null && !sim_key.equals("")) {
                query = String.format("INSERT INTO simulations (sim_key, sim_file, sim_description,simulation_Server) VALUES('%s', '%s', '%s', '%s')",
                        sim_key, "direct", projectName, hostName);

                dbManager.execute(query);

                query = "SELECT core.prepare_simulation_for_start('" + sim_key + "')";
                dbManager.execute(query);
                query = "UPDATE simulations SET sim_ready = true where sim_key ='"+sim_key+"'";
                dbManager.execute(query);
                query = "UPDATE simulations SET sim_started = true where sim_key ='"+sim_key+"'";
                dbManager.execute(query);
            }
        }
    }

    /**
     * This method initialises the persistence manager and builds all worker threads. The method blocks until all
     * workers have finished the simulation.
     *
     * @param threads amount of parallel threads.
     */
    public void run(int threads) {
        //if (DEBUG) {
        //    threads = 1;
        //}

        initPM();

        if(this.parameterClass.isDefined(ParamFlag.FLAG_SEQUENTIAL_EXECUTION) && this.parameterClass.isTrue(ParamFlag.FLAG_SEQUENTIAL_EXECUTION)){
            this.simulator = new SequentialSimulator(this.PM, this.dbConnector);
        }else{
            this.simulator = new HierarchicalSimulator(this.PM);
        }

        this.simulator.run(threads); //this will block

        if(!external_shutdown_received)
            finish();
    }

    public boolean isRunningSimulation(){
        return this.simulator.isRunningSimulation();
    }

    private void initPM() {
        if (TPS_Logger.isLogging(SeverenceLogLevel.INFO)) {
            TPS_Logger.log(SeverenceLogLevel.INFO, "Initialize Persistence Manager ...");
        }
        long t0 = System.currentTimeMillis();

        PM.init();

        long t1 = System.currentTimeMillis();
        if (TPS_Logger.isLogging(SeverenceLogLevel.INFO)) {
            TPS_Logger.log(SeverenceLogLevel.INFO, "... finished in " + (t1 - t0) * 0.001 + "s");
        }
    }



    private void updateCosts() {

        TPS_Mode mode;
        mode = TPS_Mode.get(ModeType.BIKE);
        mode.velocity = ParamValue.VELOCITY_BIKE;
        mode.cost_per_km = this.parameterClass.paramValueClass.getDoubleValue(ParamValue.BIKE_COST_PER_KM);
        mode.cost_per_km_base = this.parameterClass.paramValueClass.getDoubleValue(ParamValue.BIKE_COST_PER_KM_BASE);
        mode.useBase = this.parameterClass.isDefined(ParamString.DB_NAME_MATRIX_TT_BIKE_BASE);

        mode = TPS_Mode.get(ModeType.MIT);
        mode.velocity = ParamValue.VELOCITY_CAR;
        mode.cost_per_km = this.parameterClass.paramValueClass.getDoubleValue(ParamValue.MIT_GASOLINE_COST_PER_KM);
        mode.cost_per_km_base = this.parameterClass.paramValueClass.getDoubleValue(
                ParamValue.MIT_GASOLINE_COST_PER_KM_BASE);
        mode.variable_cost_per_km = this.parameterClass.paramValueClass.getDoubleValue(
                ParamValue.MIT_VARIABLE_COST_PER_KM);
        mode.variable_cost_per_km_base = this.parameterClass.paramValueClass.getDoubleValue(
                ParamValue.MIT_VARIABLE_COST_PER_KM_BASE);
        mode.useBase = this.parameterClass.isDefined(ParamString.DB_NAME_MATRIX_TT_MIT_BASE);

        mode = TPS_Mode.get(ModeType.MIT_PASS);
        mode.velocity = ParamValue.VELOCITY_CAR;
        mode.cost_per_km = this.parameterClass.paramValueClass.getDoubleValue(ParamValue.PASS_COST_PER_KM);
        mode.cost_per_km_base = this.parameterClass.paramValueClass.getDoubleValue(ParamValue.PASS_COST_PER_KM_BASE);
        mode.useBase = this.parameterClass.isDefined(ParamString.DB_NAME_MATRIX_TT_MIT_BASE);

        mode = TPS_Mode.get(ModeType.TAXI);
        mode.velocity = ParamValue.VELOCITY_CAR;
        mode.cost_per_km = this.parameterClass.paramValueClass.getDoubleValue(ParamValue.TAXI_COST_PER_KM);
        mode.cost_per_km_base = this.parameterClass.paramValueClass.getDoubleValue(ParamValue.TAXI_COST_PER_KM_BASE);
        mode.useBase = this.parameterClass.isDefined(ParamString.DB_NAME_MATRIX_TT_MIT_BASE);

        mode = TPS_Mode.get(ModeType.PT);
        mode.velocity = ParamValue.VELOCITY_TRAIN;
        mode.cost_per_km = this.parameterClass.paramValueClass.getDoubleValue(ParamValue.PT_COST_PER_KM);
        mode.cost_per_km_base = this.parameterClass.paramValueClass.getDoubleValue(ParamValue.PT_COST_PER_KM_BASE);
        mode.useBase = this.parameterClass.isDefined(ParamString.DB_NAME_MATRIX_TT_PT_BASE);

        mode = TPS_Mode.get(ModeType.CAR_SHARING);
        mode.velocity = ParamValue.VELOCITY_CAR;
        mode.cost_per_km = this.parameterClass.paramValueClass.getDoubleValue(ParamValue.MIT_VARIABLE_COST_PER_KM);
        mode.cost_per_km_base = this.parameterClass.paramValueClass.getDoubleValue(ParamValue.MIT_VARIABLE_COST_PER_KM_BASE);
        mode.useBase = this.parameterClass.isDefined(ParamString.DB_NAME_MATRIX_TT_MIT_BASE);

        mode = TPS_Mode.get(ModeType.WALK);
        mode.velocity = ParamValue.VELOCITY_FOOT;
        mode.cost_per_km = this.parameterClass.paramValueClass.getDoubleValue(ParamValue.WALK_COST_PER_KM);
        mode.cost_per_km_base = this.parameterClass.paramValueClass.getDoubleValue(ParamValue.WALK_COST_PER_KM_BASE);
        mode.useBase = this.parameterClass.isDefined(ParamString.DB_NAME_MATRIX_TT_WALK_BASE);

    }

    /**
     * This enumeration lists all run types for the simulation.
     *
     * @author mark_ma
     */
    public enum RunType {
        /**
         * This state indicates, that a local simulation is running, which has to initialise and finish itself.
         */
        ALL,
        /**
         * This state indicates, that a remote simulation is running, which only simulates the households and does
         * nothing else.
         */
        SIMULATE,
        NEW_SIM
    }

    /**
     * This class represents the current state of the simulation.
     *
     * @author mark_ma
     */
    public static class TPS_State {
        /**
         * Flag if the simulation has finished
         */
        private boolean finished;

        /**
         * Flag if the simulation is running
         */
        private boolean running;

        /**
         * Initialises the flags.
         */
        public TPS_State() {
            running = true;
            finished = false;
        }

        /**
         * @return finished flag
         */
        public boolean isFinished() {
            return finished;
        }

        /**
         * @return running flag
         */
        public boolean isRunning() {
            return running;
        }

        /**
         * @param running new running flag value
         */
        public void setRunning(boolean running) {
            synchronized (this) {
                this.running = running;
                this.notifyAll();
            }
        }

        /**
         * Sets the finished flag on true and wakes up all waiting threads.
         */
        public void setFinished() {
            synchronized (this) {
                this.finished = true;
                //this.setRunning(false);
                this.notifyAll();
            }
        }

        /**
         * Waits for the end of this simulation.
         *
         * @param timeout timeout in milliseconds to wait. The value 0 indicates an endless waiting.
         */
        public void waitFor(long timeout) {
            if (timeout > 0) {
                long start = System.currentTimeMillis();
                long duration = 0;
                synchronized (this) {
                    while ((duration = System.currentTimeMillis() - start) < timeout && !this.isFinished()) {
                        try {
                            this.wait(timeout - duration + 10);
                        } catch (InterruptedException e) {
                            // nothing to do just wait
                        }
                    }
                }
            } else {
                synchronized (this) {
                    while (!this.isFinished()) {
                        try {
                            this.wait();
                        } catch (InterruptedException e) {
                            // nothing to do just wait
                        }
                    }
                }
            }
        }
    }

    private TPS_PersistenceManager initAndGetPersistenceManager(TPS_ParameterClass parameters){

        try {
            Class<?> clazz = Class.forName(parameters.getString(ParamString.CLASS_DATA_SCOURCE_ORIGIN));
            return (TPS_PersistenceManager) clazz.getConstructor(TPS_ParameterClass.class).newInstance(parameters);
        } catch (Exception e) {
            TPS_Logger.log(SeverenceLogLevel.FATAL, "Application shutdown: unhandable exception", e);
            throw new RuntimeException(e);
        }

    }
}
