/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.persistence.db;

import de.dlr.ivf.tapas.constants.TPS_ActivityConstant.TPS_ActivityCodeType;
import de.dlr.ivf.tapas.constants.TPS_SettlementSystem.TPS_SettlementSystemType;
import de.dlr.ivf.tapas.loc.TPS_Location;
import de.dlr.ivf.tapas.loc.TPS_Region;
import de.dlr.ivf.tapas.log.LogHierarchy;
import de.dlr.ivf.tapas.log.TPS_Logger;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface.HierarchyLogLevel;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface.SeverenceLogLevel;
import de.dlr.ivf.tapas.mode.TPS_ModeSet;
import de.dlr.ivf.tapas.persistence.TPS_PersistenceManager;
import de.dlr.ivf.tapas.person.TPS_Household;
import de.dlr.ivf.tapas.person.TPS_Person;
import de.dlr.ivf.tapas.plan.TPS_LocatedStay;
import de.dlr.ivf.tapas.plan.TPS_Plan;
import de.dlr.ivf.tapas.plan.TPS_PlannedTrip;
import de.dlr.ivf.tapas.runtime.util.IPInfo;
import de.dlr.ivf.tapas.scheme.*;
import de.dlr.ivf.tapas.util.parameters.ParamFlag;
import de.dlr.ivf.tapas.util.parameters.ParamString;
import de.dlr.ivf.tapas.util.parameters.ParamValue;
import de.dlr.ivf.tapas.util.parameters.TPS_ParameterClass;

import java.io.IOException;
import java.net.InetAddress;
import java.sql.*;
import java.util.*;

/**
 * Class for IO management regarding Database IO like connection check, statement execution, temporary table creation, higher-level functions
 */
@LogHierarchy(hierarchyLogLevel = HierarchyLogLevel.THREAD)
public class TPS_DB_IOManager implements TPS_PersistenceManager {
    /// The current behaviour of the simulation server.
    public static Behaviour BEHAVIOUR = Behaviour.FAT;
    /// The instance of a connector to the db, each thread may have its own connection
    private final TPS_DB_Connector dbConnector;
    /// The instance of an Database IO module
    private final TPS_DB_IO dbIO;
    private final Queue<Integer> insertedHouseHolds = new LinkedList<>();
    private final Queue<TPS_Plan> plansToStore = new LinkedList<>();
    private TPS_ModeSet modeSet;
    /// The region for this run, eg. Berlin, Hamburg etc.
    private TPS_Region region;
    private TPS_SchemeSet schemeSet;

    /**
     * Constructor class: creates a new db connection, file handler, statement map and the general pattern of the trip-statement
     *
     * @throws ClassNotFoundException
     * @throws IOException
     */
    public TPS_DB_IOManager(TPS_ParameterClass parameterClass) throws IOException, ClassNotFoundException {
        this.dbConnector = new TPS_DB_Connector(parameterClass.getString(ParamString.DB_USER),
                parameterClass.getString(ParamString.DB_PASSWORD), parameterClass);
        this.dbIO = new TPS_DB_IO(this);
    }

    /**
     * Method to build a sql-function from the given parameters
     *
     * @param parameterClass parameter class reference
     * @param function       The name of the sql-function to call
     * @param parameters     A set of parameters for the sql-function
     * @return A String which holds the complete SQL-Query.
     */
    public static String buildQuery(TPS_ParameterClass parameterClass, String function, Object... parameters) {
        StringBuilder sb = new StringBuilder(
                "SELECT " + parameterClass.getString(ParamString.DB_SCHEMA_CORE) + function + "(");
        if (parameters != null) {
            for (Object parameter : parameters) {
                if (parameter instanceof ParamString) {
                    ParamString ps = (ParamString) parameter;
                    sb.append("'").append(parameterClass.getString(ps)).append("',");
                } else if (parameter instanceof String) {
                    String string = (String) parameter;
                    sb.append("'").append(string).append("',");
                } else if (parameter instanceof InetAddress) {
                    InetAddress address = (InetAddress) parameter;
                    sb.append("inet '").append(address.getHostAddress()).append("',");
                } else {
                    sb.append(parameter.toString()).append(",");
                }
            }
        }
        sb.setCharAt(sb.length() - 1, ')');
        return sb.toString();
    }

    private void batchStoreTrips() {
        String statement = "";
        PreparedStatement pS;
        TPS_Plan plan;
        TPS_Person p;
        TPS_Household hh;
        int index, numberOfPlans = 0;
        int[] batchResultCounter;
        TPS_Stay prevStay, nextStay;
        TPS_Location prevLoc, nextLoc;
        TPS_LocatedStay nextStayLocated;
        TPS_PlannedTrip pt;
        double duration;
        boolean planStoreSuccessful = true;
        final int chunkSize = 128;

        try {
            Connection con = this.dbConnector.getConnection(this);

            statement = "INSERT INTO " + this.getParameters().getString(ParamString.DB_TABLE_TRIPS) +
                    " VALUES (?,?,?,?,?, ?,?,?,?,?, ?,?,?,?,?, ?,?,?,?,?, ?,?,?,?,?, ?,?,?,?,? ,?,?,?,?, ?)";
            pS = con.prepareStatement(statement);
            while (!this.plansToStore.isEmpty()) {
                plan = this.plansToStore.poll();
                plan.balanceStarts(); // because some location references could be reseted from other plans, balance starts again!


                p = plan.getPerson();
                hh = p.getHousehold();
                int lastStart = -10000000, start;

                for (TPS_TourPart tp : plan.getScheme().getTourPartIterator()) {
                    for (TPS_Trip t : tp.getTripIterator()) {
                        index = 1;
                        pS.setInt(index++, p.getId());
                        pS.setInt(index++, hh.getId());
                        pS.setInt(index++, plan.getScheme().getId());
                        if (Double.isNaN(plan.getAcceptanceProbability()) || Double.isInfinite(
                                plan.getAcceptanceProbability())) {
                            TPS_Logger.log(SeverenceLogLevel.FATAL,
                                    "NaN detected in getAcceptanceProbability for person " + p.getId());
                        }
                        pS.setDouble(index++, plan.getAcceptanceProbability());
                        if (Double.isNaN(plan.getBudgetAcceptanceProbability()) || Double.isInfinite(
                                plan.getBudgetAcceptanceProbability())) {
                            TPS_Logger.log(SeverenceLogLevel.FATAL,
                                    "NaN detected in getBudgetcceptanceProbability for person " + p.getId());
                        }
                        pS.setDouble(index++, plan.getBudgetAcceptanceProbability());
                        if (Double.isNaN(plan.getTimeAcceptanceProbability()) || Double.isInfinite(
                                plan.getTimeAcceptanceProbability())) {
                            TPS_Logger.log(SeverenceLogLevel.FATAL,
                                    "NaN detected in getTimeAcceptanceProbability for person " + p.getId());
                        }
                        pS.setDouble(index++, plan.getTimeAcceptanceProbability());

                        prevStay = tp.getPreviousStay(t);
                        nextStay = tp.getNextStay(t);
                        prevLoc = plan.getLocatedStay(prevStay).getLocation();
                        nextStayLocated = plan.getLocatedStay(nextStay);
                        nextLoc = nextStayLocated.getLocation();
                        pS.setInt(index++, prevLoc.getTrafficAnalysisZone().getTAZId());
                        pS.setBoolean(index++,
                                prevLoc.getTrafficAnalysisZone().hasToll(this.getParameters().getSimulationType()));
                        pS.setInt(index++, (prevLoc.hasBlock() ? prevLoc.getBlock().getId() : -1));
                        pS.setInt(index++, prevLoc.getId());
                        pS.setDouble(index++, prevLoc.getCoordinate().getValue(0));
                        pS.setDouble(index++, prevLoc.getCoordinate().getValue(1));

                        pS.setInt(index++, nextLoc.getTrafficAnalysisZone().getTAZId());
                        pS.setBoolean(index++,
                                nextLoc.getTrafficAnalysisZone().hasToll(this.getParameters().getSimulationType()));
                        pS.setInt(index++, (nextLoc.hasBlock() ? nextLoc.getBlock().getId() : -1));
                        pS.setInt(index++, nextLoc.getId());
                        pS.setDouble(index++, nextLoc.getCoordinate().getValue(0));
                        pS.setDouble(index++, nextLoc.getCoordinate().getValue(1));

                        pt = plan.getPlannedTrip(t);
                        start = pt.getStart();
                        if (start - lastStart < 1.0) {
                            start = lastStart + 1;
                        }
                        lastStart = start;

                        pS.setInt(index++, (int) ((start * 1.66666666e-2) + 0.5)); //sec to min incl round
                        if (Double.isNaN(pt.getDuration()) || Double.isInfinite(pt.getDuration())) {
                            TPS_Logger.log(SeverenceLogLevel.FATAL,
                                    "NaN detected in getDuration for person " + p.getId());
                        }
                        pS.setDouble(index++, pt.getDuration());
                        pS.setInt(index++, pt.getMode().getMCTCode());
                        pS.setInt(index++, tp.getCar() == null ? -1 : tp.getCar().getId());
                        if (Double.isNaN(pt.getDistanceBeeline()) || Double.isInfinite(pt.getDistanceBeeline())) {
                            TPS_Logger.log(SeverenceLogLevel.FATAL,
                                    "NaN detected in getDistanceBeeline for person " + p.getId());
                        }
                        pS.setDouble(index++, pt.getDistanceBeeline());
                        if (Double.isNaN(pt.getDistance()) || Double.isInfinite(pt.getDistance())) {
                            TPS_Logger.log(SeverenceLogLevel.FATAL,
                                    "NaN detected in getDistance for person " + p.getId());
                        }
                        pS.setDouble(index++, pt.getDistance());
                        pS.setInt(index++, nextStay.getActCode().getCode(TPS_ActivityCodeType.ZBE));
                        pS.setBoolean(index++, nextStay.isAtHome());
                        pS.setInt(index++,
                                (int) ((nextStayLocated.getStart() * 1.66666666e-2) + 0.5)); // secs to min incl round
                        duration = nextStayLocated.getDuration();
                        //sum durations of concurring stays
                        while (tp.getNextEpisode(nextStay).isStay()) {
                            nextStay = (TPS_Stay) tp.getNextEpisode(nextStay);
                            duration += plan.getLocatedStay(nextStay).getDuration();
                        }
                        pS.setInt(index++, (int) ((duration * 1.66666666e-2) + 0.5)); // secs to min incl round
                        if (tp.getCar() != null) {
                            pS.setInt(index++, tp.getCar().index);
                            pS.setBoolean(index++, tp.getCar().isRestricted());
                        } else {
                            pS.setInt(index++, -1);
                            pS.setBoolean(index++, false);
                        }
                        pS.setInt(index++, p.getPersGroup().getCode());
                        pS.setInt(index++,
                                prevLoc.getTrafficAnalysisZone().getBbrType().getCode(TPS_SettlementSystemType.FORDCP));
                        pS.setInt(index++, hh.getLocation().getTrafficAnalysisZone().getBbrType()
                                             .getCode(TPS_SettlementSystemType.FORDCP));
                        pS.setInt(index++, nextStay.locationChoiceMotive.code);// the location_selection_motive
                        pS.setInt(index++, nextStay.locationChoiceMotiveSupply.code);// the location_selection_motive
                        pS.addBatch();
                        numberOfPlans++;
                        if (numberOfPlans > 0 && numberOfPlans % chunkSize == 0) {
                            batchResultCounter = pS.executeBatch();
                            for (index = 0; index < batchResultCounter.length && planStoreSuccessful; ++index) {
                                planStoreSuccessful &= batchResultCounter[index] != PreparedStatement.EXECUTE_FAILED;
                            }
                        }
                    }
                }
                plan.reset();
            }

            batchResultCounter = pS.executeBatch();

            for (index = 0; index < batchResultCounter.length && planStoreSuccessful; ++index) {
                planStoreSuccessful &= batchResultCounter[index] != PreparedStatement.EXECUTE_FAILED;
            }

            if (!planStoreSuccessful) {
                TPS_Logger.log(SeverenceLogLevel.ERROR, "Storing of plans failed!");
            }
            pS.close();
        } catch (SQLException e) {
            TPS_Logger.log(SeverenceLogLevel.ERROR, "Error during sql-statement: " + statement);
            TPS_Logger.log(SeverenceLogLevel.ERROR, e.getMessage(), e);
            TPS_Logger.log(SeverenceLogLevel.ERROR, "Next exception:");
            TPS_Logger.log(SeverenceLogLevel.ERROR, e.getNextException().getMessage(), e.getNextException());
        }
    }

    public void close() {
        this.dbConnector.closeConnections();
    }

    public void createTemporaryAndOutputTables() {
        // create additional temporary tables in database
        if (TPS_Logger.isLogging(HierarchyLogLevel.CLIENT, SeverenceLogLevel.INFO)) {
            TPS_Logger.log(HierarchyLogLevel.CLIENT, SeverenceLogLevel.INFO, "Create temporary location table");
        }
        functionExecute("create_temp_locations", ParamString.DB_TABLE_LOCATION, ParamString.DB_TABLE_LOCATION_TMP,
                !Behaviour.FAT.equals(TPS_DB_IOManager.BEHAVIOUR));


        if (TPS_Logger.isLogging(HierarchyLogLevel.CLIENT, SeverenceLogLevel.INFO)) {
            TPS_Logger.log(HierarchyLogLevel.CLIENT, SeverenceLogLevel.INFO, "Create temporary household table");
        }
        functionExecute("create_hh_sample", ParamString.DB_TABLE_HOUSEHOLD, ParamString.DB_TABLE_HOUSEHOLD_TMP,
                ParamString.DB_TABLE_PERSON, this.getParameters().getString(ParamString.DB_HOUSEHOLD_AND_PERSON_KEY),
                this.getParameters().getLongValue(ParamValue.RANDOM_SEED_NUMBER),
                this.getParameters().getDoubleValue(ParamValue.DB_HH_SAMPLE_SIZE));

        if (TPS_Logger.isLogging(HierarchyLogLevel.CLIENT, SeverenceLogLevel.INFO)) {
            TPS_Logger.log(HierarchyLogLevel.CLIENT, SeverenceLogLevel.INFO, "Create output trip table");
        }
        String query = "CREATE TABLE " + this.getParameters().getString(ParamString.DB_TABLE_TRIPS) +
                " (p_id integer, hh_id integer, scheme_id integer, score_combined double precision," +
                " score_finance double precision, score_time double precision, taz_id_start integer, taz_has_toll_start boolean," +
                " block_id_start integer, loc_id_start integer, lon_start double precision, lat_start double precision, taz_id_end integer, taz_has_toll_end boolean, block_id_end integer," +
                " loc_id_end integer, lon_end double precision, lat_end double precision, start_time_min integer, travel_time_sec double precision, mode integer," +
                " car_type integer, distance_bl_m double precision, distance_real_m double precision, activity integer," +
                " is_home boolean, activity_start_min integer, activity_duration_min integer, emission_type integer, is_restricted boolean," +
                " p_group integer, taz_bbr_type_start integer, bbr_type_home integer, PRIMARY KEY (p_id, hh_id, start_time_min))";
        this.execute(query);
    }

    public void dropTemporaryTables() {
        if (this.getParameters().isTrue(ParamFlag.FLAG_DELETE_TEMPORARY_TABLES)) {
            this.dbIO.dropTemporaryTables();
        }
    }

    /**
     * Method to execute a SQL-Query without any results
     *
     * @param query The query to execute
     * @throws SQLException
     */
    public void execute(String query) {
        this.dbConnector.execute(query, this);
    }

    /**
     * Method to execute a SQL-Query witch provides an result set
     *
     * @param query The sql-query
     * @return A SQL ResultSet containing the result
     */
    public ResultSet executeQuery(String query) {
        return this.dbConnector.executeQuery(query, this);
    }

    /**
     * Commit pending data and finish all db connections.
     */
    public boolean finish() {
        this.insertPlansIntoDB();
        //if this method is called all other remaining households are reset for new calculation
        this.resetUnfinishedHouseholds();
        return true;
    }

    /**
     * Method to execute a function. Calls buildQuery and execute
     *
     * @param function   The name of the function
     * @param parameters The parameters for the function
     */
    public void functionExecute(String function, Object... parameters) {
        this.execute(buildQuery(this.getParameters(), function, parameters));
    }

    /**
     * Method to execute a function. Calls buildQuery and executeQuery
     *
     * @param function   The name of the function
     * @param parameters The parameters for the function
     * @return A SQL ResultSet containing the result
     */
    public ResultSet functionExecuteQuery(String function, Object... parameters) {
        return this.executeQuery(buildQuery(this.getParameters(), function, parameters));
    }

    /**
     * @return the dbConnector
     */
    public TPS_DB_Connector getDbConnector() {
        return dbConnector;
    }

    /*
     * (non-Javadoc)
     *
     * @see de.dlr.ivf.tapas.persistence.TPS_PersistenceManager#getModeSet()
     */
    public TPS_ModeSet getModeSet() {
        return this.modeSet;
    }

    /*
     * (non-Javadoc)
     *
     * @see de.dlr.ivf.tapas.persistence.TPS_PersistenceManager#getNextHousehold()
     */
    public TPS_Household getNextHousehold() {
        try {
            long time = System.nanoTime();
            TPS_Household hh = this.dbIO.getNextHousehold(this.getRegion());
            time = System.nanoTime() - time;
            if (TPS_Logger.isLogging(SeverenceLogLevel.DEBUG)) {
                TPS_Logger.log(SeverenceLogLevel.DEBUG, "Read next household in " + (time * 0.000001) + "ms");
            }
            return hh;
        } catch (Exception e) {
            TPS_Logger.log(SeverenceLogLevel.ERROR, e);
            throw new RuntimeException(e);
        }
    }

    public TPS_ParameterClass getParameters() {
        return this.dbConnector.getParameters();
    }

    /*
     * (non-Javadoc)
     *
     * @see de.dlr.ivf.tapas.persistence.TPS_PersistenceManager#getRegion()
     */
    public TPS_Region getRegion() {
        return region;
    }


    /*
     * (non-Javadoc)
     *
     * @see de.dlr.ivf.tapas.persistence.TPS_PersistenceManager#getSchemesSet()
     */
    public TPS_SchemeSet getSchemesSet() {
        return schemeSet;
    }


    /*
     * (non-Javadoc)
     *
     * @see de.dlr.ivf.tapas.persistence.TPS_PersistenceManager#incrementOccupancy(de.dlr.ivf.tapas.plan.TPS_Plan)
     */
    public boolean incrementOccupancy() {
        this.storeLocationOccupanyInDB();
        return true;
    }


    /*
     * (non-Javadoc)
     *
     * @see de.dlr.ivf.tapas.persistence.TPS_PersistenceManager#init()
     */
    public void init() {
        try {
            this.dbConnector.checkConnectivity();
            this.dbIO.initStart();

            // clearing initial information from database
            if (TPS_Logger.isLogging(HierarchyLogLevel.CLIENT, SeverenceLogLevel.INFO)) {
                TPS_Logger.log(HierarchyLogLevel.CLIENT, SeverenceLogLevel.INFO,
                        "Clearing constants (all codes: age," + " distance, person, activity, location, ...)");
            }
            this.dbIO.clearConstants();

            // reading initial information from database
            if (TPS_Logger.isLogging(HierarchyLogLevel.CLIENT, SeverenceLogLevel.INFO)) {
                TPS_Logger.log(HierarchyLogLevel.CLIENT, SeverenceLogLevel.INFO,
                        "Reading constants (all codes: age," + " distance, person, activity, location, ...)");
            }
            this.dbIO.readConstants();

            if (TPS_Logger.isLogging(HierarchyLogLevel.CLIENT, SeverenceLogLevel.INFO)) {
                TPS_Logger.log(HierarchyLogLevel.CLIENT, SeverenceLogLevel.INFO,
                        "Reading parameters for the utility function");
            }
            this.dbIO.readUtilityFunction();

            // read scheme set
            if (TPS_Logger.isLogging(HierarchyLogLevel.CLIENT, SeverenceLogLevel.INFO)) {
                TPS_Logger.log(HierarchyLogLevel.CLIENT, SeverenceLogLevel.INFO,
                        "Reading scheme set (includes scheme classes, schemes and episodes)");
            }
            this.schemeSet = this.dbIO.readSchemeSet();

            if (TPS_Logger.isLogging(HierarchyLogLevel.CLIENT, SeverenceLogLevel.INFO)) {
                TPS_Logger.log(HierarchyLogLevel.CLIENT, SeverenceLogLevel.INFO,
                        "Reading region with all blocks and locations");
            }
            this.region = this.dbIO.readRegion();

            if (TPS_Logger.isLogging(HierarchyLogLevel.CLIENT, SeverenceLogLevel.INFO)) {
                TPS_Logger.log(HierarchyLogLevel.CLIENT, SeverenceLogLevel.INFO,
                        "Reading mode set with mode choice tree");
            }
            this.modeSet = new TPS_ModeSet(this.dbIO.readModeChoiceTree(), this.dbIO.readExpertKnowledgeTree(),
                    this.getParameters());

            if (TPS_Logger.isLogging(HierarchyLogLevel.CLIENT, SeverenceLogLevel.INFO)) {
                TPS_Logger.log(HierarchyLogLevel.CLIENT, SeverenceLogLevel.INFO,
                        "Reading matrices (distances, travel times, access and egress times)");
            }
            this.dbIO.readMatrices(this.getRegion());
        } catch (SQLException e) {
            TPS_Logger.log(SeverenceLogLevel.ERROR, e);
            throw new RuntimeException(e);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see de.dlr.ivf.tapas.persistence.TPS_PersistenceManager#writePlan(de.dlr.ivf.tapas.plan.TPS_Plan)
     */
    private void insertPlansIntoDB() throws NumberFormatException {
        //write plans
        if (this.plansToStore.size() > 0) {
            this.incrementOccupancy();
            this.batchStoreTrips();
        }
        this.storeFinishedHouseholdIDsToDB();
    }

    /**
     * Method to insert all pending Trips.
     */
    public void insertTrips() {
        try {
            while (this.insertedHouseHolds.size() < this.dbIO.getNumberOfFetchedHouseholds()) {
                Thread.sleep(10);
            }
            long time = System.nanoTime();
            if (TPS_Logger.isLogging(HierarchyLogLevel.THREAD, SeverenceLogLevel.INFO)) {
                TPS_Logger.log(HierarchyLogLevel.THREAD, SeverenceLogLevel.INFO, "Inserting trips");
            }
            this.insertPlansIntoDB();
            time = System.nanoTime() - time;
            if (TPS_Logger.isLogging(SeverenceLogLevel.DEBUG)) {
                TPS_Logger.log(SeverenceLogLevel.DEBUG, "Wrote trips back into db in " + (time * 0.000001) + "ms");
            }
        } catch (InterruptedException e) {
            TPS_Logger.log(SeverenceLogLevel.ERROR, e.getMessage(), e);
        }
    }

    public void resetUnfinishedHouseholds() {
        try {
            //String query = "SELECT core.reset_unfinished_households('" + ParamString.RUN_IDENTIFIER.getString() + "', '" +IPInfo.getEthernetInetAddress().getHostAddress()+"')";
            // create additional temporary tables in database
            ResultSet rs = this.executeQuery(TPS_DB_IOManager
                    .buildQuery(this.getParameters(), "reset_unfinished_households",
                            this.getParameters().getString(ParamString.RUN_IDENTIFIER),
                            IPInfo.getEthernetInetAddress().getHostAddress()));
            if (rs.next()) {
                int count = rs.getInt(1);
                if (count > 0) {
                    if (TPS_Logger.isLogging(SeverenceLogLevel.WARN)) {
                        TPS_Logger.log(SeverenceLogLevel.WARN,
                                "Resetted " + count + " households for new calculation!");
                    }
                }
            }
            rs.close();
        } catch (IOException | SQLException e) {
            TPS_Logger.log(SeverenceLogLevel.ERROR, e);

        }
    }

    /*
     * (non-Javadoc)
     *
     * @see de.dlr.ivf.tapas.persistence.TPS_PersistenceManager#returnHousehold(de.dlr.ivf.tapas.person.TPS_Household)
     */
    public void returnHousehold(TPS_Household hh) {
        synchronized (this.insertedHouseHolds) {
            this.insertedHouseHolds.add(hh.getId());
        }
    }

    private void storeFinishedHouseholdIDsToDB() {
        ArrayList<Integer> tmpIds = new ArrayList<>();
        String statement = "";
        PreparedStatement pS;
        int numberOfPlans = 0, numberOfHouseholds = 0;
        int[] batchResultCounter;
        int index, chunkSize = 256;
        boolean planStoreSuccessful = true, householdStoreSuccesful = true;

        try {
            Connection con = this.dbConnector.getConnection(this);
            con.setAutoCommit(false);
            //write households
            if (this.insertedHouseHolds.size() > 0) {
                if (TPS_Logger.isLogging(SeverenceLogLevel.INFO)) {
                    TPS_Logger.log(SeverenceLogLevel.INFO,
                            "Finishing " + this.insertedHouseHolds.size() + " households.");
                }

                //sort to avoid dead locks!
                while (this.insertedHouseHolds.size() > 0) {
                    tmpIds.add(this.insertedHouseHolds.poll());
                }
                Collections.sort(tmpIds);

                statement = "UPDATE " + this.getParameters().getString(ParamString.DB_TABLE_HOUSEHOLD_TMP) +
                        " SET hh_started = true, hh_finished = true WHERE hh_id = ?";
                pS = con.prepareStatement(statement);
                for (Integer i : tmpIds) {
                    pS.setInt(1, i);
                    pS.addBatch();
                    numberOfHouseholds++;
                    if (numberOfHouseholds > 0 && numberOfHouseholds % chunkSize == 0) {
                        //lock table
                        statement = "LOCK TABLE " + this.getParameters().getString(ParamString.DB_TABLE_HOUSEHOLD_TMP) +
                                " IN EXCLUSIVE MODE";
                        this.dbConnector.execute(statement, this);
                        batchResultCounter = pS.executeBatch();
                        //commit
                        con.commit();

                        for (index = 0; index < batchResultCounter.length && householdStoreSuccesful; ++index) {
                            householdStoreSuccesful &= batchResultCounter[index] != PreparedStatement.EXECUTE_FAILED;
                        }
                    }
                }
                //lock table
                statement = "LOCK TABLE " + this.getParameters().getString(ParamString.DB_TABLE_HOUSEHOLD_TMP) +
                        " IN EXCLUSIVE MODE";
                this.dbConnector.execute(statement, this);
                batchResultCounter = pS.executeBatch();
                //commit
                con.commit();
                for (index = 0; index < batchResultCounter.length && householdStoreSuccesful; ++index) {
                    householdStoreSuccesful &= batchResultCounter[index] != PreparedStatement.EXECUTE_FAILED;
                }
                if (!householdStoreSuccesful) {
                    TPS_Logger.log(SeverenceLogLevel.ERROR, "Finishing of households failed!");
                }
                pS.close();
            }

            //now store the whole thing in db!
            if (numberOfHouseholds + numberOfPlans > 0) {
                if (householdStoreSuccesful && planStoreSuccessful) {
                    if (TPS_Logger.isLogging(SeverenceLogLevel.INFO)) {
                        TPS_Logger.log(SeverenceLogLevel.INFO, "Saved trips to database.");
                    }
                } else {
                    if (TPS_Logger.isLogging(SeverenceLogLevel.ERROR)) {
                        TPS_Logger.log(SeverenceLogLevel.ERROR,
                                "ERROR: Error during commit to DB! Plans successful:" + planStoreSuccessful +
                                        " households successful: " + householdStoreSuccesful);
                    }
                }
            }
            con.setAutoCommit(true);

        } catch (SQLException e) {
            TPS_Logger.log(SeverenceLogLevel.ERROR, "Error during sql-statement: " + statement);
            TPS_Logger.log(SeverenceLogLevel.ERROR, e.getMessage(), e);
            TPS_Logger.log(SeverenceLogLevel.ERROR, "Next exception:");
            TPS_Logger.log(SeverenceLogLevel.ERROR, e.getNextException().getMessage(), e.getNextException());
        }
    }

    private void storeLocationOccupanyInDB() {
        ArrayList<Integer> tmpIds = new ArrayList<>();
        ArrayList<LocWithOccupancy> locWithOccupancies = new ArrayList<>();
        String statement = "";
        PreparedStatement pS;
        //int id, lastID;
        TPS_Stay stay;
        try {


            if (TPS_Logger.isLogging(SeverenceLogLevel.INFO)) {
                TPS_Logger.log(SeverenceLogLevel.INFO,
                        "Storing " + this.plansToStore.size() + " plan elements into the database.");
            }

            for (TPS_Plan pl : this.plansToStore) {
                //get all ids for all locations
                for (TPS_Episode epi : pl.getScheme().getEpisodeIterator()) {
                    if (epi.isStay()) {
                        stay = (TPS_Stay) epi; // cast once
                        if (!stay.isHomePart) {//do not add home parts
                            tmpIds.add(pl.getLocatedStay(stay).getLocation().getId());
                        }
                    }
                }
            }
            Collections.sort(
                    tmpIds); // Sorting prevents deadlocks, because no process can wait for a process with a smaller loc_id


            //build a list with every location and its occupancy

            LocWithOccupancy tmpLoc = null;
            for (Integer tmpID : tmpIds) {
                if (tmpLoc != null && tmpID == tmpLoc.id) {
                    tmpLoc.occupancy++;
                } else {
                    tmpLoc = new LocWithOccupancy(tmpID, 1);
                    locWithOccupancies.add(tmpLoc);
                }
            }
            // now for all types of occurancy
            Collections.sort(locWithOccupancies); // sort to get locations with occupacy 1 locations with occupancy 2...

            Connection con = this.dbConnector.getConnection(this);

            pS = con.prepareStatement("UPDATE " + this.getParameters().getString(ParamString.DB_TABLE_LOCATION_TMP) +
                    " SET loc_occupancy = loc_occupancy + ? WHERE loc_id = ANY (?::int[])");
            statement = "";
            List<Integer> ids = new LinkedList<>();
            int lastOccupancy = 0;
            for (LocWithOccupancy loc : locWithOccupancies) {
                if (loc.occupancy != lastOccupancy && ids.size() > 0) { // new occupancy-> store the old ones
                    pS.setInt(1, lastOccupancy);
                    Array aArray = con.createArrayOf("integer", ids.toArray(new Integer[0]));
                    pS.setArray(2, aArray);
                    pS.addBatch();
                    ids.clear(); //new list
                }
                ids.add(loc.id);
                lastOccupancy = loc.occupancy;
            }

            //finish last statement
            if (ids.size() > 0) {
                pS.setInt(1, lastOccupancy);
                Array aArray = con.createArrayOf("integer", ids.toArray(new Integer[0]));
                pS.setArray(2, aArray);
                pS.addBatch();
            }

            //time critical lock
            con.setAutoCommit(false);

            statement = "LOCK TABLE " + this.getParameters().getString(ParamString.DB_TABLE_LOCATION_TMP) +
                    " IN EXCLUSIVE MODE";
            this.dbConnector.execute(statement, this);

            pS.executeBatch();

            con.commit();
            con.setAutoCommit(true);


        } catch (SQLException e) {
            TPS_Logger.log(SeverenceLogLevel.ERROR, "Error during sql-statement: " + statement);
            TPS_Logger.log(SeverenceLogLevel.ERROR, e.getMessage(), e);
            TPS_Logger.log(SeverenceLogLevel.ERROR, "Next exception:");
            TPS_Logger.log(SeverenceLogLevel.ERROR, e.getNextException().getMessage(), e.getNextException());
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see de.dlr.ivf.tapas.persistence.TPS_PersistenceManager#writePlan(de.dlr.ivf.tapas.plan.TPS_Plan)
     */
    public void writePlan(TPS_Plan plan) {
        synchronized (this.plansToStore) {
            this.plansToStore.add(plan);
        }
    }

    /**
     * Behaviour of the simulation server. As a fat server all possible calculations are done on the server instead of
     * the database. In the medium and thin case more calculations are shifted to the database. The current behaviour is
     * fat because the database capacities are the bottleneck of the current simulation.
     *
     * @author mark_ma
     */
    public enum Behaviour {
        /// All possible calculations are done on the local server .
        FAT, /// Medium mix for the calculations.
        MEDIUM, /// All possible calculations are done on the database server.
        THIN
    }

    private class LocWithOccupancy implements Comparable<LocWithOccupancy> {

        int id;
        int occupancy;
        public LocWithOccupancy(int id, int occupancy) {
            this.id = id;
            this.occupancy = occupancy;

        }

        @Override
        public int compareTo(LocWithOccupancy arg0) {
            //first order by occupancy then by id
            if (this.occupancy != arg0.occupancy) return this.occupancy - arg0.occupancy;
            else return this.id - arg0.id;
        }
    }

}
