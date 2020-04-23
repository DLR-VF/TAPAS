package de.dlr.ivf.tapas.tools;

import de.dlr.ivf.tapas.persistence.db.TPS_DB_Connector;
import de.dlr.ivf.tapas.util.parameters.TPS_ParameterClass;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.StringTokenizer;

/**
 * Class to calculate the PT score for blocks and TAZs
 * <p>
 * Sample usage:
 * java PTScoreCalculator berlin "T:\Aufbereitung\1 Berlin\Raumsystem\Öv-Haltestellen\U-Bus-Tram-Fähr-Bahnhöfe.txt" "T:\Aufbereitung\1 Berlin\Raumsystem\Öv-Haltestellen\S-Bahnhöfe-2.txt" pt-2008
 *
 * @author hein_mh
 */
public class TPS_PTScoreCalculator {
    /**
     * internal reference to the db-connection-manager
     */
    TPS_DB_Connector dbConnection;
    /**
     * HashMap for the Stations
     */
    HashMap<Integer, PTStation> stations = new HashMap<>();

    /**
     * the one and only standard-constructor
     *
     * @param connection reference to the db-connection-manager
     */
    public TPS_PTScoreCalculator(TPS_DB_Connector connection) {
        dbConnection = connection;
    }

    /**
     * @param args
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        if (args.length != 4) {
            System.out.println("Usage: PTScoreCalculator <region> <bvg-file> <sbahn-file> <key-name>");
            return;
        }

        File configFile = new File("T:/Simulationen/runtime_perseus.csv");

        TPS_ParameterClass parameterClass = new TPS_ParameterClass();
        parameterClass.loadRuntimeParameters(configFile);
        TPS_PTScoreCalculator worker = new TPS_PTScoreCalculator(new TPS_DB_Connector(parameterClass));
        worker.readDatabase(args[0]);
        worker.readStationsBVG(args[1]);
        worker.readStationsSBahn(args[2]);
        //worker.writeDB(args[0]+"_pt_stops", args[3]);
        //worker.calcScoresBlock(args[0], args[3],1200);
        worker.calcScoresTAZ(args[0] + "_taz_1223_multiline", args[0] + "_pt_stops", args[0] + "_taz_1223",
                args[0] + "_taz_scores", args[3], 1200);
    }

    public void calcScoresBlock(String region, String key, double radius) {
        String query = "";
        ResultSet rs;
        ArrayList<Integer> blockIds = new ArrayList<>();
        HashMap<Integer, Double> scores = new HashMap<>();
        int blockId;
        int[][] numLines = new int[2][6]; //bus, night bus, subway, train, tram, ferry for two distance classes class
        double score, minScore = 1e100, maxScore = 0, distance;

        try {
            //first, read all block ids to loop over
            System.out.println("Reading blocknumbers");
            query = "SELECT blocknr :: integer FROM core." + region + "_blocks_multiline";

            rs = this.dbConnection.executeQuery(query, this);
            while (rs.next()) {
                blockIds.add(rs.getInt("blocknr"));
            }
            rs.close();
            System.out.println("Calculating scores for " + blockIds.size() + " blocks");
            for (Integer ids : blockIds) {
                //get the id
                blockId = ids;
                //set counters to zero
                for (int i = 0; i < numLines.length; ++i) {
                    for (int j = 0; j < numLines[0].length; ++j) {
                        numLines[i][j] = 0;
                    }
                }
                //get all stations within radius around the block center
                query = "SELECT " + "distTable.num_bus, " + "distTable.num_nightbus, " + "distTable.num_subway, " +
                        "distTable.num_localtrain, " + "distTable.num_tramway, " + "distTable.num_ferry, " +
                        "distTable.distance " +
                        "FROM (SELECT name, num_bus, num_nightbus, num_subway, num_localtrain, num_tramway, num_ferry, distance_sphere(pt_stop_coordinate, (SELECT st_centroid(the_geom) FROM core." +
                        region + "_blocks_multiline WHERE blocknr = " + blockId + ")) as distance FROM core." + region +
                        "_pt_stops) as distTable " + "WHERE distTable.distance<=" + radius;

                rs = this.dbConnection.executeQuery(query, this);


                while (rs.next()) {
                    distance = rs.getDouble("distance");

                    //new calculation  scheme
                    if (distance <= radius * 0.5) {
                        numLines[0][0] += rs.getInt("num_bus");
                        numLines[0][1] += rs.getInt("num_nightbus");
                        numLines[0][2] += rs.getInt("num_subway");
                        numLines[0][3] += rs.getInt("num_localtrain");
                        numLines[0][4] += rs.getInt("num_tramway");
                        numLines[0][5] += rs.getInt("num_ferry");
                    } else {
                        numLines[1][0] += rs.getInt("num_bus");
                        numLines[1][1] += rs.getInt("num_nightbus");
                        numLines[1][2] += rs.getInt("num_subway");
                        numLines[1][3] += rs.getInt("num_localtrain");
                        numLines[1][4] += rs.getInt("num_tramway");
                        numLines[1][5] += rs.getInt("num_ferry");
                    }


                    //old calculation  scheme
//					if(distance <= radius*0.5){
//						numLines[0][0]+=rs.getInt("num_bus")>0?1:0;
//						numLines[0][1]+=rs.getInt("num_nightbus")>0?1:0;
//						numLines[0][2]+=rs.getInt("num_subway")>0?1:0;
//						numLines[0][3]+=rs.getInt("num_localtrain")>0?1:0;
//						numLines[0][4]+=rs.getInt("num_tramway")>0?1:0;
//						numLines[0][5]+=rs.getInt("num_ferry")>0?1:0;
//					}
//					else{
//						numLines[1][0]+=rs.getInt("num_bus")>0?1:0;
//						numLines[1][1]+=rs.getInt("num_nightbus")>0?1:0;
//						numLines[1][2]+=rs.getInt("num_subway")>0?1:0;
//						numLines[1][3]+=rs.getInt("num_localtrain")>0?1:0;
//						numLines[1][4]+=rs.getInt("num_tramway")>0?1:0;
//						numLines[1][5]+=rs.getInt("num_ferry")>0?1:0;
//					}

                }
                rs.close();
                //calc score for closer distance
                score = (numLines[0][0] + numLines[0][4]) * 0.5 + (numLines[0][2] + numLines[0][3]) * 1.0 +
                        (numLines[0][5]) * 0.25;
                //calc score for farer distance
                score += (numLines[1][0] + numLines[1][4]) * 0.2 + (numLines[1][2] + numLines[1][3]) * 0.8 +
                        (numLines[1][5]) * 0.0;
                score = Math.max(0.1, score);
                //System.out.println("Block: "+blockId+" score: "+doubleToStringWithDecimalPlaces(score,1));
                minScore = Math.min(minScore, score);
                maxScore = Math.max(maxScore, score);
                scores.put(blockId, score);

            }
            System.out.println("minScore: " + minScore + " maxScore: " + maxScore);

            //calc 5 cathegories:
            double intervalStep = (maxScore - minScore) / 5.0;
            double[] intervalBorders = new double[4];

            for (int i = 0; i < intervalBorders.length; ++i) {
                intervalBorders[i] = minScore + (i + 1) * intervalStep;
            }
            int scoreCathegory;
            System.out.println("Commiting to db");
            for (Entry<Integer, Double> entry : scores.entrySet()) {
                //get ids
                blockId = entry.getKey();
                score = entry.getValue();
                //look if blocknumber exists
                query = "SELECT blk_id FROM core." + region + "_blocks WHERE blk_id = " + blockId;
                rs = this.dbConnection.executeQuery(query, this);
                if (!rs.next()) { //this block does not exist in our list
                    System.out.println("Block " + blockId + " does not exist in blocks table!");
                    rs.close();
                    continue;
                }
                rs.close();

                //get cathegory
                for (scoreCathegory = 0; scoreCathegory < intervalBorders.length; ++scoreCathegory) {
                    if (score < intervalBorders[scoreCathegory]) {
                        break;
                    }
                }
                //we start at one
                scoreCathegory++;
                //store value in db
                //determine update or insert
                query = "SELECT score_blk_id FROM core." + region + "_block_scores WHERE score_blk_id = " + blockId +
                        " AND score_name = '" + key + "'";
                rs = this.dbConnection.executeQuery(query, this);
                if (rs.next()) {
                    query = "UPDATE core." + region + "_block_scores SET score = " + new BigDecimal(score).setScale(1,
                            RoundingMode.HALF_UP) + ", score_cat = " + scoreCathegory + " WHERE score_blk_id = " +
                            blockId + " AND score_name = '" + key + "'";
                } else {
                    query = "INSERT INTO core." + region +
                            "_block_scores (score_blk_id, score, score_cat, score_name) VALUES (" + blockId + "," +
                            new BigDecimal(score).setScale(1, RoundingMode.HALF_UP) + "," + scoreCathegory + ",'" +
                            key + "')";
                }

                rs.close();
                //commit to db
                this.dbConnection.execute(query, this);

            }
            System.out.println("Finished!");

        } catch (SQLException e) {
            System.out.println("Error in SQL! Query: " + query);
            e.printStackTrace();
        }
    }

    public void calcScoresTAZ(String multilineTable, String ptStopTable, String tazTable, String tazScoresTable, String key, double radius) {
        String query = "";
        ResultSet rs;
        ArrayList<Integer> tazIds = new ArrayList<>();
        HashMap<Integer, Double> scores = new HashMap<>();
        int tazId;
        int[][] numLines = new int[2][6]; //bus, night bus, subway, train, tram, ferry for two distance classes class
        double score, minScore = 1e100, maxScore = 0, distance;

        try {
            //first, read all block ids to loop over
            System.out.println("Reading TAZ-numbers");
            query = "SELECT gid FROM core." + multilineTable;

            rs = this.dbConnection.executeQuery(query, this);
            while (rs.next()) {
                tazIds.add(rs.getInt("gid"));
            }
            rs.close();
            System.out.println("Calculating scores for " + tazIds.size() + " TAZs");
            for (Integer ids : tazIds) {
                //get the id
                tazId = ids;
                //set counters to zero
                for (int i = 0; i < numLines.length; ++i) {
                    for (int j = 0; j < numLines[0].length; ++j) {
                        numLines[i][j] = 0;
                    }
                }
                //get all stations within radius around the block center
                query = "SELECT " + "distTable.num_bus, " + "distTable.num_nightbus, " + "distTable.num_subway, " +
                        "distTable.num_localtrain, " + "distTable.num_tramway, " + "distTable.num_ferry, " +
                        "distTable.distance " +
                        "FROM (SELECT name, num_bus, num_nightbus, num_subway, num_localtrain, num_tramway, num_ferry, distance_sphere(pt_stop_coordinate, (SELECT st_centroid(the_geom) FROM core." +
                        multilineTable + " WHERE gid = " + tazId + ")) as distance FROM core." + ptStopTable +
                        ") as distTable " + "WHERE distTable.distance<=" + radius;

                rs = this.dbConnection.executeQuery(query, this);


                while (rs.next()) {
                    distance = rs.getDouble("distance");

                    //new calculation  scheme
                    if (distance <= radius * 0.5) {
                        numLines[0][0] += rs.getInt("num_bus");
                        numLines[0][1] += rs.getInt("num_nightbus");
                        numLines[0][2] += rs.getInt("num_subway");
                        numLines[0][3] += rs.getInt("num_localtrain");
                        numLines[0][4] += rs.getInt("num_tramway");
                        numLines[0][5] += rs.getInt("num_ferry");
                    } else {
                        numLines[1][0] += rs.getInt("num_bus");
                        numLines[1][1] += rs.getInt("num_nightbus");
                        numLines[1][2] += rs.getInt("num_subway");
                        numLines[1][3] += rs.getInt("num_localtrain");
                        numLines[1][4] += rs.getInt("num_tramway");
                        numLines[1][5] += rs.getInt("num_ferry");
                    }

                    //old calculation  scheme
//					if(distance <= radius*0.5){
//						numLines[0][0]+=rs.getInt("num_bus")>0?1:0;
//						numLines[0][1]+=rs.getInt("num_nightbus")>0?1:0;
//						numLines[0][2]+=rs.getInt("num_subway")>0?1:0;
//						numLines[0][3]+=rs.getInt("num_localtrain")>0?1:0;
//						numLines[0][4]+=rs.getInt("num_tramway")>0?1:0;
//						numLines[0][5]+=rs.getInt("num_ferry")>0?1:0;
//					}
//					else{
//						numLines[1][0]+=rs.getInt("num_bus")>0?1:0;
//						numLines[1][1]+=rs.getInt("num_nightbus")>0?1:0;
//						numLines[1][2]+=rs.getInt("num_subway")>0?1:0;
//						numLines[1][3]+=rs.getInt("num_localtrain")>0?1:0;
//						numLines[1][4]+=rs.getInt("num_tramway")>0?1:0;
//						numLines[1][5]+=rs.getInt("num_ferry")>0?1:0;
//					}

                }
                rs.close();
                //calc score for closer distance
                score = (numLines[0][0] + numLines[0][4]) * 0.5 + (numLines[0][2] + numLines[0][3]) * 1.0 +
                        (numLines[0][5]) * 0.25;
                //calc score for farer distance
                score += (numLines[1][0] + numLines[1][4]) * 0.2 + (numLines[1][2] + numLines[1][3]) * 0.8 +
                        (numLines[1][5]) * 0.0;
                score = Math.max(0.1, score);
                //System.out.println("Block: "+blockId+" score: "+doubleToStringWithDecimalPlaces(score,1));
                minScore = Math.min(minScore, score);
                maxScore = Math.max(maxScore, score);
                scores.put(tazId, score);

            }
            System.out.println("minScore: " + minScore + " maxScore: " + maxScore);

            //calc 5 cathegories:
            double intervalStep = (maxScore - minScore) / 5.0;
            double[] intervalBorders = new double[4];

            for (int i = 0; i < intervalBorders.length; ++i) {
                intervalBorders[i] = minScore + (i + 1) * intervalStep;
            }
            int scoreCategory;
            System.out.println("Commiting to db");
            for (Entry<Integer, Double> entry : scores.entrySet()) {
                //get ids
                tazId = entry.getKey();
                score = entry.getValue();
                //look if blocknumber exists
                query = "SELECT taz_id FROM core." + tazTable + " WHERE taz_id = " + tazId;
                rs = this.dbConnection.executeQuery(query, this);
                if (!rs.next()) { //this block does not exist in our list
                    //System.out.println("TAZ "+tazId+" does not exist in TAZ table!");
                    //rs.close();
                    //continue;
                }
                rs.close();

                //get cathegory
                for (scoreCategory = 0; scoreCategory < intervalBorders.length; ++scoreCategory) {
                    if (score < intervalBorders[scoreCategory]) {
                        break;
                    }
                }
                //we start at one
                scoreCategory++;
                //store value in db
                //determine update or insert
                query = "SELECT score_taz_id FROM core." + tazScoresTable + " WHERE score_taz_id = " + tazId +
                        " AND score_name = '" + key + "'";
                rs = this.dbConnection.executeQuery(query, this);
                if (rs.next()) {
                    query = "UPDATE core." + tazScoresTable + " SET score = " + new BigDecimal(score).setScale(1,
                            RoundingMode.HALF_UP) + ", score_cat = " + scoreCategory + " WHERE score_blk_id = " +
                            tazId + " AND score_name = '" + key + "'";
                } else {
                    query = "INSERT INTO core." + tazScoresTable +
                            " (score_taz_id, score, score_cat, score_name) VALUES (" + tazId + "," + new BigDecimal(
                            score).setScale(1, RoundingMode.HALF_UP) + "," + scoreCategory + ",'" + key + "')";
                }
                rs.close();

                //commit to db
                this.dbConnection.execute(query, this);
                System.out.println(
                        "TAZ: " + tazId + " score: " + new BigDecimal(score).setScale(1, RoundingMode.HALF_UP) +
                                " cat: " + scoreCategory);

            }
            System.out.println("Finished!");

        } catch (SQLException e) {
            System.out.println("Error in SQL! Query: " + query);
            e.printStackTrace();
        }
    }

    public void readDatabase(String region) {
        String query;
        int id, num = 0;
        String name;
        try {
            query = "SELECT no,name FROM core." + region + "_pt_stops";
            System.out.println("Looking for stations: " + query);
            ResultSet rs = this.dbConnection.executeQuery(query, this);
            while (rs.next()) {
                id = rs.getInt("no");
                name = rs.getString("name");
                this.stations.put(id, new PTStation(id, name));
                num++;
            }
            rs.close();
            System.out.println("Stations found: " + num);

        } catch (SQLException e) {

            e.printStackTrace();
        }
    }

    public void readStationsBVG(String fileName) {
        FileReader in = null;
        BufferedReader input = null;
        String line;
        try {
            int id, idHub;
            PTStation actStation;
            String name;
            in = new FileReader(fileName);
            input = new BufferedReader(in);
            System.out.println("\t '--> File opened: " + fileName);
            while ((line = input.readLine()) != null) {
                if (line.startsWith("$") || line.startsWith("#")) // comment
                    continue;
                StringTokenizer tok = new StringTokenizer(line, "; \t");
                //check format
                if (tok.countTokens() != 5) continue;
                //name of line
                name = tok.nextToken();
                //service number
                tok.nextToken();
                //direction
                tok.nextToken();
                //station
                id = Integer.parseInt(tok.nextToken());
                //stationhub (should equal station)
                idHub = Integer.parseInt(tok.nextToken());
                if (id != idHub) System.out.println("BVG: Please check line: " + line);
                actStation = this.stations.get(id);
                actStation.AddLine(name);
            }
        } catch (Exception ex) {
            System.out.println("\t '--> Error: " + ex.getMessage());
            ex.printStackTrace();
        }//catch
        finally {
            try {
                if (input != null) input.close();
                if (in != null) in.close();
            }//try
            catch (Exception ex) {
                System.out.println("\t '--> Could not close : " + fileName);
            }//catch
        }//finally
    }

    public void readStationsSBahn(String fileName) {
        FileReader in = null;
        BufferedReader input = null;
        String line;
        try {
            String name, lineName;
            ArrayList<Integer> numStationsFound = new ArrayList<>();
            PTStation actStation;
            in = new FileReader(fileName);
            input = new BufferedReader(in);
            System.out.println("\t '--> File opened: " + fileName);
            while ((line = input.readLine()) != null) {
                if (line.startsWith("$") || line.startsWith("#")) // comment
                    continue;
                if (line.contains(":")) // header for new service line
                    continue;
                StringTokenizer tok = new StringTokenizer(line, ";");
                //check format
                if (tok.countTokens() < 2) continue;
                //name of station
                name = tok.nextToken();
                numStationsFound.clear();
                for (Entry<Integer, PTStation> iStation : this.stations.entrySet()) {
                    actStation = null;
                    if (iStation.getValue().isStation(name)) {
                        actStation = iStation.getValue();
                    }
                    if (actStation != null) {
                        if (!numStationsFound.contains(iStation.getKey())) numStationsFound.add(iStation.getKey());
                        while (tok.hasMoreTokens()) {
                            lineName = tok.nextToken();
                            if (lineName.startsWith("S")) actStation.AddLine(lineName);
                        }
                        //reset tok
                        tok = new StringTokenizer(line, ";");
                        tok.nextToken();
                    }
                }
                if (numStationsFound.size() != 1) {
                    System.out.println("SBahn: Entries found: " + numStationsFound + " Please check line: " + line);
                }
            }
        } catch (Exception ex) {
            System.out.println("\t '--> Error: " + ex.getMessage());
            ex.printStackTrace();
        }//catch
        finally {
            try {
                if (input != null) input.close();
                if (in != null) in.close();
            }//try
            catch (Exception ex) {
                System.out.println("\t '--> Could not close : " + fileName);
            }//catch
        }//finally

    }

    public void writeDB(String ptStopsName, String key) {
        PTStation act;
        String query;
        for (Entry<Integer, PTStation> iStation : this.stations.entrySet()) {
            act = iStation.getValue();
            System.out.println(
                    "Station: " + act.getName() + " id: " + act.getId() + " Bus: " + act.getNumBus() + " Night: " +
                            act.getNumNightService() + " Tram: " + act.getNumTram() + " Sub: " + act.getNumSubway() +
                            " Local: " + act.getNumLocalTrain() + " Ferry: " + act.getNumFerry());
            if (act.getNumBus() + act.getNumNightService() + act.getNumTram() + act.getNumSubway() +
                    act.getNumLocalTrain() + act.getNumFerry() != act.getLines().size()) {
                System.out.println("Unrecognized lines detected:");
                for (String line : act.getLines()) {
                    System.out.println("\t" + line);
                }
            }
            query = "UPDATE core." + ptStopsName + " SET " + "num_bus = " + act.getNumBus() + "," + "num_subway = " +
                    act.getNumSubway() + "," + "num_localtrain = " + act.getNumLocalTrain() + "," + "num_nightbus = " +
                    act.getNumNightService() + "," + "num_ferry = " + act.getNumFerry() + "," + "num_tramway = " +
                    act.getNumTram() + " " + "WHERE no = " + act.getId();

            this.dbConnection.execute(query, this);

/*			if( (act.getName().startsWith("S ")||act.getName().startsWith("S+U "))){
				if(act.getNumLocalTrain()==0&&!act.getName().contains("/")){
					System.out.println("Bad Station detected:");
					for(String line : act.getLines()){
						System.out.println("\t"+line);
					}
				}
			}
			else if( (act.getName().startsWith("U ")||act.getName().startsWith("S+U "))){
				if(act.getNumSubway()==0&&!act.getName().contains("/")){
					System.out.println("Bad Station detected:");
					for(String line : act.getLines()){
						System.out.println("\t"+line);
					}
				}
			}
			else if(act.getNumLocalTrain()>0 || act.getNumSubway()>0){
				System.out.println("Bad Station detected:");
				for(String line : act.getLines()){
					System.out.println("\t"+line);
				}
			}
*/
        }
    }

    class PTStation {
        int id;
        String name;
        /**
         * Buslinien
         */
        int numBus = 0;
        /**
         * Nachtbuslinien
         */
        int numNightService = 0;
        /**
         * UBahnlinien
         */
        int numSubway = 0;
        /**
         * SBahnlinien
         */
        int numLocalTrain = 0;
        /**
         * Fährlinien
         */
        int numFerry = 0;
        /**
         * Strßenbahnlinien
         */
        int numTram = 0;
        /**
         * Fernbahnlinien (Regio u.a.)
         */
        int numRegionalTrain = 0;
        /**
         * Sonstige Linien
         */
        int numMisc = 0;

        ArrayList<String> lines = new ArrayList<>();

        public PTStation(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public boolean AddLine(String line) {
            if (this.lines.contains(line)) {
                return false;
            }
            this.lines.add(line);
            if (line.startsWith("U")) this.numSubway++;
            else if (line.startsWith("S")) this.numLocalTrain++;
            else if (line.startsWith("B-N")) this.numNightService++;
            else if (line.startsWith("B-")) this.numBus++;
            else if (line.startsWith("T-")) this.numTram++;
            else if (line.startsWith("FW-")) this.numFerry++;
            return true;
        }

        public int getId() {
            return id;
        }

        public ArrayList<String> getLines() {
            return lines;
        }

        public String getName() {
            return name;
        }

        public int getNumBus() {
            return numBus;
        }

        public int getNumFerry() {
            return numFerry;
        }

        public int getNumLocalTrain() {
            return numLocalTrain;
        }

        public int getNumMisc() {
            return numMisc;
        }

        public int getNumNightService() {
            return numNightService;
        }

        public int getNumRegionalTrain() {
            return numRegionalTrain;
        }

        public int getNumSubway() {
            return numSubway;
        }

        public int getNumTram() {
            return numTram;
        }

        public boolean isStation(String testName) {
            testName = testName.replace("traße", "tr.");
            testName = testName.replace("Betriebsbf.", "Betriebsbahnhof");

            if (this.name.compareToIgnoreCase("S " + testName) == 0) return true;
            return this.name.compareToIgnoreCase("S+U " + testName) == 0;
        }

    }

}
