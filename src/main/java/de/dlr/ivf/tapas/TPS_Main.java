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
import de.dlr.ivf.tapas.util.parameters.ParamFlag;
import de.dlr.ivf.tapas.util.parameters.ParamString;
import de.dlr.ivf.tapas.util.parameters.ParamValue;
import de.dlr.ivf.tapas.util.parameters.TPS_ParameterClass;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
/**
 * This class is the main entry point into a TAPAS simulation. It provides a main method which can start in different
 * modes indicated by the {@link RunType} of the simulation.
 *
 * @author mark_ma
 */
@Command(name = "TPS_Main", mixinStandardHelpOptions = true, description = "Starts a specified TAPAS simulation.")
@LogHierarchy(hierarchyLogLevel = HierarchyLogLevel.CLIENT)
public class TPS_Main  implements Callable<Integer> {


    @Option(names = { "-c", "--configuration" }, required = true, paramLabel = "<configuration file>", description = "Configuration file of the simulation.")
    private String				configFile;

    @Option(names = { "-d",
            "--database-configuration-file" }, required = true, paramLabel = "<path-to-database-configuration-file>", description = "Loads the database connection parameters from configuration file.")
    private String				dbConfFile;

    @Option(names = { "-s", "--simulation-key" }, defaultValue = "NEW", paramLabel = "<sim key>", description = "Specifies a Simulation key to run")
    private String				simKey;

    @Option(names = { "-t", "--threads" }, defaultValue = "-1", paramLabel = "<threads>", description = "Number of threads to use, or -1 for all available CPUs.")
    private Integer				threads;

    @Option(names = { "-o", "--output" }, defaultValue = "", paramLabel = "<output file>", description = "File to write the trips to, or null if no export is whished.")
    private String				outputFile;

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
     * If you call TPS_Mail this way, yu have to call init(String parameterFilename, String credentialsFilename, String sim_key).
     */
    public TPS_Main(){
    }

    public void init(String parameterFilename, String credentialsFilename, String sim_key) {
        this.init(new File(parameterFilename), new File(credentialsFilename), sim_key);
    }

    public void init(File paramFile, File credentialsFile, String simKey) {
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
            this.init();
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

     */
    public static void main(String... args) {
        System.exit(new CommandLine(new TPS_Main()).execute(args));
    }
    @Override
    public Integer call() throws Exception {


        // check parameters
        File configParam = new File(configFile) ;
        File loginParam = new File(dbConfFile) ;
        if(simKey ==null || simKey.equalsIgnoreCase("new"))
            simKey = getDefaultSimKey();

        if(threads <=0) //if it is negative or zero assume, it i9t a modifier to the max core count
            threads = Math.max(1,Runtime.getRuntime().availableProcessors()-threads);
        // initialise and start
        this.init(configParam, loginParam, simKey);
        this.run(threads);
        this.finish();
        this.PM.close();

        STATE.setFinished();
        if(this.outputFile!=null && this.outputFile.length()>0){
            try {
                exportSimulationToFile();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        this.writeSimKeyToDisk("simkey.txt");
        return 0;
    }

    private void writeSimKeyToDisk(String name)throws  IOException {
        FileWriter fw = new FileWriter(new File(name));
        fw.write(simKey+"\n");
        fw.close();
    }

    private void exportSimulationToFile() throws SQLException, IOException, ClassNotFoundException {
        TPS_DB_Connector dbConnector = new TPS_DB_Connector(parameterClass);
        String schemaname = dbConnector.readParameter(simKey, "DB_SCHEMA_CORE",this);
        String region = dbConnector.readParameter(simKey, "DB_REGION",this);
        String tablename = dbConnector.readParameter(simKey, "DB_TABLE_TRIPS",this);
        tablename = tablename+"_"+simKey;
        String table_household = dbConnector.readParameter(simKey, "DB_TABLE_HOUSEHOLD",this);
        table_household = schemaname+table_household;
        String table_locations = dbConnector.readParameter(simKey, "DB_TABLE_LOCATION",this);
        table_locations = schemaname+table_locations;
        String table_persons = dbConnector.readParameter(simKey, "DB_TABLE_PERSON",this);
        table_persons = schemaname+table_persons;
        String p_hh_key = dbConnector.readParameter(simKey, "DB_HOUSEHOLD_AND_PERSON_KEY",this);
        String loc_key = dbConnector.readParameter(simKey, "DB_LOCATION_KEY",this);
        String table_taz = dbConnector.readParameter(simKey, "DB_TABLE_TAZ",this);
        table_taz = schemaname+table_taz;

        String command;

        command =
        "SELECT "+
                // Person attributes from $region_persons ps
        "ps.p_id,"+
        "ps.group as p_group,"+
        "ps.age as p_age,"+
        "ps.sex as p_sex,"+
        "(ps.pt_abo = 1)::text as p_has_season_ticket,"+
        "(ps.driver_license = 1)::text as p_has_licence, "+
        // Household attributes from $region_households hs
        "hs.hh_id,"+
        "hs.hh_persons as hh_members,"+
        "(hs.hh_has_child)::text as hh_has_child,"+
        "hs.hh_cars,"+
        "hs.hh_income,"+
                // Plan attributes from trip table $tablename ts
        "ts.scheme_id,"+
        "ts.score_combined,"+
        "ts.score_finance,"+
        "ts.score_time, "+
        // Location attributes for departure
        "ts.taz_id_start,"+
        "ts.block_id_start,"+
        "ts.loc_id_start,"+
        "ST_X(COALESCE(lss.loc_coordinate, hs.hh_coordinate)) as loc_coord_x_start,"+
        "ST_Y(COALESCE(lss.loc_coordinate, hs.hh_coordinate)) as loc_coord_y_start,"+
        "tss.taz_bbr_type as taz_bbr_type_start,"+
        "(ts.taz_has_toll_start)::text as taz_has_toll_start, "+
                // Location attributes for arrival
        "ts.taz_id_end,"+
        "ts.block_id_end,"+
        "ts.loc_id_end,"+
        "ST_X(COALESCE(lse.loc_coordinate, hs.hh_coordinate)) as loc_coord_x_end,"+
        "ST_Y(COALESCE(lse.loc_coordinate, hs.hh_coordinate)) as loc_coord_y_end,"+
        "tse.taz_bbr_type as taz_bbr_type_end,"+
        "(ts.taz_has_toll_end)::text as taz_has_toll_end,  "+
                // Additional trip attributes
        "ts.start_time_min,"+
        "ts.travel_time_sec,"+
        "ts.mode,"+
        "-1 as car_type,"+
        "ts.distance_bl_m,"+
        "ts.distance_real_m,"+
        "ts.activity,"+
        "(ts.is_home)::text as is_home,"+
        "ts.activity_start_min,"+
        "ts.activity_duration_min,"+
        "ts.car_index as car_id,"+
        "(ts.is_restricted)::text as is_restricted "+
                // FROM CLAUSE WITH JOINS
        "FROM "+ tablename +" ts "+
                // inner joins for person and household tables: notice the ON clause with the p_hh_key because of the compound primary keys and the additional where clause
        "INNER      JOIN " + table_persons +" ps    ON ts.p_id  = ps.p_id  AND '"+ p_hh_key +"' = ps.key "+
        "INNER      JOIN " + table_household + " hs ON ts.hh_id = hs.hh_id AND '" + p_hh_key +"' = hs.hh_key "+
                // left outer joins for the location tables: this joins is needed, because the households don't have a location in the location table
                // Therefore the coordinate is null in lines where the trip starts or ends at home. Normally these lines are deleted, but with a left outer join
                // they are just filled with null values. These values leads to the CASE clause for the coordinates above
        "LEFT OUTER JOIN " + table_locations + " lss ON ts.loc_id_start = lss.loc_id AND '"+loc_key+"' = lss.key "+
        "LEFT OUTER JOIN " + table_locations + " lse ON ts.loc_id_end   = lse.loc_id AND '"+loc_key+"' = lse.key "+
                // inner joins for the taz attributes
        "INNER      JOIN " + table_taz + " tss       ON ts.taz_id_start = tss.taz_id "+
        "INNER      JOIN " + table_taz + " tse       ON ts.taz_id_end   = tse.taz_id "+
                // WHERE CLAUSE
                // This clause is needed because the primary key of a person consits of three parts. Two of them are already checked in the inner join's on clause and here the last
        "WHERE hs.hh_id = ps.hh_id "+
                // ORDER CLAUSE
        "ORDER BY hs.hh_id, ps.p_id, ts.start_time_min";

        Connection connection =dbConnector.getConnection(this);
        FileWriter fw = new FileWriter(new File(outputFile));
        //header
        fw.write("p_id\tp_group\tp_age\tp_sex\tp_has_season_ticket\tp_has_licence\thh_id\thh_members\thh_has_child\thh_cars\thh_income\tscheme_id\tscore_combined\tscore_finance\tscore_time\ttaz_id_start\tblock_id_start\tloc_id_start\tloc_coord_x_start\tloc_coord_y_start\ttaz_bbr_type_start\ttaz_has_toll_start\ttaz_id_end\tblock_id_end\tloc_id_end\tloc_coord_x_end\tloc_coord_y_end\ttaz_bbr_type_end\ttaz_has_toll_end\tstart_time_min\ttravel_time_sec\tmode\tcar_type\tdistance_bl_m\tdistance_real_m\tactivity\tis_home\tactivity_start_min\tactivity_duration_min\tcar_id\tis_restricted\n");
        ResultSet rs = dbConnector.executeQuery(command, this);
        while(rs.next()){
            fw.write(rs.getInt("p_id") + "\t");
            fw.write(rs.getInt("p_group") + "\t");
            fw.write(rs.getInt("p_age") + "\t");
            fw.write(rs.getInt("p_sex") + "\t");
            fw.write(rs.getString("p_has_season_ticket") + "\t");
            fw.write(rs.getString("p_has_licence") + "\t");
            fw.write(rs.getInt("hh_id") + "\t");
            fw.write(rs.getInt("hh_members") + "\t");
            fw.write(rs.getString("hh_has_child") + "\t");
            fw.write(rs.getInt("hh_cars") + "\t");
            fw.write(rs.getInt("hh_income") + "\t");
            fw.write(rs.getInt("scheme_id") + "\t");
            fw.write(rs.getDouble("score_combined") + "\t");
            fw.write(rs.getDouble("score_finance") + "\t");
            fw.write(rs.getDouble("score_time") + "\t");
            fw.write(rs.getInt("taz_id_start") + "\t");
            fw.write(rs.getInt("block_id_start") + "\t");
            fw.write(rs.getInt("loc_id_start") + "\t");
            fw.write(rs.getDouble("loc_coord_x_start") + "\t");
            fw.write(rs.getDouble("loc_coord_y_start") + "\t");
            fw.write(rs.getInt("taz_bbr_type_start") + "\t");
            fw.write(rs.getString("taz_has_toll_start") + "\t");
            fw.write(rs.getInt("taz_id_end") + "\t");
            fw.write(rs.getInt("block_id_end") + "\t");
            fw.write(rs.getInt("loc_id_end") + "\t");
            fw.write(rs.getDouble("loc_coord_x_end") + "\t");
            fw.write(rs.getDouble("loc_coord_y_end") + "\t");
            fw.write(rs.getInt("taz_bbr_type_end") + "\t");
            fw.write(rs.getString("taz_has_toll_end") + "\t");
            fw.write(rs.getInt("start_time_min") + "\t");
            fw.write(rs.getInt("travel_time_sec") + "\t");
            fw.write(rs.getInt("mode") + "\t");
            fw.write(rs.getInt("car_type") + "\t");
            fw.write(rs.getDouble("distance_bl_m") + "\t");
            fw.write(rs.getDouble("distance_real_m") + "\t");
            fw.write(rs.getInt("activity") + "\t");
            fw.write(rs.getString("is_home") + "\t");
            fw.write(rs.getInt("activity_start_min") + "\t");
            fw.write(rs.getInt("activity_duration_min") + "\t");
            fw.write(rs.getInt("car_id") + "\t");
            fw.write(rs.getString("is_restricted") + "\n");
        }
        //and finally close everything
        fw.close();
        rs.close();
        dbConnector.closeConnection(this);

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
