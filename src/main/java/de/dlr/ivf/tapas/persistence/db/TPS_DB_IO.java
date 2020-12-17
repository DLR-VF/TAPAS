/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.persistence.db;

import de.dlr.ivf.tapas.constants.*;
import de.dlr.ivf.tapas.constants.TPS_ActivityConstant.TPS_ActivityCodeType;
import de.dlr.ivf.tapas.constants.TPS_LocationConstant.TPS_LocationCodeType;
import de.dlr.ivf.tapas.constants.TPS_PersonGroup.TPS_PersonGroupType;
import de.dlr.ivf.tapas.constants.TPS_SettlementSystem.TPS_SettlementSystemType;
import de.dlr.ivf.tapas.distribution.TPS_DiscreteDistribution;
import de.dlr.ivf.tapas.loc.*;
import de.dlr.ivf.tapas.loc.TPS_TrafficAnalysisZone.ScenarioTypeValues;
import de.dlr.ivf.tapas.log.LogHierarchy;
import de.dlr.ivf.tapas.log.TPS_Logger;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface.HierarchyLogLevel;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface.SeverenceLogLevel;
import de.dlr.ivf.tapas.mode.*;
import de.dlr.ivf.tapas.mode.TPS_Mode.ModeType;
import de.dlr.ivf.tapas.modechoice.TPS_ExpertKnowledgeNode;
import de.dlr.ivf.tapas.modechoice.TPS_ExpertKnowledgeTree;
import de.dlr.ivf.tapas.modechoice.TPS_ModeChoiceTree;
import de.dlr.ivf.tapas.modechoice.TPS_Node;
import de.dlr.ivf.tapas.persistence.TPS_RegionResultSet;
import de.dlr.ivf.tapas.persistence.db.TPS_DB_IOManager.Behaviour;
import de.dlr.ivf.tapas.person.TPS_Car;
import de.dlr.ivf.tapas.person.TPS_Household;
import de.dlr.ivf.tapas.person.TPS_Person;
import de.dlr.ivf.tapas.runtime.util.IPInfo;
import de.dlr.ivf.tapas.scheme.*;
import de.dlr.ivf.tapas.util.*;
import de.dlr.ivf.tapas.util.TPS_AttributeReader.TPS_Attribute;
import de.dlr.ivf.tapas.util.parameters.*;
import org.apache.commons.lang3.ArrayUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.InetAddress;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;


@LogHierarchy(hierarchyLogLevel = HierarchyLogLevel.CLIENT)
public class TPS_DB_IO {

    public static final double INTERCHANGE_FACTOR = 0.01;
    private static final int fetchSizePerProcessor = 256;
    private static final int fetchSizePerProcessorPrefetchHouseholds = 256;
    /// The ip address of this machine
    private static InetAddress ADDRESS = null;
    /// The number of households to fetch per cpu
    private final int numberToFetch;
    Map<Integer, TPS_Household> houseHoldMap = new TreeMap<>();
    Map<Integer, TPS_Car> carMap = new HashMap<>();
    /// The reference to the persistence manager
    private final TPS_DB_IOManager PM;
    /// A queue for prefetched households. Filled by getNextHouseholds .
    private final Queue<TPS_Household> prefetchedHouseholds = new LinkedList<>();
    /// The number of households fetched for this chunk. should be cpu*numberToFetch or the remainders in db for the
    // last fetch
    private int numberOfFetchedHouseholds;
    private int lastCleanUp = -1;

    /**
     * Constructor
     *
     * @param pm the managing class for sql-commands
     * @throws IOException if the machine has no IP address, an exception is thrown.
     */
    public TPS_DB_IO(TPS_DB_IOManager pm) throws IOException {
        if (ADDRESS == null) {
            ADDRESS = IPInfo.getEthernetInetAddress();
        }
        this.PM = pm;
        // TODO: use the defined number of threads
        if (this.PM.getParameters().isTrue(ParamFlag.FLAG_PREFETCH_ALL_HOUSEHOLDS)) {
            numberToFetch = fetchSizePerProcessorPrefetchHouseholds * Runtime.getRuntime().availableProcessors();
        } else {
            numberToFetch = fetchSizePerProcessor * Runtime.getRuntime().availableProcessors();
        }
        numberOfFetchedHouseholds = -1;
    }

    /**
     * Helper function to extract an sql-array to a Java double array
     *
     * @param rs    The ResultSet containing a sql-Array
     * @param index The index position of the SQL-Array
     * @return A double array
     * @throws SQLException
     */
    public static double[] extractDoubleArray(ResultSet rs, String index) throws SQLException {
        Object array = rs.getArray(index).getArray();
        if (array instanceof double[]) {
            return (double[]) array;
        } else if (array instanceof Double[]) {
            return ArrayUtils.toPrimitive((Double[]) array); // like casting Double[] to double[]
        } else {
            throw new SQLException("Cannot cast to int array");
        }
    }

    /**
     * Helper function to extract an sql-array to a Java int array
     *
     * @param rs    The ResultSet containing a sql-Array
     * @param index The index position of the SQL-Array
     * @return A int array
     * @throws SQLException
     */
    public static int[] extractIntArray(ResultSet rs, String index) throws SQLException {
        Object array = rs.getArray(index).getArray();
        if (array instanceof int[]) {
            return (int[]) array;
        } else if (array instanceof Integer[]) {
            return ArrayUtils.toPrimitive((Integer[]) array); // like casting Integer[] to int[]
        } else {
            throw new SQLException("Cannot cast to int array");
        }
    }

    /**
     * Helper function to extract an sql-array to a Java String array
     *
     * @param rs    The ResultSet containing a sql-Array
     * @param index The index position of the SQL-Array
     * @return A String array
     * @throws SQLException
     */
    public static String[] extractStringArray(ResultSet rs, String index) throws SQLException {
        Object array = rs.getArray(index).getArray();
        if (array instanceof String[]) {
            return (String[]) array;
        } else {
            throw new SQLException("Cannot cast to string array");
        }
    }

    /**
     * Method to convert matrixelements to a sql-parsable array
     *
     * @param array         the array
     * @param decimalPlaces number of decimal places for the string
     * @return
     */
    public static String matrixToSQLArray(Matrix array, int decimalPlaces) {
        StringBuilder buffer;
        StringBuilder totalBuffer = new StringBuilder("ARRAY[");
        int size = array.getNumberOfColums();
        for (int j = 0; j < size; ++j) {
            buffer = new StringBuilder();
            for (int k = 0; k < size; ++k) {
                buffer.append(new BigDecimal(array.getValue(j, k)).setScale(decimalPlaces, RoundingMode.HALF_UP))
                      .append(",");
            }
            if (j < size - 1) totalBuffer.append(buffer);
            else totalBuffer.append(buffer.substring(0, buffer.length() - 1)).append("]");
        }

        return totalBuffer.toString();
    }

    /**
     * Here we create a person for a given SQL-ResultSet to add it to a household.
     *
     * @param pRs
     * @return
     * @throws SQLException
     */
    private TPS_Person addPersonToHousehold(ResultSet pRs) throws SQLException {
        boolean hasBike = pRs.getBoolean("p_has_bike") && Randomizer.random() < this.PM.getParameters().getDoubleValue(
                ParamValue.AVAILABILITY_FACTOR_BIKE);// TODO: make
        // a better model
        double working = pRs.getInt("p_working") / 100.0;
        double budget = (pRs.getInt("p_budget_it") + pRs.getInt("p_budget_pt")) / 100.0;

        TPS_Person person = new TPS_Person(pRs.getInt("p_id"), TPS_Sex.getEnum(pRs.getInt("p_sex")),
                TPS_PersonGroup.getPersonGroupByTypeAndCode(TPS_PersonGroupType.TAPAS, pRs.getInt("p_group")),
                pRs.getInt("p_age"), pRs.getBoolean("p_abo"), hasBike, budget, working, false, pRs.getInt("p_work_id"),
                pRs.getInt("p_education"), this.PM.getParameters().isTrue(ParamFlag.FLAG_USE_SHOPPING_MOTIVES));

        if (this.PM.getParameters().isTrue(ParamFlag.FLAG_USE_DRIVING_LICENCE)) {
            int licCode = pRs.getInt("p_driver_license");
            if (licCode == 1) {
                person.setDrivingLicenseInformation(TPS_DrivingLicenseInformation.CAR);
            } else {
                person.setDrivingLicenseInformation(TPS_DrivingLicenseInformation.NO_DRIVING_LICENSE);
            }
        } else {
            person.setDrivingLicenseInformation(TPS_DrivingLicenseInformation.UNKNOWN);
        }

        // case for robotaxis: all if wanted
        boolean isCarPooler = Randomizer.random() < this.PM.getParameters().getDoubleValue(
                ParamValue.AVAILABILITY_FACTOR_CARSHARING);
        if (this.PM.getParameters().isFalse(ParamFlag.FLAG_USE_ROBOTAXI)) {
            // no robotaxis: must be able to drive a car and be older than MIN_AGE_CARSHARING
            isCarPooler &= person.getAge() >= this.PM.getParameters().getIntValue(ParamValue.MIN_AGE_CARSHARING) &&
                    person.mayDriveACar();
        }
        person.setCarPooler(isCarPooler);
        //
        TPS_Household hh = houseHoldMap.get(pRs.getInt("p_hh_id"));
        hh.addMember(person);
        return person;
    }

    /**
     * Empty all global static constants maps
     */
    public void clearConstants() {

        TPS_TrafficAnalysisZone.LOCATION2ACTIVITIES_MAP.clear();
        TPS_TrafficAnalysisZone.ACTIVITY2LOCATIONS_MAP.clear();


        TPS_ActivityConstant.clearActivityConstantMap();
        TPS_AgeClass.clearAgeClassMap();
        TPS_Distance.clearDistanceMap();
        TPS_Income.clearIncomeMap();
        TPS_LocationConstant.clearLocationConstantMap();
        TPS_Mode.clearModeMap();
        TPS_PersonGroup.clearPersonGroupMap();
        TPS_SettlementSystem.clearSettlementSystemMap();
    }

    /**
     * Method to drop all temporary tables in the db
     */
    void dropTemporaryTables() {
        this.PM.functionExecute("drop_temp_tables",
                this.PM.getParameters().getString(ParamString.DB_TABLE_HOUSEHOLD_TMP),
                this.PM.getParameters().getString(ParamString.DB_TABLE_LOCATION_TMP),
                !Behaviour.FAT.equals(TPS_DB_IOManager.BEHAVIOUR));
    }

    /**
     * Here we load a new set of households and return the number of fetched ones.
     * We have to check if the simulation is running and still existent. Someone might have deleted it!
     * Then we fetch the unfinished households from any previous simulation (crashed sim pick up).
     * Then we mark new households in the temporary household table (job table), that we will pick them.
     * Here we have to check for race conditions with other computers!
     * Next we load the household IDs
     * Then we load the persons and finally we store them in the household-pool and return.
     *
     * @param region with households to fetch
     * @return the number of fetched households
     */

    private int fetchNextSetOfHouseholds(TPS_Region region) {
        String query = "";
        try {        //check if simulation is running
            query = "SELECT sim_started FROM " + this.PM.getParameters().getString(ParamString.DB_TABLE_SIMULATIONS) +
                    " WHERE sim_key = '" + this.PM.getParameters().getString(ParamString.RUN_IDENTIFIER) + "'";
            ResultSet sRs = PM.executeQuery(query);
            if (!sRs.next()) {
                TPS_Logger.log(HierarchyLogLevel.THREAD, SeverenceLogLevel.INFO, "simulation key removed from db!");
                return 0;
            }
            if (sRs.getBoolean("sim_started")) {
                if (TPS_Logger.isLogging(HierarchyLogLevel.THREAD, SeverenceLogLevel.INFO)) {
                    TPS_Logger.log(HierarchyLogLevel.THREAD, SeverenceLogLevel.INFO, "Fetching new set of households");
                }
                String hhtemptable = this.PM.getParameters().getString(ParamString.DB_TABLE_HOUSEHOLD_TMP);


                int actualCount = numberToFetch;
                List<Integer> hIDs = new ArrayList<>();
                // first fetch number unfinished hh for this server
                query = "SELECT count(*) as unfinished FROM " + hhtemptable +
                        " WHERE hh_started = true AND hh_finished = false AND server_ip = inet '" +
                        ADDRESS.getHostAddress() + "'";
                ResultSet hRs = PM.executeQuery(query);
                if (hRs.next()) {
                    actualCount -= hRs.getInt("unfinished");
                }
                hRs.close();

                //mark new set of households: This is done in a single statement.
                //Deadlocks are avoided by sorting by prio, which is a random number initialized during
                // simulation-setup. So its fix for the whole sim and guarantees, that no "ring lock" will happen!
                if (actualCount > 0) {
                    Connection con = this.PM.getDbConnector().getConnection(this);

                    //time critical lock
                    con.setAutoCommit(false);
                    Statement st = con.createStatement();
                    query = "LOCK TABLE " + hhtemptable + " IN EXCLUSIVE MODE";
                    st.execute(query);
                    query = "UPDATE " + hhtemptable + " SET hh_started = true, server_ip = inet '" +
                            ADDRESS.getHostAddress() + "' WHERE hh_started = false AND hh_id = ANY(SELECT hh_id FROM " +
                            hhtemptable + " WHERE hh_started = false ORDER BY prio, hh_id LIMIT " + actualCount + ")";
                    st.execute(query);
                    con.commit();
                    con.setAutoCommit(true);
                    if (TPS_Logger.isLogging(HierarchyLogLevel.THREAD, SeverenceLogLevel.INFO)) {
                        TPS_Logger.log(HierarchyLogLevel.THREAD, SeverenceLogLevel.INFO,
                                "Assigned " + actualCount + " new households this instance.");
                    }
                }

                //get all hhId which should be processed in the next round
                query = "SELECT hh_id FROM " + hhtemptable +
                        " WHERE hh_started = true AND hh_finished = false AND server_ip = inet '" +
                        ADDRESS.getHostAddress() + "' ORDER BY prio, hh_id";
                hRs = PM.executeQuery(query);
                int id;
                while (hRs.next()) {
                    id = hRs.getInt("hh_id");
                    hIDs.add(id);
                }
                hRs.close();

                if (TPS_Logger.isLogging(HierarchyLogLevel.THREAD, SeverenceLogLevel.INFO)) {
                    TPS_Logger.log(HierarchyLogLevel.THREAD, SeverenceLogLevel.INFO,
                            "Fetching data for " + hIDs.size() + " households.");
                }

                List<TPS_Household> hhs;
                if (hIDs.size() == 0) {
                    this.houseHoldMap.clear();
                } else {
                    hhs = this.loadHouseholds(region, hIDs);
                    for (TPS_Household hh : hhs) {
                        this.prefetchedHouseholds.offer(hh);
                    }
                }
            }
            this.numberOfFetchedHouseholds = this.prefetchedHouseholds.size();
            if (numberOfFetchedHouseholds > 0) {
                if (TPS_Logger.isLogging(HierarchyLogLevel.THREAD, SeverenceLogLevel.INFO)) {
                    TPS_Logger.log(HierarchyLogLevel.THREAD, SeverenceLogLevel.INFO,
                            "Fetched " + numberOfFetchedHouseholds + " households");
                }
            }
        } catch (SQLException e) {
            TPS_Logger.log(HierarchyLogLevel.THREAD, SeverenceLogLevel.ERROR, "error during one of th sqls: " + query,
                    e);
            TPS_Logger.log(HierarchyLogLevel.THREAD, SeverenceLogLevel.ERROR, "next exception:", e.getNextException());
        }
        return this.prefetchedHouseholds.size();
    }

    /**
     * Household prefetching from database:
     * ATTENTION: THIS MUST BE DONE SYNCHRONIZED! ONLY ONE FUNCTION CALL ALLOWED PER COMPUTER!
     * If the pool of prefetched households is not empty, the top of the pool is returned.
     * If the pool is empty a new pool is fetched from the db.
     * If there is no further household to process, null is returned
     *
     * @param region The region to fetch
     * @return top of the household pool
     */
    TPS_Household getNextHousehold(TPS_Region region) {
        synchronized (prefetchedHouseholds) {
            if (prefetchedHouseholds.isEmpty()) { //household pool is empty
                if (this.numberOfFetchedHouseholds != 0) {//last fetch returned something
                    this.PM.insertTrips();
                    this.updateOccupancyTable(region);
                    this.vacuumTempTables();
                    if (this.fetchNextSetOfHouseholds(region) == 0) {
                        if (TPS_Logger.isLogging(HierarchyLogLevel.THREAD, SeverenceLogLevel.INFO)) {
                            TPS_Logger.log(HierarchyLogLevel.THREAD, SeverenceLogLevel.INFO, "Finished all households");
                        }
                        return null;
                    }
                }
            }
            return this.prefetchedHouseholds.poll();
        }
    }

    /**
     * @return The number of households fetched in this round.
     */
    int getNumberOfFetchedHouseholds() {
        return numberOfFetchedHouseholds;
    }

    /**
     * Method to return the locations, which are possible to use for the given activity.
     *
     * @param region            The region to look in
     * @param comingFrom        The location where the person is currently present
     * @param arrivalDuration   the duration of getting to comingFrom
     * @param goingTo           the location the person wants to visit afterwards
     * @param departureDuration the time he has to reach goingTo
     * @param activityCode      The activity code for this query
     * @return A TPS_RegionResultSet of appropriate locations
     * @throws SQLException Exceptions during sql-queries
     */
    public TPS_RegionResultSet getTrafficAnalysisZonesAround(TPS_Region region, Locatable comingFrom, double arrivalDuration, Locatable goingTo, double departureDuration, TPS_ActivityConstant activityCode) throws SQLException {

        TPS_RegionResultSet regionRS = new TPS_RegionResultSet();

        int i;
        // normal case
        double incFactor = this.PM.getParameters().getDoubleValue(ParamValue.MAX_SYSTEM_SPEED) /
                this.PM.getParameters().getIntValue(
                        ParamValue.MAX_TRIES_LOCATION_SELECTION); // speed slices from MAX_SYSTEM_SPEED/
        // MAX_TRIES_LOCATION_SELECTION to MAX_SYSTEM_SPEED
        double arrivalDistance, departureDistance;
        String tableLocTemp = this.PM.getParameters().getString(ParamString.DB_TABLE_LOCATION_TMP);
        String tableTAZ = this.PM.getParameters().getString(ParamString.DB_TABLE_TAZ);
        String query = null;

        //TODO: make this better!
        //some trips start per default at different i and not at i=1!
        switch (activityCode.getCode(TPS_ActivityCodeType.ZBE)) {
            case 720:
            case 721:
            case 722:
            case 723:
            case 724:
            case 640:
            case 300: //free time
                i = this.PM.getParameters().getIntValue(ParamValue.MAX_TRIES_LOCATION_SELECTION) / 2 +
                        1; //in case of MAX_TRIES_LOCATION_SELECTION=1 this is still 1
                break;
            case 211: //work
                i = this.PM.getParameters().getIntValue(
                        ParamValue.MAX_TRIES_LOCATION_SELECTION); //allways look at the maximum range!
                break;
            default: //others
                i = 1;
                break;
        }
        for (; i <= this.PM.getParameters().getIntValue(ParamValue.MAX_TRIES_LOCATION_SELECTION) + 1 &&
                regionRS.size() < 2; i++) {
            regionRS.clear();
            // long time = System.currentTimeMillis();

            if (i <= this.PM.getParameters().getIntValue(ParamValue.MAX_TRIES_LOCATION_SELECTION)) {
                arrivalDistance = arrivalDuration * incFactor * (double) i;
                departureDistance = departureDuration * incFactor * (double) i;
            } else {
                //desperate last try: drop distance constraint!
                arrivalDistance = Double.MAX_VALUE;
                departureDistance = Double.MAX_VALUE;
            }

            switch (TPS_DB_IOManager.BEHAVIOUR) {
                case MEDIUM:
                    StringBuilder sb = new StringBuilder("ARRAY[");
                    for (TPS_TrafficAnalysisZone taz : region) {
                        if (TPS_Geometrics.isWithin(taz.getCoordinate(), comingFrom.getCoordinate(),
                                this.PM.getParameters().getDoubleValue(ParamValue.MIN_DIST), arrivalDistance) &&
                                TPS_Geometrics.isWithin(taz.getCoordinate(), goingTo.getCoordinate(),
                                        this.PM.getParameters().getDoubleValue(ParamValue.MIN_DIST),
                                        departureDistance)) {
                            sb.append(taz.getTAZId() + ",");
                        }
                    }
                    sb.setCharAt(sb.length() - 1, ']');
                    query = "SELECT * FROM core.select_taz_and_loc_rep('" + tableLocTemp + "',  '" + sb.toString() +
                            "', " + activityCode.getCode(TPS_ActivityCodeType.ZBE) + ", " + Randomizer.random() + ")";
                    break;
                case THIN:
                    query = "SELECT * FROM core.select_taz_and_loc_rep('" + tableTAZ + "', '" + tableLocTemp + "', " +
                            comingFrom.getTAZId() + ", " + goingTo.getTAZId() + ", " + arrivalDistance + ", " +
                            departureDistance + ", " + activityCode.getCode(TPS_ActivityCodeType.ZBE) + ", " +
                            Randomizer.random() + ")";
                    break;
                default:
                    //nothing
            }

            if (query != null) {
                ResultSet rs = PM.executeQuery(query);
                while (rs.next()) {
                    TPS_TrafficAnalysisZone taz = region.getTrafficAnalysisZone(rs.getInt(1));
                    TPS_Location location = region.getLocation(rs.getInt(2));
                    regionRS.add(taz, location, rs.getDouble(3));
                }
                rs.close();
            } else if (TPS_DB_IOManager.BEHAVIOUR.equals(Behaviour.FAT)) {
                for (TPS_TrafficAnalysisZone taz : region) {
                    //					if (TPS_Geometrics.isWithin(taz.getCoordinate(), comingFrom.getCoordinate(),
                    //					arrivalDistance)
                    //							&& TPS_Geometrics.isWithin(taz.getCoordinate(), goingTo.getCoordinate
                    //							(), departureDistance)) {
                    //						TPS_Location loc = taz.getData().generateLocationRepr(activityCode);
                    //						if (loc != null) {
                    //							regionRS.add(taz, loc, taz.getData().getWeight(
                    //									TPS_AbstractConstant.getConnectedConstants(activityCode,
                    //									TPS_LocationCode.class)));
                    //						}
                    //					}
                    if (taz.getData() != null) { // empty taz data
                        //buffer around coming from and going to
                        if (TPS_Geometrics.isBetweenLine(goingTo.getCoordinate(), departureDistance,
                                comingFrom.getCoordinate(), arrivalDistance, taz.getCoordinate(),
                                this.PM.getParameters().getDoubleValue(ParamValue.MIN_DIST))) {
                            double weight = taz.getActivityWeightSum(activityCode);
                            if (weight > 0) { // do not process "zero weight"-locations
                                TPS_Location loc = taz.getData().generateLocationRepr(activityCode);
                                if (loc != null) { //is there a requested location in this taz
                                    regionRS.add(taz, loc, weight);
                                }
                            }
                        }
                    }
                }
            } else {
                throw new RuntimeException("Unknown case");
            }
        }
        return regionRS;
    }

    /**
     * Method to initializes some variables for starting the new simulation
     */
    public void initStart() {
        this.numberOfFetchedHouseholds = -1;
        this.houseHoldMap.clear(); //just in case...
    }

    /**
     * Here we load the households, persons and car data from the DB for a given list of household ids!
     * First we look at our household-map if the house hold is already loaded (prefetched).
     * If we do not find it, we load it with all attributes, cars and persons.
     * Finally we store the households in a list and return this list.
     *
     * @param region in which the households are
     * @param hIDs   list of household ids
     * @return list of loaded households
     */
    private List<TPS_Household> loadHouseholds(TPS_Region region, List<Integer> hIDs) {
        String hhtable = this.PM.getParameters().getString(ParamString.DB_TABLE_HOUSEHOLD);
        String persTable = this.PM.getParameters().getString(ParamString.DB_TABLE_PERSON);
        String carsTable = this.PM.getParameters().getString(ParamString.DB_TABLE_CARS);
        String query = "";
        int id, sumPersons = 0;
        List<TPS_Household> fetchedHouseholds = new ArrayList<>();

        ResultSet hRs;
        ResultSet pRs;
        ResultSet cRs;
        StringBuffer sb;

        //clear household map if necessary
        if (this.PM.getParameters().isFalse(ParamFlag.FLAG_PREFETCH_ALL_HOUSEHOLDS)) {
            this.houseHoldMap.clear();
        }
        try {
            if (houseHoldMap.isEmpty()) {
                if (this.PM.getParameters().isTrue(ParamFlag.FLAG_PREFETCH_ALL_HOUSEHOLDS)) {
                    query = "SELECT hh_id, hh_cars, hh_car_ids, hh_income, hh_taz_id, hh_type, ST_X(hh_coordinate) as x, ST_Y(hh_coordinate) as y FROM " +
                            hhtable + " WHERE hh_key='" + this.PM.getParameters().getString(
                            ParamString.DB_HOUSEHOLD_AND_PERSON_KEY) + "'";
                } else {
                    Collections.sort(hIDs); //VERY IMPORTANT! Otherwise the person fetch will be HUGE!
                    sb = new StringBuffer("ARRAY[");
                    for (Integer hID : hIDs) {
                        sb.append(hID + ",");
                    }
                    sb.setCharAt(sb.length() - 1, ']');
                    query = "SELECT hh_id, hh_cars, hh_car_ids, hh_income, hh_taz_id, hh_type, ST_X(hh_coordinate) as x, ST_Y(hh_coordinate) as y FROM " +
                            hhtable + " WHERE hh_id = ANY(" + sb.toString() + ") AND hh_key='" +
                            this.PM.getParameters().getString(ParamString.DB_HOUSEHOLD_AND_PERSON_KEY) + "'";
                }
                hRs = PM.executeQuery(query);
                while (hRs.next()) {
                    // read cars
                    id = hRs.getInt("hh_id");
                    int carNum = hRs.getInt("hh_cars");
                    TPS_Car[] cars = null;
                    if (carNum > 0) {
                        int[] carId = extractIntArray(hRs, "hh_car_ids");
                        if (carNum != carId.length) {
                            TPS_Logger.log(HierarchyLogLevel.THREAD, SeverenceLogLevel.ERROR,
                                    "HH_id: " + id + " expected cars: " + carNum + " found cars: " + carId.length);
                        }
                        // init the cars
                        cars = new TPS_Car[carId.length];
                        for (int i = 0; i < cars.length; ++i) {
                            cars[i] = new TPS_Car(carId[i]);
                            //store default values
                            cars[i].init(TPS_Car.FUEL_TYPE_ARRAY[1], 1, TPS_Car.EMISSION_TYPE_ARRAY[1], 0.0, false,
                                    false, this.PM.getParameters(), i);

                        }
                    }
                    // read other attributes
                    TPS_TrafficAnalysisZone taz = region.getTrafficAnalysisZone(hRs.getInt("hh_taz_id"));
                    int income = hRs.getInt("hh_income"); // TODO: why int?
                    int type = hRs.getInt("hh_type");
                    TPS_Location loc = new TPS_Location(-1 * id, -1, TPS_LocationConstant.HOME, hRs.getDouble("x"),
                            hRs.getDouble("y"), taz, null, this.PM.getParameters());
                    loc.initCapacity(0, false);
                    TPS_Household hh = new TPS_Household(id, income, type, loc, cars);
                    houseHoldMap.put(id, hh);
                }
                hRs.close();
                if (this.PM.getParameters().isTrue(ParamFlag.FLAG_PREFETCH_ALL_HOUSEHOLDS)) {
                    if (TPS_Logger.isLogging(HierarchyLogLevel.THREAD, SeverenceLogLevel.INFO)) {
                        TPS_Logger.log(HierarchyLogLevel.THREAD, SeverenceLogLevel.INFO,
                                "Prefetching persons for all " + houseHoldMap.size() + " households");
                    }
                    int chunks = 10; //avoid big fetches! the modulo operation is quite fast and scales very well
                    for (int i = 0; i < chunks; ++i) {
                        query = "SELECT p_id, p_has_bike, p_sex, p_group, p_age, p_abo, p_budget_pt, p_budget_it, " +
                                "p_working, p_work_id, p_driver_license, p_hh_id, p_education FROM " + persTable +
                                " WHERE p_key='" + this.PM.getParameters().getString(
                                ParamString.DB_HOUSEHOLD_AND_PERSON_KEY) + "'and p_hh_id%" + chunks + "=" + i;
                        pRs = PM.executeQuery(query);
                        while (pRs.next()) {
                            this.addPersonToHousehold(pRs);
                            sumPersons++;
                        }
                        pRs.close();
                    }
                } else { //chunky way
                    if (TPS_Logger.isLogging(HierarchyLogLevel.THREAD, SeverenceLogLevel.INFO)) {
                        TPS_Logger.log(HierarchyLogLevel.THREAD, SeverenceLogLevel.INFO,
                                "Fetching persons for " + houseHoldMap.size() + " households");
                    }

                    List<Integer> copyOfhIDs = new ArrayList<>(hIDs);

                    final int chunkSize = 1000;
                    while (!copyOfhIDs.isEmpty()) {
                        sb = new StringBuffer("ARRAY[");
                        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
                        for (int i = 0; i < chunkSize && !copyOfhIDs.isEmpty(); i++) {
                            id = copyOfhIDs.remove(0); //handy: remove returns the removed element
                            min = Math.min(id, min);
                            max = Math.max(max, id);
                            sb.append(id + ",");

                        }
                        sb.setCharAt(sb.length() - 1, ']');
                        query = "SELECT p_id, p_has_bike, p_sex, p_group, p_age, p_abo, p_budget_pt, p_budget_it, " +
                                "p_working, p_work_id, p_driver_license, p_hh_id, p_education FROM " + persTable +
                                " WHERE p_hh_id = ANY(" + sb.toString() + ") " + " AND p_hh_id>=" + min +
                                " AND p_hh_id <= " + max + " AND p_key='" + this.PM.getParameters().getString(
                                ParamString.DB_HOUSEHOLD_AND_PERSON_KEY) + "'";
                        pRs = PM.executeQuery(query);
                        while (pRs.next()) {
                            this.addPersonToHousehold(pRs);
                            sumPersons++;
                        }
                        pRs.close();
                    }
                }

                if (TPS_Logger.isLogging(HierarchyLogLevel.THREAD, SeverenceLogLevel.INFO)) {
                    TPS_Logger.log(HierarchyLogLevel.THREAD, SeverenceLogLevel.INFO,
                            "Fetched " + sumPersons + " persons");
                    TPS_Logger.log(HierarchyLogLevel.THREAD, SeverenceLogLevel.INFO, "Fetching car types");
                }

                if (this.PM.getParameters().isTrue(ParamFlag.FLAG_PREFETCH_ALL_HOUSEHOLDS)) {
                    if (this.carMap.isEmpty()) {
                        //load all cars
                        query = "SELECT car_id,kba_no, fix_costs, engine_type, is_company_car, emission_type, " +
                                "restriction,automation_level FROM " + carsTable + " WHERE car_key='" +
                                this.PM.getParameters().getString(ParamString.DB_CAR_FLEET_KEY) + "'";
                    } else { //nothing to do!
                        query = "";
                    }

                } else {
                    this.carMap.clear();
                    //scrap needed car ids
                    sb = new StringBuffer("ARRAY[");
                    List<Integer> carIDs = new ArrayList<>();
                    for (TPS_Household ihh : this.houseHoldMap.values()) {
                        if (ihh.getMembers(TPS_Household.Sorting.NONE).size() > 0) {
                            //get the cars
                            for (int i = 0; i < ihh.getCarNumber(); ++i) {
                                if (!carIDs.contains(ihh.getCar(i).getId())) {
                                    sb.append(ihh.getCar(i).getId() + ",");
                                    carIDs.add(ihh.getCar(i).getId());
                                }
                            }
                        }
                    }
                    sb.setCharAt(sb.length() - 1, ']');
                    query = "SELECT car_id,kba_no, fix_costs, engine_type, is_company_car, emission_type, " +
                            "restriction,automation_level FROM " + carsTable + " WHERE car_id = ANY(" + sb.toString() +
                            ") AND car_key='" + this.PM.getParameters().getString(ParamString.DB_CAR_FLEET_KEY) + "'";
                }

                if (query.length() > 0) {// we have to fill the car-map
                    cRs = PM.executeQuery(query);
                    while (cRs.next()) {
                        TPS_Car tmp = new TPS_Car(cRs.getInt("car_id"));
                        int engineType = cRs.getInt("engine_type");
                        int emissionType = cRs.getInt("emission_type");
                        if (engineType >= 0 && engineType < TPS_Car.FUEL_TYPE_ARRAY.length && emissionType >= 0 &&
                                emissionType < TPS_Car.EMISSION_TYPE_ARRAY.length) {
                            tmp.init(TPS_Car.FUEL_TYPE_ARRAY[engineType], cRs.getInt("kba_no"),
                                    TPS_Car.EMISSION_TYPE_ARRAY[emissionType], cRs.getDouble("fix_costs"),
                                    cRs.getBoolean("is_company_car"), cRs.getBoolean("restriction"),
                                    this.PM.getParameters(), -1);
                        }
                        int automationLevel = cRs.getInt("automation_level");
                        if (this.PM.getParameters().getDoubleValue(ParamValue.GLOBAL_AUTOMATION_PROBABILITY) >
                                Math.random()) {
                            automationLevel = this.PM.getParameters().getIntValue(ParamValue.GLOBAL_AUTOMATION_LEVEL);
                        }
                        tmp.setAutomation(automationLevel);
                        carMap.put(tmp.getId(), tmp);
                    }
                    cRs.close();
                }

                if (TPS_Logger.isLogging(HierarchyLogLevel.THREAD, SeverenceLogLevel.INFO)) {
                    TPS_Logger.log(HierarchyLogLevel.THREAD, SeverenceLogLevel.INFO, "Assigning cars to households");
                }

                for (TPS_Household ihh : houseHoldMap.values()) {
                    if (ihh.getMembers(TPS_Household.Sorting.NONE).size() > 0) {
                        //get the car values
                        for (int i = 0; i < ihh.getCarNumber(); ++i) {
                            TPS_Car car = ihh.getCar(i);
                            if (carMap.containsKey(car.getId())) {
                                car.cloneCar(carMap.get(car.getId()));
                            } else {
                                if (TPS_Logger.isLogging(HierarchyLogLevel.THREAD, SeverenceLogLevel.WARN)) {
                                    TPS_Logger.log(HierarchyLogLevel.THREAD, SeverenceLogLevel.WARN,
                                            "Unknown car id " + car.getId() + " in household " + ihh.getId());
                                }
                            }
                        }
                    } else {
                        this.returnHousehold(ihh);
                    }
                }
            }
        } catch (SQLException e) {
            TPS_Logger.log(SeverenceLogLevel.ERROR, "Error in sqlStatement " + query, e);
            TPS_Logger.log(SeverenceLogLevel.ERROR, "Next exception: ", e.getNextException());
        } catch (Exception e) {
            TPS_Logger.log(SeverenceLogLevel.ERROR, "Unknown Error in loadHouseholds!", e);

        }
        for (Integer hid : hIDs) {
            TPS_Household ihh = this.houseHoldMap.get(hid);
            if (ihh != null) {
                fetchedHouseholds.add(ihh);
            } else {
                TPS_Logger.log(SeverenceLogLevel.ERROR, "Household not loaded: " + hid);
            }
        }
        return fetchedHouseholds;
    }

    /**
     * Reads and assigns activities to locations and vice versa
     * <p>
     * Note: First the activity and locations must be read,
     * i.e. readActivityConstantCodes and readLocationConstantCodes must be called beforehand
     */
    private void readActivity2LocationCodes() {
        String query = "SELECT * FROM " + this.PM.getParameters().getString(
                ParamString.DB_TABLE_CONSTANT_ACTIVITY_2_LOCATION) +  " where key='" +
                this.PM.getParameters().getString(ParamString.DB_ACTIVITY_2_LOCATION_KEY)+ "'";
        TPS_ActivityConstant actCode;
        int ac;
        TPS_LocationConstant locCode;
        int loc;
        try (ResultSet rs = PM.executeQuery(query)) {
            while (rs.next()) {
                ac = rs.getInt("act_code");
                loc = rs.getInt("loc_code");
                actCode = TPS_ActivityConstant.getActivityCodeByTypeAndCode(TPS_ActivityCodeType.ZBE, ac);
                locCode = TPS_LocationConstant.getLocationCodeByTypeAndCode(TPS_LocationCodeType.TAPAS, loc);
                //double percentage = rs.getDouble("loc_capa_percentage");
                TPS_TrafficAnalysisZone.ACTIVITY2LOCATIONS_MAP.put(actCode, locCode);
                TPS_TrafficAnalysisZone.LOCATION2ACTIVITIES_MAP.put(locCode, actCode);
                // replaced by the two lines above:           TPS_AbstractConstant.connectConstants(actCode, locCode);
            }
        } catch (SQLException e) {
            TPS_Logger.log(SeverenceLogLevel.ERROR, "SQL error in readConstant! Query: " + query + " constant:" +
                    this.PM.getParameters().getString(ParamString.DB_TABLE_CONSTANT_ACTIVITY_2_LOCATION) + " or " +
                    this.PM.getParameters().getString(ParamString.DB_ACTIVITY_2_LOCATION_KEY), e);
            throw new RuntimeException("Error loading constant " +
                    this.PM.getParameters().getString(ParamString.DB_TABLE_CONSTANT_ACTIVITY_2_LOCATION) + " or " +
                    this.PM.getParameters().getString(ParamString.DB_ACTIVITY_2_LOCATION_KEY));
        }
    }

    /**
     * Reads all activity constants codes from the database and stores them in to a global static map
     * An ActivityConstant has the form (id, 3-tuples of (name, code, type), istrip, isfix, attr)
     * Example: (5, ("school", "2", "TAPAS"), ("SCHOOL", "410", "MCT"), True, False, "SCHOOL")
     */
    private void readActivityConstantCodes() {
        String query = "SELECT * FROM " + this.PM.getParameters().getString(ParamString.DB_TABLE_CONSTANT_ACTIVITY);
        try (ResultSet rs = PM.executeQuery(query)) {
            TPS_ActivityConstant tac;
            boolean istrip;
            boolean isfix;
            String attr;
            while (rs.next()) {
                String[] attributes = new String[rs.getMetaData().getColumnCount() - 6];
                for (int i = 0; i < attributes.length; i++) {
                    attributes[i] = rs.getString(i + 4);
                }
                istrip = rs.getBoolean("istrip");
                isfix = rs.getBoolean("isfix");
                attr = rs.getString("attribute");
                tac = new TPS_ActivityConstant(rs.getInt("id"), attributes, istrip, isfix, attr);
                // add activity code object to a global static map which is a collection of all activity constants
                tac.addActivityConstantToMap();
            }
        } catch (SQLException e) {
            TPS_Logger.log(SeverenceLogLevel.ERROR,
                    "SQL error in readActivityConstantCodes! Query: " + query + " constant:" +
                            this.PM.getParameters().getString(ParamString.DB_TABLE_CONSTANT_ACTIVITY), e);
            throw new RuntimeException("Error loading constant " +
                    this.PM.getParameters().getString(ParamString.DB_TABLE_CONSTANT_ACTIVITY));
        }
    }


    /**
     * Reads all age classes codes from the database and stores them in to a global static map
     * An AgeClass has the form (id, 3-tuples of (name, code, type), min, max)
     * Example: (5, (under 35, 7, STBA), (under 45, 4, PersGroup), 30, 34)
     */
    private void readAgeClassCodes() {
        String query = "SELECT * FROM " + this.PM.getParameters().getString(ParamString.DB_TABLE_CONSTANT_AGE);
        try (ResultSet rs = PM.executeQuery(query)) {
            TPS_AgeClass tac;
            int min;
            int max;
            while (rs.next()) {
                min = rs.getInt("min");
                max = rs.getInt("max");
                String[] attributes = new String[rs.getMetaData().getColumnCount() - 5];
                for (int i = 0; i < attributes.length; i++) {
                    attributes[i] = rs.getString(i + 4);
                }
                tac = new TPS_AgeClass(rs.getInt("id"), attributes, min, max);
                // add age class object to a global static map which is a collection of all
                tac.addAgeClassToMap();
            }
        } catch (SQLException e) {
            TPS_Logger.log(SeverenceLogLevel.ERROR, "SQL error in readAgeClassCodes! Query: " + query + " constant:" +
                    this.PM.getParameters().getString(ParamString.DB_TABLE_CONSTANT_AGE), e);
            throw new RuntimeException(
                    "Error loading constant " + this.PM.getParameters().getString(ParamString.DB_TABLE_CONSTANT_AGE));
        }
    }


    /**
     * Reads all car codes from the database and stores them through enums
     * A CarCode has the form (name_cars, code_cars)
     * Example: (YES, 1)
     */
    private void readCarCodes() {
        String query = "SELECT * FROM " + this.PM.getParameters().getString(ParamString.DB_TABLE_CONSTANT_CARS);
        try (ResultSet rs = PM.executeQuery(query)) {
            while (rs.next()) {
                try {
                    TPS_CarCode s = TPS_CarCode.valueOf(rs.getString("name_cars"));
                    s.code = rs.getInt("code_cars");
                } catch (IllegalArgumentException e) {
                    TPS_Logger.log(SeverenceLogLevel.WARN,
                            "Read invalid car information from DB:" + rs.getString("name_cars"));
                }
            }
        } catch (SQLException e) {
            TPS_Logger.log(SeverenceLogLevel.ERROR, "SQL error in readCarCodes! Query: " + query + " constant" + ":" +
                    this.PM.getParameters().getString(ParamString.DB_TABLE_CONSTANT_CARS), e);
            throw new RuntimeException(
                    "Error loading constant " + this.PM.getParameters().getString(ParamString.DB_TABLE_CONSTANT_CARS));
        }
    }

    /**
     * Method to read the constant values from the database. It provides the mapping of the enums to a value, which
     * is used in the survey or similar.
     */
    void readConstants() {
        TPS_ModeDistribution.clearDistributions(); //TODO necessary?

        //read all constants
        this.readActivityConstantCodes();
        this.readAgeClassCodes();
        this.readCarCodes();
        this.readDistanceCodes();
        this.readDrivingLicenseCodes();
        this.readIncomeCodes();
        this.readLocationConstantCodes();
        this.readModes();
        this.readPersonGroupCodes();
        this.readSettlementSystemCodes();
        this.readSexCodes();

        //must be after reading of activities and locations because they are used in it
        this.readActivity2LocationCodes();
    }

    /**
     * Reads all distance codes from the database and stores them in to a global static map
     * A Distance has the form (id, 3-tuples of (name, code, type), max)
     * Example: (5, (under 5k, 1, VOT),	(under 2k, 2000, MCT), 2000)
     */
    private void readDistanceCodes() {
        String query = "SELECT * FROM " + this.PM.getParameters().getString(ParamString.DB_TABLE_CONSTANT_DISTANCE);
        try (ResultSet rs = PM.executeQuery(query)) {
            while (rs.next()) {
                String[] attributes = new String[rs.getMetaData().getColumnCount() - 4];
                for (int i = 0; i < attributes.length; i++) {
                    attributes[i] = rs.getString(i + 4);
                }
                TPS_Distance td = new TPS_Distance(rs.getInt("id"), attributes, rs.getInt("max"));
                // add settlement system object to a global static map which is a collection of all settlement systems
                td.addDistanceToMap();
            }
        } catch (SQLException e) {
            TPS_Logger.log(SeverenceLogLevel.ERROR, "SQL error in readDistanceCodes! Query: " + query + " constant:" +
                    this.PM.getParameters().getString(ParamString.DB_TABLE_CONSTANT_DISTANCE), e);
            throw new RuntimeException("Error loading constant " +
                    this.PM.getParameters().getString(ParamString.DB_TABLE_CONSTANT_DISTANCE));
        }
    }

    /**
     * Reads all driving license codes from the database and stores them through enums
     * A DrivingLicenseCodes has the form (name_dli, code_dli)
     * Example: (no, 2)
     */
    private void readDrivingLicenseCodes() {
        String query = "SELECT name, code_dli FROM " + this.PM.getParameters().getString(
                ParamString.DB_TABLE_CONSTANT_DRIVING_LICENSE_INFORMATION);
        try (ResultSet rs = PM.executeQuery(query)) {
            while (rs.next()) {
                try {
                    TPS_DrivingLicenseInformation s = TPS_DrivingLicenseInformation.valueOf(rs.getString("name"));
                    s.setCode(rs.getInt("code_dli"));
                } catch (IllegalArgumentException e) {
                    TPS_Logger.log(SeverenceLogLevel.WARN, "Invalid driving license code: " + rs.getString("name"));
                }
            }
        } catch (SQLException e) {
            TPS_Logger.log(SeverenceLogLevel.ERROR,
                    "SQL error in readDrivingLicenseCodes! Query: " + query + " constant:" + this.PM.getParameters()
                                                                                                    .getString(
                                                                                                            ParamString.DB_TABLE_CONSTANT_DRIVING_LICENSE_INFORMATION),
                    e);
            throw new RuntimeException("Error loading constant " +
                    this.PM.getParameters().getString(ParamString.DB_TABLE_CONSTANT_DRIVING_LICENSE_INFORMATION));
        }
    }

    /**
     * This method reads the mode choice tree used for the pivot-point model.
     *
     * @return The tree read from the db.
     * @throws SQLException
     */
    public TPS_ExpertKnowledgeTree readExpertKnowledgeTree() throws SQLException {
        TPS_ExpertKnowledgeNode root = null;
        if (this.PM.getParameters().isDefined(ParamString.DB_TABLE_EKT) && this.PM.getParameters().getString(
                ParamString.DB_TABLE_EKT) != null && !this.PM.getParameters().getString(ParamString.DB_TABLE_EKT)
                                                             .equals("") && this.PM.getParameters().isDefined(
                ParamString.DB_NAME_EKT) && this.PM.getParameters().getString(ParamString.DB_NAME_EKT) != null &&
                !this.PM.getParameters().getString(ParamString.DB_NAME_EKT).equals("")) {

            String query = "SELECT node_id, parent_node_id, attribute_values, split_variable, summand, factor FROM " +
                    this.PM.getParameters().getString(ParamString.DB_TABLE_EKT) + " WHERE name='" +
                    this.PM.getParameters().getString(ParamString.DB_NAME_EKT) + "' ORDER BY node_id";
            ResultSet rs = PM.executeQuery(query);
            while (rs.next()) {
                int id = rs.getInt("node_id");
                // skip level
                // skip size
                int idParent = rs.getInt("parent_node_id");

                List<Integer> c = new LinkedList<>();
                for (Integer i : extractIntArray(rs, "attribute_values")) {
                    c.add(i);
                }

                String splitVar = rs.getString("split_variable");
                TPS_Attribute sv = null;
                if (splitVar != null && splitVar.length() > 1) {
                    sv = TPS_Attribute.valueOf(splitVar);
                }

                double[] values = extractDoubleArray(rs, "summand");
                TPS_DiscreteDistribution<TPS_Mode> summand = new TPS_DiscreteDistribution<>(TPS_Mode.getConstants());
                for (int i = 0; i < values.length; i++) {
                    summand.setValueByPosition(i, values[i]);
                }

                values = extractDoubleArray(rs, "factor");
                TPS_DiscreteDistribution<TPS_Mode> factor = new TPS_DiscreteDistribution<>(TPS_Mode.getConstants());
                for (int i = 0; i < values.length; i++) {
                    factor.setValueByPosition(i, values[i]);
                }


                // We assume that the first row contains the root node data.
                if (root == null) {
                    root = new TPS_ExpertKnowledgeNode(id, sv, c, summand, factor, null);
                } else {
                    TPS_ExpertKnowledgeNode parent = (TPS_ExpertKnowledgeNode) root.getChild(idParent);
                    TPS_ExpertKnowledgeNode child = new TPS_ExpertKnowledgeNode(id, sv, c, summand, factor, parent);

                    // if (parent.getId() != idParent) {
                    // log.error("\t\t\t\t '--> ModeChoiceTree.readTable: Parent not found -> Id: " + idParent);
                    // throw new IOException("ModeChoiceTree.readTable: Parent not found -> Id: " + idParent);
                    // }

                    parent.addChild(child);
                }
            }
            rs.close();
        }

        if (root == null) {
            //no expert knowledge: create a root-node with dummy values
            List<Integer> c = new LinkedList<>();
            c.add(0);

            TPS_DiscreteDistribution<TPS_Mode> summand = new TPS_DiscreteDistribution<>(TPS_Mode.getConstants());
            for (int i = 0; i < summand.size(); i++) {
                summand.setValueByPosition(i, 0);
            }

            TPS_DiscreteDistribution<TPS_Mode> factor = new TPS_DiscreteDistribution<>(TPS_Mode.getConstants());
            for (int i = 0; i < factor.size(); i++) {
                factor.setValueByPosition(i, 1);
            }
            root = new TPS_ExpertKnowledgeNode(0, null, c, summand, factor, null);
        }
        return new TPS_ExpertKnowledgeTree(root);
    }

    /**
     * Reads all income codes from the database and stores them in to a global static map
     * An Income has the form (id, name, code, max)
     * Example: (5, under 2600, 4, 2600)
     */
    private void readIncomeCodes() {
        String query = "SELECT * FROM " + this.PM.getParameters().getString(ParamString.DB_TABLE_CONSTANT_INCOME);
        try (ResultSet rs = PM.executeQuery(query)) {
            while (rs.next()) {
                TPS_Income ti = new TPS_Income(rs.getInt("id"), rs.getString("name_income"), rs.getInt("code_income"),
                        rs.getInt("max"));
                ti.addToIncomeMap();
            }
        } catch (SQLException e) {
            TPS_Logger.log(SeverenceLogLevel.ERROR, "SQL error in readIncomeCodes! Query: " + query + " constant:" +
                    this.PM.getParameters().getString(ParamString.DB_TABLE_CONSTANT_INCOME), e);
            throw new RuntimeException("Error loading constant " +
                    this.PM.getParameters().getString(ParamString.DB_TABLE_CONSTANT_INCOME));
        }
    }

    /**
     * Reads all location constant codes from the database and stores them in to a global static map
     * A LocationConstant has the form (id, 3-tuples of (name, code, type))
     * Example: (5, (club, 7, GENERAL), (club, 7, TAPAS))
     */
    private void readLocationConstantCodes() {
        String query = "SELECT * FROM " + this.PM.getParameters().getString(ParamString.DB_TABLE_CONSTANT_LOCATION);
        try (ResultSet rs = PM.executeQuery(query)) {
            TPS_LocationConstant tlc;
            while (rs.next()) {
                String[] attributes = new String[rs.getMetaData().getColumnCount() - 3];
                for (int i = 0; i < attributes.length; i++) {
                    attributes[i] = rs.getString(i + 4);
                }
                tlc = new TPS_LocationConstant(rs.getInt("id"), attributes);
                //add this location code object to a static map which is a global collection of all location constants
                tlc.addLocationCodeToMap();
            }
        } catch (SQLException e) {
            TPS_Logger.log(SeverenceLogLevel.ERROR,
                    "SQL error in readLocationConstantCodes! Query: " + query + " constant:" +
                            this.PM.getParameters().getString(ParamString.DB_TABLE_CONSTANT_LOCATION), e);
            throw new RuntimeException("Error loading constant " +
                    this.PM.getParameters().getString(ParamString.DB_TABLE_CONSTANT_LOCATION));
        }
    }

    /**
     * Method to read all matrices for the given region (travel times, distances etc.)
     *
     * @param region The region to look for.
     * @throws SQLException
     */
    public void readMatrices(TPS_Region region) throws SQLException {
        final int sIndex = region.getSmallestId(); // this is the offset between the TAZ_ids and the matrix index

        this.readMatrix(ParamString.DB_NAME_MATRIX_DISTANCES_STREET, ParamMatrix.DISTANCES_STREET, null, sIndex);

        //walk net distances
        if (this.PM.getParameters().isDefined(ParamString.DB_NAME_MATRIX_DISTANCES_WALK)) {
            this.readMatrix(ParamString.DB_NAME_MATRIX_DISTANCES_WALK, ParamMatrix.DISTANCES_WALK, null, sIndex);
        } else {
            TPS_Logger.log(SeverenceLogLevel.INFO, "Setting walk distances equal to street distances.");
            this.PM.getParameters().setMatrix(ParamMatrix.DISTANCES_WALK,
                    this.PM.getParameters().getMatrix(ParamMatrix.DISTANCES_STREET)); //reference the MIV-matrix
        }

        //bike net distances
        if (this.PM.getParameters().isDefined(ParamString.DB_NAME_MATRIX_DISTANCES_BIKE)) {
            this.readMatrix(ParamString.DB_NAME_MATRIX_DISTANCES_BIKE, ParamMatrix.DISTANCES_BIKE, null, sIndex);
        } else {
            TPS_Logger.log(SeverenceLogLevel.INFO, "Setting bike distances equal to street distances.");
            this.PM.getParameters().setMatrix(ParamMatrix.DISTANCES_BIKE,
                    this.PM.getParameters().getMatrix(ParamMatrix.DISTANCES_STREET)); //reference the MIV-matrix
        }

        //pt net distances
        if (this.PM.getParameters().isDefined(ParamString.DB_NAME_MATRIX_DISTANCES_PT)) {
            this.readMatrix(ParamString.DB_NAME_MATRIX_DISTANCES_PT, ParamMatrix.DISTANCES_PT, null, sIndex);
        } else {
            TPS_Logger.log(SeverenceLogLevel.INFO, "Setting public transport distances equal to street distances.");
            this.PM.getParameters().setMatrix(ParamMatrix.DISTANCES_PT,
                    this.PM.getParameters().getMatrix(ParamMatrix.DISTANCES_STREET)); //reference the MIV-matrix
        }

        //beeline dist
        TPS_Logger.log(SeverenceLogLevel.INFO, "Calculate beeline distances distances.");
        Matrix bl = new Matrix(region.getTrafficAnalysisZones().size(), region.getTrafficAnalysisZones().size(),
                sIndex);
        for (TPS_TrafficAnalysisZone tazfrom : region.getTrafficAnalysisZones()) {
            for (TPS_TrafficAnalysisZone tazto : region.getTrafficAnalysisZones()) {
                double dist = TPS_Geometrics.getDistance(tazfrom.getTrafficAnalysisZone().getCenter(),
                        tazto.getTrafficAnalysisZone().getCenter(),
                        this.PM.getParameters().getDoubleValue(ParamValue.MIN_DIST));
                bl.setValue(tazfrom.getTAZId(), tazto.getTAZId(), dist);
            }
        }
        this.PM.getParameters().setMatrix(ParamMatrix.DISTANCES_BL, bl);

        //walk
        if (this.PM.getParameters().isDefined(ParamString.DB_NAME_MATRIX_TT_WALK)) {
            this.readMatrix(ParamString.DB_NAME_MATRIX_TT_WALK, ParamMatrixMap.TRAVEL_TIME_WALK,
                    SimulationType.SCENARIO, sIndex);
            if (this.PM.getParameters().isDefined(ParamString.DB_NAME_MATRIX_ACCESS_WALK)) {
                this.readMatrix(ParamString.DB_NAME_MATRIX_ACCESS_WALK, ParamMatrixMap.ARRIVAL_WALK,
                        SimulationType.SCENARIO, sIndex);
            }
            if (this.PM.getParameters().isDefined(ParamString.DB_NAME_MATRIX_EGRESS_WALK)) {
                this.readMatrix(ParamString.DB_NAME_MATRIX_EGRESS_WALK, ParamMatrixMap.EGRESS_WALK,
                        SimulationType.SCENARIO, sIndex);
            }
        }

        //bike
        if (this.PM.getParameters().isDefined(ParamString.DB_NAME_MATRIX_TT_BIKE)) {
            this.readMatrix(ParamString.DB_NAME_MATRIX_TT_BIKE, ParamMatrixMap.TRAVEL_TIME_BIKE,
                    SimulationType.SCENARIO, sIndex);
            if (this.PM.getParameters().isDefined(ParamString.DB_NAME_MATRIX_ACCESS_BIKE)) {
                this.readMatrix(ParamString.DB_NAME_MATRIX_ACCESS_BIKE, ParamMatrixMap.ARRIVAL_BIKE,
                        SimulationType.SCENARIO, sIndex);
            }
            if (this.PM.getParameters().isDefined(ParamString.DB_NAME_MATRIX_EGRESS_BIKE)) {
                this.readMatrix(ParamString.DB_NAME_MATRIX_EGRESS_BIKE, ParamMatrixMap.EGRESS_BIKE,
                        SimulationType.SCENARIO, sIndex);
            }
        }

        //MIT, MIT passenger, Taxi
        if (this.PM.getParameters().isDefined(ParamString.DB_NAME_MATRIX_TT_MIT)) {
            this.readMatrix(ParamString.DB_NAME_MATRIX_TT_MIT, ParamMatrixMap.TRAVEL_TIME_MIT, SimulationType.SCENARIO,
                    sIndex);
            if (this.PM.getParameters().isDefined(ParamString.DB_NAME_MATRIX_ACCESS_MIT)) {
                this.readMatrix(ParamString.DB_NAME_MATRIX_ACCESS_MIT, ParamMatrixMap.ARRIVAL_MIT,
                        SimulationType.SCENARIO, sIndex);
            }
            if (this.PM.getParameters().isDefined(ParamString.DB_NAME_MATRIX_EGRESS_MIT)) {
                this.readMatrix(ParamString.DB_NAME_MATRIX_EGRESS_MIT, ParamMatrixMap.EGRESS_MIT,
                        SimulationType.SCENARIO, sIndex);
            }
        }

        //pt, train
        if (this.PM.getParameters().isDefined(ParamString.DB_NAME_MATRIX_TT_PT)) {
            this.readMatrix(ParamString.DB_NAME_MATRIX_TT_PT, ParamMatrixMap.TRAVEL_TIME_PT, SimulationType.SCENARIO,
                    sIndex);
            if (this.PM.getParameters().isDefined(ParamString.DB_NAME_MATRIX_ACCESS_PT)) {
                this.readMatrix(ParamString.DB_NAME_MATRIX_ACCESS_PT, ParamMatrixMap.ARRIVAL_PT,
                        SimulationType.SCENARIO, sIndex);
            }
            if (this.PM.getParameters().isDefined(ParamString.DB_NAME_MATRIX_EGRESS_PT)) {
                this.readMatrix(ParamString.DB_NAME_MATRIX_EGRESS_PT, ParamMatrixMap.EGRESS_PT, SimulationType.SCENARIO,
                        sIndex);
            }
            if (this.PM.getParameters().isDefined(ParamString.DB_NAME_MATRIX_INTERCHANGE_PT)) {
                this.readMatrix(ParamString.DB_NAME_MATRIX_INTERCHANGE_PT, ParamMatrixMap.INTERCHANGES_PT,
                        SimulationType.SCENARIO, sIndex);
            }
        }
        if (this.PM.getParameters().isDefined(ParamString.DB_NAME_PTBIKE_ACCESS_TAZ)) {
            this.readMatrix(ParamString.DB_NAME_PTBIKE_ACCESS_TAZ, ParamMatrixMap.PTBIKE_ACCESS_TAZ,
                    SimulationType.SCENARIO, sIndex);
        }
        if (this.PM.getParameters().isDefined(ParamString.DB_NAME_PTBIKE_EGRESS_TAZ)) {
            this.readMatrix(ParamString.DB_NAME_PTBIKE_EGRESS_TAZ, ParamMatrixMap.PTBIKE_EGRESS_TAZ,
                    SimulationType.SCENARIO, sIndex);
        }
        if (this.PM.getParameters().isDefined(ParamString.DB_NAME_PTCAR_ACCESS_TAZ)) {
            this.readMatrix(ParamString.DB_NAME_PTCAR_ACCESS_TAZ, ParamMatrixMap.PTCAR_ACCESS_TAZ,
                    SimulationType.SCENARIO, sIndex);
        }
        if (this.PM.getParameters().isDefined(ParamString.DB_NAME_PTBIKE_INTERCHANGES)) {
            this.readMatrix(ParamString.DB_NAME_PTBIKE_INTERCHANGES, ParamMatrixMap.PTBIKE_INTERCHANGES,
                    SimulationType.SCENARIO, sIndex);
        }
        if (this.PM.getParameters().isDefined(ParamString.DB_NAME_PTCAR_INTERCHANGES)) {
            this.readMatrix(ParamString.DB_NAME_PTCAR_INTERCHANGES, ParamMatrixMap.PTCAR_INTERCHANGES,
                    SimulationType.SCENARIO, sIndex);
        }

        // providing base case travel times in case they are needed
        if (this.PM.getParameters().isTrue(ParamFlag.FLAG_RUN_SZENARIO)) {
            // travel times for the base case
            //walk
            if (this.PM.getParameters().isDefined(ParamString.DB_NAME_MATRIX_TT_WALK_BASE)) {
                this.readMatrix(ParamString.DB_NAME_MATRIX_TT_WALK_BASE, ParamMatrixMap.TRAVEL_TIME_WALK,
                        SimulationType.BASE, sIndex);
                if (this.PM.getParameters().isDefined(ParamString.DB_NAME_MATRIX_ACCESS_WALK_BASE)) {
                    this.readMatrix(ParamString.DB_NAME_MATRIX_ACCESS_WALK_BASE, ParamMatrixMap.ARRIVAL_WALK,
                            SimulationType.BASE, sIndex);
                }
                if (this.PM.getParameters().isDefined(ParamString.DB_NAME_MATRIX_EGRESS_WALK_BASE)) {
                    this.readMatrix(ParamString.DB_NAME_MATRIX_EGRESS_WALK_BASE, ParamMatrixMap.EGRESS_WALK,
                            SimulationType.BASE, sIndex);
                }
            }

            //bike
            if (this.PM.getParameters().isDefined(ParamString.DB_NAME_MATRIX_TT_BIKE_BASE)) {
                this.readMatrix(ParamString.DB_NAME_MATRIX_TT_BIKE_BASE, ParamMatrixMap.TRAVEL_TIME_BIKE,
                        SimulationType.BASE, sIndex);
                if (this.PM.getParameters().isDefined(ParamString.DB_NAME_MATRIX_ACCESS_BIKE_BASE)) {
                    this.readMatrix(ParamString.DB_NAME_MATRIX_ACCESS_BIKE_BASE, ParamMatrixMap.ARRIVAL_BIKE,
                            SimulationType.BASE, sIndex);
                }
                if (this.PM.getParameters().isDefined(ParamString.DB_NAME_MATRIX_EGRESS_BIKE_BASE)) {

                    this.readMatrix(ParamString.DB_NAME_MATRIX_EGRESS_BIKE_BASE, ParamMatrixMap.EGRESS_BIKE,
                            SimulationType.BASE, sIndex);
                }
            }

            //MIT, MIT passenger, Taxi,
            if (this.PM.getParameters().isDefined(ParamString.DB_NAME_MATRIX_TT_MIT_BASE)) {
                // car
                this.readMatrix(ParamString.DB_NAME_MATRIX_TT_MIT_BASE, ParamMatrixMap.TRAVEL_TIME_MIT,
                        SimulationType.BASE, sIndex);
                if (this.PM.getParameters().isDefined(ParamString.DB_NAME_MATRIX_ACCESS_MIT_BASE)) {
                    this.readMatrix(ParamString.DB_NAME_MATRIX_ACCESS_MIT_BASE, ParamMatrixMap.ARRIVAL_MIT,
                            SimulationType.BASE, sIndex);
                }
                if (this.PM.getParameters().isDefined(ParamString.DB_NAME_MATRIX_EGRESS_MIT_BASE)) {
                    this.readMatrix(ParamString.DB_NAME_MATRIX_EGRESS_MIT_BASE, ParamMatrixMap.EGRESS_MIT,
                            SimulationType.BASE, sIndex);
                }
            }

            if (this.PM.getParameters().isDefined(ParamString.DB_NAME_MATRIX_TT_PT_BASE)) {
                // public transport
                this.readMatrix(ParamString.DB_NAME_MATRIX_TT_PT_BASE, ParamMatrixMap.TRAVEL_TIME_PT,
                        SimulationType.BASE, sIndex);
                if (this.PM.getParameters().isDefined(ParamString.DB_NAME_MATRIX_ACCESS_PT_BASE)) {
                    this.readMatrix(ParamString.DB_NAME_MATRIX_ACCESS_PT_BASE, ParamMatrixMap.ARRIVAL_PT,
                            SimulationType.BASE, sIndex);
                }
                if (this.PM.getParameters().isDefined(ParamString.DB_NAME_MATRIX_EGRESS_PT_BASE)) {
                    this.readMatrix(ParamString.DB_NAME_MATRIX_EGRESS_PT_BASE, ParamMatrixMap.EGRESS_PT,
                            SimulationType.BASE, sIndex);
                }
                if (this.PM.getParameters().isDefined(ParamString.DB_NAME_MATRIX_INTERCHANGE_PT_BASE)) {
                    this.readMatrix(ParamString.DB_NAME_MATRIX_INTERCHANGE_PT_BASE, ParamMatrixMap.INTERCHANGES_PT,
                            SimulationType.BASE, sIndex);
                }
            }
        }

        if (this.PM.getParameters().isDefined(ParamString.DB_TABLE_BLOCK_NEXT_PT_STOP)) {
            ResultSet rs = PM.functionExecuteQuery("get_avg_dist_next_stop",
                    this.PM.getParameters().getString(ParamString.DB_TABLE_BLOCK_NEXT_PT_STOP),
                    this.PM.getParameters().getString(ParamString.DB_NAME_BLOCK_NEXT_PT_STOP));
            if (rs.next()) {
                this.PM.getParameters().setValue(ParamValue.AVERAGE_DISTANCE_PT_STOP, rs.getDouble(1));
            } else {
                throw new SQLException("Couldn't select average distance pt stop from database");
            }
            rs.close();
        }
    }

    /**
     * Method to read a single specified matrix
     *
     * @param matrixName The name for this matrix as a String
     * @param matrix     the matrix to store in
     * @param simType    the simulation type
     * @param sIndex     the index for reading in the db. Should be zero.
     * @throws SQLException
     */
    private void readMatrix(ParamString matrixName, ParamMatrix matrix, SimulationType simType, int sIndex) throws SQLException {
        this.readMatrix(this.PM.getParameters().getString(matrixName), matrix, simType, sIndex);
    }

    /**
     * Method to read a single specified matrix
     *
     * @param matrixName The name for this matrix
     * @param matrix     the matrix to store in
     * @param simType    the simulation type
     * @param sIndex     the index for reading in the db. Should be zero.
     * @throws SQLException
     */
    private void readMatrix(String matrixName, ParamMatrix matrix, SimulationType simType, int sIndex) throws SQLException {
        String query = "SELECT matrix_values FROM " + this.PM.getParameters().getString(ParamString.DB_TABLE_MATRICES) +
                " WHERE matrix_name='" + matrixName + "'";
        ResultSet rs = PM.executeQuery(query);
        TPS_Logger.log(SeverenceLogLevel.INFO, "Loading " + matrix);
        if (rs.next()) {
            int[] iArray = extractIntArray(rs, "matrix_values");
            int len = (int) Math.sqrt(iArray.length);
            Matrix m = new Matrix(len, len, sIndex);
            for (int index = 0; index < iArray.length; index++) {
                m.setRawValue(index, iArray[index]);
            }
            if (simType != null) this.PM.getParameters().setMatrix(matrix, m, simType);
            else this.PM.getParameters().setMatrix(matrix, m);
        } else {
            TPS_Logger.log(HierarchyLogLevel.CLIENT, SeverenceLogLevel.WARN, "No matrix found for query: " + query);
        }
        TPS_Logger.log(SeverenceLogLevel.INFO, "Loaded matrix from DB: " + matrixName + " Average value: " +
                this.PM.getParameters().getMatrix(matrix).getAverageValue(false, true));
        rs.close();
    }

    /**
     * Method to read a single specified matrix map
     *
     * @param matrixName The name for this matrix map
     * @param matrix     the matrixmap  to store in
     * @param type       The Simulation type to store to
     * @param sIndex     the index for reading in the db. Should be zero.
     */
    private void readMatrix(ParamString matrixName, ParamMatrixMap matrix, SimulationType type, int sIndex) {
        TPS_Logger.log(SeverenceLogLevel.INFO, "Loading matrix map " + matrix + " from DB.");
        MatrixMap m = PM.getDbConnector().readMatrixMap(this.PM.getParameters().getString(matrixName), sIndex, this);

        if (type != null) this.PM.getParameters().paramMatrixMapClass.setMatrixMap(matrix, m, type);
        else this.PM.getParameters().paramMatrixMapClass.setMatrixMap(matrix, m);
    }

    /**
     * This method reads the mode choice tree used for the pivot-point model.
     *
     * @return The tree read from the db.
     * @throws SQLException
     */
    public TPS_ModeChoiceTree readModeChoiceTree() throws SQLException {
        String query = "SELECT node_id, parent_node_id, attribute_values, split_variable, distribution FROM " +
                this.PM.getParameters().getString(ParamString.DB_TABLE_MCT) + " WHERE name='" +
                this.PM.getParameters().getString(ParamString.DB_NAME_MCT) + "' ORDER BY node_id";
        ResultSet rs = PM.executeQuery(query);

        TPS_Node root = null;
        while (rs.next()) {
            int id = rs.getInt("node_id");
            // skip level
            // skip size
            int idParent = rs.getInt("parent_node_id");

            List<Integer> c = new LinkedList<>();
            for (Integer i : extractIntArray(rs, "attribute_values")) {
                c.add(i);
            }
            String splitVar = rs.getString("split_variable");
            TPS_Attribute sv = null;
            if (splitVar != null && splitVar.length() > 1) {
                sv = TPS_Attribute.valueOf(splitVar);
            }
            double[] values = extractDoubleArray(rs, "distribution");
            TPS_DiscreteDistribution<TPS_Mode> ipd = new TPS_DiscreteDistribution<>(TPS_Mode.getConstants());
            for (int i = 0; i < values.length; i++) {
                ipd.setValueByPosition(i, values[i]);
            }

            // We assume that the first row contains the root node data.
            if (root == null) {
                root = new TPS_Node(id, sv, c, ipd, null);
            } else {
                TPS_Node parent = root.getChild(idParent);
                TPS_Node child = new TPS_Node(id, sv, c, ipd, parent);
                parent.addChild(child);
            }
        }
        rs.close();
        return new TPS_ModeChoiceTree(root);
    }

    /**
     * Reads all mode constant codes from the database and stores them in to a global static map
     * A Mode has the form (id, 3-tuples of (name, code, type), isfix)
     * Example: (3, (MIT, 2, MCT), (MIT, 1, VOT), true)
     */
    private void readModes() {
        String query = "SELECT * FROM " + this.PM.getParameters().getString(ParamString.DB_TABLE_CONSTANT_MODE);
        try (ResultSet rs = PM.executeQuery(query)) {
            while (rs.next()) {
                String name = rs.getString("name");
                String clasz = rs.getString("class");

                String[] attributes = new String[rs.getMetaData().getColumnCount() - 4];
                //read to second to last column
                for (int i = 0; i < attributes.length; i++) {
                    attributes[i] = rs.getString(i + 4);
                }
                //read last column (which is a bool)
                boolean isFix = rs.getBoolean("isfix");
                TPS_Mode tm;
                switch (clasz) {
                    case "de.dlr.ivf.tapas.mode.TPS_NonMotorisedMode": {
                        tm = new TPS_NonMotorisedMode(name, attributes, isFix, this.PM.getParameters());
                        break;
                    }
                    case "de.dlr.ivf.tapas.mode.TPS_IndividualTransportMode": {
                        tm = new TPS_IndividualTransportMode(name, attributes, isFix, this.PM.getParameters());
                        break;
                    }
                    case "de.dlr.ivf.tapas.mode.TPS_MassTransportMode": {
                        tm = new TPS_MassTransportMode(name, attributes, isFix, this.PM.getParameters());
                        break;
                    }
                    default:
                        throw new RuntimeException("Could not read proper TPS_Mode class");
                }
                tm.addModeToMap();
            }
        } catch (SQLException e) {
            TPS_Logger.log(SeverenceLogLevel.ERROR, "SQL error in readModes! Query: " + query + " constant:" +
                    this.PM.getParameters().getString(ParamString.DB_TABLE_CONSTANT_MODE), e);
            throw new RuntimeException("Error loading modes " + ParamString.DB_TABLE_CONSTANT_MODE.toString());
        } catch (IllegalArgumentException e) {
            TPS_Logger.log(SeverenceLogLevel.ERROR, "Class-related error in readModes! Query: " + query + " constant:" +
                    this.PM.getParameters().getString(ParamString.DB_TABLE_CONSTANT_MODE), e);
            throw new RuntimeException("Error loading constant " + ParamString.DB_TABLE_CONSTANT_MODE.toString());
        }
    }

    /**
     * Reads all person group codes from the database and stores them in to a global static map
     * A PersGroup has the form (id, 3-tuples of (name, code, type), code_ageclass, code_sex, code_cars, persType)
     * Example: (3, (RoP65-74, 12, VISEVA_R), 6, ,1, 2, RETIREE)
     */
    private void readPersonGroupCodes() {
        String query = "SELECT * FROM " + this.PM.getParameters().getString(ParamString.DB_TABLE_CONSTANT_PERSON);
        try (ResultSet rs = PM.executeQuery(query)) {
            TPS_PersonGroup tpg;
            while (rs.next()) {
                String[] attributes = new String[rs.getMetaData().getColumnCount() - 3];
                for (int i = 0; i < attributes.length; i++) {
                    attributes[i] = rs.getString(i + 4);
                }
                tpg = new TPS_PersonGroup(rs.getInt("id"), attributes);
                // add person group object to a global static map which is a collection of all person groups
                tpg.addPersonGroupToMap();
            }
        } catch (SQLException e) {
            TPS_Logger.log(SeverenceLogLevel.ERROR,
                    "SQL error in readPersonGroupCodes! Query: " + query + " constant:" +
                            this.PM.getParameters().getString(ParamString.DB_TABLE_CONSTANT_PERSON), e);
            throw new RuntimeException("Error loading constant " +
                    this.PM.getParameters().getString(ParamString.DB_TABLE_CONSTANT_PERSON));
        }
    }

    /**
     * Reads all region specific parameters from the DB
     *
     * @return The region, specified in the Parameter set
     * @throws SQLException Error during SQL-processing
     */
    public TPS_Region readRegion() throws SQLException {
        TPS_Region region = new TPS_Region(PM);
        TPS_Block blk;
        String query;
        // read values of time
        query = "SELECT * FROM " + this.PM.getParameters().getString(ParamString.DB_TABLE_VOT) + " WHERE name='" +
                this.PM.getParameters().getString(ParamString.DB_NAME_VOT) + "'";
        ResultSet rsVOT = PM.executeQuery(query);

        region.setValuesOfTime(
                this.readVariableMap(rsVOT, 2, 0, this.PM.getParameters().getDoubleValue(ParamValue.DEFAULT_VOT)));
        rsVOT.close();
        // read cfn values
        TPS_CFN cfn = new TPS_CFN(TPS_SettlementSystemType.TAPAS, TPS_ActivityCodeType.TAPAS);
        query = "SELECT \"CURRENT_TAZ_SETTLEMENT_CODE_TAPAS\",value FROM " + this.PM.getParameters().getString(
                ParamString.DB_TABLE_CFNX) + " WHERE key = '" + this.PM.getParameters().getString(
                ParamString.DB_REGION_CNF_KEY) + "'";
        ResultSet rsCFNX = PM.executeQuery(query);
        int key, reg;
        double val;
        while (rsCFNX.next()) {
            key = rsCFNX.getInt("CURRENT_TAZ_SETTLEMENT_CODE_TAPAS");
            val = rsCFNX.getDouble("value");
            cfn.addToCFNXMap(key, val);
        }
        rsCFNX.close();

        query = "SELECT \"CURRENT_EPISODE_ACTIVITY_CODE_TAPAS\",\"CURRENT_TAZ_SETTLEMENT_CODE_TAPAS\",value FROM " +
                this.PM.getParameters().getString(ParamString.DB_TABLE_CFN4) + " WHERE key = '" +
                this.PM.getParameters().getString(ParamString.DB_ACTIVITY_CNF_KEY) + "'";
        ResultSet rsCFN4 = PM.executeQuery(query);
        while (rsCFN4.next()) {
            reg = rsCFN4.getInt("CURRENT_TAZ_SETTLEMENT_CODE_TAPAS");
            key = rsCFN4.getInt("CURRENT_EPISODE_ACTIVITY_CODE_TAPAS");
            val = rsCFN4.getDouble("value");
            cfn.addToCFN4Map(reg, key, val);
        }
        rsCFN4.close();

        region.setCfn(cfn);

        String tazTable = this.PM.getParameters().getString(ParamString.DB_TABLE_TAZ);
        if (tazTable.indexOf(".") > 0) {
            tazTable = tazTable.substring(tazTable.indexOf(".") + 1);
        }


        // read traffic analysis zones
        query = "SELECT taz_id, taz_bbr_type, taz_num_id, ST_X(taz_coordinate) as x, ST_Y(taz_coordinate) as y FROM " +
                this.PM.getParameters().getString(ParamString.DB_TABLE_TAZ);
        ResultSet rsTAZ = PM.executeQuery(query);
        while (rsTAZ.next()) {
            TPS_TrafficAnalysisZone taz = region.createTrafficAnalysisZone(rsTAZ.getInt("taz_id"));
            taz.setBbrType(TPS_SettlementSystem
                    .getSettlementSystem(TPS_SettlementSystemType.FORDCP, rsTAZ.getInt("taz_bbr_type")));
            taz.setCenter(rsTAZ.getDouble("x"), rsTAZ.getDouble("y"));

            if (rsTAZ.getInt("taz_num_id") != 0) {
                taz.setExternalId(rsTAZ.getInt("taz_num_id"));
            } else {
                taz.setExternalId(-1);
            }
        }

        rsTAZ.close();
        if (this.PM.getParameters().isDefined(ParamString.DB_TABLE_TAZ_SCORES)) {
            query = "SELECT * FROM " + this.PM.getParameters().getString(ParamString.DB_TABLE_TAZ_SCORES) +
                    " WHERE score_name='" + this.PM.getParameters().getString(ParamString.DB_NAME_TAZ_SCORES) + "'";
            rsTAZ = PM.executeQuery(query);

            while (rsTAZ.next()) {
                TPS_TrafficAnalysisZone taz = region.getTrafficAnalysisZone(rsTAZ.getInt("score_taz_id"));
                taz.setScore(rsTAZ.getDouble("score"));
                taz.setScoreCat(rsTAZ.getInt("score_cat"));
            }
            rsTAZ.close();
        }

        // read taz infos
        // Check if the travel time matrices have values on the diagonal positions (intra zone times);
        // else read specific intra information files if necessary and provided
        ResultSet rsTAZInfo;
        if (this.PM.getParameters().isDefined(ParamString.DB_TABLE_TAZ_INTRA_MIT_INFOS) &&
                this.PM.getParameters().isDefined(ParamString.DB_TABLE_TAZ_INTRA_MIT_INFOS) &&
                this.PM.getParameters().isDefined(ParamString.DB_NAME_TAZ_INTRA_MIT_INFOS) &&
                this.PM.getParameters().isDefined(ParamString.DB_NAME_TAZ_INTRA_PT_INFOS)) {
            query = "SELECT mi.info_taz_id, mi.beeline_factor_mit, " + "mi.average_speed_mit, pi.average_speed_pt, " +
                    "mi.has_intra_traffic_mit, pi.has_intra_traffic_pt, pi.pt_zone FROM " +
                    this.PM.getParameters().getString(ParamString.DB_TABLE_TAZ_INTRA_MIT_INFOS) + " mi, " +
                    this.PM.getParameters().getString(ParamString.DB_TABLE_TAZ_INTRA_PT_INFOS) + " pi " +
                    "WHERE mi.info_taz_id = pi.info_taz_id " + "AND mi.info_name = '" +
                    this.PM.getParameters().getString(ParamString.DB_NAME_TAZ_INTRA_MIT_INFOS) +
                    "' AND pi.info_name = '" + this.PM.getParameters().getString(
                    ParamString.DB_NAME_TAZ_INTRA_PT_INFOS) + "'";
            rsTAZInfo = PM.executeQuery(query);
            this.readTAZInfos(rsTAZInfo, region, SimulationType.SCENARIO);
            rsTAZInfo.close();
        }

        // travel times for the base case
        if (this.PM.getParameters().isDefined(ParamString.DB_TABLE_TAZ_INTRA_MIT_INFOS) &&
                this.PM.getParameters().isDefined(ParamString.DB_TABLE_TAZ_INTRA_PT_INFOS) &&
                this.PM.getParameters().isDefined(ParamString.DB_NAME_TAZ_INTRA_MIT_INFOS_BASE) &&
                this.PM.getParameters().isDefined(ParamString.DB_NAME_TAZ_INTRA_PT_INFOS_BASE) &&
                this.PM.getParameters().isTrue(ParamFlag.FLAG_RUN_SZENARIO)) {
            query = "SELECT mi.info_taz_id, mi.beeline_factor_mit, " + "mi.average_speed_mit, pi.average_speed_pt, " +
                    "mi.has_intra_traffic_mit, pi.has_intra_traffic_pt, pi.pt_zone FROM " +
                    this.PM.getParameters().getString(ParamString.DB_TABLE_TAZ_INTRA_MIT_INFOS) + " mi, " +
                    this.PM.getParameters().getString(ParamString.DB_TABLE_TAZ_INTRA_PT_INFOS) + " pi " +
                    "WHERE mi.info_taz_id = pi.info_taz_id " + "AND mi.info_name = '" +
                    this.PM.getParameters().getString(ParamString.DB_NAME_TAZ_INTRA_MIT_INFOS_BASE) +
                    "' AND pi.info_name = '" + this.PM.getParameters().getString(
                    ParamString.DB_NAME_TAZ_INTRA_PT_INFOS_BASE) + "'";
            rsTAZInfo = PM.executeQuery(query);
            this.readTAZInfos(rsTAZInfo, region, SimulationType.BASE);
            rsTAZInfo.close();
        }

        // read fees and tolls
        query = "SELECT * FROM " + this.PM.getParameters().getString(ParamString.DB_TABLE_TAZ_FEES_TOLLS) +
                " WHERE ft_name='" + this.PM.getParameters().getString(ParamString.DB_NAME_FEES_TOLLS) + "'";
        ResultSet rsFeesTolls = PM.executeQuery(query);
        while (rsFeesTolls.next()) {
            TPS_TrafficAnalysisZone taz = region.getTrafficAnalysisZone(rsFeesTolls.getInt("ft_taz_id"));
            taz.initFeesTolls(rsFeesTolls.getBoolean("has_toll_base"), rsFeesTolls.getInt("toll_type_base"),
                    rsFeesTolls.getBoolean("has_fee_base"), rsFeesTolls.getInt("fee_type_base"),
                    rsFeesTolls.getBoolean("has_toll_scen"), rsFeesTolls.getInt("toll_type_scen"),
                    rsFeesTolls.getBoolean("has_fee_scen"), rsFeesTolls.getInt("fee_type_scen"),
                    rsFeesTolls.getBoolean("has_car_sharing_base"), rsFeesTolls.getBoolean("has_car_sharing"),
                    this.PM.getParameters());
            taz.setRestricted(rsFeesTolls.getBoolean("is_restricted"));
            taz.setPNR(rsFeesTolls.getBoolean("is_park_and_ride"));
        }
        rsFeesTolls.close();

        // read blocks
        if (this.PM.getParameters().isDefined(ParamString.DB_TABLE_BLOCK)) {
            query = "SELECT b.blk_id, b.blk_taz_id, ST_X(b.blk_coordinate) as x, ST_Y(b.blk_coordinate) as y, bs.score, bs.score_cat, bn.next_pt_stop FROM " +
                    this.PM.getParameters().getString(ParamString.DB_TABLE_BLOCK) + " b, " +
                    this.PM.getParameters().getString(ParamString.DB_TABLE_BLOCK_SCORES) + " bs, " +
                    this.PM.getParameters().getString(ParamString.DB_TABLE_BLOCK_NEXT_PT_STOP) +
                    " bn WHERE bs.score_name='" + this.PM.getParameters().getString(ParamString.DB_NAME_BLOCK_SCORES) +
                    "' AND bn.next_pt_stop_name='" + this.PM.getParameters().getString(
                    ParamString.DB_NAME_BLOCK_NEXT_PT_STOP) +
                    "' AND b.blk_id = bs.score_blk_id AND b.blk_id = bn.next_pt_stop_blk_id";
            ResultSet rsBLK = PM.executeQuery(query);
            while (rsBLK.next()) {
                TPS_TrafficAnalysisZone taz = region.getTrafficAnalysisZone(rsBLK.getInt("blk_taz_id"));
                blk = taz.getBlock(rsBLK.getInt("blk_id"));
                blk.setScore(rsBLK.getDouble("score"));
                blk.setScoreCat(rsBLK.getInt("score_cat"));
                blk.setNearestPubTransStop(rsBLK.getDouble("next_pt_stop"));
                blk.setCenter(rsBLK.getDouble("x"), rsBLK.getDouble("y"));
            }
            rsBLK.close();
        }

        // read locations
        query = "SELECT loc_id, loc_group_id, loc_code, loc_taz_id, loc_blk_id, loc_has_fix_capacity, loc_capacity, ST_X(loc_coordinate) as x,ST_Y(loc_coordinate) as y FROM " +
                this.PM.getParameters().getString(ParamString.DB_TABLE_LOCATION) +
                " WHERE loc_capacity >0 and key = '" + this.PM.getParameters().getString(ParamString.DB_LOCATION_KEY) + "'";  // do not add zero capacity locations
        ResultSet rsLoc = PM.executeQuery(query);

        double totalCapacity = 0;
        int numOfLocations = 0;
        while (rsLoc.next()) {
            int locId = rsLoc.getInt("loc_id");
            int groupId = -1;
            if (this.PM.getParameters().isTrue(ParamFlag.FLAG_USE_LOCATION_GROUPS)) groupId = rsLoc.getInt(
                    "loc_group_id");
            TPS_LocationConstant locCode = TPS_LocationConstant.getLocationCodeByTypeAndCode(TPS_LocationCodeType.TAPAS,
                    rsLoc.getInt("loc_code"));
            double x = rsLoc.getDouble("x");
            double y = rsLoc.getDouble("y");

            int taz_id = rsLoc.getInt("loc_taz_id");
            int block_id = rsLoc.getInt("loc_blk_id");
            if (rsLoc.wasNull()) {
                block_id = -1;
            }
            boolean fixedCap = rsLoc.getBoolean("loc_has_fix_capacity");
            int cap = rsLoc.getInt("loc_capacity");
            TPS_TrafficAnalysisZone taz = region.getTrafficAnalysisZone(taz_id);
            if (taz != null) { //locations in unknown tazes are discarded!
                TPS_Block block = block_id < 0 ? null : taz.getBlock(block_id);

                // build location
                TPS_Location location = new TPS_Location(locId, groupId, locCode, x, y, taz, block,
                        this.PM.getParameters());
                // now adapt the capacity to the sample size
                cap = (int) ((cap * this.PM.getParameters().getDoubleValue(ParamValue.DB_HH_SAMPLE_SIZE)) +
                        0.5); // including round
                if (cap == 0) cap = 1;// every non-zero capacity has at least one place to go!
                location.initCapacity(cap, fixedCap);
                totalCapacity += cap;
                numOfLocations++;

                // add location to the taz and the block
                taz.addLocation(location);
                region.addLocation(location);
                if (block != null) {
                    block.addLocation(location);
                }
            }
        }
        //now update the occupancy values from the temporary table
        this.updateOccupancyTable(region);
        if (TPS_Logger.isLogging(HierarchyLogLevel.CLIENT, SeverenceLogLevel.INFO)) {
            TPS_Logger.log(HierarchyLogLevel.CLIENT, SeverenceLogLevel.INFO,
                    "Total number of locations: " + numOfLocations + " capacity sum: " + totalCapacity);
        }
        rsLoc.close();
        return region;
    }

    /**
     * This method reads the scheme set, scheme class distribution values and all episodes from the db.
     *
     * @return The TPS_SchemeSet containing all episodes.
     * @throws SQLException
     */
    public TPS_SchemeSet readSchemeSet() throws SQLException {
        ResultSet rs;
        int timeSlotLength = this.PM.getParameters().getIntValue(ParamValue.SEC_TIME_SLOT);

        // build scheme classes (with time distributions)
        TPS_SchemeSet schemeSet = new TPS_SchemeSet();
        rs = PM.executeQuery("SELECT * FROM " + this.PM.getParameters().getString(ParamString.DB_TABLE_SCHEME_CLASS) +
                " where key = '" + this.PM.getParameters().getString(ParamString.DB_SCHEME_CLASS_KEY) + "'");
        while (rs.next()) {
            TPS_SchemeClass schemeClass = schemeSet.getSchemeClass(rs.getInt("scheme_class_id"));
            double mean = rs.getDouble("avg_travel_time") * 60;
            schemeClass.setTimeDistribution(mean, mean * rs.getDouble("proz_std_dev"));
        }

        // build the schemes, assigning them to the right scheme classes
        Map<Integer, TPS_Scheme> schemeMap = new HashMap<>();
        rs = PM.executeQuery("SELECT * FROM " + this.PM.getParameters().getString(ParamString.DB_TABLE_SCHEME) +
                " where key = '" + this.PM.getParameters().getString(ParamString.DB_SCHEME_KEY) + "'");
        while (rs.next()) {
            TPS_SchemeClass schemeClass = schemeSet.getSchemeClass(rs.getInt("scheme_class_id"));
            TPS_Scheme scheme = schemeClass.getScheme(rs.getInt("scheme_id"), this.PM.getParameters());
            schemeMap.put(scheme.getId(), scheme);
        }

        // read the episodes the schemes are made of and add them to the respective schemes
        // we read them into a temporary storage first
        rs = PM.executeQuery("SELECT scheme_id, act_code_zbe, home, start, duration, tournumber FROM " +
                this.PM.getParameters().getString(ParamString.DB_TABLE_EPISODE) + " where key = '"
                + this.PM.getParameters().getString(ParamString.DB_EPISODE_KEY) + "' ORDER BY scheme_id, start");
        int counter = 1;
        HashMap<Integer, Vector<TPS_Episode>> episodesMap = new HashMap<>();
        TPS_Episode lastEpisode = null;
        int lastScheme = -1;
        while (rs.next()) {
            TPS_ActivityConstant actCode = TPS_ActivityConstant.getActivityCodeByTypeAndCode(TPS_ActivityCodeType.ZBE,
                    rs.getInt("act_code_zbe"));
            int actScheme = rs.getInt("scheme_id");
            if (lastScheme != actScheme) {
                lastScheme = actScheme;
                lastEpisode = null;
                episodesMap.put(actScheme, new Vector<>());
            }
            TPS_Episode episode = null;
            if (actCode.isTrip()) {
                episode = new TPS_Trip(counter++, actCode, rs.getInt("start") * timeSlotLength,
                        rs.getInt("duration") * timeSlotLength, this.PM.getParameters());
            } else {
                if (lastEpisode != null && lastEpisode.isStay()) {
                    // two subsequent stays: add their duration and adjust the activity code and the priority
                    TPS_Stay previousStay = (TPS_Stay) lastEpisode;
                    if (previousStay.getPriority() < actCode.getCode(TPS_ActivityCodeType.PRIORITY)) {
                        // the last stay has a different activity code and is "less important" than the current one
                        previousStay.setActCode(actCode); // adjust the activity code!
                    }
                    previousStay.setOriginalDuration(
                            previousStay.getOriginalDuration() + rs.getInt("duration") * timeSlotLength);
                    previousStay.setOriginalStart(
                            Math.min(previousStay.getOriginalStart(), rs.getInt("start") * timeSlotLength));
                } else {
                    episode = new TPS_Stay(counter++, actCode, rs.getInt("start") * timeSlotLength,
                            rs.getInt("duration") * timeSlotLength, 0, 0, 0, 0, this.PM.getParameters());
                }
            }
            if (episode != null) {
                episode.isHomePart = rs.getBoolean("home");
                episode.tourNumber = rs.getInt("tournumber");
                episodesMap.get(actScheme).add(episode);
                lastEpisode = episode;
            }
        }
        // now we store them into the schemes
        for (Integer schemeID : episodesMap.keySet()) {
            TPS_Scheme scheme = schemeMap.get(schemeID);
            for (TPS_Episode e : episodesMap.get(schemeID)) {
                scheme.addEpisode(e);
            }
        }


        // read the mapping from person group to scheme classes
        String query = "SELECT * FROM " + this.PM.getParameters().getString(ParamString.DB_TABLE_SCHEME_CLASS_DISTRIBUTION) +
                " WHERE name='" + this.PM.getParameters().getString(ParamString.DB_NAME_SCHEME_CLASS_DISTRIBUTION) +
                "' and key = '" + this.PM.getParameters().getString(ParamString.DB_SCHEME_CLASS_DISTRIBUTION_KEY) +
                "' ORDER BY person_group, scheme_class_id";
        rs = PM.executeQuery(query);


        HashMap<Integer, HashMap<Integer, Double>> personGroupSchemeProbabilityMap = new HashMap<>();
        while (rs.next()) {//read all persons group <-> (schemeClassId, probability) correspondences and store them
            int pers_group_id = rs.getInt("person_group");
            if (!personGroupSchemeProbabilityMap.containsKey(pers_group_id)) {
                personGroupSchemeProbabilityMap.put(pers_group_id, new HashMap<>());
            }
            personGroupSchemeProbabilityMap.get(pers_group_id).put(rs.getInt("scheme_class_id"),
                    rs.getDouble("probability"));
        }
        for (Integer key : personGroupSchemeProbabilityMap.keySet()) { //add distributions to the schemeSet
            schemeSet.addDistribution(TPS_PersonGroup.getPersonGroupByTypeAndCode(TPS_PersonGroupType.TAPAS, key),
                    new TPS_DiscreteDistribution<>(personGroupSchemeProbabilityMap.get(key)));
        }

        schemeSet.init();

        return schemeSet;
    }

    /**
     * Reads all settlement system codes from the database and stores them in to a global static map
     * A SettlementSystem has the form (id, 3-tuples of (name, code, type))
     * Example: (7, ("R1, K1, Kernstadt > 500000", "1", "FORDCP"))
     */
    public void readSettlementSystemCodes() {
        String query = "SELECT * FROM " + this.PM.getParameters().getString(ParamString.DB_TABLE_CONSTANT_SETTLEMENT);
        try (ResultSet rs = PM.executeQuery(query)) {
            TPS_SettlementSystem tss;
            while (rs.next()) {
                String[] attributes = new String[rs.getMetaData().getColumnCount() - 3];
                for (int i = 0; i < attributes.length; i++) {
                    attributes[i] = rs.getString(i + 4);
                }
                tss = new TPS_SettlementSystem(rs.getInt("id"), attributes);
                // add settlement system object to a global static map which is a collection of all settlement systems
                tss.addSettlementSystemToMap();
            }
        } catch (SQLException e) {
            TPS_Logger.log(SeverenceLogLevel.ERROR,
                    "SQL error in readSettlementSystemCodes! Query: " + query + " constant:" +
                            this.PM.getParameters().getString(ParamString.DB_TABLE_CONSTANT_SETTLEMENT), e);
            throw new RuntimeException("Error loading constant " +
                    this.PM.getParameters().getString(ParamString.DB_TABLE_CONSTANT_SETTLEMENT));
        }
    }

    /**
     * Reads all sex codes from the database and stores them through enums
     * A SexCodes has the form (name_sex, code_sex)
     * Example: (FEMALE, 2)
     */
    private void readSexCodes() {
        String query = "SELECT name_sex, code_sex FROM " + this.PM.getParameters().getString(
                ParamString.DB_TABLE_CONSTANT_SEX);
        try (ResultSet rs = PM.executeQuery(query)) {
            while (rs.next()) {
                try {
                    TPS_Sex s = TPS_Sex.valueOf(rs.getString("name_sex"));
                    s.code = rs.getInt("code_sex");
                } catch (IllegalArgumentException e) {
                    TPS_Logger.log(SeverenceLogLevel.WARN,
                            "Read invalid sex type name from DB:" + rs.getString("name_sex"));
                }
            }
        } catch (SQLException e) {
            TPS_Logger.log(SeverenceLogLevel.ERROR, "SQL error in readSexCodes! Query: " + query + " constant:" +
                    this.PM.getParameters().getString(ParamString.DB_TABLE_CONSTANT_SEX), e);
            throw new RuntimeException(
                    "Error loading constant " + this.PM.getParameters().getString(ParamString.DB_TABLE_CONSTANT_SEX));
        }
    }

    /**
     * This method processes the ResultSet for the intra cell-infos like travel speed or pt zone
     *
     * @param set    The ResultSet from a appropriate sql-query. Must contain: info_taz_id, beeline_factor_mit, average_speed_mit, average_speed_pt, has_intra_traffic_mit, has_intra_traffic_pt, pt_zone
     * @param region The region, where the infos are stored in
     * @param type   theSimulationType (BASE/SCENARIO)
     * @throws SQLException
     */
    private void readTAZInfos(ResultSet set, TPS_Region region, SimulationType type) throws SQLException {
        TPS_TrafficAnalysisZone taz;
        ScenarioTypeValues stv;
        while (set.next()) {
            taz = region.getTrafficAnalysisZone(set.getInt("info_taz_id"));
            stv = taz.getSimulationTypeValues(type);
            stv.setBeelineFactorMIT(set.getDouble("beeline_factor_mit"));
            stv.setAverageSpeedMIT(set.getDouble("average_speed_mit"));
            stv.setAverageSpeedPT(set.getDouble("average_speed_pt"));
            stv.setIntraMITTrafficAllowed(set.getBoolean("has_intra_traffic_mit"));
            stv.setIntraPTTrafficAllowed(set.getBoolean("has_intra_traffic_pt"));
            stv.setPtZone(set.getInt("pt_zone"));
        }
    }

    /**
     * Method to read the parameters of the utility function
     *
     * @throws SQLException
     */
    public void readUtilityFunction() throws SQLException {
        ResultSet rs;
        String utilityFunctionName = this.PM.getParameters().getString(ParamString.UTILITY_FUNCTION_CLASS);
        utilityFunctionName = utilityFunctionName.substring(utilityFunctionName.lastIndexOf('.') + 1);
        // read the parameters for the utility function of the modes
        rs = PM.executeQuery(
                "SELECT mode_class, parameters FROM " + this.PM.getParameters().getString(ParamString.DB_SCHEMA_CORE) +
                        this.PM.getParameters().getString(ParamString.DB_NAME_MODEL_PARAMETERS) +
                        " WHERE utility_function_class='" + utilityFunctionName + "' and key='" +
                        this.PM.getParameters().getString(ParamString.UTILITY_FUNCTION_KEY) + "'");
        while (rs.next()) {
            String mode = rs.getString("mode_class");
            double[] parameters = extractDoubleArray(rs, "parameters");
            TPS_Mode.getUtilityFunction(this.PM.getParameters()).setParameterSet(TPS_Mode.get(ModeType.valueOf(mode)),
                    parameters);
        }
        rs.close();

    }

    /**
     * Method to read variable maps, like value of time.
     *
     * @param set          The SQL- ResultSet
     * @param offset       an offset for the storing positin in one atribute element
     * @param comment      specifies the number of comments included in the ResultSet
     * @param defaultValue defines a default value in case of null-columns
     * @return The initialized TPS_VariableMap
     * @throws SQLException
     */
    private TPS_VariableMap readVariableMap(ResultSet set, int offset, int comment, double defaultValue) throws SQLException {
        Collection<TPS_Attribute> attributes = new LinkedList<>();
        int i;
        int cols = set.getMetaData().getColumnCount() - comment;
        for (i = offset; i < cols; i++) {
            try {
                attributes.add(TPS_Attribute.valueOf(set.getMetaData().getColumnName(i)));
            } catch (IllegalArgumentException e) {
                TPS_Logger.log(SeverenceLogLevel.FATAL, "Unknown attribute: " + set.getMetaData().getColumnName(i), e);
            }
        }
        TPS_VariableMap varMap = new TPS_VariableMap(attributes, defaultValue);
        List<Integer> list = new ArrayList<>();
        while (set.next()) {
            list.clear();
            for (i = offset; i < cols; i++) {
                list.add(set.getInt(i));
            }
            varMap.addValue(list, set.getDouble(i));
        }
        return varMap;
    }

    /**
     * This method returns one household back to the database, e.g. if there where no members in this household found. Usually this indicates an error in the data set
     *
     * @param hh The household to return
     * @throws SQLException
     */
    private void returnHousehold(TPS_Household hh) {

        //removing the location from the location set automatically removes it from the taz, too!
        // TODO: yes, but why???
        //hh.getLocation().getLocSet().removeLocation(hh.getLocation());

        if (!TPS_DB_IOManager.BEHAVIOUR.equals(Behaviour.FAT)) {
            PM.functionExecute("finish_hh", this.PM.getParameters().getString(ParamString.DB_TABLE_HOUSEHOLD_TMP),
                    hh.getId());
        }
    }

    /**
     * In this method we update the occupancies of the locations.
     * We have to work with increments, because other threads might have updated the occupancies in between.
     * I call this "sloppy synchronization", because this is a classical shared resource for a multi-machine setup.
     * However, a little overbooking is not so bad!
     * After that we update our occupancies with the values stored in db.
     *
     * @param region with locations where the occupancy gets updated
     */
    private void updateOccupancyTable(TPS_Region region) {
        String query = "";
        try {
            if (this.PM.getParameters().isTrue(ParamFlag.FLAG_UPDATE_LOCATION_WEIGHTS)) {
                // update the local occupancies/weights
                if (TPS_Logger.isLogging(HierarchyLogLevel.THREAD, SeverenceLogLevel.INFO)) {
                    TPS_Logger.log(HierarchyLogLevel.THREAD, SeverenceLogLevel.INFO, "Updating locations");
                }
                query = "SELECT loc_id, loc_occupancy FROM " + this.PM.getParameters().getString(
                        ParamString.DB_TABLE_LOCATION_TMP) + " WHERE loc_id >= 0";

                ResultSet rsOcc = PM.executeQuery(query);
                while (rsOcc.next()) {
                    TPS_Location loc = region.getLocation(rsOcc.getInt("loc_id"));
                    if (loc != null) {
                        loc.updateOccupancy(rsOcc.getInt("loc_occupancy"));
                    } else if (TPS_Logger.isLogging(HierarchyLogLevel.THREAD, SeverenceLogLevel.DEBUG)) {
                        TPS_Logger.log(HierarchyLogLevel.THREAD, SeverenceLogLevel.DEBUG,
                                "Location " + rsOcc.getInt("loc_id") + " not found!");
                    }
                }
                rsOcc.close();
            }
        } catch (SQLException e) {
            TPS_Logger.log(HierarchyLogLevel.THREAD, SeverenceLogLevel.ERROR,
                    "error during one of the sql queries: " + query, e);
            TPS_Logger.log(HierarchyLogLevel.THREAD, SeverenceLogLevel.ERROR, "next exception:", e.getNextException());
        }

    }

    /**
     * Method to vacuum the heavily used temp tables. It determines the type of clean up needed,
     * looks if someone else is cleaning up and starts cleaning if no other tread is busy doing this job.
     * This is a classic task for a job-scheduling manager and not for the worker threads :(
     */
    private void vacuumTempTables() {
        String query = "", query2 = "", query3, query4;
        try {
            query = "SELECT sim_progress, sim_total FROM " + this.PM.getParameters().getString(
                    ParamString.DB_TABLE_SIMULATIONS) + " WHERE sim_key = '" + this.PM.getParameters().getString(
                    ParamString.RUN_IDENTIFIER) + "'";


            ResultSet mRs = PM.executeQuery(query);

            int before = 0;
            int total = 1;
            if (mRs.next()) {
                before = mRs.getInt("sim_progress");
                total = mRs.getInt("sim_total");
            }
            mRs.close();

            PM.functionExecute("finish_hh", this.PM.getParameters().getString(ParamString.DB_TABLE_HOUSEHOLD_TMP),
                    this.PM.getParameters().getString(ParamString.RUN_IDENTIFIER), ADDRESS);


            //haha! since postgre 9.2 the column is named "query" not "current_query"  anymore!
            query = "SELECT version()";
            String columnName = "current_query";

            mRs = PM.executeQuery(query);

            if (mRs.next()) {
                String version = mRs.getString("version");
                String mayorVersion;
                String minorVersion;
                //extract version number
                String startString = "PostgreSQL ";
                String stopString = "."; // just interpret the mayor version
                mayorVersion = version.substring(version.indexOf(startString) + startString.length(),
                        version.indexOf(stopString));
                minorVersion = version.substring(version.indexOf(stopString) + stopString.length(),
                        version.indexOf(stopString) + 2);
                if (Integer.parseInt(mayorVersion) >= 10 || (Integer.parseInt(mayorVersion) == 9 && Integer.parseInt(
                        minorVersion) >= 2)) columnName = "query";
            }

            mRs.close();


            int after = before;

            query = "SELECT sim_progress, sim_total FROM " + this.PM.getParameters().getString(
                    ParamString.DB_TABLE_SIMULATIONS) + " WHERE sim_key = '" + this.PM.getParameters().getString(
                    ParamString.RUN_IDENTIFIER) + "'";
            mRs = PM.executeQuery(query);


            if (mRs.next()) {
                after = mRs.getInt("sim_progress");
            }
            mRs.close();


            if (after < total) {

                //see if someone else is doing our job:

                query = "SELECT " + columnName + " FROM pg_stat_activity";
                mRs = PM.executeQuery(query);
                query = "VACUUM temp.locations_" + this.PM.getParameters().getString(ParamString.RUN_IDENTIFIER);
                query2 = "VACUUM temp.households_" + this.PM.getParameters().getString(ParamString.RUN_IDENTIFIER);
                boolean doCleanup = true;
                while (mRs.next() && doCleanup) {
                    if (query.compareToIgnoreCase(mRs.getString(columnName)) == 0) // is someone else doing this job?
                        doCleanup = false;
                    if (query2.compareToIgnoreCase(mRs.getString(columnName)) == 0) // is someone else doing this job?
                        doCleanup = false;
                }
                mRs.close();
                if (doCleanup) {
                    if (TPS_Logger.isLogging(HierarchyLogLevel.THREAD, SeverenceLogLevel.DEBUG)) {
                        TPS_Logger.log(HierarchyLogLevel.THREAD, SeverenceLogLevel.DEBUG,
                                "Vacuuming temporary tables for locations in simulation " +
                                        this.PM.getParameters().getString(ParamString.RUN_IDENTIFIER));
                    }
                    PM.execute(query);
                    if (TPS_Logger.isLogging(HierarchyLogLevel.THREAD, SeverenceLogLevel.DEBUG)) {
                        TPS_Logger.log(HierarchyLogLevel.THREAD, SeverenceLogLevel.DEBUG,
                                "Vacuuming temporary tables for households in simulation " +
                                        this.PM.getParameters().getString(ParamString.RUN_IDENTIFIER));
                    }
                    PM.execute(query2);
                }

                //every 61*fetchSizePerProcessor households do a reindex of the temp tables
                if (lastCleanUp < 0) { //init
                    lastCleanUp = after;
                }
                if (Math.random() <
                        0.1) { //random is better than planed, because servers are lining up to clean the db multiple
                    // times
                    lastCleanUp = after;
                    if (TPS_Logger.isLogging(HierarchyLogLevel.THREAD, SeverenceLogLevel.INFO)) {
                        TPS_Logger.log(HierarchyLogLevel.THREAD, SeverenceLogLevel.INFO,
                                "Time to reindex and clean the simulation " +
                                        this.PM.getParameters().getString(ParamString.RUN_IDENTIFIER));
                    }
                    //see if someone else is doing our job:
                    query = "SELECT " + columnName + " FROM pg_stat_activity";


                    mRs = PM.executeQuery(query);
                    query = "VACUUM FULL temp.locations_" + this.PM.getParameters().getString(
                            ParamString.RUN_IDENTIFIER);
                    query2 = "VACUUM FULL temp.households_" + this.PM.getParameters().getString(
                            ParamString.RUN_IDENTIFIER);
                    //reindexing is faster than analyzing
                    query3 = "REINDEX TABLE temp.locations_" + this.PM.getParameters().getString(
                            ParamString.RUN_IDENTIFIER);
                    query4 = "REINDEX TABLE temp.households_" + this.PM.getParameters().getString(
                            ParamString.RUN_IDENTIFIER);

                    doCleanup = true;
                    while (mRs.next() && doCleanup) {
                        if (query.compareToIgnoreCase(mRs.getString(columnName)) ==
                                0) // is someone else doing this job?
                            doCleanup = false;
                        if (query2.compareToIgnoreCase(mRs.getString(columnName)) ==
                                0) // is someone else doing this job?
                            doCleanup = false;
                        if (query3.compareToIgnoreCase(mRs.getString(columnName)) ==
                                0) // is someone else doing this job?
                            doCleanup = false;
                        if (query4.compareToIgnoreCase(mRs.getString(columnName)) ==
                                0) // is someone else doing this job?
                            doCleanup = false;
                    }
                    mRs.close();
                    if (doCleanup) {
                        if (TPS_Logger.isLogging(HierarchyLogLevel.THREAD, SeverenceLogLevel.INFO)) {
                            TPS_Logger.log(HierarchyLogLevel.THREAD, SeverenceLogLevel.INFO,
                                    "Reindexing temporary tables");
                        }
                        PM.execute(query);
                        PM.execute(query2);
                        PM.execute(query3);
                        PM.execute(query4);
                    } else {
                        if (TPS_Logger.isLogging(HierarchyLogLevel.THREAD, SeverenceLogLevel.INFO)) {
                            TPS_Logger.log(HierarchyLogLevel.THREAD, SeverenceLogLevel.INFO,
                                    "Someone else is reindexing!");
                        }
                    }
                }
            } else {
                lastCleanUp = -1; //for new sim!
                if (after > before) { // avoid that every thread reindexes. just the last who commited changes!
                    //finally: shrink the temp tables to the minimum and reindex
                    if (TPS_Logger.isLogging(HierarchyLogLevel.THREAD, SeverenceLogLevel.DEBUG)) {
                        TPS_Logger.log(HierarchyLogLevel.THREAD, SeverenceLogLevel.DEBUG,
                                "Vacuuming temporary tables for locations in simulation " +
                                        this.PM.getParameters().getString(ParamString.RUN_IDENTIFIER));
                    }
                    query = "VACUUM FULL temp.locations_" + this.PM.getParameters().getString(
                            ParamString.RUN_IDENTIFIER);
                    PM.execute(query);
                    if (TPS_Logger.isLogging(HierarchyLogLevel.THREAD, SeverenceLogLevel.DEBUG)) {
                        TPS_Logger.log(HierarchyLogLevel.THREAD, SeverenceLogLevel.DEBUG,
                                "Vacuuming temporary tables for households in simulation " +
                                        this.PM.getParameters().getString(ParamString.RUN_IDENTIFIER));
                    }
                    query = "VACUUM FULL temp.households_" + this.PM.getParameters().getString(
                            ParamString.RUN_IDENTIFIER);
                    PM.execute(query);
                    if (TPS_Logger.isLogging(HierarchyLogLevel.THREAD, SeverenceLogLevel.DEBUG)) {
                        TPS_Logger.log(HierarchyLogLevel.THREAD, SeverenceLogLevel.DEBUG,
                                "Reindexing temporary tables for locations in simulation " +
                                        this.PM.getParameters().getString(ParamString.RUN_IDENTIFIER));
                    }
                    query = "REINDEX TABLE temp.locations_" + this.PM.getParameters().getString(
                            ParamString.RUN_IDENTIFIER);
                    PM.execute(query);
                    if (TPS_Logger.isLogging(HierarchyLogLevel.THREAD, SeverenceLogLevel.DEBUG)) {
                        TPS_Logger.log(HierarchyLogLevel.THREAD, SeverenceLogLevel.DEBUG,
                                "Reindexing temporary tables for households in simulation " +
                                        this.PM.getParameters().getString(ParamString.RUN_IDENTIFIER));
                    }
                    query = "REINDEX TABLE temp.households_" + this.PM.getParameters().getString(
                            ParamString.RUN_IDENTIFIER);
                    PM.execute(query);


                    //check if sumo has to be started and tell it the status table
                    if (this.PM.getParameters().getIntValue(ParamValue.ITERATION) < this.PM.getParameters().getIntValue(
                            ParamValue.MAX_SUMO_ITERATION)) {

                        //TODO: hack to make sumo iterations shorter!
                        PM.getDbConnector().updateSingleParameter(
                                this.PM.getParameters().getString(ParamString.RUN_IDENTIFIER),
                                ParamValue.DB_HH_SAMPLE_SIZE.name(), "1.0");

                        //somehow the Interation becomes 0 everytime
                        //I suspect "Generate default values" to set it to 0
                        // so read the "real value" back from the DB
                        String iterS = PM.getDbConnector().readSingleParameter(
                                this.PM.getParameters().getString(ParamString.RUN_IDENTIFIER),
                                ParamValue.ITERATION.name());

                        query = "INSERT INTO " + this.PM.getParameters().getString(ParamString.DB_TABLE_SUMO_STATUS) +
                                " VALUES ('" + this.PM.getParameters().getString(ParamString.RUN_IDENTIFIER) + "'," +
                                iterS + ",now(),'> TAPAS finished, triggering SUMO for simulation " +
                                this.PM.getParameters().getString(ParamString.RUN_IDENTIFIER) + "','pending')";
                        PM.execute(query);
                    }
                }
            }
        } catch (SQLException e) {
            TPS_Logger.log(HierarchyLogLevel.THREAD, SeverenceLogLevel.ERROR,
                    "error during one of th sqls: " + query + "\n or: " + query2, e);
            TPS_Logger.log(HierarchyLogLevel.THREAD, SeverenceLogLevel.ERROR, "next exception:", e.getNextException());
        }
    }


}
