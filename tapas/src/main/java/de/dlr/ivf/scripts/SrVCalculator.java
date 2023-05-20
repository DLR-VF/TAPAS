/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.scripts;

import de.dlr.ivf.tapas.misc.Helpers;
import de.dlr.ivf.tapas.persistence.db.TPS_DB_Connector;
import de.dlr.ivf.tapas.persistence.db.TPS_DB_IO;
import de.dlr.ivf.tapas.model.TPS_Geometrics;
import de.dlr.ivf.tapas.model.parameter.TPS_ParameterClass;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * This class was originally designed to calculate some traveltimes of working trips for statistical areas for the SrV-Dataset.
 * Now it converts a lot of matrices including MiV and PT to an appropriate format for mode estimation.
 *
 * @author hein_mh
 */
public class SrVCalculator {

    static final int WALK = 0;
    static final int BIKE = 1;
    static final int MIT = 2;
    static final int MIT_PASS = 3;
    static final int TAXI = 4;
    static final int PT = 5;
    static final int TRAIN = 6;

    static final double speedBike = 4; // in m/s
    static final double speedWalk = 1; // in m/s

    static final double costMIT = 0.000109502854; //€/m
    static final double costSmall = 0.000090745262; //€/m
    static final double costMedium = 0.000107012555; //€/m
    static final double costLarge = 0.000137319786; //€/m
    static final double costVariable = 0.00002712; //€/m
    static final double costPT = 0.0002810; //€/m


    /**
     * internal reference to the db-connection-manager
     */
    TPS_DB_Connector dbConnection;
    double[][] ttMIV = null;
    double[][] distTAZ = null;
    /**
     * hashmap for id conversion
     */
    private final HashMap<Integer, Integer> tazToStatisticArea = new HashMap<>();
    private final HashMap<Integer, List<Integer>> statisticAreaToTAZ = new HashMap<>();
    private final List<Integer> statisticArea = new ArrayList<>();
    /**
     * sum of tt-array for each mode
     */

    private double[][][] ttArray = null;
    /**
     * sum of dist-array for each mode
     */

    private double[][][] distArray = null;
    /**
     * num of O/D-array for each mode
     */

    private int[][][] odArray = null;
    private TAZ[] tazWeight = null;
    private PTTAZInfo[][] ptTaz = null;

    /**
     * the one and only standard-constructor
     *
     * @param connection reference to the db-connection-manager
     */
    public SrVCalculator(TPS_DB_Connector connection) {
        dbConnection = connection;
    }

    /**
     * @param args
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        if (args.length != 4) {
            System.out.println(
                    "Usage: SrVCalculator <miv-travel time matrix> <dist matrix> <pt matrix prefix> <output>");
            return;
        }
        System.out.println("Start of SrVCalculation");

        File configFile = new File("T:\\Simulationen\\runtime_herakles_admin.csv");

        TPS_ParameterClass parameterClass = new TPS_ParameterClass();
        parameterClass.loadRuntimeParameters(configFile);
        SrVCalculator worker = new SrVCalculator(new TPS_DB_Connector(parameterClass));
        System.out.println("Loading TAZ codes and statistical area codes");
        //if(worker.loadTAZToArea()){
        if (worker.loadTAZToMapping("Statistisches Gebiet zu TVZ 1223", "core.berlin_taz_1223",
                "core.berlin_taz_mapping_values")) {
            worker.readMatrixFromDB(args[0], worker.ttMIV, 1.0, "core.berlin_matrices");
            worker.readMatrixFromDB(args[1], worker.distTAZ, "core.berlin_matrices");
            worker.readPTMatricesFromDB(args[2]);
            //worker.readPTMatrix(args[1]);
            worker.initAreasWithDefaultTimes();
            //worker.topxPTTAZInfo(3, 0.8);
            System.out.println(
                    "Found " + worker.tazToStatisticArea.size() + " TAZ codes and " + worker.odArray[0].length +
                            " statistical area codes.");
            //System.out.println("Loading Simulation "+args[0]);
            System.out.println("Writing csv-file " + args[3]);
            worker.aggregateAndWriteCSVFile(args[3]);
            int count = 0; // count walkers
            for (int i = 0; i < worker.ptTaz.length; ++i) {
                for (int j = 0; j < worker.ptTaz[i].length; ++j) {

                    if (i != j && worker.ptTaz[i][j].tt == 0) count++;
                }
            }
            System.out.println("Number of walkers: " + count);

            //keine wichtung!
//			if(worker.readTrips(args[0],args[2])){
//				System.out.println("Writing csv-file "+args[2]);
//				worker.writeCSVFile(args[2]);
//			}
        }
        worker.closeConnection();
        System.out.println("End of SrVCalculation");

    }

    /**
     * Method to aggregate the taz to statistical areas and write them to a given csv-file
     *
     * @param fileName the file to write
     */
    public void aggregateAndWriteCSVFile(String fileName) {
        try {
            FileWriter writer = new FileWriter(fileName);
            String line;
            //the headder
            writer.append(
                    "von;nach;rw;rw_pt;beeline;cost_miv;cost_small;cost_medium;cost_large;cost_variable;cost_pt;rz_miv;rz_pt;rz_bike;rz_walk;pt_interchange;pt_init_waiting;pt_sum_waiting;pt_access;pt_egress;tazConnections\n");
            int i, j, anz;
            double tt, ttPT, dist, distPT;
            for (i = 0; i < odArray[0].length; ++i) { //from
                int from = this.statisticArea.get(i);
                for (j = 0; j < odArray[0][i].length; ++j) { //to
                    int to = this.statisticArea.get(j);

                    //aggregate the values
                    //now the pt-matrix
                    double interchange = 0, maxWaiting = 0, sumWaiting = 0, access = 0, egress = 0, totGeoWeight = 0, geoDist = 0;
                    dist = 0;
                    distPT = 0;
                    ttPT = 0;
                    for (Integer m : this.statisticAreaToTAZ.get(from)) {
                        for (Integer n : this.statisticAreaToTAZ.get(to)) {
                            totGeoWeight++;
                            interchange += this.ptTaz[m - 1][n - 1].interchanges;
                            maxWaiting += this.ptTaz[m - 1][n - 1].maxWaiting;
                            sumWaiting += this.ptTaz[m - 1][n - 1].sumWaiting;
                            access += this.ptTaz[m - 1][n - 1].access;
                            egress += this.ptTaz[m - 1][n - 1].egress;
                            distPT += this.ptTaz[m - 1][n - 1].dist;
                            ttPT += this.ptTaz[m - 1][n - 1].tt;
                            dist += this.distTAZ[m - 1][n - 1];
                            geoDist += this.tazWeight[m - 1].getDistance(this.tazWeight[n - 1]);
                        }
                    }
                    // number of taz connections
                    int tazConnectors = this.statisticAreaToTAZ.get(from).size() * this.statisticAreaToTAZ.get(to)
                                                                                                          .size();

                    interchange /= totGeoWeight;
                    maxWaiting /= totGeoWeight;
                    sumWaiting /= totGeoWeight;
                    access /= totGeoWeight;
                    egress /= totGeoWeight;
                    geoDist /= totGeoWeight;
                    dist /= totGeoWeight;
                    distPT /= totGeoWeight;
                    ttPT /= totGeoWeight;
                    //store the values
                    line = from + ";" + to + ";";
                    line += new BigDecimal(dist).setScale(0, RoundingMode.HALF_UP) + ";";
                    line += new BigDecimal(distPT).setScale(0, RoundingMode.HALF_UP) + ";";
                    line += new BigDecimal(geoDist).setScale(0, RoundingMode.HALF_UP) + ";";
                    line += new BigDecimal(dist * costMIT).setScale(6, RoundingMode.HALF_UP) + ";";
                    line += new BigDecimal(dist * costSmall).setScale(6, RoundingMode.HALF_UP) + ";";
                    line += new BigDecimal(dist * costMedium).setScale(6, RoundingMode.HALF_UP) + ";";
                    line += new BigDecimal(dist * costLarge).setScale(6, RoundingMode.HALF_UP) + ";";
                    line += new BigDecimal(dist * costVariable).setScale(6, RoundingMode.HALF_UP) + ";";
                    line += new BigDecimal(dist * costPT).setScale(6, RoundingMode.HALF_UP) + ";";

                    //miv
                    anz = (this.odArray[MIT][i][j] + this.odArray[MIT_PASS][i][j]);
                    if (anz > 0) {
                        tt = (this.ttArray[MIT][i][j] + this.ttArray[MIT_PASS][i][j]) / anz;
                    } else {
                        tt = 0;
                    }
                    line += new BigDecimal(tt).setScale(0, RoundingMode.HALF_UP) + ";";

                    //pt
//					anz = this.odArray[PT][i][j];
//					if(anz>0){
//						tt = this.ttArray[PT][i][j]/anz;
//					}
//					else{
//						tt=0;
//					}
                    line += new BigDecimal(ttPT).setScale(0, RoundingMode.HALF_UP) + ";";

                    //bike
                    line += new BigDecimal(dist / speedBike).setScale(0, RoundingMode.HALF_UP) + ";";

                    //walk
                    line += new BigDecimal(dist / speedWalk).setScale(0, RoundingMode.HALF_UP) + ";";

                    line += new BigDecimal(interchange).setScale(2, RoundingMode.HALF_UP) + ";";
                    line += new BigDecimal(maxWaiting).setScale(0, RoundingMode.HALF_UP) + ";";
                    line += new BigDecimal(sumWaiting).setScale(0, RoundingMode.HALF_UP) + ";";
                    line += new BigDecimal(access).setScale(0, RoundingMode.HALF_UP) + ";";
                    line += new BigDecimal(egress).setScale(0, RoundingMode.HALF_UP) + ";";
                    line += tazConnectors; // no tailing semicolon

                    line += "\n"; //  new line!
                    writer.append(line);
                }
            }
            writer.flush();
            writer.close();

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * private function to close the connection. Do not call this, when using this in the main tapas programm.
     */
    private void closeConnection() {
        try {
            this.dbConnection.closeConnection(this);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method to calculate some default times. These are usually aggregated times for walk/bike
     */
    public void initAreasWithDefaultTimes() {
        int i, j;
        double Dist;
//todo revise this
//        TPS_Geometrics.calcTop3(this.distTAZ);
//        TPS_Geometrics.calcTop3(this.ttMIV);

        for (i = 0; i < this.odArray[0].length; ++i) { //from
            this.tazWeight[i].weight++;
            int from = this.statisticArea.get(i);
            for (j = 0; j < odArray[0][i].length; ++j) { //to
                int to = this.statisticArea.get(j);
                for (Integer m : this.statisticAreaToTAZ.get(from)) {
                    for (Integer n : this.statisticAreaToTAZ.get(to)) {
                        Dist = this.distTAZ[m - 1][n - 1];
                        //BIKE
                        this.distArray[BIKE][i][j] += Dist;
                        this.odArray[BIKE][i][j]++;
                        this.ttArray[BIKE][i][j] += Dist / speedBike;
                        //WALK
                        this.distArray[WALK][i][j] += Dist;
                        this.odArray[WALK][i][j]++;
                        this.ttArray[WALK][i][j] += Dist / speedWalk;
                        //MIV
                        this.distArray[MIT][i][j] += Dist;
                        this.odArray[MIT][i][j]++;
                        this.ttArray[MIT][i][j] += this.ttMIV[m - 1][n - 1];
                        //PT
                        this.distArray[PT][i][j] += Dist;
                        this.odArray[PT][i][j]++;
                        this.ttArray[PT][i][j] += this.ptTaz[m - 1][n - 1].tt;
                    }
                }
            }
        }
    }

    /**
     * Method to load all TAZ for the region and create a mapping for the statistical areas
     *
     * @return true for success
     */

    public boolean loadTAZToArea(String tazTable) {
        //load the data from the db
        String query =
                "SELECT taz_id, taz_statistical_area, st_X(taz_coordinate) as lat, st_Y(taz_coordinate) as lon FROM " +
                        tazTable;
        try {
            int taz, area, count, maxTaz = 0;
            ResultSet rs = this.dbConnection.executeQuery(query, this);
            while (rs.next()) {
                taz = rs.getInt("taz_id");
                maxTaz = Math.max(taz, maxTaz);
                area = rs.getInt("taz_statistical_area");
                this.tazToStatisticArea.put(taz, area);
                if (!this.statisticAreaToTAZ.containsKey(area)) {
                    this.statisticAreaToTAZ.put(area, new LinkedList<>());
                }
                List<Integer> tazesInArea = this.statisticAreaToTAZ.get(area);
                tazesInArea.add(taz);

                if (!this.statisticArea.contains(area)) this.statisticArea.add(area);
            }
            rs.close();
            count = this.statisticArea.size();
            this.ttArray = new double[7][count][count];
            this.distArray = new double[7][count][count];
            this.odArray = new int[7][count][count];
            for (int i = 0; i < 7; ++i) {
                for (int j = 0; j < count; ++j) {
                    for (int k = 0; k < count; ++k) {
                        this.ttArray[i][j][k] = 0;
                        this.distArray[i][j][k] = 0;
                        this.odArray[i][j][k] = 0;

                    }
                }
            }

            this.ptTaz = new PTTAZInfo[maxTaz][maxTaz];
            this.ttMIV = new double[maxTaz][maxTaz];
            this.distTAZ = new double[maxTaz][maxTaz];
            this.tazWeight = new TAZ[maxTaz];
            for (int j = 0; j < maxTaz; ++j) {
                for (int k = 0; k < maxTaz; ++k) {
                    this.ptTaz[j][k] = new PTTAZInfo();
                }
                this.tazWeight[j] = new TAZ();
            }

            query = "SELECT taz_id, taz_statistical_area, st_X(taz_coordinate) as lat, st_Y(taz_coordinate) as lon FROM " +
                    tazTable;

            rs = this.dbConnection.executeQuery(query, this);
            while (rs.next()) {
                taz = rs.getInt("taz_id");

                this.tazWeight[taz - 1].taz_id = taz;
                this.tazWeight[taz - 1].stat_area = rs.getInt("taz_statistical_area");
                this.tazWeight[taz - 1].lat = rs.getDouble("lat");
                this.tazWeight[taz - 1].lon = rs.getDouble("lon");
            }

            return true;
        } catch (SQLException e) {
            System.out.println("SQL error!");
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Method to load all TAZ for the region and create a mapping for the statistical areas
     *
     * @return true for success
     */

    public boolean loadTAZToMapping(String mappingCode, String tazTable, String mappingTable) {
        //load the data from the db
        String query = "SELECT map_value, taz_values FROM " + mappingTable + " WHERE name ='" + mappingCode + "'";
        try {
            int area, count, maxTaz = 0;
            ResultSet rs = this.dbConnection.executeQuery(query, this);
            while (rs.next()) {
                int[] tazes = Helpers.extractIntArray(rs, "taz_values");
                area = rs.getInt("map_value");
                for (int taz : tazes) {
                    maxTaz = Math.max(taz, maxTaz);
                    this.tazToStatisticArea.put(taz, area);
                    if (!this.statisticAreaToTAZ.containsKey(area)) {
                        this.statisticAreaToTAZ.put(area, new LinkedList<>());
                    }
                    List<Integer> tazesInArea = this.statisticAreaToTAZ.get(area);
                    tazesInArea.add(taz);

                    if (!this.statisticArea.contains(area)) this.statisticArea.add(area);
                }
            }
            rs.close();
            count = this.statisticArea.size();
            this.ttArray = new double[7][count][count];
            this.distArray = new double[7][count][count];
            this.odArray = new int[7][count][count];
            for (int i = 0; i < 7; ++i) {
                for (int j = 0; j < count; ++j) {
                    for (int k = 0; k < count; ++k) {
                        this.ttArray[i][j][k] = 0;
                        this.distArray[i][j][k] = 0;
                        this.odArray[i][j][k] = 0;

                    }
                }
            }

            this.ptTaz = new PTTAZInfo[maxTaz][maxTaz];
            this.ttMIV = new double[maxTaz][maxTaz];
            this.distTAZ = new double[maxTaz][maxTaz];
            this.tazWeight = new TAZ[maxTaz];
            for (int j = 0; j < maxTaz; ++j) {
                for (int k = 0; k < maxTaz; ++k) {
                    this.ptTaz[j][k] = new PTTAZInfo();
                }
                this.tazWeight[j] = new TAZ();
            }

            query = "SELECT taz_id, taz_statistical_area, st_X(taz_coordinate) as lat, st_Y(taz_coordinate) as lon FROM " +
                    tazTable;

            rs = this.dbConnection.executeQuery(query, this);
            while (rs.next()) {
                int taz = rs.getInt("taz_id");

                this.tazWeight[taz - 1].taz_id = taz;
                this.tazWeight[taz - 1].stat_area = this.tazToStatisticArea.getOrDefault(taz, -1);
                this.tazWeight[taz - 1].lat = rs.getDouble("lat");
                this.tazWeight[taz - 1].lon = rs.getDouble("lon");
            }

            return true;
        } catch (SQLException e) {
            System.out.println("SQL error!");
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Reads a single matrix from the db and stores them into the given array
     *
     * @param tableentry The name of the matrix
     * @param matrix     a reference to an array to put the values
     */
    public void readMatrixFromDB(String tableentry, double[][] matrix, String matrixTable) {
        readMatrixFromDB(tableentry, matrix, 1.0, matrixTable);
    }

    /**
     * Reads a single matrix from the db, multiplys the values with the given factor and stores them into the given array
     *
     * @param tableentry The name of the matrix
     * @param matrix     a reference to an array to put the values
     * @param factor     the modification factor
     */
    public void readMatrixFromDB(String tableentry, double[][] matrix, double factor, String matrixTable) {
        String querry = "";
        try {
            querry = "SELECT matrix_values FROM " + matrixTable + " WHERE matrix_name='" + tableentry + "'";
            ResultSet rs = this.dbConnection.executeQuery(querry, this);
            if (rs.next()) {
                int[] iArray;
                iArray = Helpers.extractIntArray(rs, "matrix_values");
                int len = (int) Math.sqrt(iArray.length);
                if (len != matrix.length) {
                    System.err.println("Error length " + len + " is not " + matrix.length);
                    return;
                }
                for (int index = 0; index < iArray.length; index++) {
                    int i = index / len; // row
                    int j = index % len; // col
                    matrix[i][j] = iArray[index] * factor;
                }
            } else {
                System.out.println("No results for: " + querry);
            }
            rs.close();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            System.out.println("Error in sql-satement: " + querry);
            e.printStackTrace();
        }
    }

    /**
     * Method to read a set of PT-Marices from the db. The matices must follow a certain name scheme:
     * basename+EGT Egress time
     * basename+ACT access time
     * basename+TT  travel time in vecicle
     * basename+DIS pt-distance
     * basename+IWT initial waiting time
     * basename+TWT trip waiting time
     * basename+NTR number of interchanges (times 100!)
     *
     * @param baseName the basename for the name scheme
     */
    public void readPTMatricesFromDB(String baseName) {

        int numOfTaz = this.ptTaz.length;
        double[][] egt = new double[numOfTaz][numOfTaz]; //access time
        double[][] act = new double[numOfTaz][numOfTaz]; //egress time
        double[][] tt = new double[numOfTaz][numOfTaz]; //travel time w/o waiting!
        double[][] dist = new double[numOfTaz][numOfTaz];  //pt dist
        double[][] iwt = new double[numOfTaz][numOfTaz]; // initial waiting time
        double[][] twt = new double[numOfTaz][numOfTaz]; //total waiting time (appart form initial wt)
        double[][] chg = new double[numOfTaz][numOfTaz]; //number of interchanges, !!multiplied by hundred in db!!
        this.readMatrixFromDB(baseName + "EGT", egt, "core.berlin_matrices");
        this.readMatrixFromDB(baseName + "ACT", act, "core.berlin_matrices");
        this.readMatrixFromDB(baseName + "SUM_TT", tt, "core.berlin_matrices");
        this.readMatrixFromDB(baseName + "DIS", dist, "core.berlin_matrices");
        this.readMatrixFromDB(baseName + "IWT", iwt, "core.berlin_matrices");
        this.readMatrixFromDB(baseName + "BDH", twt, "core.berlin_matrices");
        this.readMatrixFromDB(baseName + "NTR", chg, 0.01, "core.berlin_matrices"); //divide by 100!


        for (int i = 0; i < numOfTaz; ++i) {
            for (int j = 0; j < numOfTaz; ++j) {
                this.ptTaz[i][j].interchanges = chg[i][j];
                this.ptTaz[i][j].maxWaiting = twt[i][j];
                this.ptTaz[i][j].sumWaiting = iwt[i][j];
                this.ptTaz[i][j].access = act[i][j];
                this.ptTaz[i][j].egress = egt[i][j];
                this.ptTaz[i][j].tt = tt[i][j];
                this.ptTaz[i][j].dist = dist[i][j];
            }
        }
    }

    /**
     * Method to read the old pt-files from Dresden
     *
     * @param filename the file to read
     */
    public void readPTMatrix(String filename) {
        FileReader in;

        final int PT_TOK_FROM = 0;
        final int PT_TOK_TO = 1;
        final int PT_TOK_START = 2;
        final int PT_TOK_END = 3;
        final int PT_TOK_INTER = 4;
        final int PT_TOK_MW = 5;
        final int PT_TOK_SW = 6;
        final int PT_TOK_ACC = 7;
        final int PT_TOK_EGR = 8;

        try {
            HashMap<Integer, Integer> tazConverter = new HashMap<>();
            if (this.ptTaz.length == 879) {
                int j = 0;
                for (int i = 1; i <= 881; ++i) {
                    if (i != 186 && i != 187) {//staken
                        tazConverter.put(i, j);
                        j++;
                    }
                }
            } else {
                int j = 1;
                for (int i = 0; i < this.ptTaz.length; ++i) {
                    tazConverter.put(j, i);
                    j++;
                }
            }
            in = new FileReader(filename);
            String line, cleanedLine = "";
            BufferedReader input = new BufferedReader(in);
            int lineNr = 1;
            input.readLine();
            lineNr++;//header
            input.readLine();
            lineNr++;//header
            int from, to;
            while (cleanedLine != null) {
                cleanedLine = input.readLine();
                lineNr++;
                if (cleanedLine != null) {

                    do {
                        line = cleanedLine;
                        cleanedLine = line.replace("  ", " "); // delete double spaces
                    } while (cleanedLine.length() != line.length());
                    String[] tokens = line.trim().split(" ");
                    if (tokens.length == 9) {

                        from = Integer.parseInt(tokens[PT_TOK_FROM]);
                        to = Integer.parseInt(tokens[PT_TOK_TO]);
                        if (!tazConverter.containsKey(from) || !tazConverter.containsKey(to)) {
                            continue;
                        }
                        from = tazConverter.get(from);
                        to = tazConverter.get(to);
                        int tt = (Integer.parseInt(tokens[PT_TOK_END]) - Integer.parseInt(tokens[PT_TOK_START]));
                        int inter = Integer.parseInt(tokens[PT_TOK_INTER]);
                        int maxW = Integer.parseInt(tokens[PT_TOK_MW]);
                        int sumW = Integer.parseInt(tokens[PT_TOK_SW]);
                        int acc = Integer.parseInt(tokens[PT_TOK_ACC]);
                        int egr = Integer.parseInt(tokens[PT_TOK_EGR]);
                        this.ptTaz[to][from].tt = this.ptTaz[from][to].tt = tt * 60;
                        this.ptTaz[to][from].interchanges = this.ptTaz[from][to].interchanges = inter;
                        this.ptTaz[to][from].maxWaiting = this.ptTaz[from][to].maxWaiting = maxW * 60;
                        this.ptTaz[to][from].sumWaiting = this.ptTaz[from][to].sumWaiting = sumW * 60;
                        this.ptTaz[to][from].access = this.ptTaz[from][to].access = acc * 60;
                        this.ptTaz[to][from].egress = this.ptTaz[from][to].egress = egr * 60;

                    } else {
                        System.out.printf("Wrong number of tokens: %d in line %d: %s\n", tokens.length, lineNr, line);
                    }
                }
            }
            input.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Old (obsolete?!?) methodd to read parameters for working trips
     *
     * @param key the simulation  key
     * @return true for success
     */
    public boolean readTrips(String key) {
        //load the data from the db
        String query;
        try {
            int from, to, mode, counter, fromTAZ, toTAZ;
            double tt, dist;
            query = "SELECT * FROM core.exist_table('public', '" + key + "')";
            ResultSet rs = this.dbConnection.executeQuery(query, this);
            if (rs.next()) {
                if (!rs.getBoolean(1)) {
                    System.out.println("Table " + key + " does not exist!");
                    rs.close();
                    return false;
                }
            }
            rs.close();

            int step = 0;
            int chunk = 1000000;
            counter = 0;
            boolean recordsFound;
            do {
                recordsFound = false;
                query = "SELECT p_id, taz_id_start, taz_id_end, loc_id_start, loc_id_end, mode, travel_time_sec, distance_real_m FROM public." +
                        key + " ORDER BY hh_id, p_id, start_time_min LIMIT " + chunk + " OFFSET " + (step * chunk);
                rs = this.dbConnection.executeQuery(query, this);
                step++;
                while (rs.next()) {
                    recordsFound = true;
                    fromTAZ = rs.getInt("taz_id_start");
                    toTAZ = rs.getInt("taz_id_end");

                    this.tazWeight[fromTAZ - 1].weight++;
                    this.tazWeight[toTAZ - 1].weight++;

                    from = this.tazToStatisticArea.get(fromTAZ) - 1;
                    to = this.tazToStatisticArea.get(toTAZ) - 1;
                    mode = rs.getInt("mode");
                    tt = rs.getDouble("travel_time_sec");
                    dist = rs.getDouble("distance_real_m");
                    this.odArray[mode][from][to]++;
                    this.ttArray[mode][from][to] += tt;
                    this.distArray[mode][from][to] += dist;
                    counter++;
                    if (counter % 10000 == 0) System.out.println("Processed " + counter + " work trips");

                }
                rs.close();

            } while (recordsFound);
            return true;
        } catch (SQLException e) {
            System.out.println("SQL error!");
            e.printStackTrace();
            return false;
        }
    }

    /**
     * TOPx-calculation for the diagonal values for the following matrices:
     * travel time
     * interchanges
     * maxWaiting (inital Waiting)
     * sumWaiting
     * access
     * egress
     * distance
     *
     * @param num    the number of neighbours to considder
     * @param weight the weight for the average - should be between 0.2 and 0.8
     */
    public void topxPTTAZInfo(int num, double weight) {
        double[] vals = new double[ptTaz.length];

        weight /= num;
        //MEGASTUPID code copy: Does anybody know how to implement generic methods for class fields?

        //tt
        for (int i = 0; i < ptTaz.length; ++i) {
            for (int j = 0; j < ptTaz[i].length; ++j) {
                vals[j] = ptTaz[i][j].tt;
            }
            Arrays.sort(vals);
            ptTaz[i][i].tt = 0;
            for (int j = 1; j <= num; ++j) { // first element is zero!
                ptTaz[i][i].tt += vals[j];
            }
            ptTaz[i][i].tt *= weight;
        }

        //interchanges
        for (int i = 0; i < ptTaz.length; ++i) {
            for (int j = 0; j < ptTaz[i].length; ++j) {
                vals[j] = ptTaz[i][j].interchanges;
            }
            Arrays.sort(vals);
            ptTaz[i][i].interchanges = 0;
            for (int j = 1; j <= num; ++j) { // first element is zero!
                ptTaz[i][i].interchanges += vals[j];
            }
            ptTaz[i][i].interchanges *= weight;
        }

        //maxWaiting
        for (int i = 0; i < ptTaz.length; ++i) {
            for (int j = 0; j < ptTaz[i].length; ++j) {
                vals[j] = ptTaz[i][j].maxWaiting;
            }
            Arrays.sort(vals);
            ptTaz[i][i].maxWaiting = 0;
            for (int j = 1; j <= num; ++j) { // first element is zero!
                ptTaz[i][i].maxWaiting += vals[j];
            }
            ptTaz[i][i].maxWaiting *= weight;
        }

        //sumWaiting
        for (int i = 0; i < ptTaz.length; ++i) {
            for (int j = 0; j < ptTaz[i].length; ++j) {
                vals[j] = ptTaz[i][j].sumWaiting;
            }
            Arrays.sort(vals);
            ptTaz[i][i].sumWaiting = 0;
            for (int j = 1; j <= num; ++j) { // first element is zero!
                ptTaz[i][i].sumWaiting += vals[j];
            }
            ptTaz[i][i].sumWaiting *= weight;
        }

        //access
        for (int i = 0; i < ptTaz.length; ++i) {
            for (int j = 0; j < ptTaz[i].length; ++j) {
                vals[j] = ptTaz[i][j].access;
            }
            Arrays.sort(vals);
            ptTaz[i][i].access = 0;
            for (int j = 1; j <= num; ++j) { // first element is zero!
                ptTaz[i][i].access += vals[j];
            }
            ptTaz[i][i].access *= weight;
        }

        //egress
        for (int i = 0; i < ptTaz.length; ++i) {
            for (int j = 0; j < ptTaz[i].length; ++j) {
                vals[j] = ptTaz[i][j].egress;
            }
            Arrays.sort(vals);
            ptTaz[i][i].egress = 0;
            for (int j = 1; j <= num; ++j) { // first element is zero!
                ptTaz[i][i].egress += vals[j];
            }
            ptTaz[i][i].egress *= weight;
        }

        //dist
        for (int i = 0; i < ptTaz.length; ++i) {
            for (int j = 0; j < ptTaz[i].length; ++j) {
                vals[j] = ptTaz[i][j].dist;
            }
            Arrays.sort(vals);
            ptTaz[i][i].dist = 0;
            for (int j = 1; j <= num; ++j) { // first element is zero!
                ptTaz[i][i].dist += vals[j];
            }
            ptTaz[i][i].dist *= weight;
        }
    }

    class PTTAZInfo {
        double tt;
        double interchanges;
        double maxWaiting;
        double sumWaiting;
        double access;
        double egress;
        double dist;

    }

    public class TAZ {
        public int taz_id;
        public int stat_area;
        public int weight = 0;
        public double lat;
        public double lon;
        public String description;
        public boolean ignore = false;

        public double getDistance(TAZ ref) {

            return TPS_Geometrics.getDistance(this.lat, this.lon, ref.lat, ref.lon);

        }
    }

}

