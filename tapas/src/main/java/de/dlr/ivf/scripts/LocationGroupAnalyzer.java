/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.scripts;

import de.dlr.ivf.tapas.tools.persitence.db.TPS_BasicConnectionClass;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map.Entry;

public class LocationGroupAnalyzer extends TPS_BasicConnectionClass {

    HashMap<Integer, Integer> locations = new HashMap<>();
    HashMap<String, Integer> groupStatistics = new HashMap<>();

    /**
     * @param args
     */
    public static void main(String[] args) {
        LocationGroupAnalyzer worker = new LocationGroupAnalyzer();
        worker.readLocations("core.berlin_locations");
        worker.analyzeTrips("berlin_trips_2012y_09m_28d_12h_59m_12s_969ms");
        worker.printStatistics();

    }

    public void analyzeTrips(String key) {
        String query = "";
        try {
            boolean recordsFound;
            int step = 0;
            int chunk = 1000000;
            int pID = -1, hhID = -1;
            int accMode = -1, betweenMode = -1, egrMode, groupCount = 0;
            int homeId = 0;
            boolean enteringGroup = false;

            do {
                recordsFound = false;
                query = "SELECT p_id, hh_id, start_time_min, mode, loc_id_start, loc_id_end FROM " + key +
                        " ORDER BY hh_id, p_id, start_time_min LIMIT " + chunk + " OFFSET " + (step * chunk);
                ResultSet rs = dbCon.executeQuery(query, this);
                step++;
                while (rs.next()) {
                    recordsFound = true;
                    int actPID = rs.getInt("p_id");
                    int actHHID = rs.getInt("hh_id");
                    int actMode = rs.getInt("mode");
                    int locStart = rs.getInt("loc_id_start");
                    int locEnd = rs.getInt("loc_id_end");
                    if (pID == actPID && hhID == actHHID && locStart != homeId) {
                        if (locations.get(locStart).equals(locations.get(locEnd)) && locations.get(locStart) !=
                                null) { // is this the same location group?
                            if (!enteringGroup) { //first enter
                                betweenMode = actMode;
                                enteringGroup = true;
                                groupCount = 1;
                            } else {
                                if (actMode != betweenMode) {
                                    System.out.println(
                                            "Mode change: " + actMode + " expected: " + betweenMode + " Loc start: " +
                                                    locStart + " Loc end: " + locEnd);
                                } else {
                                    groupCount++;
                                }
                            }
                        } else {
                            if (enteringGroup) { // did we enter a group?
                                egrMode = actMode;
                                String keyVal = accMode + "-" + betweenMode + "-" + egrMode;
                                if (groupStatistics.containsKey(keyVal)) groupCount += groupStatistics.get(keyVal);
                                groupStatistics.put(keyVal, groupCount);
                                groupCount = 0;
                                enteringGroup = false;
                            }
                            accMode = actMode;
                        }
                    } else { //new person or trip from home
                        if (enteringGroup) { // did we enter a group?
                            egrMode = actMode;
                            String keyVal = accMode + "-" + betweenMode + "-" + egrMode;
                            if (groupStatistics.containsKey(keyVal)) groupCount += groupStatistics.get(keyVal);
                            groupStatistics.put(keyVal, groupCount);
                        }
                        pID = actPID;
                        hhID = actHHID;
                        homeId = locStart;
                        accMode = actMode;
                        groupCount = 0;
                        enteringGroup = false;
                    }
                }
                rs.close();
            } while (recordsFound);

        } catch (SQLException e) {
            System.err.println(
                    this.getClass().getCanonicalName() + " analyzeTrips: SQL-Error during statement: " + query);
            e.printStackTrace();
        }
    }

    public void printStatistics() {
        int strangeModeSets = 0;
        int illegalCars = 0;
        int illegalBikes = 0;
        for (Entry<String, Integer> entries : this.groupStatistics.entrySet()) {
            System.out.println("Relation " + entries.getKey() + " count " + entries.getValue());
            if (entries.getKey().charAt(2) != '0') strangeModeSets += entries.getValue();
            if ((entries.getKey().charAt(0) == '1' && entries.getKey().charAt(4) != '1') || (entries.getKey().charAt(
                    4) == '1' && entries.getKey().charAt(0) != '1')) illegalBikes += entries.getValue();
            if ((entries.getKey().charAt(0) == '2' && entries.getKey().charAt(4) != '2') || (entries.getKey().charAt(
                    4) == '2' && entries.getKey().charAt(0) != '2')) illegalCars += entries.getValue();
        }
        System.out.println("Sum of non walking trips in same location groups:" + strangeModeSets);
        System.out.println("Sum of illegal bike trips in same location groups:" + illegalBikes);
        System.out.println("Sum of illegal car trips in same location groups:" + illegalCars);

    }

    public void readLocations(String table) {
        String query = "";
        try {

            query = "SELECT loc_id, loc_group_id FROM " + table + " WHERE loc_group_id>=0";
            ResultSet rs = dbCon.executeQuery(query, this);
            while (rs.next()) {
                locations.put(rs.getInt("loc_id"), rs.getInt("loc_group_id"));
            }
            rs.close();
        } catch (SQLException e) {
            System.err.println(
                    this.getClass().getCanonicalName() + " readLocations: SQL-Error during statement: " + query);
            e.printStackTrace();
        }
    }

}
