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
import de.dlr.ivf.tapas.person.TPS_Worker;
import de.dlr.ivf.tapas.util.TPS_Argument;
import de.dlr.ivf.tapas.util.TPS_Argument.TPS_ArgumentType;
import de.dlr.ivf.tapas.util.parameters.ParamString;
import de.dlr.ivf.tapas.util.parameters.ParamValue;
import de.dlr.ivf.tapas.util.parameters.TPS_ParameterClass;

import java.io.File;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
    private final TPS_PersistenceManager PM;
    /**
     * container which holds all parameter values
     */
    private final TPS_ParameterClass parameterClass;

    /**
     * This constructor reads all parameters and tries to initialise all data which is available via the
     * TPS_PersistenceManager . If any Exception is thrown it is logged and the application stops.
     *
     * @param file   filename of the parameter file with all run information. This file leads to all other files with
     *               e.g.
     *               logging, database, etc. information.
     * @param simKey key of the simulation
     */
    public TPS_Main(File file, String simKey) {
        this.parameterClass = new TPS_ParameterClass();

        if (simKey == null) simKey = TPS_Main.getDefaultSimKey();
        try {
            this.parameterClass.setString(ParamString.RUN_IDENTIFIER, simKey);
            this.parameterClass.loadRuntimeParameters(file);
            TPS_DB_Connector dbConnector = new TPS_DB_Connector(parameterClass);

            File tmpFile = new File(file.getPath());
            while (!tmpFile.getPath().endsWith(
                    this.parameterClass.SIM_DIR.substring(0, this.parameterClass.SIM_DIR.length() - 1))) {
                tmpFile = tmpFile.getParentFile();
            }
            this.parameterClass.setString(ParamString.FILE_WORKING_DIRECTORY, tmpFile.getParent());

            //try to load parameters from db
            dbConnector.readRuntimeParametersFromDB(simKey);
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

            dbConnector.closeConnection(this);
            Class<?> clazz = Class.forName(this.parameterClass.getString(ParamString.CLASS_DATA_SCOURCE_ORIGIN));
            PM = (TPS_PersistenceManager) clazz.getConstructor(TPS_ParameterClass.class).newInstance(
                    this.parameterClass);
        } catch (Exception e) {
            TPS_Logger.log(SeverenceLogLevel.FATAL, "Application shutdown: unhandable exception", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * The constructor builds a File from the given filename and calls TPS_Main(File, String).
     *
     * @param filename filename of the run properties file
     * @param sim_key  key of the simulation
     */
    public TPS_Main(String filename, String sim_key) {
        this(new File(filename), sim_key);
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
     *             O args[2] simulation key<br>
     *             O args[3] amount of parallel threads<br>
     */
    public static void main(String[] args) {
        // List of parameters
        List<TPS_ArgumentType<?>> list = new ArrayList<>(4);
        list.add(new TPS_ArgumentType<>("absolute run configuration filename", File.class));
        list.add(new TPS_ArgumentType<>("constant for run type", RunType.class, RunType.ALL));
        list.add(new TPS_ArgumentType<>("simulation key", String.class, getDefaultSimKey()));
        list.add(new TPS_ArgumentType<>("amount of parallel threads", Integer.class,
                Runtime.getRuntime().availableProcessors()));

        // check parameters
        Object[] parameters = TPS_Argument.checkArguments(args, list);

        // initialise and start
        TPS_Main main = new TPS_Main((File) parameters[0], (String) parameters[2]);
        int numOfThreads = (Integer) parameters[3];

        switch ((RunType) parameters[1]) {
            // REMOTE
            case SIMULATE:
                main.run(numOfThreads);
                break;
            // LOCAL
            case ALL:
                main.init();
                main.run(numOfThreads);
                main.finish();
                break;
        }
        main.PM.close();

        STATE.setFinished();
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
            dbManager.createTemporaryAndOutputTables();
        }
    }

    /**
     * This method initialises the persistence manager and builds all worker threads. The method blocks until all
     * workers have finished the simulation.
     *
     * @param threads amount of parallel threads.
     */
    public void run(int threads) {
        long t0, t1;
        actStats.clear();
        try {
            waitForMe = true;
            // initialise persistence manager
            if (TPS_Logger.isLogging(SeverenceLogLevel.INFO)) {
                TPS_Logger.log(SeverenceLogLevel.INFO, "Initialize Persistence Manager ...");
            }
            t0 = System.currentTimeMillis();
            PM.init();
            t1 = System.currentTimeMillis();
            if (TPS_Logger.isLogging(SeverenceLogLevel.INFO)) {
                TPS_Logger.log(SeverenceLogLevel.INFO, "... finished in " + (t1 - t0) * 0.001 + "s");
            }
            if (DEBUG) {
                threads = 1;
            }
            threads = Math.min(48, threads); // limit to 48 cores
            // now update the costs variables for the modes, which are not initialized again!
            this.updateCosts();

            // start worker threads
            if (TPS_Logger.isLogging(SeverenceLogLevel.INFO)) {
                TPS_Logger.log(SeverenceLogLevel.INFO, "Initialize " + threads + " working threads ...");
            }


            ExecutorService es = Executors.newFixedThreadPool(threads);
            List<Future<Exception>> col = new LinkedList<>();
            for (int activeThreads = 0; activeThreads < threads; activeThreads++) {
                col.add(es.submit(new TPS_Worker(this.PM)));
            }

            // wait for worker threads
            if (TPS_Logger.isLogging(SeverenceLogLevel.INFO)) {
                TPS_Logger.log(SeverenceLogLevel.INFO, "... wait for all working threads ...");
            }

            for (Future<Exception> future : col) {
                future.get();
            }
            es.shutdown();
            // reset unfinished households
            //PM.resetUnfinishedHouseholds();

            waitForMe = false;
        } catch (Exception e) {
            waitForMe = false;
            TPS_Logger.log(SeverenceLogLevel.FATAL, "Application shutdown: unhandable exception", e);
        }
        TPS_Logger.closeLoggers();
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

        mode = TPS_Mode.get(ModeType.TRAIN);
        mode.velocity = ParamValue.VELOCITY_TRAIN;
        mode.cost_per_km = this.parameterClass.paramValueClass.getDoubleValue(ParamValue.TRAIN_COST_PER_KM);
        mode.cost_per_km_base = this.parameterClass.paramValueClass.getDoubleValue(ParamValue.TRAIN_COST_PER_KM_BASE);
        mode.useBase = this.parameterClass.isDefined(ParamString.DB_NAME_MATRIX_TT_PT_BASE);

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
}
