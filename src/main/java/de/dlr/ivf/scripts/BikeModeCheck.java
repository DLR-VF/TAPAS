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
import java.util.LinkedList;
import java.util.List;

public class BikeModeCheck extends TPS_BasicConnectionClass {

    List<Trip> defectTrips = new LinkedList<>();
    int tripCounter = 0;

    /**
     * @param args
     */
    public static void main(String[] args) {
        BikeModeCheck worker = new BikeModeCheck();
        worker.readTrips(args[0]);
        for (Trip tr : worker.defectTrips) {
            System.out.println(tr.getpID() + ";" + tr.getHhID() + ";" + tr.getStartTime());
        }
        System.out.println("Total defectTrips: " + worker.defectTrips.size() + " of " + worker.tripCounter + " trips");
    }

    public void readTrips(String key) {
        String query = "";
        try {
            boolean recordsFound;
            int step = 0;
            int chunk = 1000000;
            int pID = -1, hhID = -1, homeID = -1;
            boolean bikeForbidden = false;
            do {
                recordsFound = false;
                query = "SELECT p_id, hh_id, start_time_min, mode, loc_id_start FROM " + key +
                        " ORDER BY hh_id, p_id, start_time_min LIMIT " + chunk + " OFFSET " + (step * chunk);
                ResultSet rs = dbCon.executeQuery(query, this);
                step++;
                while (rs.next()) {
                    recordsFound = true;
                    int actPID = rs.getInt("p_id");
                    int actHHID = rs.getInt("hh_id");
                    int actMode = rs.getInt("mode");
                    int modeBike = 1;
                    if (pID == actPID && hhID == actHHID) {
                        if (rs.getInt("loc_id_start") == homeID) { //trip starts at home
                            bikeForbidden = actMode != modeBike; //is this a Bike?
                            this.tripCounter++;
                        } else if (actMode == modeBike && bikeForbidden) { //illegal use of bike?
                            this.defectTrips.add(new Trip(actPID, actHHID, rs.getInt("start_time_min")));
                        }
                    } else { //new person
                        this.tripCounter++;
                        pID = actPID;
                        hhID = actHHID;
                        bikeForbidden = actMode != modeBike; //is this a Bike?
                        homeID = rs.getInt("loc_id_start"); // all trips start at home
                    }
                }
                rs.close();
            } while (recordsFound);

        } catch (SQLException e) {
            System.err.println(this.getClass().getCanonicalName() + " readTrips: SQL-Error during statement: " + query);
            e.printStackTrace();
        }
    }

    public class Trip {
        int pID, hhID, startTime;

        public Trip(int pID, int hhID, int startTime) {
            this.pID = pID;
            this.hhID = hhID;
            this.startTime = startTime;
        }

        /**
         * @return the hhID
         */
        public int getHhID() {
            return hhID;
        }

        /**
         * @return the startTime
         */
        public int getStartTime() {
            return startTime;
        }

        /**
         * @return the pID
         */
        public int getpID() {
            return pID;
        }
    }

}
