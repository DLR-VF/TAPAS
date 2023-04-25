/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.analyzer.tum.databaseConnector;

import de.dlr.ivf.tapas.analyzer.inputfileconverter.TapasTrip;
import de.dlr.ivf.tapas.analyzer.inputfileconverter.TapasTripReader;
import de.dlr.ivf.tapas.constants.TPS_SettlementSystem;
import de.dlr.ivf.tapas.constants.TPS_SettlementSystem.TPS_SettlementSystemType;
import de.dlr.ivf.tapas.persistence.db.TPS_DB_Connector;
import de.dlr.ivf.tapas.persistence.db.TPS_DB_IO;
import de.dlr.ivf.tapas.persistence.db.TPS_DB_IOManager;
import de.dlr.ivf.tapas.util.FuncUtils;
import de.dlr.ivf.tapas.parameter.TPS_ParameterClass;

import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * This class provides a way to export {@link TapasTrip}s directly from the database without using a file export first.<br>
 * It implements the {@link Iterable} for easy use. The <code>main</code> method provides a minimal example of usage.
 *
 * <em>As this class creates temporary tables and sequences on the database,
 * it is recommended to use the <code>close()</code> method.</em>
 *
 * @author boec_pa
 */
public class DBTripReader implements TapasTripReader {

    /**
     * Needed to mark the end of the queue.
     */
    private static final TapasTrip POISON_INSTANCE = new TapasTrip();
    /**
     * This is the maximal number of {@link TapasTrip}s that are prefetched from the database. Memory size is the only
     * restriction here.
     */
    private final int QUEUE_SIZE = 100000;
    /**
     * The number of rows fetched from the database at once. A large number reduces overhead of Java-DB communication.
     */
    private final int FETCH_SIZE = 10000;
    public String region = "";
    public String description = "";
    protected long cntTrips = 0;
    /**
     * Every output goes here
     */
    private StyledDocument console;
    private long totalEstimate;
    private long totaltripcount;
    private boolean isCancelled = false;
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    private TPS_SettlementSystemType settlementType;
    private TripIterator tripIterator;

    /**
     * @param settlementType may be <code>null</code>
     * @param tazFilter      may be <code>null</code>. The taz ids to accept.
     * @throws SQLException           if SQL commands could not be executed properly. Check connection!
     * @throws ClassNotFoundException
     * @throws IOException            if the connection file could not be found or read.
     */
    public DBTripReader(String simulation, String hhkey, String schema, String region, TPS_SettlementSystemType settlementType, Set<Integer> tazFilter, TPS_DB_Connector connection, StyledDocument console) throws SQLException, ClassNotFoundException, IOException {
        init(simulation, hhkey, schema, region, settlementType, tazFilter, connection, console);
    }

    public DBTripReader(String simulation, StyledDocument console, TPS_SettlementSystemType settlementType, Set<Integer> tazFilter, TPS_DB_Connector connection) throws SQLException, ClassNotFoundException, IOException {
        this.console = console;
        this.settlementType = settlementType;

//        String q = "SELECT sim_par[1] as region, sim_par[2] as hhkey, sim_description" + " FROM simulations " +
//                " WHERE sim_key = '" + simulation + "'";

        String q = "SELECT sp.param_value as region, sp2.param_value as hhkey, sim_description" +
                "    FROM simulations s join simulation_parameters sp on (s.sim_key = sp.sim_key)" +
                "        join simulation_parameters sp2 on (s.sim_key = sp2.sim_key) " +
                "WHERE s.sim_key = '" + simulation +  "' and sp.param_key = 'DB_REGION' " +
                "and sp2.param_key = 'DB_HOUSEHOLD_AND_PERSON_KEY' ORDER BY s.sim_key";

        ResultSet rs = connection.executeQuery(q, this);

        if (rs.next()) {
            this.region = rs.getString("region");
            this.description = rs.getString("sim_description");
            String hhkey = rs.getString("hhkey");
            String schema = "core";
            init(simulation, hhkey, schema, this.region, settlementType, tazFilter, connection, console);
        } else {
            throw new IllegalStateException("Could not retrieve information about simulation" + simulation);
        }
        rs.close();
    }

    @SuppressWarnings("unused")
    public static void main(String[] args) throws SQLException, ClassNotFoundException, IOException {

        String hhkey = "MID2008_Y2008";
        String schema = "core";
        String region = "berlin";

        long before = System.currentTimeMillis();
        String loginInfo = "T:\\Simulationen\\runtime_perseus.csv";

        // DBTripReader tripReader = new DBTripReader(
        // "berlin_trips_2013y_01m_04d_17h_22m_36s_448ms", hhkey, schema,
        // region); // big example

        TPS_ParameterClass parameterClass = new TPS_ParameterClass();
        parameterClass.loadRuntimeParameters(new File(loginInfo));
        TPS_DB_Connector dbCon = new TPS_DB_Connector(parameterClass);

        DBTripReader tripReader = new DBTripReader("berlin_trips_2013y_01m_14d_09h_47m_38s_709ms", hhkey, schema,
                region, null, null, dbCon, null);// small example

        int cnt = 0;
        while (tripReader.getIterator().hasNext()) {
            TapasTrip t = tripReader.getIterator().next();
            cnt++;
        }

        tripReader.close();

        System.out.println("Es wurden " + cnt + " Trips gefunden.");

        System.out.println("Es hat " + (System.currentTimeMillis() - before) + "ms gedauert.");
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {

        this.pcs.addPropertyChangeListener(listener);
    }

    public void cancel() {

        this.isCancelled = true;
        close();
    }

    public void close() {
        tripIterator.close();
    }

    @Override
    public Iterator<TapasTrip> getIterator() {
        return tripIterator;
    }

    /**
     * Returns an estimated progress between <code>0</code> and <code>100</code> . <code>0</code> means, the trip table
     * is still build. <code>100</code> does not guaranty that the process is finished.
     */
    public int getProgress() {
        if (totalEstimate > 0) {
            if (cntTrips == 0) {
                return 0;
            }
            int prg = (int) (100 * cntTrips / (double) totalEstimate);
            return Math.max(1, Math.min(prg, 100));
        }

        return -1;
    }

//    protected void finalize() throws Throwable {
//        try {
//            close();
//        } catch (Throwable t) {
//            throw t;
//        } finally {
//            super.finalize();
//        }
//    }

    @Override
    public String getSource() {
        return tripIterator.getSource();
    }

    public long getTotal() {

        return totaltripcount;
    }

    private void init(String simulation, String hhkey, String schema, String region, TPS_SettlementSystemType settlementType, Set<Integer> tazFilter, TPS_DB_Connector connection, StyledDocument console) throws SQLException, ClassNotFoundException, IOException {

        this.console = console;
        this.settlementType = settlementType;
        ResultSet rs;
        String q = "SELECT param_value FROM simulation_parameters WHERE sim_key = '" + simulation +
                "' and param_key = 'DB_TABLE_TRIPS'";
        String trip_table;
        rs = connection.executeQuery(q, this);
        if (rs.next()) {
            trip_table = rs.getString("param_value") + "_" + simulation;
        } else {
            throw new SQLException("Neccessary value for key DB_TABLE_TRIPS not found! Query = " + q);
        }
        tripIterator = new TripIterator(simulation, hhkey, schema, region, tazFilter, trip_table, connection, true);

        // try getting estimate for number

        q = "SELECT sim_total FROM simulations WHERE sim_key = '" + simulation + "'";
        rs.close();

        rs = connection.executeQuery(q, this);
        if (rs.next()) {
            totalEstimate = (long) rs.getDouble("sim_total") * 8;
        } else {
            totalEstimate = -1;
        }
        rs.close();
    }

    public boolean isCancelled() {


        return this.isCancelled;
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {

        this.pcs.removePropertyChangeListener(listener);
    }

    /**
     * This class is the actual connection to the database that uses a separate thread.
     *
     * @author boec_pa
     */


    private class DBTripFetcher implements Runnable {

        private final String simulation;
        private final String hhkey;
        private final String schema;
        private final String region;
        private final boolean verbose;

        private final TPS_DB_Connector dbCon;

        private final PreparedStatement fetchNext;
        private Statement fillTableStatement;

        private int fetchStart = 0;

        private final BlockingQueue<TapasTrip> queue;

        private boolean isFinished = false;

        public DBTripFetcher(BlockingQueue<TapasTrip> q, String simulation, String hhkey, String schema, String region, TPS_DB_Connector connection, boolean verbose) throws SQLException, IOException, ClassNotFoundException {

            //this.simulation = region + "_trips_" + simulation;
            this.simulation = simulation;
            this.hhkey = hhkey;
            this.schema = schema;
            this.region = region;
            this.verbose = verbose;
            this.queue = q;

            dbCon = connection;
            try {

                // get settlement type parameters
                TPS_DB_IOManager dbIOM = new TPS_DB_IOManager(dbCon.getParameters());
                TPS_DB_IO dbIO = new TPS_DB_IO(dbIOM);
                dbIO.initStart();

                dbIO.readSettlementSystemCodes(FuncUtils.toRawSimKey.apply(this.simulation));

            } catch (IOException | ClassNotFoundException e) {
                throw e; // handle that outside of the class
            }

            try {
                fetchNext = dbCon.getConnection(this).prepareStatement(
                        "SELECT * FROM tt_" + this.simulation + " WHERE t_id >= ? AND t_id < ? " + " ORDER BY t_id");

            } catch (SQLException e) {
                e.printStackTrace();
                System.err.println("Could no prepare fetch statement.");
                throw e;
            }
        }

        private boolean cleanDB() {

            if (verbose) {
                // Check if tables exist
                try {
                    ArrayList<String> tables = new ArrayList<>();
                    DatabaseMetaData metaData = dbCon.getConnection(this).getMetaData();
                    ResultSet res = metaData.getTables(null, null, null, new String[]{"TABLE"});

                    while (res.next()) {
                        tables.add(res.getString("TABLE_NAME"));
                    }

                    if (tables.contains("tt_" + simulation)) {
                        System.err.println("Trip table already existed and will be dropped.");
                        if (dbCon.getConnection(this).createStatement().executeUpdate("DROP TABLE tt_" + simulation) <
                                0) {
                            System.err.println("Could not drop table!");
                            return false;
                        }
                    }
                } catch (SQLException e) {
                    System.err.println("Could not fetch meta data. Check connection to database.");
                    return false;
                }
            }
            return true;
        }

        private void close() {
            try {
                if (dbCon != null) {
                    if (fillTableStatement != null) {
                        fillTableStatement.cancel();
                    }
                    dbCon.getConnection(this).createStatement().executeUpdate("DROP TABLE tt_" + simulation);
                    dbCon.getConnection(this).close();
                }

            } catch (SQLException e) {
                // do nothing
            }

        }

        private boolean createTripTable() throws BadLocationException, SQLException {


            if (verbose && console != null) {
                console.insertString(console.getLength(), "Trip table for " + simulation + " will be created\n", null);
            } else {
                System.out.println("Trip table for " + simulation + " will be created");
            }

            cleanDB();

            String createTable = "CREATE TABLE tt_%tablename% " +
                    "( t_id SERIAL NOT NULL CONSTRAINT tt_%tablename%p_key PRIMARY KEY, " +
                    " p_id INTEGER, hh_id INTEGER, p_group INTEGER, t_duration DOUBLE PRECISION, " +
                    " t_start_time INTEGER, t_mode INTEGER, t_distance_bl DOUBLE PRECISION, " +
                    " t_distance_real DOUBLE PRECISION, t_activity INTEGER, t_activity_start_min INTEGER, t_activity_duration_min INTEGER, t_is_home BOOLEAN, " +
                    " t_taz_id_start INTEGER, t_taz_id_end INTEGER, t_loc_id_start INTEGER, t_loc_id_end INTEGER, taz_bbr_type_start INTEGER, " +
                    " bbr_type_home INTEGER )";

            String fillTable, updateBBRStart, updateBBRHome;

            //find out if we use the "old" triptable, where we have to join several attributes or the new one
            String tableCheck = "SELECT column_name" + " FROM information_schema.columns " +
                    "WHERE table_name='%tablename%' and column_name='p_group'";
            tableCheck = tableCheck.replaceAll("%tablename%", simulation);
            try {
                fillTableStatement = dbCon.getConnection(this).createStatement();
                ResultSet rs = fillTableStatement.executeQuery(tableCheck);
                if (rs.next()) { // new format
                    fillTable = "INSERT INTO tt_%tablename%( " +
                            " p_id, hh_id, p_group, t_duration,t_start_time, t_mode, t_distance_bl, " +
                            " t_distance_real, t_activity, t_activity_start_min, t_activity_duration_min, t_is_home,t_taz_id_start,t_taz_id_end,t_loc_id_start,t_loc_id_end, taz_bbr_type_start, bbr_type_home " +
                            ") " + " SELECT " +
                            " ts.p_id, ts.hh_id, ts.p_group, ts.travel_time_sec, ts.start_time_min, ts.mode, ts.distance_bl_m, " +
                            " ts.distance_real_m, ts.activity, ts.activity_start_min, ts.activity_duration_min, ts.is_home, ts.taz_id_start, ts.taz_id_end, ts.loc_id_start, ts.loc_id_end, ts.taz_bbr_type_start, ts.bbr_type_home " +
                            " FROM %tablename% ts";
                    updateBBRStart = "";
                    updateBBRHome = "";
                } else {
                    fillTable = "INSERT INTO tt_%tablename%( " +
                            " p_id, hh_id, p_group, t_duration,t_start_time, t_mode, t_distance_bl, " +
                            " t_distance_real, t_activity, t_activity_start_min, t_activity_duration_min, t_is_home,t_taz_id_start,t_taz_id_end,t_loc_id_start,t_loc_id_end" +
                            ") " + " SELECT " +
                            " ps.p_id, ts.hh_id, ps.group, ts.travel_time_sec, ts.start_time_min, ts.mode, ts.distance_bl_m, " +
                            " ts.distance_real_m, ts.activity, ts.activity_start_min, ts.activity_duration_min, ts.is_home, ts.taz_id_start, ts.taz_id_end, ts.loc_id_start, ts.loc_id_end " +
                            " FROM %tablename% ts " +
                            " INNER JOIN %schemaname%.%region%_persons ps ON ts.p_id  = ps.p_id and ts.hh_id = ps.hh_id AND '%p_hh_key%' = ps.key";
                    updateBBRStart = "UPDATE tt_%tablename% as tt set taz_bbr_type_start = " +
                            "(select taz_bbr_type from %schemaname%.%region%_taz as taz where taz.taz_id= tt.t_taz_id_start)";
                    updateBBRHome = "UPDATE tt_%tablename% as tt set bbr_type_home = " +
                            "(select taz_bbr_type from %schemaname%.%region%_households as hh join %schemaname%.%region%_taz as taz " +
                            "on hh.hh_taz_id = taz.taz_id where hh.hh_id= tt.hh_id and hh.hh_key = '%p_hh_key%')";
                }


            } catch (SQLException e) {
                e.printStackTrace();
                System.err.println("Error while filling trip table with query " + tableCheck);
                return false;
            }


            createTable = createTable.replaceAll("%tablename%", simulation);
            fillTable = fillTable.replaceAll("%tablename%", simulation).replaceAll("%region%", region).replaceAll(
                    "%schemaname%", schema).replaceAll("%p_hh_key%", hhkey);
            updateBBRStart = updateBBRStart.replaceAll("%tablename%", simulation).replaceAll("%region%", region)
                                           .replaceAll("%schemaname%", schema).replaceAll("%p_hh_key%", hhkey);
            updateBBRHome = updateBBRHome.replaceAll("%tablename%", simulation).replaceAll("%region%", region)
                                         .replaceAll("%schemaname%", schema).replaceAll("%p_hh_key%", hhkey);


            try {
                dbCon.getConnection(this).createStatement().executeUpdate(createTable);
            } catch (SQLException e) {
                e.printStackTrace();
                System.err.println("Could not create table! query = " + createTable);
                return false;
            }


            if (fillTable.length() > 0) {
                if (verbose && console != null) {
                    console.insertString(console.getLength(),
                            "Start filling the trip table. This may take a while...\n", null);
                } else {
                    System.out.println("Start filling the trip table. This may take a while...");
                }
                try {
                    fillTableStatement = dbCon.getConnection(this).createStatement();
                    fillTableStatement.executeUpdate(fillTable);
                } catch (SQLException e) {
                    e.printStackTrace();
                    System.err.println("Error while filling trip table with query " + fillTable);
                    return false;
                }
            } else {
                return false;
            }

            if (updateBBRStart.length() > 0) {
                if (verbose && console != null) {
                    console.insertString(console.getLength(),
                            "Updating the bbsr-type of starts in trip table. This may take a while...\n", null);
                } else {
                    System.out.println("Updating the bbsr-type of starts in trip table. This may take a while...");
                }

                try {
                    fillTableStatement = dbCon.getConnection(this).createStatement();
                    fillTableStatement.executeUpdate(updateBBRStart);
                } catch (SQLException e) {
                    e.printStackTrace();
                    System.out.println("Error while filling trip table with query " + updateBBRStart);
                    return false;
                }
            }

            if (updateBBRHome.length() > 0) {

                if (verbose && console != null) {
                    console.insertString(console.getLength(),
                            "Updating the bbsr-type of homes in trip table. This may take a while...\n", null);
                } else {
                    System.out.println("Updating the bbsr-type of homes in trip table. This may take a while...");
                }

                try {
                    fillTableStatement = dbCon.getConnection(this).createStatement();
                    fillTableStatement.executeUpdate(updateBBRHome);

                } catch (SQLException e) {
                    e.printStackTrace();
                    System.err.println("Error while filling trip table with query " + updateBBRHome);
                    return false;
                }
            }

            if (verbose && console != null) {
                console.insertString(console.getLength(), "Trip table successfully created.\n", null);
            } else {
                System.out.println("Trip table successfully created.");
            }
            Statement st = dbCon.getConnection(this).createStatement();
            ResultSet rs = st.executeQuery("select count(*) from " + simulation);
            if (rs.next()) {
                totaltripcount = rs.getInt(1);
                rs.close();
            }
            return true;
        }

        public String getSource() {
            return simulation.replaceFirst("[^\\s]*_trips_", "");
        }

        private TapasTrip getTripFromResult(ResultSet rs) {
            int tripId = -1;
            try {
                tripId = rs.getInt("t_id");
                int pId = rs.getInt("p_id");
                int hhId = rs.getInt("hh_id");
                int pGroup = rs.getInt("p_group");
                double dur = rs.getDouble("t_duration");
                int startTime = rs.getInt("t_start_time");
                int mode = rs.getInt("t_mode");
                double distBL = rs.getDouble("t_distance_bl");
                double dist = rs.getDouble("t_distance_real");
                int activity = rs.getInt("t_activity");
                int activityStart = rs.getInt("t_activity_start_min");
                int activityDuration = rs.getInt("t_activity_duration_min");
                boolean isHome = rs.getBoolean("t_is_home");
                int tazIdStart = rs.getInt("t_taz_id_start");
                int tazIdEnd = rs.getInt("t_taz_id_end");
                int locIdStart = rs.getInt("t_loc_id_start");
                int locIdEnd = rs.getInt("t_loc_id_end");
                int tazBbrTypeStart = rs.getInt("taz_bbr_type_start");
                int bbrTypeHome = rs.getInt("bbr_type_home");

                // bbr type conversion
                if (null != settlementType) {
                    TPS_SettlementSystem rc = TPS_SettlementSystem.getSettlementSystem(settlementType, tazBbrTypeStart);
                    if (null == rc) {
                        throw new IllegalArgumentException("The Settlement Type must be wrong.");
                    }
                    tazBbrTypeStart = rc.getCode(TPS_SettlementSystemType.TAPAS);
                    bbrTypeHome = rc.getCode(TPS_SettlementSystemType.TAPAS);
                }

                return new TapasTrip(activity, activityStart, activityDuration, tazBbrTypeStart, bbrTypeHome,
                        tazIdStart, tazIdEnd, locIdStart, locIdEnd, distBL, dist, mode, pId, hhId, pGroup, startTime,
                        dur, isHome);
            } catch (SQLException e) {
                System.err.println("Could not parse trip with id " + tripId);
                e.printStackTrace();
                return null;
            }
        }

//        @Override
//        protected void finalize() throws Throwable {
//            try {
//                close();
//            } catch (Throwable t) {
//                throw t;
//            } finally {
//                super.finalize();
//            }
//        }

        public void run() {

            try {
                if (!createTripTable()) {

                    isFinished = true;
                    pcs.firePropertyChange("triptable", null, false);

                    Thread.currentThread().interrupt();
                    close();
                } else {
                    pcs.firePropertyChange("triptable", null, true);
                }
            } catch (BadLocationException | SQLException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }

            while (!isFinished && !isCancelled) {
                try {
                    fetchNext.setInt(1, fetchStart);
                    fetchNext.setInt(2, fetchStart + FETCH_SIZE);

                    ResultSet currentSet = fetchNext.executeQuery();

                    isFinished = !currentSet.next();

                    if (!isFinished) {
                        do {
                            TapasTrip tt = getTripFromResult(currentSet);
                            if (tt != null) {
                                try {
                                    queue.put(tt);
                                } catch (InterruptedException e) {
                                    close();
                                }
                            } else {
                                System.err.println("Bad trip found");
                            }
                        } while (currentSet.next() && !isCancelled);
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    isFinished = true;
                }
                fetchStart += FETCH_SIZE;
            }
            try {
                queue.put(POISON_INSTANCE);
            } catch (InterruptedException e) {
                close();
            }
            close();
        }
    }

    private class TripIterator implements Iterator<TapasTrip>, Runnable {

        private final BlockingQueue<TapasTrip> q = new ArrayBlockingQueue<>(QUEUE_SIZE);

        private final Thread fetcherThread;
        private final DBTripFetcher tripFetcher;

        private final Set<Integer> acceptedTAZs;

        private TapasTrip nextItem = null;

        public TripIterator(String simulation, String hhkey, String schema, String region, Set<Integer> acceptedTAZs, String trip_table, TPS_DB_Connector connection, boolean verbose) throws SQLException, ClassNotFoundException, IOException {
            try {
                tripFetcher = new DBTripFetcher(q, trip_table, hhkey, schema, region, connection, verbose);
                fetcherThread = new Thread(tripFetcher);
                this.acceptedTAZs = acceptedTAZs;
            } catch (SQLException | ClassNotFoundException | IOException e) {
                e.printStackTrace();
                throw e;
            }

            new Thread(this).start();
        }

        private void close() {
            tripFetcher.close();
        }

        public String getSource() {
            return tripFetcher.getSource();
        }

        public boolean hasNext() {
            if (nextItem == null) {
                try {
                    if (acceptedTAZs == null) {
                        nextItem = q.take();
                        DBTripReader.this.cntTrips++;
                    } else {
                        do {
                            nextItem = q.take();
                            DBTripReader.this.cntTrips++;
                        } while (!acceptedTAZs.contains(nextItem.getBez92Arr()) && !acceptedTAZs.contains(
                                nextItem.getBez92Dep()));
                    }
                } catch (InterruptedException e) {
                    close();
                    return false;
                }
            }
            return nextItem != POISON_INSTANCE;
        }

        public TapasTrip next() {
            try {
                if (nextItem == null) { //TODO: must i check for accepted TAZ like it was done in the hasNext routine?!?
                    nextItem = q.take();
                    DBTripReader.this.cntTrips++;
                }
                TapasTrip tt = nextItem;
                //TODO: must i check for accepted TAZ like it was done in the hasNext routine?!?
                nextItem = q.take();
                DBTripReader.this.cntTrips++;
                return tt;
            } catch (InterruptedException e) {
                close();
                return null;
            }
        }

        public void remove() {
            throw new UnsupportedOperationException("Not supported.");
        }

//        @Override
//        protected void finalize() throws Throwable {
//            try {
//                close();
//            } catch (Throwable t) {
//                throw t;
//            } finally {
//                super.finalize();
//            }
//        }

        @Override
        public void run() {
            fetcherThread.start();
        }
    }
}
