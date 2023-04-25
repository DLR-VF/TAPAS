/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.tools;

import de.dlr.ivf.tapas.tools.persitence.db.TPS_BasicConnectionClass;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.Map.Entry;

public class TPS_PTStopWeightCalculator extends TPS_BasicConnectionClass {
    public Map<Integer, List<PTStop>> taz2StopMap = new HashMap<>();
    public Map<Integer, List<Household>> taz2HouseholdMap = new HashMap<>();
    public Map<String, List<Integer>> stops2TAZMap = new HashMap<>();

    public static void main(String[] args) {
        String stopTable = "core.berlin_pt_stop_points";
        String tazTable = "core.berlin_taz_1223_umland";
        String hhTable = "core.berlin_households_1223";
        String hhKey = "IHK_MID2008_Y2030_REF";
        //System.out.println("Radius\tAngebunden\tHochleistungs ÖV\tS/U-Bahn+Zug\tTram\tBRT-Linien");
        int i = 200;
        //for(i=0; i<301; i+=25) {
        TPS_PTStopWeightCalculator worker = new TPS_PTStopWeightCalculator();
        worker.loadPTStops(stopTable, tazTable, i);
        worker.loadHouseholds(hhTable, hhKey);
        worker.calcWeights(PTLineType.BRT); // calc only HPC nodes: This puts extra weights on the HPC-connections
        worker.calcWeights(PTLineType.Unknown); // calc all nodes
        worker.savePTStopsPerTAZToFile("T:\\runs\\evoBus\\Kenngroessen\\Anbindungen\\anbindung_2_07_09_" + i + ".csv",
                1.4, 10);
        //worker.printTAZPerPTStop();
        //System.out.print(i+"\t");
        worker.printPTStatisticsPerTAZ();
        //}
        worker.updateWeights(stopTable);
    }

    /**
     * This method takes all households and searches for the closest pt stop in the same taz. Then it adds the number of persons to this pt stop.
     */
    public void calcWeights(PTLineType minLine) {
        List<Household> workingListHouseholds;
        List<PTStop> worlingListPTStops;
        for (Entry<Integer, List<Household>> hhs : this.taz2HouseholdMap.entrySet()) {
            workingListHouseholds = hhs.getValue();

            worlingListPTStops = this.taz2StopMap.get(hhs.getKey());
            if (worlingListPTStops == null) { //no pt stop!
                continue;
            }
            if (workingListHouseholds == null) { // no households in this taz
                continue;
            }
            for (Household hh : workingListHouseholds) {
                PTStop shortestStop = null;
                double minDist = Double.MAX_VALUE, dist;
                for (PTStop stop : worlingListPTStops) {
                    dist = TPS_Geometrics.getDistance(hh.lat, hh.lon, stop.lat, stop.lon);
                    if (dist < minDist && stop.hasQualityOfServiceLevel(minLine)) {

                        minDist = dist;
                        shortestStop = stop;
                    }
                }
                if (shortestStop != null) { // fopund something?
                    // now we have the shortest stop!
                    shortestStop.weight += hh.persons;
                    shortestStop.dist += minDist * hh.persons;
                }
            }
        }
    }

    /**
     * This methods loads the households from the given table with the given key and groups them into tazes.
     *
     * @param table
     * @param key
     */
    public void loadHouseholds(String table, String key) {
        String query = "";

        try {
            query = "select hh_id, hh_taz_id, hh_persons, st_x(st_transform(hh_coordinate,4326)) as lon, st_y(st_transform(hh_coordinate,4326)) as lat from " +
                    table + " where hh_key = '" + key + "'";
            ResultSet rs = this.dbCon.executeQuery(query, this);
            while (rs.next()) {

                Household newHousehold = new Household();
                newHousehold.id = rs.getInt("hh_id");
                newHousehold.taz = rs.getInt("hh_taz_id");
                newHousehold.lon = rs.getDouble("lon");
                newHousehold.lat = rs.getDouble("lat");
                newHousehold.persons = rs.getInt("hh_persons");

                //get the household list for this taz and add this household to it
                List<Household> workingList = taz2HouseholdMap.get(newHousehold.taz);
                if (workingList == null) { //new taz
                    workingList = new LinkedList<>();
                }
                workingList.add(newHousehold);
                taz2HouseholdMap.put(newHousehold.taz, workingList);

            }
        } catch (SQLException e) {
            System.err.println("Error in sqlstatement: " + query);
            e.printStackTrace();
        }
    }

    /**
     * Load the pt stops from the visum export. This method loads the data from the given table.
     * It converts the coordinates and prepares the lines stopping at this stop.
     * Finally, it adds all stops to a list of pt stops for each taz in the taz2StopMap membervariable.
     *
     * @param stopsTable
     */
    public void loadPTStops(String stopsTable, String tazTable, int bufferSize) {
        String query = "";

        try {

            query = "with stops as (select node_number, st_setsrid(the_geom,25833) as the_geom, line_names from " +
                    stopsTable + ")," + "		taz as (select tapas_taz_id, vbz_no, st_buffer(the_geom," +
                    bufferSize + ") as the_geom from " + tazTable + ")" +
                    "select node_number, st_x(st_transform(stops.the_geom,4326)) as lon, st_y(st_transform(stops.the_geom,4326)) as lat, tapas_taz_id, vbz_no, line_names " +
                    "		from stops join taz on st_within (stops.the_geom, taz.the_geom) where tapas_taz_id<=1223";

            //query = "select stops.gid, st_x(st_transform(st_setsrid(stops.the_geom,25833),4326)) as lon, st_y(st_transform(st_setsrid(stops.the_geom,25833),4326)) as lat, tapas_taz_id, stops.line_names from "+stopsTable+" stops join "+tazTable+" taz on stops.bezirk_id = taz.vbz_no";

            //delete all previous stops
            taz2StopMap.clear();
            stops2TAZMap.clear();

            ResultSet rs = this.dbCon.executeQuery(query, this);
            while (rs.next()) {

                PTStop newStop = new PTStop();
                String tmpLines;

                newStop.id = rs.getString("node_number");
                newStop.taz = rs.getInt("tapas_taz_id");
                newStop.lon = rs.getDouble("lon");
                newStop.lat = rs.getDouble("lat");
                newStop.vzID = rs.getInt("vbz_no");
                tmpLines = rs.getString("line_names");

                if (tmpLines == null) continue; // do not add stops with no lines
                String[] linesAsSting = tmpLines.split(",");
                //now count the frequency of the lines
                for (String lineName : linesAsSting) {
                    PTLine line = newStop.lines.get(lineName);
                    if (line == null) { //create a new line
                        line = new PTLine();
                        line.setName(lineName);
                        line.frequency = 1;
                        newStop.lines.put(lineName, line);
                    } else {
                        line.frequency += 1; // just increase the frequency
                    }
                }

                //get the pt-stoplist for this taz and add this stop to it
                List<PTStop> workingList = taz2StopMap.get(newStop.taz);
                if (workingList == null) { //new taz
                    workingList = new LinkedList<>();
                }
                workingList.add(newStop);
                taz2StopMap.put(newStop.taz, workingList);
                List<Integer> tazes2Stop = this.stops2TAZMap.get(newStop.id);
                if (tazes2Stop == null) {
                    tazes2Stop = new LinkedList<>();
                }
                tazes2Stop.add(newStop.vzID);
                stops2TAZMap.put(newStop.id, tazes2Stop);

            }
        } catch (SQLException e) {
            System.err.println("Error in sqlstatement: " + query);
            e.printStackTrace();
        }
    }

    public void printPTStatisticsPerTAZ() {

        double avgStop = 0, stdDevStop = 0;
        int maxStop = 0, num = 0, numOfStops;

        for (Entry<Integer, List<PTStop>> stops : this.taz2StopMap.entrySet()) {
            if (stops.getKey() > 1223) continue;
            num++;
            numOfStops = stops.getValue().size();
            avgStop += numOfStops;
            maxStop = Math.max(maxStop, numOfStops);
        }

        avgStop /= num;

        for (Entry<Integer, List<PTStop>> stops : this.taz2StopMap.entrySet()) {
            if (stops.getKey() > 1223) continue;
            numOfStops = stops.getValue().size();
            stdDevStop += (numOfStops - avgStop) * (numOfStops - avgStop);
        }
        stdDevStop /= num;

        System.out.println("Avg Stops per TAZ: " + avgStop + " max: " + maxStop + " sdtDev:" + Math.sqrt(stdDevStop));

        List<Integer> tazWithHighCapacityStops = new LinkedList<>();
        boolean highCappaStopFound;
        for (Entry<Integer, List<PTStop>> stops : this.taz2StopMap.entrySet()) {
            if (stops.getKey() > 1223) continue;
            highCappaStopFound = false;
            for (PTStop stop : stops.getValue()) {
                for (PTLine e : stop.lines.values()) {
                    if (e.type == PTLineType.S || e.type == PTLineType.U || e.type == PTLineType.Tram ||
                            e.type == PTLineType.Train || e.type == PTLineType.BRT) {
                        tazWithHighCapacityStops.add(stops.getKey());
                        highCappaStopFound = true;
                        break;
                    }
                }
                if (highCappaStopFound) break;
            }
        }
        List<Integer> tazWithSUTrain = new LinkedList<>();
        for (Entry<Integer, List<PTStop>> stops : this.taz2StopMap.entrySet()) {
            if (stops.getKey() > 1223) continue;
            highCappaStopFound = false;
            for (PTStop stop : stops.getValue()) {
                for (PTLine e : stop.lines.values()) {
                    if (e.type == PTLineType.S || e.type == PTLineType.U || e.type == PTLineType.Train) {
                        tazWithSUTrain.add(stops.getKey());
                        highCappaStopFound = true;
                        break;
                    }
                }
                if (highCappaStopFound) break;
            }
        }

        List<Integer> tazWithTram = new LinkedList<>();
        for (Entry<Integer, List<PTStop>> stops : this.taz2StopMap.entrySet()) {
            if (stops.getKey() > 1223) continue;
            highCappaStopFound = false;
            for (PTStop stop : stops.getValue()) {
                for (PTLine e : stop.lines.values()) {
                    if (e.type == PTLineType.Tram) {
                        tazWithTram.add(stops.getKey());
                        highCappaStopFound = true;
                        break;
                    }
                }
                if (highCappaStopFound) break;
            }
        }

        List<Integer> tazWithBRT = new LinkedList<>();
        for (Entry<Integer, List<PTStop>> stops : this.taz2StopMap.entrySet()) {
            if (stops.getKey() > 1223) continue;
            highCappaStopFound = false;
            for (PTStop stop : stops.getValue()) {
                for (PTLine e : stop.lines.values()) {
                    if (e.type == PTLineType.BRT) {
                        tazWithBRT.add(stops.getKey());
                        highCappaStopFound = true;
                        break;
                    }
                }
                if (highCappaStopFound) break;
            }
        }


        System.out.println(
                this.taz2StopMap.size() + "\t" + tazWithHighCapacityStops.size() + "\t" + tazWithSUTrain.size() + "\t" +
                        tazWithTram.size() + "\t" + tazWithBRT.size());
    }

    /**
     * Prints all stops per TAZ on screen
     */
    public void printPTStopsPerTAZ() {
        for (Entry<Integer, List<PTStop>> stops : this.taz2StopMap.entrySet()) {
            //System.out.println("TAZ: "+stops.getKey());
            if (stops.getValue().isEmpty()) {
                System.out.println("Has no stops");
            }

            for (PTStop stop : stops.getValue()) {
                int stopsCounter = 0;
                for (PTLine e : stop.lines.values()) {
                    stopsCounter += e.frequency;
                }
                System.out.println(
                        "\tStop: " + stop.id + " number of lines: " + stop.lines.size() + " number of stops: " +
                                stopsCounter + " weight: " + stop.weight);
            }
        }
    }

    public void printTAZPerPTStop() {
        System.out.println("Stop\tTAZes");
        for (Entry<String, List<Integer>> tazes : this.stops2TAZMap.entrySet()) {
            StringBuilder tazList = new StringBuilder();
            for (Integer taz : tazes.getValue()) {
                tazList.append(taz).append(",");
            }
            tazList = new StringBuilder(tazList.substring(0, tazList.length() - 2));
            System.out.println(tazes.getKey() + "\t" + tazList);
        }
    }

    /**
     * Prints all stops per TAZ on screen
     */
    public void savePTStopsPerTAZToFile(String filename, double detourFactor, int maxStops) {
        try {
            FileWriter writer = new FileWriter(filename);
            writer.append("BEZIRK-ID\tNode number\tRICHTUNG\tGEWICHT\tDISTANZ\n");
            int maxDist = 0, dist;
            for (Entry<Integer, List<PTStop>> stops : this.taz2StopMap.entrySet()) {
                //System.out.println("TAZ: "+stops.getKey());
                if (stops.getValue().isEmpty()) {
                    System.out.println("Has no stops");
                }
                Collections.sort(stops.getValue()); // sort by weight

                int num = 0;
                for (PTStop stop : stops.getValue()) {
                    if (stop.weight > 1 || num < maxStops) {
                        dist = stop.getAvertageDistance(detourFactor);
                        writer.append(stop.vzID + "\t" + stop.id + "\tQ\t" + stop.weight + "\t" + dist + "\n");
                        writer.append(stop.vzID + "\t" + stop.id + "\tZ\t" + stop.weight + "\t" + dist + "\n");
                        //statisitcs
                        maxDist = Math.max(maxDist, dist);
                        num++;
                    }

                }
            }
            writer.close();
            System.out.println("Max dist: " + maxDist);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void updateWeights(String table) {
        String query = "";

        try {
            //clear old weights
            query = "update " + table + " set weight =0";
            this.dbCon.execute(query, this);

            query = "update " + table + " set weight =weight+? where node_number = ?";
            PreparedStatement prepSt = this.dbCon.getConnection(this).prepareStatement(query);
            for (Entry<Integer, List<PTStop>> stops : this.taz2StopMap.entrySet()) {
                for (PTStop stop : stops.getValue()) {
                    prepSt.setInt(1, stop.weight);
                    prepSt.setString(2, stop.id);
                    prepSt.addBatch();
                }
                prepSt.executeBatch();
            }
        } catch (SQLException e) {
            System.err.println("Error in sqlstatement: " + query);
            e.printStackTrace();
            e.getNextException().printStackTrace();
        }
    }


    public enum PTLineType {
        Train(0), S(1), U(2), Tram(3), BRT(4), Bus(5), LowPriBus(6), Unknown(7);
        public int code;

        PTLineType(int code) {
            this.code = code;
        }
    }

    class PTStop implements Comparable<PTStop> {
        String id;
        int taz;
        int vzID;
        double lon, lat;
        int weight = 1;
        double dist = 400; //default distance for walking
        Map<String, PTLine> lines = new HashMap<>();

        @Override
        public int compareTo(PTStop o) {
            if (this.weight == o.weight) {
                return o.lines.size() - this.lines.size();
            } else {
                return o.weight - this.weight;
            }
        }

        public int getAvertageDistance(double detourfactor) {
            return (int) (0.5 + dist * detourfactor / weight);
        }

        public boolean hasQualityOfServiceLevel(PTLineType qos) {

            for (PTLine e : lines.values()) {
                if (e.compareToServiceLevel(qos) <= 0) {
                    return true;
                }
            }

            return false;
        }
    }

    class Household {
        int id;
        int taz;
        int persons;
        double lon, lat;
    }

    class PTLine {
        String name;
        PTLineType type = PTLineType.Unknown;
        int frequency = 0;

        /**
         * Compare the service level of this line to a given Line type.
         * Negative integers are better. Zero is same level. Positive integers are worse.
         *
         * @param o
         * @return
         */
        public int compareToServiceLevel(PTLineType o) {

            return this.type.code - o.code;
        }

        public void setName(String name) {
            this.name = name;

            name = name.replace("(1)", "");// name correction for shortened lines

            if (name.startsWith("R")) { //covers "R" "RB" "RE"
                type = PTLineType.Train;
            } else if (name.startsWith("S")) {
                type = PTLineType.S;
            } else if (name.startsWith("U")) {
                type = PTLineType.U;
            } else if (name.startsWith("TXL") || name.startsWith("X")) {
                type = PTLineType.BRT;
            } else if (name.length() == 3 && name.startsWith("1")) {
                type = PTLineType.Bus;
            } else if (name.length() == 3) {
                type = PTLineType.LowPriBus;
            } else if (name.length() == 2) {
                type = PTLineType.Tram;
            } else if (name.startsWith("M")) { //some Ms are bus some Tram
                int lineNumber = 1;
                try {
                    lineNumber = Integer.parseInt(name.substring(1));
                } catch (NumberFormatException e) {
                    System.err.println("Unparsable line number: " + name);
                    e.printStackTrace();
                }

                if (lineNumber > 18 || lineNumber == 11) {
                    type = PTLineType.BRT;
                } else {
                    type = PTLineType.Tram;
                }
            } else {
                System.err.println("Bad line: " + name);
                type = PTLineType.Unknown;
            }

        }

    }

}
