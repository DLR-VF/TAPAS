/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.tools.locationallocation;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

public class PTTripProcessorBraunschweig {

    private final int NUM_TRIP_COLUMNS = 22;
    private final String DELIMITER = ";";
    private final HashMap<Integer, String> headerMap = new HashMap<>();
    private final HashMap<String, Trip> valueMap = new HashMap<>();
    private final HashMap<String, PTStop> stopMap = new HashMap<>();
    private final List<String> unknownPTStops = new ArrayList<>();
    private final List<String> knownPTStops = new ArrayList<>();
    private int validTips = 0;
    private int totalTips = 0;

    /**
     * @param args
     */
    public static void main(String[] args) {
        FileReader inStops = null, inTrips = null;
        BufferedReader input = null;
        String line, header;
        PTTripProcessorBraunschweig worker = new PTTripProcessorBraunschweig();
        try {
            inStops = new FileReader(args[0]);
            input = new BufferedReader(inStops);
            //read header
            header = input.readLine();
            worker.loadHeaders(header);
            line = input.readLine();

            //read pt stops
            while (line != null) {
                worker.processPTStop(line);
                line = input.readLine();
            }

            System.out.println("Found " + worker.stopMap.size() + " stops");
            input.close();
            input = null;
            inStops.close();
            inStops = null;

            File dir = new File(args[1]);

            File[] children = dir.listFiles(new FileFilter() {
                public boolean accept(File file) {
                    String filename = file.getAbsolutePath();
                    return filename.toLowerCase().endsWith(".csv");
                }
            });
            if (children == null) {
                // Either dir does not exist or is not a directory
            } else {
                for (File child : children) {
                    // Get filename of file or directory
                    inTrips = new FileReader(child);
                    input = new BufferedReader(inTrips);

                    line = input.readLine();

                    //read pt stops
                    while (line != null) {
                        worker.processTrip(line);
                        line = input.readLine();
                    }
                    System.out.println("Found " + worker.valueMap.size() + " trips");
                    input.close();
                    input = null;
                    inTrips.close();
                    inTrips = null;
                }
            }


            System.out.println("Matched " + worker.knownPTStops.size() + " stops of " +
                    (worker.knownPTStops.size() + worker.unknownPTStops.size()) + " possible ones");
            if (worker.unknownPTStops.size() > 0) {
                System.out.println("Unknown:");
                Collections.sort(worker.unknownPTStops);
                FileWriter outBad = new FileWriter(args[1] + "/badStops.txt");
                for (String notFound : worker.unknownPTStops) {
                    //System.out.println(notFound);
                    outBad.write(notFound + "\n");
                }
                outBad.close();
            }
            System.out.println("Total trips: " + worker.totalTips);
            System.out.println("Valid trips: " + worker.validTips);
            if (worker.valueMap.size() > 1) {
                FileWriter outTrip = new FileWriter(args[1] + "/Trips.csv");
                boolean writeHeader = true;
                for (Entry<String, Trip> entry : worker.valueMap.entrySet()) {
                    Trip myTrip = entry.getValue();
                    if (writeHeader) {
                        outTrip.write(myTrip.getHeader());
                        writeHeader = false;
                    }
                    outTrip.write(myTrip.toCSVString());
                }
                outTrip.close();

            }


        } catch (Throwable ex) {
            System.out.println("\t '--> Error: " + ex.getMessage());
            ex.printStackTrace();
        }//catch
        finally {
            try {
                if (input != null) input.close();
                if (inStops != null) inStops.close();
                if (inTrips != null) inTrips.close();
            }//try
            catch (IOException ex) {
                System.out.println("\t '--> Could not close : " + args[0]);
            }//catch
        }//finally

    }

    private String formatCSVLine(String entry) {
        if (entry == null) return null;
        entry = entry.trim();
        //put double quotes into empty tags
        String tmp = entry.replaceAll(";;", ";\"\";");

        while (tmp.length() != entry.length()) {
            entry = tmp;
            tmp = entry.replaceAll(";;", ";\"\";");
        }
        if (entry.endsWith(";")) entry = entry + "\"\"";
        return entry;
    }

    /**
     * Method to initialize the header tokens.
     *
     * @param headerString the header string of the csv-file
     */
    public void loadHeaders(String headerString) {

        headerString = formatCSVLine(headerString);

        StringTokenizer tok = new StringTokenizer(headerString, ";");
        int i = 0;
        String header;
        while (tok.hasMoreTokens()) {
            header = tok.nextToken();
            this.headerMap.put(i++, header);
        }

    }

    public void processPTStop(String entry) {
        entry = formatCSVLine(entry);


        StringTokenizer tok = new StringTokenizer(entry, ";");
        int i = 0;
        String token;
        if (tok.countTokens() != PTTripProcessorBraunschweig.this.headerMap.size()) {
            System.out.println("Error: Found " + tok.countTokens() + " tokens. Expected: " +
                    PTTripProcessorBraunschweig.this.headerMap.size() + " tokens.");
            while (tok.hasMoreTokens()) {
                System.out.println(tok.nextToken());
            }
            return;
        }

        String name = "";
        double x = 0, y = 0;
        boolean Bahn = false, Bus = false, Tram = false;
        int id = -1;
        int tokensFound = 0;
        int stopsFound = 0;


        while (tok.hasMoreTokens()) {
            token = tok.nextToken();
            token = token.trim();
            if (!token.equals("\"\"") && !token.equals("")) {
                switch (i) {
                    case 0:
                        id = Integer.parseInt(token);
                        tokensFound++;
                        break;
                    case 2:
                        name = token;
                        tokensFound++;
                        break;
                    case 3:
                        x = Double.parseDouble(token);
                        tokensFound++;
                        break;
                    case 4:
                        y = Double.parseDouble(token);
                        tokensFound++;
                        break;
                    case 5:
                        Bahn = Integer.parseInt(token) > 0;
                        stopsFound++;
                        break;
                    case 8:
                        Tram = Integer.parseInt(token) > 0;
                        stopsFound++;
                        break;
                    case 11:
                        Bus = Integer.parseInt(token) > 0;
                        stopsFound++;
                        break;
                    default:
                        break;
                }
            }
            ++i;
        }
        if (id >= 0 && !name.equals("") && tokensFound == 4 && stopsFound > 0) {
            this.stopMap.put(name, new PTStop(x, y, id, Bus, Tram, Bahn));
        }

    }

    public void processTrip(String entry) {
        entry = formatCSVLine(entry);

        StringTokenizer tok = new StringTokenizer(entry, ";");
        int i = 0;
        String token;
        if (tok.countTokens() != NUM_TRIP_COLUMNS) {
            System.out.println(
                    "Error: Found " + tok.countTokens() + " tokens. Expected: " + NUM_TRIP_COLUMNS + " tokens.");
            while (tok.hasMoreTokens()) {
                System.out.println(tok.nextToken());
            }
            return;
        }

        StringBuilder start = new StringBuilder();
        StringBuilder stop = new StringBuilder();
        boolean arr = false, arrStop = false, depStop = false;
        int tokensFound = 0;

        while (tok.hasMoreTokens()) {
            token = tok.nextToken();
            token = token.trim();
            if (!token.equals("\"\"") && !token.equals("")) {
                switch (i) {
                    case 3:
                        arr = token.equals("arr");
                        tokensFound++;
                        break;
                    case 6:
                    case 11:
                        if ((arr && i == 6) || (!arr && i == 11)) arrStop = token.equals("stop");
                        else depStop = token.equals("stop");
                        tokensFound++;
                        break;
                    case 9:
                    case 14:
                        if ((arr && i == 9) || (!arr && i == 14)) start = new StringBuilder(token + ", ");
                        else stop = new StringBuilder(token + ", ");
                        tokensFound++;
                        break;
                    case 10:
                    case 15:
                        if ((arr && i == 10) || (!arr && i == 15)) start.append(token);
                        else stop.append(token);
                        tokensFound++;
                        break;
                    default:
                        break;
                }
            }
            ++i;
        }
        if (tokensFound == 7 && arrStop && depStop) {
            this.totalTips++;
            String key = start + "-" + stop;
            if (this.valueMap.containsKey(key)) {
                Trip myTrip = this.valueMap.get(key);
                myTrip.incNum();
            } else {
                this.valueMap.put(key, new Trip(start.toString(), stop.toString()));
                if (this.stopMap.containsKey(start.toString())) {
                    if (!this.knownPTStops.contains(start.toString())) this.knownPTStops.add(start.toString());
                } else {
                    if (!this.unknownPTStops.contains(start.toString())) this.unknownPTStops.add(start.toString());
                }
                if (this.stopMap.containsKey(stop.toString())) {
                    if (!this.knownPTStops.contains(stop.toString())) this.knownPTStops.add(stop.toString());
                } else {
                    if (!this.unknownPTStops.contains(stop.toString())) this.unknownPTStops.add(stop.toString());
                }
            }
            if (this.knownPTStops.contains(stop.toString()) && this.knownPTStops.contains(start.toString()))
                this.validTips++;
        }
    }

    private class Trip {
        String start, stop;

        int num;

        public Trip(String start, String stop) {
            this.start = start;
            this.stop = stop;
            num = 1;
        }

        public String getHeader() {
            return "Start Name" + DELIMITER + "Start X" + DELIMITER + "Start Y" + DELIMITER + "Stop Name" + DELIMITER +
                    "Stop X" + DELIMITER + "Stop Y" + DELIMITER + "Num of trips\n";
        }

        /**
         * @return the num
         */
        @SuppressWarnings("unused")
        public int getNum() {
            return num;
        }


        /**
         * @param num the num to set
         */
        @SuppressWarnings("unused")
        public void setNum(int num) {
            this.num = num;
        }


        /**
         * @return the start
         */
        @SuppressWarnings("unused")
        public String getStart() {
            return start;
        }

        /**
         * @return the stop
         */
        @SuppressWarnings("unused")
        public String getStop() {
            return stop;
        }

        public void incNum() {
            this.num++;
        }

        public String toCSVString() {
            String entry = "";
            PTStop ptStart = PTTripProcessorBraunschweig.this.stopMap.get(this.start);
            PTStop ptStop = PTTripProcessorBraunschweig.this.stopMap.get(this.stop);

            if (ptStart != null && ptStop != null) {
                entry = this.start + DELIMITER + ptStart.getX() + DELIMITER + ptStart.getY() + DELIMITER + this.stop +
                        DELIMITER + ptStop.getX() + DELIMITER + ptStop.getY() + DELIMITER + this.num + "\n";

            }
            return entry;
        }


    }

    private class PTStop {
        double x, y;
        int id;
        boolean RBahn;
        boolean Tram;
        boolean Bus;

        public PTStop(double x, double y, int id, boolean bus, boolean tram, boolean bahn) {
            this.x = x;
            this.y = y;
            this.id = id;
            this.Bus = bus;
            this.Tram = tram;
            this.RBahn = bahn;
        }

        /**
         * @return the id
         */
        @SuppressWarnings("unused")
        public int getID() {
            return id;
        }

        /**
         * @return the x
         */
        public double getX() {
            return x;
        }

        /**
         * @return the y
         */
        public double getY() {
            return y;
        }

        /**
         * @return the bus
         */
        @SuppressWarnings("unused")
        public boolean isBus() {
            return Bus;
        }

        /**
         * @return the rBahn
         */
        @SuppressWarnings("unused")
        public boolean isRBahn() {
            return RBahn;
        }

        /**
         * @return the tram
         */
        @SuppressWarnings("unused")
        public boolean isTram() {
            return Tram;
        }
    }

}
