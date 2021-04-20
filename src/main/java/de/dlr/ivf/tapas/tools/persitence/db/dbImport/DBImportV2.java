/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.tools.persitence.db.dbImport;

import de.dlr.ivf.tapas.persistence.db.TPS_DB_Connector;
import de.dlr.ivf.tapas.tools.fileModifier.TPS_ModeChoiceTreeConverter;
import de.dlr.ivf.tapas.util.parameters.TPS_ParameterClass;

import java.io.*;
import java.sql.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class DBImportV2 {

    TPS_DB_Connector dbCon = null;

    public boolean createRegion(String name) {
        String query = "";
        try {
            //create new region db
            Connection con = this.dbCon.getConnection(this);
            query = "SELECT * FROM core.create_region_based_tables('" + name + "', 'core')";
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(query);
            boolean result = false;
            if (rs.next()) {
                result = rs.getBoolean(1);
            }
            st.close();
            return result;
        } catch (SQLException e) {
            System.err.println("SQL-Error in createRegion for region " + name + " and query " + query);
            e.printStackTrace();
        }
        return false;
    }

    protected void finalize() {
        this.dbCon.closeConnections();
    }

    public boolean importAct2Loc(File file, String region) {
        String query = "";
        try {
            //load data
            List<Act2Loc> act2Loc = new LinkedList<>();
            BufferedReader in = new BufferedReader(new FileReader(file));
            String line;

            while ((line = in.readLine()) != null) {
                if (line.startsWith("CODE") || line.startsWith("activity")) //header
                    continue;
                String[] tok = line.split("[ \t;]");
                if (tok.length >= 2) {
                    Act2Loc tmp = new Act2Loc();
                    tmp.actID = Integer.parseInt(tok[0]);
                    tmp.locID = Integer.parseInt(tok[1]);

                    StringBuilder comment = new StringBuilder();
                    if (tok.length > 2) {
                        comment = new StringBuilder(tok[2]);
                        for (int i = 3; i < tok.length; ++i) {
                            comment.append(" ").append(tok[i]);
                        }
                    }
                    tmp.comment = comment.toString();

                    act2Loc.add(tmp);
                }
            }
            in.close();

            //store into db
            Connection con = this.dbCon.getConnection(this);
            query = "INSERT INTO core." + region + "_act_2_loc_code (act_code, loc_code, comment) VALUES (?, ?,?)";
            PreparedStatement st = con.prepareStatement(query);
            for (Act2Loc act : act2Loc) {
                st.setInt(1, act.actID);
                st.setInt(2, act.locID);
                st.setString(3, act.comment);
                st.execute();
            }
            st.close();
            return true;

        } catch (SQLException e) {
            System.err.println("SQL-Error in importAct2Loc for file " + file.getAbsolutePath() + " and query " + query);
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            System.err.println(
                    "File-Error in importAct2Loc for file " + file.getAbsolutePath() + " and query " + query);
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("IO-Error in importAct2Loc for file " + file.getAbsolutePath() + " and query " + query);
            e.printStackTrace();
        }


        return false;
    }

    public boolean importBlock(File file, String region, boolean adoptIDs) {
        String query = "";
        try {
            Map<Integer, Integer> idMap = new HashMap<>();

            //get id mapping
            Connection con = this.dbCon.getConnection(this);
            Statement st = con.createStatement();
            query = "SELECT taz_id, taz_num_id from core." + region + "_taz";
            ResultSet rs = st.executeQuery(query);
            while (rs.next()) {
                if (adoptIDs) idMap.put(rs.getInt("taz_num_id"), rs.getInt("taz_id"));
                else idMap.put(rs.getInt("taz_id"), rs.getInt("taz_id")); //1:1 mapping
            }
            st.close();
            //load data
            List<Block> blocks = new LinkedList<>();
            BufferedReader in = new BufferedReader(new FileReader(file));
            String line;

            while ((line = in.readLine()) != null) {
                String[] tok = line.split("[;\t,]");
                if (tok.length == 4) {
                    Block tmp = new Block();
                    tmp.blockID = Integer.parseInt(tok[0]);
                    tmp.tazID = idMap.get(Integer.parseInt(tok[1]));
                    tmp.lat = Double.parseDouble(tok[2]);
                    tmp.lon = Double.parseDouble(tok[3]);

                    blocks.add(tmp);
                }
            }
            in.close();

            //store into db
            query = "INSERT INTO core." + region +
                    "_blocks (blk_id, blk_taz_id, taz_coordinate) VALUES (?, ?, st_setsrid(st_makepoint(?,?), 4326))";
            PreparedStatement pSt = con.prepareStatement(query);
            for (Block tmp : blocks) {
                pSt.setInt(1, tmp.blockID);
                pSt.setInt(2, tmp.tazID);
                pSt.setDouble(3, tmp.lat);
                pSt.setDouble(4, tmp.lon);
                pSt.execute();
            }
            pSt.close();
            return true;

        } catch (SQLException e) {
            System.err.println("SQL-Error in importBlock for file " + file.getAbsolutePath() + " and query " + query);
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            System.err.println("File-Error in importBlock for file " + file.getAbsolutePath() + " and query " + query);
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("IO-Error in importBlock for file " + file.getAbsolutePath() + " and query " + query);
            e.printStackTrace();
        }


        return false;
    }

    public boolean importBlockNextPTStop(File file, String region, String name) {
        String query = "";
        try {
            //load data
            List<BlockPTStop> blocks = new LinkedList<>();
            BufferedReader in = new BufferedReader(new FileReader(file));
            String line;

            while ((line = in.readLine()) != null) {
                String[] tok = line.split("[;\t,]");
                if (tok.length == 2) {
                    BlockPTStop tmp = new BlockPTStop();
                    tmp.blockID = Integer.parseInt(tok[0]);
                    tmp.distance = Double.parseDouble(tok[1]);

                    blocks.add(tmp);
                }
            }
            in.close();

            //store into db
            Connection con = this.dbCon.getConnection(this);
            query = "INSERT INTO core." + region +
                    "_block_next_pt_stop (next_pt_stop_blk_id, next_pt_stop, next_pt_stop_name) VALUES (?, ?, '" +
                    name + "')";
            PreparedStatement pSt = con.prepareStatement(query);
            for (BlockPTStop tmp : blocks) {
                pSt.setInt(1, tmp.blockID);
                pSt.setDouble(2, tmp.distance);
                pSt.execute();
            }
            pSt.close();
            return true;

        } catch (SQLException e) {
            System.err.println("SQL-Error in importBlock for file " + file.getAbsolutePath() + " and query " + query);
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            System.err.println("File-Error in importBlock for file " + file.getAbsolutePath() + " and query " + query);
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("IO-Error in importBlock for file " + file.getAbsolutePath() + " and query " + query);
            e.printStackTrace();
        }
        return false;
    }

    public boolean importCFN4(File file, String region) {

        String query = "";
        try {
            //load data
            List<CFN4> cfn4s = new LinkedList<>();
            BufferedReader in = new BufferedReader(new FileReader(file));
            String line;

            while ((line = in.readLine()) != null) {
                if (line.startsWith("CUR")) //header
                    continue;
                String[] tok = line.split("[\t;,]");
                if (tok.length >= 3) {
                    CFN4 tmp = new CFN4();
                    tmp.actCode = Integer.parseInt(tok[0]);
                    tmp.bbrCode = Integer.parseInt(tok[1]);
                    tmp.value = Double.parseDouble(tok[2]);
                    if (tok.length > 3) {
                        tmp.comment = tok[3];
                        if (tok.length == 4) {
                            tmp.raumtyp = tok[3];
                        }
                    }

                    cfn4s.add(tmp);
                }
            }
            in.close();

            //store into db
            Connection con = this.dbCon.getConnection(this);
            query = "INSERT INTO core." + region +
                    "_cfn4 (\"CURRENT_EPISODE_ACTIVITY_CODE_TAPAS\", \"CURRENT_TAZ_SETTLEMENT_CODE_TAPAS\", value, comment, raumtyp) VALUES (?,?,?,?,?)";
            PreparedStatement st = con.prepareStatement(query);
            for (CFN4 cfn4 : cfn4s) {
                st.setInt(1, cfn4.actCode);
                st.setInt(2, cfn4.bbrCode);
                st.setDouble(3, cfn4.value);
                st.setString(4, cfn4.comment);
                st.setString(5, cfn4.raumtyp);
                st.execute();
            }
            st.close();
            return true;

        } catch (SQLException e) {
            System.err.println("SQL-Error in importCFN4 for file " + file.getAbsolutePath() + " and query " + query);
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            System.err.println("File-Error in importCFN4 for file " + file.getAbsolutePath() + " and query " + query);
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("IO-Error in importCFN4 for file " + file.getAbsolutePath() + " and query " + query);
            e.printStackTrace();
        }

        return false;
    }

    public boolean importCFN4Ind(File file, String region) {
        String query = "";
        try {
            //load data
            List<CFN4Index> cfn4Indexes = new LinkedList<>();
            BufferedReader in = new BufferedReader(new FileReader(file));
            String line;

            while ((line = in.readLine()) != null) {
                if (line.startsWith("Special")) //header
                    continue;
                String[] tok = line.split("[\t;,]");
                if (tok.length == 5) {
                    CFN4Index tmp = new CFN4Index();
                    tmp.name = tok[0];
                    tmp.bbr = Integer.parseInt(tok[1]);
                    tmp.value = Double.parseDouble(tok[2]);
                    tmp.comment = tok[3];
                    tmp.raumtyp = tok[4];

                    cfn4Indexes.add(tmp);
                }
            }
            in.close();

            //store into db
            Connection con = this.dbCon.getConnection(this);
            query = "INSERT INTO core." + region +
                    "_cfn4_ind (name, \"CURRENT_TAZ_SETTLEMENT_CODE_TAPAS\", value, comment, raumtyp) VALUES (?,?,?,?,?)";
            PreparedStatement st = con.prepareStatement(query);
            for (CFN4Index cfn4Ind : cfn4Indexes) {
                st.setString(1, cfn4Ind.name);
                st.setInt(2, cfn4Ind.bbr);
                st.setDouble(3, cfn4Ind.value);
                st.setString(4, cfn4Ind.comment);
                st.setString(5, cfn4Ind.raumtyp);
                st.execute();
            }
            st.close();
            return true;

        } catch (SQLException e) {
            System.err.println("SQL-Error in importCFN4Ind for file " + file.getAbsolutePath() + " and query " + query);
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            System.err.println(
                    "File-Error in importCFN4Ind for file " + file.getAbsolutePath() + " and query " + query);
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("IO-Error in importCFN4Ind for file " + file.getAbsolutePath() + " and query " + query);
            e.printStackTrace();
        }

        return false;
    }

    public boolean importHH(File file, String region, String name, boolean adoptIDs) {
        String query = "";
        try {
            Map<Integer, Integer> idMap = new HashMap<>();
            List<HouseHold> hhs = new LinkedList<>();
            //get id mapping
            Connection con = this.dbCon.getConnection(this);
            Statement st = con.createStatement();
            query = "SELECT taz_id, taz_num_id from core." + region + "_taz";
            ResultSet rs = st.executeQuery(query);
            while (rs.next()) {
                if (adoptIDs) idMap.put(rs.getInt("taz_num_id"), rs.getInt("taz_id"));
                else idMap.put(rs.getInt("taz_id"), rs.getInt("taz_id")); //1:1 mapping
            }
            st.close();

            //load data
            BufferedReader in = new BufferedReader(new FileReader(file));
            String line;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("id")) {//header
                    continue;
                }
                String[] tok = line.split("[;\t,]");

                if (tok.length == 8) {
                    HouseHold tmp = new HouseHold();
                    tmp.id = Integer.parseInt(tok[0]);
                    //ignore tok[1] "gemeinde"
                    tmp.taz = idMap.get(Integer.parseInt(tok[2]));
                    tmp.lat = Double.parseDouble(tok[3]);
                    tmp.lon = Double.parseDouble(tok[4]);
                    tmp.persons = Integer.parseInt(tok[5]);
                    tmp.cars = Integer.parseInt(tok[6]);
                    tmp.income = Integer.parseInt(tok[7]);
                    hhs.add(tmp);
                }
            }
            in.close();

            //store into db
            query = "INSERT INTO core." + region +
                    "_households (hh_id, hh_persons, hh_cars, hh_income, hh_taz_id, hh_key, hh_coordinate) VALUES (?,?,?,?,?,'" +
                    name + "',st_setsrid(st_makepoint(?,?), 4326))";
            PreparedStatement pSt = con.prepareStatement(query);
            int chunk = 0;
            int chunksize = 128;

            for (HouseHold tmp : hhs) {
                pSt.setInt(1, tmp.id);
                pSt.setInt(2, tmp.persons);
                pSt.setInt(3, tmp.cars);
                pSt.setInt(4, tmp.income);
                pSt.setInt(5, tmp.taz);
                pSt.setDouble(6, tmp.lat);
                pSt.setDouble(7, tmp.lon);
                pSt.addBatch();
                chunk++;
                if (chunk == chunksize) {
                    pSt.executeBatch();
                    chunk = 0;
                }
            }
            pSt.executeBatch();
            pSt.close();
            return true;

        } catch (SQLException e) {
            System.err.println("SQL-Error in importHH for file " + file.getAbsolutePath() + " and query " + query);
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            System.err.println("File-Error in importHH for file " + file.getAbsolutePath() + " and query " + query);
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("IO-Error in importHH for file " + file.getAbsolutePath() + " and query " + query);
            e.printStackTrace();
        }
        return false;
    }

    public boolean importIntraMIT(File file, String region, String name, boolean adoptIDs) {
        String query = "";
        try {
            Map<Integer, Integer> idMap = new HashMap<>();

            //get id mapping
            Connection con = this.dbCon.getConnection(this);
            Statement st = con.createStatement();
            query = "SELECT taz_id, taz_num_id from core." + region + "_taz";
            ResultSet rs = st.executeQuery(query);
            while (rs.next()) {
                if (adoptIDs) idMap.put(rs.getInt("taz_num_id"), rs.getInt("taz_id"));
                else idMap.put(rs.getInt("taz_id"), rs.getInt("taz_id")); //1:1 mapping
            }
            st.close();


            //load data
            List<TAZIntraMITInfo> intraMITInfo = new LinkedList<>();
            BufferedReader in = new BufferedReader(new FileReader(file));
            String line;

            while ((line = in.readLine()) != null) {
                if (line.startsWith("TVZ")) //header
                    continue;
                String[] tok = line.split("[;\t, ]");

                if (tok.length == 3 || tok.length == 4) {
                    TAZIntraMITInfo tmp = new TAZIntraMITInfo();
                    tmp.tazID = idMap.get(Integer.parseInt(tok[0]));
                    tmp.blFactor = Double.parseDouble(tok[1]);
                    tmp.avgSpeed = Double.parseDouble(tok[2]);
                    if (tok.length == 4) tmp.hasIntraInfo = Boolean.parseBoolean(tok[3]);

                    intraMITInfo.add(tmp);
                }
            }
            in.close();

            //store into db
            query = "INSERT INTO core." + region +
                    "_taz_intra_mit_infos (info_taz_id, beeline_factor_mit,average_speed_mit, info_name, has_intra_traffic_mit) VALUES " +
                    "	(?, ?,?, '" + name + "',?)";
            PreparedStatement pSt = con.prepareStatement(query);
            for (TAZIntraMITInfo tmp : intraMITInfo) {
                pSt.setInt(1, tmp.tazID);
                pSt.setDouble(2, tmp.blFactor);
                pSt.setDouble(3, tmp.avgSpeed);
                pSt.setBoolean(4, tmp.hasIntraInfo);
                pSt.execute();
            }
            pSt.close();
            return true;
        } catch (SQLException e) {
            System.err.println(
                    "SQL-Error in importIntraMIT for file " + file.getAbsolutePath() + " and query " + query);
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            System.err.println(
                    "File-Error in importIntraMIT for file " + file.getAbsolutePath() + " and query " + query);
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("IO-Error in importIntraMIT for file " + file.getAbsolutePath() + " and query " + query);
            e.printStackTrace();
        }

        return false;
    }

    public boolean importIntraPT(File file, String region, String name, boolean adoptIDs) {
        String query = "";
        try {
            Map<Integer, Integer> idMap = new HashMap<>();

            //get id mapping
            Connection con = this.dbCon.getConnection(this);
            Statement st = con.createStatement();
            query = "SELECT taz_id, taz_num_id from core." + region + "_taz";
            ResultSet rs = st.executeQuery(query);
            while (rs.next()) {
                if (adoptIDs) idMap.put(rs.getInt("taz_num_id"), rs.getInt("taz_id"));
                else idMap.put(rs.getInt("taz_id"), rs.getInt("taz_id")); //1:1 mapping
            }
            st.close();


            //load data
            List<TAZIntraPTInfo> intraMITInfo = new LinkedList<>();
            BufferedReader in = new BufferedReader(new FileReader(file));
            String line;

            while ((line = in.readLine()) != null) {
                if (line.startsWith("TVZ")) //header
                    continue;
                String[] tok = line.split("[;\t, ]");

                if (tok.length >= 2 && tok.length <= 4) {
                    TAZIntraPTInfo tmp = new TAZIntraPTInfo();
                    tmp.tazID = idMap.get(Integer.parseInt(tok[0]));
                    tmp.avgSpeed = Double.parseDouble(tok[1]);
                    if (tok.length >= 3) tmp.hasIntraInfo = Boolean.parseBoolean(tok[2]);
                    if (tok.length == 4) tmp.ptZone = Integer.parseInt(tok[3]);

                    intraMITInfo.add(tmp);
                }
            }
            in.close();

            //store into db
            query = "INSERT INTO core." + region +
                    "_taz_intra_pt_infos (info_taz_id, average_speed_pt, info_name, has_intra_traffic_pt, pt_zone) VALUES " +
                    "	(?, ?,'" + name + "',?,?)";
            PreparedStatement pSt = con.prepareStatement(query);
            for (TAZIntraPTInfo tmp : intraMITInfo) {
                pSt.setInt(1, tmp.tazID);
                pSt.setDouble(2, tmp.avgSpeed);
                pSt.setBoolean(3, tmp.hasIntraInfo);
                pSt.setInt(4, tmp.ptZone);
                pSt.execute();
            }
            pSt.close();
            return true;
        } catch (SQLException e) {
            System.err.println("SQL-Error in importIntraPT for file " + file.getAbsolutePath() + " and query " + query);
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            System.err.println(
                    "File-Error in importIntraPT for file " + file.getAbsolutePath() + " and query " + query);
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("IO-Error in importIntraPT for file " + file.getAbsolutePath() + " and query " + query);
            e.printStackTrace();
        }
        return false;
    }

    public boolean importLocations(File file, String region, boolean adoptIDs) {
        String query = "";
        try {
            Map<Integer, Integer> idMap = new HashMap<>();
            List<Location> locs = new LinkedList<>();
            //get id mapping
            Connection con = this.dbCon.getConnection(this);
            Statement st = con.createStatement();
            query = "SELECT taz_id, taz_num_id from core." + region + "_taz";
            ResultSet rs = st.executeQuery(query);
            while (rs.next()) {
                if (adoptIDs) idMap.put(rs.getInt("taz_num_id"), rs.getInt("taz_id"));
                else idMap.put(rs.getInt("taz_id"), rs.getInt("taz_id")); //1:1 mapping
            }
            st.close();

            //load data
            BufferedReader in = new BufferedReader(new FileReader(file));
            String line;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("Loc_ID"))//header
                    continue;
                String[] tok = line.split("[;\t,]");
                if (tok.length == 8) {
                    Location loc = new Location();
                    // parses the content out of the file line
                    loc.id = Integer.parseInt(tok[0]);
                    loc.code = Integer.parseInt(tok[1]);
                    loc.enterprise = tok[2];
                    loc.capacity = Integer.parseInt(tok[3]);
                    loc.lat = Double.parseDouble(tok[4]);
                    loc.lon = Double.parseDouble(tok[5]);
                    loc.tazID = idMap.get(Integer.parseInt(tok[6]));
                    loc.blockID = Integer.parseInt(tok[7]);

                    locs.add(loc);

                }
            }
            in.close();


            //store into db
            query = "INSERT INTO core." + region + "_locations (" + "loc_id, loc_blk_id, loc_taz_id, " +
                    "loc_code, loc_enterprise, loc_coordinate,loc_capacity) VALUES (?,?,?,?,?, st_setsrid(st_makepoint(?,?), 4326),?)";
            PreparedStatement pSt = con.prepareStatement(query);
            for (Location loc : locs) {
                pSt.setInt(1, loc.id);
                pSt.setInt(2, loc.blockID);
                pSt.setInt(3, loc.tazID);
                pSt.setInt(4, loc.code);
                pSt.setString(5, loc.enterprise);
                pSt.setDouble(6, loc.lat);
                pSt.setDouble(7, loc.lon);
                pSt.setInt(8, loc.capacity);
                pSt.execute();
            }
            pSt.close();
            return true;

        } catch (SQLException e) {
            System.err.println(
                    "SQL-Error in importLocations for file " + file.getAbsolutePath() + " and query " + query);
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            System.err.println(
                    "File-Error in importLocations for file " + file.getAbsolutePath() + " and query " + query);
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println(
                    "IO-Error in importLocations for file " + file.getAbsolutePath() + " and query " + query);
            e.printStackTrace();
        }
        return false;
    }

    public boolean importMatrix(File file, String region, String name) {
        StringBuilder query = new StringBuilder();
        try {
            //load data
            List<Integer> values = new LinkedList<>();
            BufferedReader in = new BufferedReader(new FileReader(file));
            String line;

            while ((line = in.readLine()) != null) {
                String[] tok = line.split("[;\t, ]");
                for (String s : tok) {
                    values.add(Integer.parseInt(s));
                }
            }
            in.close();

            //store into db
            Connection con = this.dbCon.getConnection(this);
            query = new StringBuilder(
                    "INSERT INTO core." + region + "_matrices (matrix_name, matrix_values) VALUES " + "('" + name +
                            "', " + "ARRAY[");

            StringBuilder arrayPart = new StringBuilder();
            for (Integer matV : values) {
                arrayPart.append(",").append(matV);
            }
            arrayPart.deleteCharAt(0); // delete first comma

            query.append(arrayPart).append("])");
            Statement st = con.createStatement();
            st.execute(query.toString());

            //generate a default matrixmap-entry
            query = new StringBuilder("INSERT INTO core." + region +
                    "_matrixmap (\"matrixMap_name\", \"matrixMap_num\",\"matrixMap_matrixNames\",\"matrixMap_distribution\") " +
                    "VALUES ('" + name + "', 1, ARRAY['" + name + "'], ARRAY[24])");
            st.execute(query.toString());
            st.close();
            return true;

        } catch (SQLException e) {
            System.err.println("SQL-Error in importMatrix for file " + file.getAbsolutePath() + " and query " + query);
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            System.err.println("File-Error in importMatrix for file " + file.getAbsolutePath() + " and query " + query);
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("IO-Error in importMatrix for file " + file.getAbsolutePath() + " and query " + query);
            e.printStackTrace();
        }
        return false;
    }

    public boolean importModeChoice(File file, String name) {
        String query = "";
        try {
            //load data
            List<ModeChoice> mct = new LinkedList<>();
            BufferedReader in = new BufferedReader(new FileReader(file));
            String line;
            TPS_ModeChoiceTreeConverter converter = new TPS_ModeChoiceTreeConverter(".", ".", ".");

            while ((line = in.readLine()) != null) {
                if (line.startsWith("\"id\"")) //header
                    continue;
                String[] tok = line.replaceAll("\"", "").split("[;\t,]");
                if (tok.length == 7) {
                    ModeChoice tmp = new ModeChoice();
                    tmp.node = Integer.parseInt(tok[0]);
                    tmp.parent = Integer.parseInt(tok[3]);
                    //array
                    tmp.attribtes = "ARRAY[" + tok[4].trim().replaceAll(" ", ",") + "]::integer[]";

                    //we need to convert the names in the split value-attribute!
                    tmp.splitVariable = converter.map.get(tok[5]);
                    //array
                    tmp.distribution = "ARRAY[" + tok[6].trim().replaceAll(" ", ",") + "]::double precision[]";

                    mct.add(tmp);
                }
            }
            in.close();


            //store into db
            Connection con = this.dbCon.getConnection(this);

            Statement st = con.createStatement();
            for (ModeChoice tmp : mct) {
                if (tmp.splitVariable == null) {
                    query = "INSERT into core.global_mode_choice_trees (name, node_id, parent_node_id, attribute_values, distribution) " +
                            "	VALUES(" + "'" + name + "'," + tmp.node + "," + tmp.parent + "," + tmp.attribtes +
                            "," + tmp.distribution + ")";
                } else {
                    query = "INSERT into core.global_mode_choice_trees (name, node_id, parent_node_id, attribute_values, split_variable, distribution) " +
                            "	VALUES(" + "'" + name + "'," + tmp.node + "," + tmp.parent + "," + tmp.attribtes +
                            ",'" + tmp.splitVariable + "', " + tmp.distribution + ")";
                }

                st.execute(query);
            }
            st.close();
            return true;

        } catch (SQLException e) {
            System.err.println(
                    "SQL-Error in importModeChoice for file " + file.getAbsolutePath() + " and query " + query);
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            System.err.println(
                    "File-Error in importModeChoice for file " + file.getAbsolutePath() + " and query " + query);
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println(
                    "IO-Error in importModeChoice for file " + file.getAbsolutePath() + " and query " + query);
            e.printStackTrace();
        }
        return false;
    }

    public boolean importPersons(File file, String region, String name) {
        String query = "";
        try {
            // load data
            List<Person> persons = new LinkedList<>();
            BufferedReader in = new BufferedReader(new FileReader(file));
            String line;

            while ((line = in.readLine()) != null) {
                if (line.startsWith("id")) {
                    continue;
                }
                String[] tok = line.split("[;\t,]");

                if (tok.length == 21) {
                    Person tmp = new Person();
                    tmp.id = Integer.parseInt(tok[0]);
                    // ignore tok[1] homex
                    // ignore tok[2] homey
                    // TODO: try to find work-location?
                    // ignore tok[3] workx
                    // ignore tok[4] worky
                    // ignore tok[5] tvz
                    tmp.hhid = Integer.parseInt(tok[6]);
                    tmp.sex = Integer.parseInt(tok[7]);
                    tmp.age = Integer.parseInt(tok[8]);
                    tmp.ageStba = Integer.parseInt(tok[9]);
                    // ignore tok[10] gemeinde id
                    tmp.working = Double.parseDouble(tok[11]);
                    tmp.abo = Integer.parseInt(tok[12]);
                    // ignore tok[13] persons inhh
                    // ignore tok[14] cars in hh
                    tmp.license = Integer.parseInt(tok[15]);
                    tmp.pGroup = Integer.parseInt(tok[16]);
                    tmp.hhTyp = Integer.parseInt(tok[17]);
                    tmp.budPT = Double.parseDouble(tok[18]);
                    tmp.budIT = Double.parseDouble(tok[19]);
                    tmp.budITFix = Double.parseDouble(tok[20]);
                    persons.add(tmp);
                }
            }
            in.close();

            // store into db
            Connection con = this.dbCon.getConnection(this);
            query = "INSERT INTO core." + region + "_persons (p_key, p_work_id, " + "		p_id, p_hh_id, p_group, " +
                    "		p_sex, p_age, p_age_stba, " + "		p_working, p_abo, p_budget_pt, p_budget_it, " +
                    "		p_budget_it_fi, p_driver_license, p_has_bike" + ") VALUES ('" + name +
                    "', -1, ?, ?, ?, ?, ?,?,?,?,?,?,?,?,?)";
            PreparedStatement pSt = con.prepareStatement(query);
            query = "UPDATE core." + region + "_households SET hh_type = ? WHERE hh_key = '" + name + "' AND hh_id = ?";
            PreparedStatement pSt2 = con.prepareStatement(query);
            query = "UPDATE core." + region + "_households SET hh_has_child = TRUE WHERE hh_key = '" + name +
                    "' AND hh_id = ?";
            PreparedStatement pSt3 = con.prepareStatement(query);
            int chunk = 0, chunksize = 1024;
            for (Person tmp : persons) {
                //person data
                pSt.setInt(1, tmp.id);
                pSt.setInt(2, tmp.hhid);
                pSt.setInt(3, tmp.pGroup);
                pSt.setInt(4, tmp.sex);
                pSt.setInt(5, tmp.age);
                pSt.setInt(6, tmp.ageStba);
                pSt.setDouble(7, tmp.working);
                pSt.setInt(8, tmp.abo);
                pSt.setDouble(9, tmp.budPT);
                pSt.setDouble(10, tmp.budIT);
                pSt.setDouble(11, tmp.budITFix);
                pSt.setInt(12, tmp.license);
                pSt.setBoolean(13, tmp.hasBike);
                pSt.addBatch();
                //household data
                pSt2.setInt(1, tmp.hhTyp);
                pSt2.setInt(2, tmp.hhid);
                pSt2.addBatch();

                if (tmp.age < 6) { //update "has child"
                    pSt3.setInt(1, tmp.hhid);
                    pSt3.addBatch();
                }

                chunk++;
                if (chunk == chunksize) {
                    pSt.executeBatch();
                    pSt2.executeBatch();
                    pSt3.executeBatch();
                    chunk = 0;
                }
            }
            pSt.executeBatch();
            pSt2.executeBatch();
            pSt3.executeBatch();
            pSt.close();
            pSt2.close();
            pSt3.close();
            return true;

        } catch (SQLException e) {
            System.err.println("SQL-Error in importHH for file " + file.getAbsolutePath() + " and query " + query);
            e.getNextException().printStackTrace();
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            System.err.println("File-Error in importHH for file " + file.getAbsolutePath() + " and query " + query);
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("IO-Error in importHH for file " + file.getAbsolutePath() + " and query " + query);
            e.printStackTrace();
        }


        return false;
    }

    public boolean importScores(File file, String region, String type, String name, boolean adoptIDs) {
        String query = "";
        try {
            Map<Integer, Integer> idMap = new HashMap<>();

            //get id mapping
            Connection con = this.dbCon.getConnection(this);
            Statement st = con.createStatement();
            query = "SELECT taz_id, taz_num_id from core." + region + "_taz";
            ResultSet rs = st.executeQuery(query);
            while (rs.next()) {
                if (adoptIDs) idMap.put(rs.getInt("taz_num_id"), rs.getInt("taz_id"));
                else idMap.put(rs.getInt("taz_id"), rs.getInt("taz_id")); //1:1 mapping
            }
            st.close();


            //load data
            List<Score> scores = new LinkedList<>();
            BufferedReader in = new BufferedReader(new FileReader(file));
            String line;

            while ((line = in.readLine()) != null) {
                if (line.startsWith("TVZ") || line.startsWith("Block")) //header
                    continue;
                String[] tok = line.split("[;\t, ]");

                if (tok.length == 3) {
                    Score tmp = new Score();
                    tmp.id = idMap.get(Integer.parseInt(tok[0]));
                    tmp.score = Double.parseDouble(tok[1]);
                    tmp.scoreCat = Integer.parseInt(tok[2]);

                    scores.add(tmp);
                }
            }
            in.close();

            //store into db
            query = "INSERT INTO core." + region + "_" + type + "_scores VALUES " + "	(?, ?,?,'" + name + "')";
            PreparedStatement pSt = con.prepareStatement(query);
            for (Score tmp : scores) {
                pSt.setInt(1, tmp.id);
                pSt.setDouble(2, tmp.score);
                pSt.setInt(3, tmp.scoreCat);
                pSt.execute();
            }
            pSt.close();
            return true;
        } catch (SQLException e) {
            System.err.println("SQL-Error in importScores for file " + file.getAbsolutePath() + " and query " + query);
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            System.err.println("File-Error in importScores for file " + file.getAbsolutePath() + " and query " + query);
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("IO-Error in importScores for file " + file.getAbsolutePath() + " and query " + query);
            e.printStackTrace();
        }
        return false;
    }

    public boolean importTAZ(File file, String region, boolean useID) {
        String query = "";
        try {
            //load data
            List<TAZ> tazes = new LinkedList<>();
            int i = 1;
            BufferedReader in = new BufferedReader(new FileReader(file));
            String line;

            while ((line = in.readLine()) != null) {
                if (line.startsWith("TVZ") || line.startsWith("TAZ")) //header
                    continue;
                String[] tok = line.split("[;\t,]");
                if (tok.length == 3) {
                    TAZ tmp = new TAZ();
                    if (useID) {
                        tmp.numID = Integer.parseInt(tok[0]);
                        tmp.id = i++;
                    } else {
                        tmp.id = Integer.parseInt(tok[0]);
                        tmp.numID = -1;
                    }
                    tmp.lat = Double.parseDouble(tok[1]);
                    tmp.lon = Double.parseDouble(tok[2]);

                    tazes.add(tmp);
                }
            }
            in.close();

            //store into db
            Connection con = this.dbCon.getConnection(this);
            query = "INSERT INTO core." + region +
                    "_taz (taz_id, taz_coordinate, taz_num_id) VALUES (?, st_setsrid(st_makepoint(?,?), 4326),?)";
            PreparedStatement st = con.prepareStatement(query);
            for (TAZ tmp : tazes) {
                st.setInt(1, tmp.id);
                st.setDouble(2, tmp.lat);
                st.setDouble(3, tmp.lon);
                st.setInt(4, tmp.numID);
                st.execute();
            }
            st.close();
            return true;

        } catch (SQLException e) {
            System.err.println("SQL-Error in importTAZ for file " + file.getAbsolutePath() + " and query " + query);
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            System.err.println("File-Error in importTAZ for file " + file.getAbsolutePath() + " and query " + query);
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("IO-Error in importTAZ for file " + file.getAbsolutePath() + " and query " + query);
            e.printStackTrace();
        }


        return false;
    }

    public boolean importTazFee(File file, String region, String name, boolean useID, boolean updateBBR) {
        String query = "";
        try {
            //load data
            Map<Integer, TAZFee> tazes = new HashMap<>();
            BufferedReader in = new BufferedReader(new FileReader(file));
            String line;
            int id;
            boolean hasBBR = false;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("T")) { //header
                    continue;
                }
                String[] tok = line.split("[;\t,]");
                if ((tok.length == 9 && !updateBBR) || (tok.length == 10)) {
                    TAZFee tmp = new TAZFee();
                    id = Integer.parseInt(tok[0]);
                    if (useID) {
                        tmp.numID = id;
                    } else {
                        tmp.id = id;
                    }
                    tmp.hasTollBase = Boolean.parseBoolean(tok[1]);
                    tmp.tollTypeBase = Integer.parseInt(tok[2]);
                    tmp.hasParkingFeeBase = Boolean.parseBoolean(tok[3]);
                    tmp.parkingFeeTypeBase = Integer.parseInt(tok[4]);
                    tmp.hasTollScen = Boolean.parseBoolean(tok[5]);
                    tmp.tollTypeScen = Integer.parseInt(tok[6]);
                    tmp.hasParkingFeeScen = Boolean.parseBoolean(tok[7]);
                    tmp.parkingFeeTypeScen = Integer.parseInt(tok[8]);
                    if (tok.length == 10) {
                        tmp.BBR = Integer.parseInt(tok[9]);
                        hasBBR = true;
                    }
                    tazes.put(id, tmp);
                }
            }
            in.close();

            Connection con = this.dbCon.getConnection(this);
            Statement st = con.createStatement();
            //check if the id should be updated
            if (useID) {
                query = "SELECT taz_id, taz_num_id from core." + region + "_taz";
                ResultSet rs = st.executeQuery(query);
                while (rs.next()) {
                    tazes.get(rs.getInt("taz_num_id")).id = rs.getInt("taz_id");
                }
            }
            st.close();

            //check if the bbr should be updated
            PreparedStatement pSt;
            query = "UPDATE core." + region + "_taz SET taz_bbr_type = ? WHERE taz_id = ?";
            pSt = con.prepareStatement(query);
            if (updateBBR && hasBBR) {
                for (TAZFee tmp : tazes.values()) {
                    pSt.setInt(1, tmp.BBR);
                    pSt.setInt(2, tmp.id);
                    pSt.execute();
                }
            }
            pSt.close();
            query = "INSERT INTO core." + region + "_taz_fees_tolls (" + "ft_name, ft_taz_id, " +
                    "has_toll_base, toll_type_base, " + "has_fee_base,fee_type_base, " +
                    "has_toll_scen, toll_type_scen," + "has_fee_scen, fee_type_scen) " + "VALUES ('" + name +
                    "',?,?,?,?,?,?,?,?,?)";
            pSt = con.prepareStatement(query);
            //store TAZFees into db
            for (TAZFee tmp : tazes.values()) {

                pSt.setInt(1, tmp.id);
                pSt.setBoolean(2, tmp.hasTollBase);
                pSt.setInt(3, tmp.tollTypeBase);
                pSt.setBoolean(4, tmp.hasParkingFeeBase);
                pSt.setInt(5, tmp.parkingFeeTypeBase);
                pSt.setBoolean(6, tmp.hasTollScen);
                pSt.setInt(7, tmp.tollTypeScen);
                pSt.setBoolean(8, tmp.hasParkingFeeBase);
                pSt.setInt(9, tmp.parkingFeeTypeScen);
                pSt.execute();

            }
            return true;

        } catch (SQLException e) {
            System.err.println("SQL-Error in importTazFee for file " + file.getAbsolutePath() + " and query " + query);
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            System.err.println("File-Error in importTazFee for file " + file.getAbsolutePath() + " and query " + query);
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("IO-Error in importTazFee for file " + file.getAbsolutePath() + " and query " + query);
            e.printStackTrace();
        }
        return false;
    }

    public boolean login(File loginInfo) {
        try {
            TPS_ParameterClass parameterClass = new TPS_ParameterClass();
            parameterClass.loadRuntimeParameters(loginInfo);
            dbCon = new TPS_DB_Connector(parameterClass);
            return true;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    class TAZ {
        int id = 0;
        int numID = 0;
        double lat, lon;
    }

    class TAZFee {
        int id = 0;
        int numID = 0;
        int BBR = 0;
        boolean hasTollBase = false;
        int tollTypeBase = 0;
        boolean hasParkingFeeBase = false;
        int parkingFeeTypeBase = 0;
        boolean hasTollScen = false;
        int tollTypeScen = 0;
        boolean hasParkingFeeScen = false;
        int parkingFeeTypeScen = 0;

    }

    class HouseHold {
        int id = 0;
        int taz;
        double lat;
        double lon;
        int persons;
        int cars;
        int income;
    }

    class Location {
        int id = 0;
        int tazID = 0;
        int blockID = 0;
        int code = 0;
        String enterprise = "";
        int capacity = 0;
        boolean hasFixCapacity = false;
        double lat;
        double lon;

    }

    class Person {
        int id;
        int hhid;
        int pGroup;
        int sex;
        int age;
        int ageStba;
        double working;
        int abo;
        int license;
        boolean hasBike = true;
        double budPT;
        double budIT;
        double budITFix;
        int hhTyp;
    }

    class Act2Loc {
        int actID;
        int locID;
        String comment;

    }

    class CFN4 {
        int actCode;
        int bbrCode;
        double value;
        String comment = "";
        String raumtyp = "";
    }

    class CFN4Index {
        String name;
        int bbr;
        double value;
        String comment;
        String raumtyp;
    }

    class BlockPTStop {
        int blockID;
        double distance;
    }

    class Score {
        int id;
        double score;
        int scoreCat;
    }

    class Block {
        int blockID;
        int tazID;
        double lat;
        double lon;
    }

    class TAZIntraMITInfo {
        int tazID;
        double blFactor;
        double avgSpeed;
        boolean hasIntraInfo = true;
    }

    class TAZIntraPTInfo {
        int tazID;
        double avgSpeed;
        boolean hasIntraInfo = true;
        int ptZone = 1;
    }

    class ModeChoice {
        int node;
        int parent;
        String attribtes;
        String splitVariable;
        String distribution;
    }

}
