/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.tools;

import de.dlr.ivf.tapas.loc.Locatable;
import de.dlr.ivf.tapas.loc.TPS_Block;
import de.dlr.ivf.tapas.loc.TPS_Coordinate;
import de.dlr.ivf.tapas.loc.TPS_TrafficAnalysisZone;
import de.dlr.ivf.tapas.model.TPS_Geometrics;
import de.dlr.ivf.tapas.persistence.db.TPS_DB_IO;
import de.dlr.ivf.tapas.tools.persitence.db.TPS_BasicConnectionClass;
import de.dlr.ivf.tapas.model.parameter.ParamValue;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class TPS_ECommerceEstimator extends TPS_BasicConnectionClass {

    double[][] beeline;
    double[][] dist;
    double[][][] tt;
    double prob = 1;
    int[] numShoping = {0, 0, 0, 0, 0, 0, 0, 0};
    int[] numSingleTripShoping = {0, 0, 0, 0, 0, 0, 0, 0};
    int[] numMultiTripShoping = {0, 0, 0, 0, 0, 0, 0, 0};
    double[] distShopping = {0, 0, 0, 0, 0, 0, 0, 0};
    double[] timeShopping = {0, 0, 0, 0, 0, 0, 0, 0};
    Map<Integer, MinimalLocation> ecomHH = new HashMap<>();
    Set<Integer> activities = new TreeSet<>();
    List<MinimalLocation> tazes = new ArrayList<>();
    List<ECommerceTrip> eComTrips = new LinkedList<>();

    @SuppressWarnings("unused")
    public static void main(String[] args) {

        //these are the parameters which should go into the command line parameter call
        String tazTable = "core.berlin_taz_1223";
        String matrixTable = "core.berlin_matrices";
        String tripPrefix = "berlin_trips_";
        String[] sims = {"2019y_01m_09d_11h_15m_27s_296ms", "2019y_01m_09d_11h_15m_27s_295ms"};

        String trips;
        String distMatrix = "WALK_IHK_DIST";
        double[] probabilities = {0.1, 0.03};
        String[] ttMatrices = {"WALK_IHK_TT", //walk times
                "BIKE_IHK_TT", //bike times
                "CAR_IHK_TT", //car times
                "CAR_IHK_TT", //passenger times
                "CAR_IHK_TT", //taxi times
                "PT_VISUM_1223_2030_NEUE_ANBINDUNG_SUM_TT", //public transport time
                "CAR_IHK_TT" //car sharing times
        };

        String bzrTable = "urmo.bzr";
        String suffix = "_2015_ref";
        String outputDirectory = "T:\\Runs\\Urmo\\Analysen\\";
        String outputTable = "core.berlin_ec_trips";

        for (int i = 0; i < sims.length; ++i) {
            // now the programm

            System.out.println("Processing " + sims[i]);
            TPS_ECommerceEstimator worker = new TPS_ECommerceEstimator();

            trips = tripPrefix + sims[i];
            worker.getParameters().setValue(ParamValue.MIN_DIST, 50);
            worker.prob = probabilities[i];
            //worker.addActivity(50); //general shopping
            worker.addActivity(51); //short term shopping
            //worker.addActivity(52); //mid term shopping
            //worker.addActivity(53); //long term shopping
            System.out.println("Loading tazes " + tazTable);
            worker.loadTAZ(tazTable);
            System.out.println("Loading matrices");
            worker.loadMatrices(distMatrix, ttMatrices, matrixTable);
            System.out.println("Collecting trips " + trips);
            worker.collectEComTrips(trips);
            //System.out.println("Add bzr infos "+bzrTable);
            //worker.addBRZInfos2EComHouseholds(bzrTable);
            //worker.writeEComTripsToFile(outputDirectory+"ecomTrips"+suffix+".csv");
            //worker.writeEComHouseholds(outputDirectory+"ecomHH"+suffix+".csv");
            //System.out.println("Storing modded trips in db "+trips);
            //worker.updateEComTripsInDB(trips);
            //worker.writeEComTripsToDB(outputTable+suffix);


            System.out.println("Walk = 0");
            System.out.println("Bike = 1");
            System.out.println("Car = 2");
            System.out.println("Passenger = 3");
            System.out.println("Taxi = 4");
            System.out.println("PT = 5");
            System.out.println("CarSh = 6");
            System.out.println("Total = 7");
            System.out.println("\n Normal shoppping trips:");
            for (int j = 0; j < 8; ++j) {
                System.out.format("Mode %d:  (num, dist in km , tt in h): \t%12d\t%12d\t%12d\n", j,
                        worker.numShoping[j], (int) (worker.distShopping[j] / 1000.0),
                        (int) (worker.timeShopping[j] / 3600));
            }

            for (int j = 0; j < 8; ++j) {
                System.out.format("Mode %d:  (num single, num multiple): \t%12d\t%12d\n", j,
                        worker.numSingleTripShoping[j], worker.numMultiTripShoping[j]);
            }

            int[] numECom = {0, 0, 0, 0, 0, 0, 0, 0};
            double[] distECom = {0, 0, 0, 0, 0, 0, 0, 0};
            double[] timeECom = {0, 0, 0, 0, 0, 0, 0, 0};
            for (ECommerceTrip trip : worker.eComTrips) {
                numECom[trip.newMode] += 1;
                distECom[trip.newMode] += trip.savedDist;
                timeECom[trip.newMode] += trip.savedTime;
                //totals
                numECom[7] += 1;
                distECom[7] += trip.savedDist;
                timeECom[7] += trip.savedTime;
            }
            System.out.println("\nECom-trip savings");
            for (int j = 0; j < 8; ++j) {
                System.out.format("Mode %d:  (num, dist in km , tt in h): \t%12d\t%12d\t%12d\n", j, numECom[j],
                        (int) (distECom[j] / 1000.0), (int) (timeECom[j] / 3600));
            }
        }
    }

    public void addActivity(int activity) {
        this.activities.add(activity);
    }

    //append the bzr-infos to all trips
    public void addBRZInfos2EComHouseholds(String bzrTable) {
//		for(MinimalLocation hh: this.ecomHH.values()){
//			this.appendBZRInfo(bzrTable, hh);
//		}


        for (ECommerceTrip e : this.eComTrips) {
            if (e.household.bzrID == -1) {
                this.appendBZRInfo(bzrTable, e.household);
            }
        }
    }

    public void appendBZRInfo(String bzrTable, MinimalLocation loc) {
        String query;
        ResultSet rs;
        try {

            query = "SELECT bzr_id from " + bzrTable + " where st_within(st_transform(st_setsrid(" + "st_makepoint(" +
                    loc.loc.getValue(0) + ", " + loc.loc.getValue(1) + "),4326),25833), the_geom)";
            rs = this.dbCon.executeQuery(query, this);
            if (rs.next()) {
                String val = rs.getString("bzr_id");
                loc.bzrID = Integer.parseInt(val);
            } else {
                rs.close();
                //try a 50 meter buffer
                query = "SELECT bzr_id from " + bzrTable + " where st_within(st_transform(st_setsrid(" +
                        "st_makepoint(" + loc.loc.getValue(0) + ", " + loc.loc.getValue(1) +
                        "),4326),25833), st_buffer(the_geom,50))";
                rs = this.dbCon.executeQuery(query, this);
                if (rs.next()) {
                    String val = rs.getString("bzr_id");
                    loc.bzrID = Integer.parseInt(val);
                } else {
                    System.out.println(query);
                }

            }
            rs.close();

        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public double calcDiffDist(ECommerceTrip trip) {
        return this.getCorrectedMatrixVal(trip.start, trip.end, this.dist) - this.getCorrectedMatrixVal(trip.start,
                trip.middle, this.dist) - this.getCorrectedMatrixVal(trip.middle, trip.end, this.dist);
    }

    public double calcDiffTT(ECommerceTrip trip, int mode) {
        return this.getCorrectedMatrixVal(trip.start, trip.end, this.tt[mode]) - this.getCorrectedMatrixVal(trip.start,
                trip.middle, this.tt[trip.mode[0]]) - this.getCorrectedMatrixVal(trip.middle, trip.end,
                this.tt[trip.mode[1]]);
    }

    public void collectEComTrips(String table) {
        String query;
        ResultSet rs;
        try {
            int currentHHID = -1, currentPID = -1;
            int p_id, hh_id, loc_id_start, taz_id_start, loc_id_end, taz_id_end, mode, activity, taz_house = 0, hh_loc_id = 0, start_time_min;
            double lon_start, lat_start, lon_end, lat_end, lon_hh = 0, lat_hh = 0;
            ECommerceTrip trip = null;
            boolean lastWasShopping = false;
            //boolean added;
            this.eComTrips.clear();
            this.ecomHH.clear();
            int maxChunk = 10;
            for (int i = 0; i < maxChunk; i++) {
                System.out.print(".");
                query = "SELECT p_id, hh_id, start_time_min, loc_id_start, taz_id_start, lon_start, lat_start, loc_id_end, taz_id_end, lon_end, lat_end, mode, activity from " +
                        table + " where hh_id%" + maxChunk + " = " + i + " order by hh_id, p_id, start_time_min ";
                rs = this.dbCon.executeQuery(query, this);
                while (rs.next()) {
                    p_id = rs.getInt("p_id");
                    hh_id = rs.getInt("hh_id");
                    start_time_min = rs.getInt("start_time_min");
                    loc_id_start = rs.getInt("loc_id_start");
                    taz_id_start = rs.getInt("taz_id_start");
                    loc_id_end = rs.getInt("loc_id_end");
                    taz_id_end = rs.getInt("taz_id_end");
                    mode = rs.getInt("mode");
                    activity = rs.getInt("activity");
                    lon_start = rs.getDouble("lon_start");
                    lat_start = rs.getDouble("lat_start");
                    lon_end = rs.getDouble("lon_end");
                    lat_end = rs.getDouble("lat_end");
                    //added =false;

                    if (p_id == currentPID && hh_id == currentHHID) {
                        if (lastWasShopping) {
                            //potential ecommerce trip
                            if (trip.middle.id != loc_id_start) {
                                System.out.println("Ohoh! Location id missmatch!");
                            }
                            trip.end = new MinimalLocation(loc_id_end, taz_id_end, lon_end, lat_end);
                            trip.mode[1] = mode;
                            trip.startTimes[1] = start_time_min;
                            double dist = this.getCorrectedMatrixVal(trip.middle, trip.end, this.dist);
                            double time = this.getCorrectedMatrixVal(trip.middle, trip.end, this.tt[mode]);
                            if (this.isShopping(activity)) {
                                this.distShopping[mode] += dist;
                                this.distShopping[7] += dist;
                                this.timeShopping[mode] += time;
                                this.timeShopping[7] += time;
                            }

                            if (trip.end.id == trip.start.id) { //single route
                                this.numSingleTripShoping[mode]++;
                                this.numSingleTripShoping[7]++;
                            } else {
                                this.numMultiTripShoping[mode]++;
                                this.numMultiTripShoping[7]++;
                            }

                            if (this.isECommerceTrip()) {
                                trip.newMode = Math.max(trip.mode[0],
                                        trip.mode[1]); //fortunately we have a hierarchy here!
                                trip.savedDist = this.calcDiffDist(trip);
                                trip.savedTime = this.calcDiffTT(trip, trip.newMode);
                                this.eComTrips.add(trip);
                                MinimalLocation hh = this.ecomHH.get(trip.household.id);
                                if (hh == null) {
                                    this.ecomHH.put(trip.household.id, trip.household);
                                } else {
                                    hh.cappa++;
                                }
                            }
                        }
                    } else {
                        //new person!
                        currentHHID = hh_id;
                        currentPID = p_id;
                        taz_house = taz_id_start;
                        hh_loc_id = loc_id_start;
                        lon_hh = lon_start;
                        lat_hh = lat_start;
                    }
                    if (this.isShopping(activity)) {
                        lastWasShopping = true;
                        trip = new ECommerceTrip();
                        trip.hhID = hh_id;
                        trip.pID = p_id;
                        trip.household = new MinimalLocation(hh_loc_id, taz_house, lon_hh, lat_hh);
                        trip.start = new MinimalLocation(loc_id_start, taz_id_start, lon_start, lat_start);
                        trip.middle = new MinimalLocation(loc_id_end, taz_id_end, lon_end, lat_end);
                        //if(!added) {
                        this.numShoping[mode] += 1;
                        this.numShoping[7] += 1;
                        double dist = this.getCorrectedMatrixVal(trip.start, trip.middle, this.dist);
                        double time = this.getCorrectedMatrixVal(trip.start, trip.middle, this.tt[mode]);
                        this.distShopping[mode] += dist;
                        this.distShopping[7] += dist;
                        this.timeShopping[mode] += time;
                        this.timeShopping[7] += time;
                        trip.mode[0] = mode;
                        trip.startTimes[0] = start_time_min;

                        //}
                    } else {
                        lastWasShopping = false;
                        trip = null;
                    }


                }
                rs.close();
            }
            System.out.println();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public double getCorrectedMatrixVal(MinimalLocation start, MinimalLocation end, double[][] matrix) {
        int startTAZIndex = start.getTAZId() - 1;
        int endTAZIndex = end.getTAZId() - 1;
        double result;
        if (startTAZIndex != endTAZIndex) {
            double beelineDistanceLoc = TPS_Geometrics.getDistance(start, end,
                    this.getParameters().getDoubleValue(ParamValue.MIN_DIST));
            double factor = beelineDistanceLoc / beeline[startTAZIndex][endTAZIndex];
            result = matrix[startTAZIndex][endTAZIndex] * factor;
        } else {
            result = matrix[startTAZIndex][endTAZIndex];
        }
        return result;
    }

    public boolean isECommerceTrip() {
        return Math.random() <= this.prob;
    }

    public boolean isShopping(int activity) {
        return this.activities.contains(activity);
    }

    public void loadMatrices(String dist, String[] tts, String table) {
        this.dist = this.loadMatrix(table, dist);
        this.tt = new double[tts.length][][];
        for (int i = 0; i < tts.length; ++i) {
            if (tts[i] != null && !tts[i].equals("")) {
                this.tt[i] = this.loadMatrix(table, tts[i]);
            }
        }
    }

    public double[][] loadMatrix(String table, String name) {
        String query;
        ResultSet rs;
        double[][] returnVal = new double[0][0];
        try {
            query = "SELECT matrix_values from " + table + " where matrix_name = '" + name + "'";
            rs = this.dbCon.executeQuery(query, this);
            if (rs.next()) {
                int[] val = TPS_DB_IO.extractIntArray(rs, "matrix_values");
                returnVal = array1Dto2D(val);
            }
            rs.close();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return returnVal;
    }

    public void loadTAZ(String table) {
        String query;
        ResultSet rs;
        try {
            query = "SELECT taz_id, st_x(taz_coordinate), st_y(taz_coordinate) from " + table + " order by taz_id";
            rs = this.dbCon.executeQuery(query, this);
            while (rs.next()) {
                int id = rs.getInt("taz_id");
                double x = rs.getDouble("st_x");
                double y = rs.getDouble("st_y");
                MinimalLocation newTaz = new MinimalLocation(-id, id, x, y);
                tazes.add(newTaz);
            }
            rs.close();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        this.beeline = new double[tazes.size()][tazes.size()];
        for (MinimalLocation tazfrom : tazes) {
            for (MinimalLocation tazto : tazes) {
                double dist = TPS_Geometrics.getDistance(tazfrom.getCoordinate(), tazto.getCoordinate(),
                        this.getParameters().getDoubleValue(ParamValue.MIN_DIST));
                this.beeline[tazfrom.getTAZId() - 1][tazto.getTAZId() - 1] = dist;
            }
        }
    }

    public void updateEComTripsInDB(String tablename) {
        String queryUpdate = "", queryDelete = "";
        try {


            queryUpdate = "UPDATE " + tablename + " SET loc_id_start =?, taz_id_start =?, lon_start =?, lat_start =?," +
                    " mode = ? , travel_time_sec =?, distance_real_m = ?, distance_bl_m =? "//, start_time_min =? "
                    + " WHERE p_id = ? AND hh_id = ? AND start_time_min = ?";
            queryDelete = "DELETE FROM " + tablename + " WHERE p_id = ? AND hh_id = ? AND start_time_min = ?";
            PreparedStatement prepStUpdate = this.dbCon.getConnection(this).prepareStatement(queryUpdate);
            PreparedStatement prepStDelete = this.dbCon.getConnection(this).prepareStatement(queryDelete);
            int modifycount = 0, maxModify = 10000, pos;
            for (ECommerceTrip trip : this.eComTrips) {
                if (trip.start.id == trip.end.id) { //delete both trips!
                    pos = 1;
                    prepStDelete.setInt(pos++, trip.pID);
                    prepStDelete.setInt(pos++, trip.hhID);
                    prepStDelete.setInt(pos++, trip.startTimes[0]);
                    prepStDelete.addBatch();
                    pos = 1;
                    prepStDelete.setInt(pos++, trip.pID);
                    prepStDelete.setInt(pos++, trip.hhID);
                    prepStDelete.setInt(pos++, trip.startTimes[1]);
                    prepStDelete.addBatch();
                } else {
                    //delete the first trip
                    pos = 1;
                    prepStDelete.setInt(pos++, trip.pID);
                    prepStDelete.setInt(pos++, trip.hhID);
                    prepStDelete.setInt(pos++, trip.startTimes[0]);
                    prepStDelete.addBatch();
                    //modify the second trip  to become the first
                    pos = 1;
                    prepStUpdate.setInt(pos++, trip.start.id);
                    prepStUpdate.setInt(pos++, trip.start.tazID);
                    prepStUpdate.setDouble(pos++, trip.start.getCoordinate().getValue(0));
                    prepStUpdate.setDouble(pos++, trip.start.getCoordinate().getValue(1));
                    prepStUpdate.setInt(pos++, trip.newMode);
                    prepStUpdate.setDouble(pos++,
                            this.getCorrectedMatrixVal(trip.start, trip.end, this.tt[trip.newMode])); //travel time sec
                    prepStUpdate.setDouble(pos++,
                            this.getCorrectedMatrixVal(trip.start, trip.end, this.dist)); //dist real
                    prepStUpdate.setDouble(pos++, TPS_Geometrics.getDistance(trip.start, trip.end,
                            this.getParameters().getDoubleValue(ParamValue.MIN_DIST))); //dist bl
                    //prepStUpdate.setInt(pos++, trip.startTimes[0]); // new start time!
                    prepStUpdate.setInt(pos++, trip.pID);
                    prepStUpdate.setInt(pos++, trip.hhID);
                    prepStUpdate.setInt(pos++, trip.startTimes[1]);
                    prepStUpdate.addBatch();
                }

                //make the commit-size smaller
                modifycount++;
                if (modifycount >= maxModify) {
                    modifycount = 0;
                    System.out.print(".");
                    prepStDelete
                            .executeBatch(); //delete first: Otherwise we might have double entries for the start_time_min
                    prepStUpdate.executeBatch();
                }
            }
            System.out.println(".");
            prepStDelete.executeBatch();  //delete first: Otherwise we might have double entries for the start_time_min
            prepStUpdate.executeBatch();
        } catch (SQLException e) {
            System.err.println("Error in sqlstatement: " + queryUpdate);
            System.err.println("Error in sqlstatement: " + queryDelete);
            e.printStackTrace();
            e.getNextException().printStackTrace();
        }
    }

    public void writeEComHouseholds(String filename) {
        try {
            FileWriter writer = new FileWriter(filename);
            writer.append("hh_id\ttaz\t\tbzrnum\tlon\tlat\n");
            //iterate over households
            for (MinimalLocation hh : this.ecomHH.values()) {
                writer.append(
                        hh.id + "\t" + hh.tazID + "\t" + hh.bzrID + "\t" + hh.cappa + "\t" + hh.loc.getValue(0) + "\t" +
                                hh.loc.getValue(1) + "\n");
            }

            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeEComTripsToDB(String tablename) {
        String query = "";
        try {

            query = "DROP TABLE IF EXISTS " + tablename;
            this.dbCon.execute(query, this);
            String tableWithoutSchema = tablename.substring(tablename.indexOf(".") + 1);
            query = "CREATE TABLE " + tablename + " (" + "  gid serial NOT NULL," + "  hh_id integer," +
                    "  taz integer," + "  bzr integer," + "  mode integer," + "  dist_m double precision," +
                    "  time_s double precision," + "  lon double precision," + "  lat double precision," +
                    "  CONSTRAINT " + tableWithoutSchema + "_pkey PRIMARY KEY (gid)" + ")" + "WITH (" + "  OIDS=FALSE" +
                    ");";
            this.dbCon.execute(query, this);

            query = "INSERT INTO " + tablename + " (" + "            hh_id, taz, bzr, mode, dist_m, time_s, lon, lat)" +
                    "    VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement prepSt = this.dbCon.getConnection(this).prepareStatement(query);
            int pos;
            for (ECommerceTrip trip : this.eComTrips) {
                pos = 1;
                prepSt.setInt(pos++, trip.household.id);
                prepSt.setInt(pos++, trip.household.tazID);
                prepSt.setInt(pos++, trip.household.bzrID);
                prepSt.setInt(pos++, trip.newMode);
                prepSt.setDouble(pos++, trip.savedDist);
                prepSt.setDouble(pos++, trip.savedTime);
                prepSt.setDouble(pos++, trip.household.loc.getValue(0));
                prepSt.setDouble(pos++, trip.household.loc.getValue(1));
                prepSt.addBatch();
            }
            prepSt.executeBatch();
        } catch (SQLException e) {
            System.err.println("Error in sqlstatement: " + query);
            e.printStackTrace();
            e.getNextException().printStackTrace();
        }
    }

    public void writeEComTripsToFile(String filename) {
        try {
            FileWriter writer = new FileWriter(filename);
            writer.append("hh_id\ttaz\tbzr\tmode\tdist\ttime\tlon\tlat\n");
            //iterate over households
            for (ECommerceTrip trip : this.eComTrips) {
                writer.append(trip.household.id + "\t" + trip.household.tazID + "\t" + trip.household.bzrID + "\t" +
                        trip.newMode + "\t" + trip.savedDist + "\t" + trip.savedTime + "\t" +
                        trip.household.loc.getValue(0) + "\t" + trip.household.loc.getValue(1) + "\n");
            }

            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    class MinimalLocation implements Locatable {
        int id, tazID, cappa = 1;
        int bzrID = -1; //id for the "Bezirksregion"
        TPS_Coordinate loc;


        public MinimalLocation(int id, int taz, double x, double y) {
            this.id = id;
            this.tazID = taz;
            this.loc = new TPS_Coordinate(x, y);
        }

        @Override
        public TPS_Block getBlock() {
            return null;
        }

        @Override
        public TPS_Coordinate getCoordinate() {
            return loc;
        }

        public int getId() {
            return id;
        }

        @Override
        public int getTAZId() {
            return tazID;
        }

        @Override
        public TPS_TrafficAnalysisZone getTrafficAnalysisZone() {
            return null;
        }

        @Override
        public boolean hasBlock() {
            return false;
        }
    }

    class ECommerceTrip implements Comparable<ECommerceTrip> {
        MinimalLocation household;
        MinimalLocation start, middle, end;
        int[] mode = new int[2];
        int[] startTimes = new int[2];
        int hhID;
        int pID;
        int newMode;
        double savedTime, savedDist;

        @Override
        public int compareTo(ECommerceTrip arg0) {
            // TODO Auto-generated method stub
            return arg0.startTimes[0] - this.startTimes[0];
        }
    }
}
