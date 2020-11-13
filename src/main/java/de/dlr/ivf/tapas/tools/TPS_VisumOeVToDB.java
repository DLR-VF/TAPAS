/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.tools;

import de.dlr.ivf.scripts.SrVCalculator;
import de.dlr.ivf.scripts.SrVCalculator.TAZ;
import de.dlr.ivf.tapas.tools.persitence.db.TPS_BasicConnectionClass;
import de.dlr.ivf.tapas.util.parameters.ParamString;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.Map.Entry;

/**
 * Class to store a result from the VISUM PT Module in the TAPAS DB.
 * It reads Visum output from the "O-Format", converts it and stores the result in the DB
 *
 * @author hein_mh
 */
public class TPS_VisumOeVToDB extends TPS_BasicConnectionClass {

    /**
     * Map for Taz-id to TAZ-Class form SrVCalculator
     */
    public HashMap<Integer, TAZ> TAZes = new HashMap<>();
    /**
     * The array for all pTNOdes
     */
    public PTNode[][] nodes = null;
    /**
     * Map from taz index to TAZ-ID
     */
    protected HashMap<Integer, Integer> reverseTAZIDMap = new HashMap<>();
    /**
     * Map from TAZ-ID to taz index
     */
    HashMap<Integer, Integer> TAZIDMap = new HashMap<>();

    public TPS_VisumOeVToDB(String loginFile) {
        super(loginFile);
    }

    public TPS_VisumOeVToDB() {
        super();
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        //all the parameters for the import run
        String visumPath = "V:\\Projekte-Berlin\\EvoBus\\Szenarien\\Basisszenario 2030\\Kenngrößen\\1-0_1_%02d-%02d_%d_Kenngrößen\\";
        String visumFile = "1-0_1_%02d-%02d_%d.";
        String matrixName = "PT_VISUM_1223_ANT2020_%02d_%02d_";
        int[] start = {6, 10, 16, 19}, end = {9, 16, 19, 23}, runNo = {31, 61, 29, 30}, cappaAddon = {0, 0, 0, 0};
        boolean[] fixPJT = {false, false, false, false};
//		String visumPath = "V:\\Projekte-Berlin\\EvoBus\\Szenarien\\Basisszenario 2030\\Kenngr��en\\1-0_1_%02d-%02d_%d_Kenngr��en\\";
//		String visumFile =  "1-0_1_%02d-%02d_%d.";
//		String matrixName = "PT_VISUM_1223_2030_1_0_1_%02d_%02d_";
//		int[] start= {6,10,16,19}, end= {9,16,19,23}, runNo= {31,61,29,30},cappaAddon={0,0,0,0};
//		boolean[] fixPJT = {false,false,false,false};
        //int[] start= {19}, end= {23}, runNo= {62},cappaAddon={0};
        //boolean[] fixPJT = {true};

        boolean usePercievedJourneyTime = true, storeInDB = true, calcDiagonal = false;
        String loginFile = "T:\\Simulationen\\runtime_herakles_admin.csv";
        String matrixTableName = "core.berlin_matrices";
        String tazMultilineName = "core.berlin_taz_1223_umland";
        int constantInitialWaiting = -1; //-1 used frequency-based aproach, otherwise a constant time in minutes
        int maxTransferTime = -1; //-1 use transfer time form visum , otherwise use this time in seconds times transfer at max
        double top3Weight = 0.8; // weighting for top3-approach to fill the diagonal
        double a = 0.5, E = 1.0; //parameters to calculate initial waiting times from the frequency

        //program starts here
        boolean tazInitialized = false;
        TPS_VisumOeVToDB importer = new TPS_VisumOeVToDB(loginFile);
        importer.dbCon.getParameters().setString(ParamString.DB_TABLE_MATRICES, matrixTableName);
        importer.loadTAZ(tazMultilineName);


        for (int i = 0; i < start.length; i++) {
            String path;
            switch ((new StringTokenizer(visumPath, "%").countTokens())) {
                case 3:
                    path = String.format(visumPath, start[i], end[i]);
                    break;
                case 4:
                    path = String.format(visumPath, start[i], end[i], runNo[i]);
                    break;
                default: //don't know this format yet!
                    path = visumPath;
                    break;
            }

            String file = String.format(visumFile, start[i], end[i], runNo[i]);
            String basename = String.format(matrixName, start[i], end[i]);

            System.out.println("Run:" + i + " Path: " + path);
            System.out.println("Run:" + i + " File: " + file);
            System.out.println("Run:" + i + " Name: " + basename);
            if (!tazInitialized) {
                tazInitialized = true;
                importer.initTAZIDs(path + file + "ACT");
                importer.calcBeelines();
            }

            importer.clearPTNodes();

            importer.readValues(path + file + "ACT", PTNodeField.ACT, 900000, 0);
            importer.readValues(path + file + "EGT", PTNodeField.EGT, 900000, 0);

            importer.readValues(path + file + "JRD", PTNodeField.JRD, 900000, 0);
            importer.readValues(path + file + "NTR", PTNodeField.NTR, 10, 0);
            if (usePercievedJourneyTime) {
                importer.readValues(path + file + "PJT", PTNodeField.SUMTT, 900000, 0);
                if (fixPJT[i]) importer.removeACTandEGTfromPJT();

                importer.transformUnits();
            } else {
                importer.readValues(path + file + "SFQ", PTNodeField.BDH, 900000, 0);
                importer.readValues(path + file + "OWTA", PTNodeField.SWT, 900000, 0);
                importer.readValues(path + file + "TWT", PTNodeField.TWT, 12000, 0);
                importer.readValues(path + file + "IVT", PTNodeField.IVT, 12000, 0);
                importer.transformUnits();
                importer.transformNumOfConnectionToInitialWaiting((end[i] - start[i] + cappaAddon[i]) * 60, a, E);

                if (constantInitialWaiting >= 0) {
                    importer.constantInitialWaiting(constantInitialWaiting);
                }
                if (maxTransferTime >= 0) {
                    importer.calcTTWithoutWaiting(maxTransferTime);
                } else {
                    importer.calcSumOfTT();
                }
            }

            //for statistics
            importer.findMissingValues();

            // fill diagonal
            if (calcDiagonal) importer.calcTop3(top3Weight);

            //store in db
            if (storeInDB) {
                importer.storeInDB(basename + "DIS", importer.getMatrixForField(PTNodeField.JRD), 0);
                importer.storeInDB(basename + "ACT", importer.getMatrixForField(PTNodeField.ACT), 0);
                importer.storeInDB(basename + "EGT", importer.getMatrixForField(PTNodeField.EGT), 0);
                //importer.storeInDB(basename+"TT", importer.getMatrixForField(PTNodeField.IVT), 0);
                importer.storeInDB(basename + "IWT", importer.getMatrixForField(PTNodeField.SWT), 0);
                //importer.storeInDB(basename+"TWT", importer.getMatrixForField(PTNodeField.TWT), 0);
                importer.storeInDB(basename + "BDH", importer.getMatrixForField(PTNodeField.BDH), 0);
                importer.storeInDB(basename + "NTR", importer.getMatrixForField(PTNodeField.NTR), 0);
                importer.storeInDB(basename + "SUM_TT", importer.getMatrixForField(PTNodeField.SUMTT), 0);
            }
        }
    }

    /**
     * Function to calculate all beelinedistances for all taz-pairs
     */
    public void calcBeelines() {
        double bl;
        Integer from, to;
        for (Entry<Integer, TAZ> i : this.TAZes.entrySet()) {
            for (Entry<Integer, TAZ> j : this.TAZes.entrySet()) {
                if (i.getValue().ignore || j.getValue().ignore) continue;
                from = i.getValue().taz_id - 1;
                to = j.getValue().taz_id - 1;
                bl = 0;
                this.nodes[from][to].idStart = i.getKey();
                this.nodes[from][to].idEnd = j.getKey();
                this.nodes[to][from].idStart = j.getKey();
                this.nodes[to][from].idEnd = i.getKey();
                if (!from.equals(to)) {
                    bl = this.TAZes.get(i.getKey()).getDistance(this.TAZes.get(j.getKey()));
                }
                bl /= 1000.0; //km
                //symmetric!
                this.nodes[from][to].bld = this.nodes[to][from].bld = bl;
            }
        }
    }

    /**
     * Method to calculate the sum of all travel times, except access and egress
     */
    public void calcSumOfTT() {

        for (PTNode[] node : this.nodes) {
            for (PTNode ptNode : node) {
                if (ptNode.ivt >= 0 && ptNode.twt >= 0 && ptNode.bdh >= 0) {
                    //sum bdh (instead of initial waiting time), ivt (in vehicle time) and twt (total waiting time)
                    ptNode.sumTT = ptNode.bdh + ptNode.ivt + ptNode.twt;
                } else ptNode.sumTT = -1; //no connection
            }
        }
    }

    /**
     * Method to remove waitingtime from the traveltime
     */
    public void calcTTWithoutWaiting(int secPerTransit) {
        double factor = secPerTransit;
        factor /= 100.0;
        int numTWTbetter = 0, numNRTbetter = 0;
        double ttTWT = 0, ttNRT = 0, ttWait = 0;
        for (PTNode[] node : this.nodes) {
            for (PTNode ptNode : node) {
                if (ptNode.ivt >= 0 && ptNode.twt >= 0 && ptNode.bdh >= 0) {
                    //sum bdh (instead of initial waiting time), ivt (in vehicle time) and twt (total waiting time)
                    //nrt = number of transfers is transfers*100, therefore the factor normalizes this integer
                    ptNode.sumTT = ptNode.bdh + ptNode.ivt +
                            //this.nodes[i][j].swt
                            +Math.min(ptNode.twt, (ptNode.nrt * factor)); //constant time per transfer

                    if (ptNode.nrt > 0) {
                        ttWait += Math.min(ptNode.twt, (ptNode.nrt * factor));
                        ttTWT += ptNode.twt;
                        ttNRT += ptNode.nrt * factor;
                        if (ptNode.twt >= (ptNode.nrt * factor)) {
                            numTWTbetter++;

                        } else {
                            numNRTbetter++;
                        }
                    }
                } else ptNode.sumTT = -1; //no connection
            }
        }
        System.out.println("TWT better: " + numTWTbetter + " nrt better: " + numNRTbetter);
        System.out.println("Wait sum: " + ttWait + " TWT sum: " + ttTWT + " nrt sum: " + ttNRT);
    }

    /**
     * Method to use the top3-technique to fill the diagonal
     */
    public void calcTop3(double factor) {

        int i, j;
        List<Double> row = null;
        if (this.nodes.length < 4) return;
        factor /= 3.0; //incl normalization of three elements
        for (i = 0; i < this.nodes.length; ++i) {
            //access time
            row = new ArrayList<>();
            for (j = 0; j < this.nodes[i].length; ++j) {
                if (i != j && this.nodes[i][j].act >= 0) row.add(this.nodes[i][j].act);
            }
            this.nodes[i][i].act = this.getTop3FromRow(row, factor);
            //egress time
            row.clear();
            for (j = 0; j < this.nodes[i].length; ++j) {
                if (i != j && this.nodes[i][j].egt >= 0) row.add(this.nodes[i][j].egt);
            }
            this.nodes[i][i].egt = this.getTop3FromRow(row, factor);
            //waiting time
            row.clear();
            for (j = 0; j < this.nodes[i].length; ++j) {
                if (i != j && this.nodes[i][j].twt >= 0) row.add(this.nodes[i][j].twt);
            }
            this.nodes[i][i].twt = this.getTop3FromRow(row, factor);
            //travel time
            row.clear();
            for (j = 0; j < this.nodes[i].length; ++j) {
                if (i != j && this.nodes[i][j].ivt >= 0) row.add(this.nodes[i][j].ivt);
            }
            this.nodes[i][i].ivt = this.getTop3FromRow(row, factor);
            //travel dist
            row.clear();
            for (j = 0; j < this.nodes[i].length; ++j) {
                if (i != j && this.nodes[i][j].jrd >= 0) row.add(this.nodes[i][j].jrd);
            }
            this.nodes[i][i].jrd = this.getTop3FromRow(row, factor);

            //sum travel time
            row.clear();
            for (j = 0; j < this.nodes[i].length; ++j) {
                if (i != j && this.nodes[i][j].sumTT >= 0) row.add(this.nodes[i][j].sumTT);
            }
            this.nodes[i][i].sumTT = this.getTop3FromRow(row, factor);

        }

    }

    /**
     * Function to check if a value is OK.
     * It converts the double to a integer and do some checks:
     * If the value is the VISUM-"No Connection tag" (999999) or diagonal (888888) it returns -1
     * If the value is the VISUM-"No value tag" (777777) it returns -2
     * If the value is higher tahn max or smaler than min it returns -3
     *
     * @param dValue   the value to check
     * @param maxValue the maximum plausible int value
     * @param minValue the minimum plausible int value
     * @return the adjusted value
     */
    private double checkValue(double dValue, int maxValue, int minValue) {
        int value = (int) Math.round(dValue);

        //diagonal
        if (value == 888888) dValue = -1;

        //no connection
        if (value == 999999) dValue = -1;

        //no value
        if (value == 777777) dValue = -2;

        //implausible value
        if (value > maxValue || value < minValue) dValue = -3;
        return dValue;
    }

    public void clearPTNodes() {
        for (PTNode[] node : this.nodes) {
            for (PTNode act : node) {
                act.act = -1; // access time
                act.egt = -1; // egress time
                act.swt = -1; // start waiting time
                act.ivt = -1; // travel time (in vehicle time)
                act.twt = -1; // transit waiting time
                act.wkt = -1; // walking time
                act.bdh = -1; // bedienhäufigkeit
                act.nrt = 0; // umsteigehäufigkeit
                act.jrd = -1; // reiseweite
                act.bld = -1; // luftlinie
                act.sumTT = -1; // summe aller Reisezeiten außer access/egress
            }
        }
    }

    /**
     * Method to set the initial waiting time to a constant
     */
    public void constantInitialWaiting(int lengthOfWaiting) {
        for (PTNode[] node : this.nodes) {
            for (PTNode ptNode : node) {
                ptNode.bdh = lengthOfWaiting;
            }
        }
    }

    /**
     * Method to find all missing values
     */
    public void findMissingValues() {
        int totalMissing = 0;
        for (int i = 0; i < this.nodes.length; ++i) {
            for (int j = 0; j < this.nodes[i].length; ++j) {
                if (i == j) continue;
                boolean missing = false;
                if (this.nodes[i][j].act < 0) missing = true;
                if (this.nodes[i][j].egt < 0) missing = true;
                if (this.nodes[i][j].sumTT < 0) {
                    if (this.nodes[i][j].ivt < 0) missing = true;
                    if (this.nodes[i][j].twt < 0) missing = true;
                    if (this.nodes[i][j].bdh < 0) missing = true;
                }
                if (this.nodes[i][j].nrt <= 0) missing = true;
                if (missing) totalMissing++;
            }
        }
        System.out.println("Missing connections: " + totalMissing);
    }

    /**
     * Method to parse all values from a given field to a double[][]-array
     *
     * @param field The field to process
     * @return A double[][]-array containing all values
     */
    public double[][] getMatrixForField(PTNodeField field) {
        //get mem
        double[][] mat = new double[this.nodes.length][this.nodes.length];

        //iterate over cells
        for (int i = 0; i < mat.length; ++i) {
            for (int j = 0; j < mat[i].length; ++j) {
                PTNode act = this.nodes[i][j];
                //select field
                switch (field) {
                    case ACT:
                        mat[i][j] = act.act;
                        break;
                    case EGT:
                        mat[i][j] = act.egt;
                        break;
                    case SWT:
                        mat[i][j] = act.swt;
                        break;
                    case IVT:
                        mat[i][j] = act.ivt;
                        break;
                    case TWT:
                        mat[i][j] = act.twt;
                        break;
                    case WKT:
                        mat[i][j] = act.wkt;
                        break;
                    case BDH:
                        mat[i][j] = act.bdh;
                        break;
                    case NTR:
                        mat[i][j] = act.nrt;
                        break;
                    case JRD:
                        mat[i][j] = act.jrd;
                        break;
                    case BLD:
                        mat[i][j] = act.bld;
                        break;
                    case SUMTT:
                        mat[i][j] = act.sumTT;
                        break;
                    default:
                        break;
                }
            }
        }

        return mat;
    }

    private double getTop3FromRow(List<Double> row, double factor) {
        if (row.size() == 0) return -1d;
        Collections.sort(row);
        double returnVal = 0;
        int max = Math.min(3, row.size());
        for (int i = 0; i < max; ++i) {
            returnVal += row.get(i);
        }

        return returnVal * factor;
    }

    /**
     * Function to initialize the TAZIDs from a VISUM File.
     *
     * @param filename Any VISUM-Output containing a matrix with the used TAZ-IDs
     */

    public void initTAZIDs(String filename) {
        try {
            BufferedReader fr = new BufferedReader(new FileReader(filename));
            String line = null;
            line = fr.readLine();
            fr.close();
            //check format
            if (line.startsWith("$M") || line.startsWith("$O") || line.startsWith("$VISION")) {
                this.initTAZIDsOMFormat(filename);
            } else if (line.startsWith("$V")) {
                this.initTAZIDsVFormat(filename);
            } else {
                throw new RuntimeException("Unknown VISUM format: " + line);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void initTAZIDsOMFormat(String filename) {
        try {
            BufferedReader fr = new BufferedReader(new FileReader(filename));
            String line = null;
            String[] tokens = null;
            int tazIndex = 0, tazID;

            Set<Integer> ids = new TreeSet<>();
            line = fr.readLine();
            //check format
            if (!line.startsWith("$M") && !line.startsWith("$O") && !line.startsWith("$VISION")) {
                fr.close();
                throw new RuntimeException("Wrong format header for M/O-Format: " + line);
            }

            //M/O - Format: vbz_from vbz_to val
            int numTokens = 3;
            int indexFrom = 0;

            //VISION format: no_from no_to vbz_from vbz_to val
            if (line.startsWith("$VISION")) {
                numTokens = 5;
                indexFrom = 2;
            }

            while ((line = fr.readLine()) != null) {
                //comment?
                if (line.startsWith("*")) continue;
                if (line.startsWith("$")) continue;
                line = this.unifyFormat(line);
                tokens = line.split(" ");
                //correct number of tokens
                if (tokens.length != numTokens) continue;
                tazID = Integer.parseInt(tokens[indexFrom]);
                ids.add(tazID); //this is a sorted list of IDs afterwards!
            }
            fr.close();

            for (Integer entry : ids.toArray(new Integer[0])) {
                if (this.TAZes.get(entry) != null) {
                    if (!this.TAZes.get(entry).ignore) {
                        reverseTAZIDMap.put(tazIndex, entry);
                        TAZIDMap.put(entry, tazIndex++);
                    }
                } else {
                    System.err.println("Unknown TAZ: " + entry);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Function to initialize the TAZIDs from a VISUM File.
     *
     * @param filename Any VISUM-Output containing a matrix with the used TAZ-IDs
     */
    public void initTAZIDsVFormat(String filename) {
        try {
            BufferedReader fr = new BufferedReader(new FileReader(filename));
            String line = null;
            String[] tokens = null;
            int tazID;
            Set<Integer> ids = new TreeSet<>();
            boolean startFound = false;
            line = fr.readLine();
            //check format
            if (!line.startsWith("$V")) {
                System.err.println("Wrong format heasder for V-Format: " + line);
                fr.close();
                throw new RuntimeException("Wrong format header for V-Format: " + line);
            }
            while ((line = fr.readLine()) != null) {
                //comment?
                if (line.startsWith("*")) {
                    if (line.startsWith("* Netzobjekt-Nummern")) {
                        //start of block
                        startFound = true;
                    } else if (line.equals("*") && startFound) {
                        //end of block
                        startFound = false;
                    }
                    continue;
                }
                if (startFound) {
                    line = this.unifyFormat(line);
                    tokens = line.split(" ");
                    for (String token : tokens) {
                        tazID = Integer.parseInt(token);
                        ids.add(tazID); //this is a sorted list of IDs afterwards!
                    }
                }
            }
            fr.close();
            int num = 0;
            for (Integer entry : ids.toArray(new Integer[0])) {
                reverseTAZIDMap.put(num, entry);
                TAZIDMap.put(entry, num);
                num++;
            }


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Function to fetch all TAZ infoirmation from the DB, containing TAZID TAZ index, coordinates and statistical area
     *
     * @return
     */
    public boolean loadTAZ(String table) {
        //load the data from the db
        String query = "";
        try {

            //query = "SELECT no, st_X(centroid(the_geom)) as lat, st_Y(centroid(the_geom)) as lon, name FROM core.berlin_taz_multiline order by no";
            query = "SELECT tapas_taz_id, no, st_X(st_centroid(st_transform(the_geom,4326))) as lat, st_Y(st_centroid(st_transform(the_geom,4326))) as lon, vbz_no, no, name FROM " +
                    table + " order by tapas_taz_id";

            ResultSet rs = this.dbCon.executeQuery(query, this);
            int taz, stat_area, maxTaz = 0;
            double lat, lon;
            SrVCalculator helper = new SrVCalculator(dbCon);
            while (rs.next()) {
                taz = rs.getInt("vbz_no");
                //taz = rs.getInt("no");
                stat_area = (taz / 100) % 1000;
                lat = rs.getDouble("lat");
                lon = rs.getDouble("lon");
                TAZ tmp = helper.new TAZ();
                //tmp.taz_id = taz;
                tmp.taz_id = rs.getInt("tapas_taz_id");
                tmp.stat_area = stat_area;
                tmp.lat = lat;
                tmp.lon = lon;
                tmp.description = rs.getString("name");
                tmp.ignore = rs.getInt("vbz_no") < 100000000;
                if (!tmp.ignore) maxTaz++;
                this.TAZes.put(taz, tmp);

            }
            //init array
            nodes = new PTNode[maxTaz][maxTaz];
            for (int i = 0; i < maxTaz; ++i) {
                for (int j = 0; j < maxTaz; ++j) {
                    nodes[i][j] = new PTNode();
                }
            }
            return true;
        } catch (SQLException e) {
            System.out.println("SQL error! Query: " + query);
            e.printStackTrace();
            return false;
        }
    }

    public void readValues(String filename, PTNodeField field, int maxValue, int minValue) {
        try {

            BufferedReader fr = new BufferedReader(new FileReader(filename));
            String line = null;
            line = fr.readLine();
            fr.close();
            //check format
            if (line.startsWith("$M") || line.startsWith("$O") || line.startsWith("$VISION")) {
                this.readValuesOMFormat(filename, field, maxValue, minValue);
            } else if (line.startsWith("$V")) {
                this.readValuesVFormat(filename, field, maxValue, minValue);
            } else {
                throw new RuntimeException("Unknown VISUM format: " + line);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Main method to parse a VISUM-File to a given PTNode-Field
     *
     * @param filename THe File to read
     * @param field    The target field, eg egress time
     */
    public void readValuesOMFormat(String filename, PTNodeField field, int maxValue, int minValue) {
        try {
            BufferedReader fr = new BufferedReader(new FileReader(filename));
            String line = null;
            String[] tokens = null;
            int fromID = 0, toID = 0, fromIndex, toIndex;
            int value;
            double dValue;
            TAZ fromTAZ, toTAZ;
            PTNode act;
            line = fr.readLine();
            int numTokens = 3;
            int indexFrom = 0;
            int indexTo = 1;
            int indexVal = 2;
            //VISION format: no_from no_to vbz_from vbz_to val
            //M/O - Format: vbz_from vbz_to val
            //check format
            if (!line.startsWith("$M") && !line.startsWith("$O") && !line.startsWith("$VISION")) {
                fr.close();
                throw new RuntimeException("Wrong format header for M/O-Format: " + line);
            }

            //VISION format: no_from no_to vbz_from vbz_to val
            //M/O - Format: vbz_from vbz_to val
            if (line.startsWith("$VISION")) {
                numTokens = 5;
                indexFrom = 2;
                indexTo = 3;
                indexVal = 4;

            }
            while ((line = fr.readLine()) != null) {
                //comment?
                if (line.startsWith("*")) continue;
                if (line.startsWith("$")) continue;

                line = this.unifyFormat(line);
                tokens = line.split(" ");

                //correct number of tokens
                if (tokens.length != numTokens) continue;


                if (!tokens[indexFrom].matches("[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?") || !tokens[indexTo].matches(
                        "[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?") || !tokens[indexVal].matches(
                        "[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?")) //not a number!
                    continue;
                //get values
                fromID = Integer.parseInt(tokens[indexFrom]);
                toID = Integer.parseInt(tokens[indexTo]);

                dValue = Double.parseDouble(tokens[indexVal]);
                value = (int) Math.round(dValue);

                dValue = this.checkValue(dValue, maxValue, minValue);

                fromTAZ = this.TAZes.get(fromID);
                toTAZ = this.TAZes.get(toID);
                fromIndex = fromTAZ.taz_id - 1;
                toIndex = toTAZ.taz_id - 1;
                if (fromTAZ.ignore || toTAZ.ignore) continue;
                //fromIndex = this.TAZIDMap.get(fromID);
                //toIndex = this.TAZIDMap.get(toID);
                act = this.nodes[fromIndex][toIndex];
                act.isDefined = true;
                act.idStart = fromID;
                act.idEnd = toID;
                switch (field) {
                    case ACT:
                        act.act = dValue;
                        break;
                    case EGT:
                        act.egt = dValue;
                        break;
                    case SWT:
                        act.swt = dValue;
                        break;
                    case IVT:
                        act.ivt = dValue;
                        break;
                    case TWT:
                        act.twt = dValue;
                        break;
                    case WKT:
                        act.wkt = dValue;
                        break;
                    case BDH:
                        act.bdh = dValue;
                        break;
                    case NTR:
                        //no interchange
                        if (value == 777777) // no value is ok!
                            dValue = 0;
                        act.nrt = dValue;
                        break;
                    case JRD:
                        act.jrd = dValue;
                        break;
                    case SUMTT:
                        act.sumTT = dValue;
                        break;
                    default:
                        break;
                }
            }
            fr.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Main method to parse a VISUM-File to a given PTNode-Field
     *
     * @param filename THe File to read
     * @param field    The target field, eg egress time
     */
    public void readValuesVFormat(String filename, PTNodeField field, int maxValue, int minValue) {
        try {
            BufferedReader fr = new BufferedReader(new FileReader(filename));
            String line = null;
            String[] tokens = null;
            int fromID = 0, toID = 0, fromIndex, toIndex, tokenIndex = 0;
            int value;
            double dValue;
            PTNode act;
            TAZ fromTAZ, toTAZ;
            int startID = -1;
            boolean startIDFound = false;
            line = fr.readLine();
            String tmp;
            //check format
            if (!line.startsWith("$V")) {
                System.err.println("Wrong format header for V-Format: " + line);
                fr.close();
                throw new RuntimeException("Wrong format header for V-Format: " + line);
            }
            while ((line = fr.readLine()) != null) {
                //comment?
                if (line.startsWith("*")) {
                    if (line.startsWith("* Obj ")) {
                        startIDFound = true;
                        tmp = line.substring("* Obj ".length(), line.indexOf("Summe") - 1);
                        startID = Integer.parseInt(tmp);
                        tokenIndex = 0;
                    } else {
                        startIDFound = false;
                    }
                    continue;
                }
                if (!startIDFound) continue;
                line = this.unifyFormat(line);
                tokens = line.split(" ");
                fromTAZ = this.TAZes.get(startID);
                fromID = fromTAZ.taz_id;

                if (fromTAZ.ignore) // is this a taz we should ignore?
                    continue;
                for (String token : tokens) {
                    //should we considder the target tvz?
                    toTAZ = this.TAZes.get(reverseTAZIDMap.get(tokenIndex));
                    tokenIndex++;
                    if (toTAZ.ignore) // is this a taz we should ignore?
                        continue;
                    if (!token.matches("[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?")) { //not a number!
                        fr.close();
                        throw new RuntimeException(
                                "Parsing not a number-exception! File: " + filename + " Line: " + line);
                    }
                    //get values
                    toID = toTAZ.taz_id;
                    dValue = Double.parseDouble(token);
                    value = (int) Math.round(dValue);
                    dValue = this.checkValue(dValue, maxValue, minValue);
                    fromIndex = fromID - 1;
                    toIndex = toID - 1;
                    if (fromIndex == 0 && toIndex == 0) fromIndex *= 1;
                    act = this.nodes[fromIndex][toIndex];
                    act.isDefined = true;
                    act.idStart = fromID;
                    act.idEnd = toID;
                    switch (field) {
                        case ACT:
                            act.act = dValue;
                            break;
                        case EGT:
                            act.egt = dValue;
                            break;
                        case SWT:
                            act.swt = dValue;
                            break;
                        case IVT:
                            act.ivt = dValue;
                            break;
                        case TWT:
                            act.twt = dValue;
                            break;
                        case WKT:
                            act.wkt = dValue;
                            break;
                        case BDH:
                            //walkers
                            act.bdh = dValue;
                            break;
                        case NTR:
                            //no interchange
                            if (value == 777777) // no value is ok!
                                dValue = 0;
                            act.nrt = dValue;
                            break;
                        case JRD:
                            act.jrd = dValue;
                            break;
                        case SUMTT:
                            act.sumTT = dValue;
                            break;
                        default:
                            break;
                    }
                }
            }
            fr.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void removeACTandEGTfromPJT() {
        for (PTNode[] node : this.nodes) {
            for (PTNode ptNode : node) {
                if (ptNode.sumTT >= 0 && ptNode.act >= 0 && ptNode.egt >= 0) {
                    double newVal = ptNode.sumTT;
                    newVal -= ptNode.act;//+this.nodes[i][j].egt;

                    ptNode.sumTT = Math.max(0, newVal);
                }
            }
        }
    }

    /**
     * Method to transform the number of connections per day to a initial waiting time. This is done by VISUM-technique:
     * waiting = a*(Period/num)^E
     *
     * @param lengthOfSimulation the length of the simulation in minutes
     * @param A                  the weigth
     * @param E                  the exponent
     */
    public void transformNumOfConnectionToInitialWaiting(int lengthOfSimulation, double A, double E) {
        double bdhOld, bdhNew;
        for (PTNode[] node : this.nodes) {
            for (PTNode ptNode : node) {
                bdhOld = ptNode.bdh;
                if (bdhOld > 0) {
                    bdhNew = A * Math.pow(lengthOfSimulation / bdhOld, E);
                    ptNode.bdh = bdhNew;
                }
            }
        }
    }

    /**
     * Method to convert all minutes to seconds and all km to meter
     */
    public void transformUnits() {
        for (PTNode[] node : this.nodes) {
            for (PTNode ptNode : node) {
                if (ptNode.act >= 0) ptNode.act *= 60; //min to sec
                if (ptNode.egt >= 0) ptNode.egt *= 60; //min to sec
                if (ptNode.ivt >= 0) ptNode.ivt *= 60; //min to sec
                if (ptNode.twt >= 0) ptNode.twt *= 60; //min to sec
                if (ptNode.bdh >= 0) ptNode.bdh *= 60; //min to sec
                if (ptNode.swt >= 0) ptNode.swt *= 60; //min to sec
                if (ptNode.jrd >= 0) ptNode.jrd *= 1000; //km to m
                if (ptNode.bld >= 0) ptNode.bld *= 1000; //km to m
                if (ptNode.nrt >= 0) ptNode.nrt *= 100; //to fix comma values with two digits!
                if (ptNode.sumTT >= 0) ptNode.sumTT *= 60; //min to sec
            }
        }
    }

    /**
     * Helper function to unify various string formats.
     * The input is cleaned for double spaces ands all spaces are transformed to tabs
     * All commas are transformed to dots for stupid German number formats!
     *
     * @param in the ugly input string
     * @return out: the beauty output string
     */
    private String unifyFormat(String in) {
        String tmp = in.trim().replaceAll("  ", " ");
        while (!tmp.equals(in)) {
            in = tmp;
            tmp = in.replaceAll("  ", " ");
        }
        in = in.replaceAll(",", ".");
        return in.replaceAll("\t", " ");
    }

    public enum PTNodeField {ACT, EGT, SWT, IVT, TWT, WKT, BDH, NTR, JRD, BLD, SUMTT}

    /**
     * Inner class for a PTnode containing all information from VISUM
     *
     * @author hein_mh
     */
    public class PTNode {
        public boolean isDefined = false;
        public double act = -1; // acces time
        public double egt = -1; // egress time
        public double swt = -1; // start waiting time
        public double ivt = -1; // travel time (in vehicle time)
        public double twt = -1; // transit waiting time
        public double wkt = -1; // walking time
        public double bdh = -1; // bedienhäufigkeit
        public double nrt = 0; // umsteigehäufigkeit
        public double jrd = -1; // reiseweite
        public double bld = -1; // luftlinie
        public double sumTT = -1; // summe aller Reisezeiten außer access/egress
        public int idStart = -1; // start TVZ
        public int idEnd = -1;  // endTVZ
    }
}
