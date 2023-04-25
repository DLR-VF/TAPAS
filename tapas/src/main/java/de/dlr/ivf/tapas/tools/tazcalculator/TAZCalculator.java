/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.tools.tazcalculator;

import de.dlr.ivf.tapas.persistence.db.TPS_DB_Connector;
import de.dlr.ivf.tapas.parameter.TPS_ParameterClass;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.HashMap;
import java.util.Map.Entry;

public class TAZCalculator {


    TPS_DB_Connector manager = null;

    /**
     * @param args
     */
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        if (args.length != 1) {
            System.out.println("Usage: TAZCalculator <region>");
            return;
        }

        String region = args[0];

        File configFile = new File("T:/Simulationen/runtime.csv");
        TPS_ParameterClass parameterClass = new TPS_ParameterClass();
        parameterClass.loadRuntimeParameters(configFile);
        TAZCalculator worker = new TAZCalculator();
        worker.manager = new TPS_DB_Connector(parameterClass);
        HashMap<Integer, Integer> mapping;
//		worker.updateTAZ(region, 1);

//		mapping = worker.getNewTAZ(region,"_blocks", "blk_id", "blk_coordinate", "TRUE");
//		if(!worker.updateTAZID(region, "_blocks", "blk_id", "blk_taz_id", "TRUE", mapping))
//			return;
//		for(Entry<Integer,Integer> i :blockMapping.entrySet()){
//			System.out.printf("Block id %7d is in TAZ %5d\n", (int)i.getKey(), (int)i.getValue());
//		}
//		mapping = worker.getNewTAZ(region,"_locations", "loc_id", "loc_coordinate", "TRUE");
//		if(!worker.updateTAZID(region, "_locations", "loc_id", "loc_taz_id", "TRUE", mapping))
//			return;
//		for(Entry<Integer,Integer> i :locationMapping.entrySet()){
//			System.out.printf("Location id %7d is in TAZ %5d\n", (int)i.getKey(), (int)i.getValue());
//		}
//		mapping = worker.getNewTAZ(region,"_households", "hh_id", "hh_coordinate", "hh_key='MID2008_Y2008'");
//		if(!worker.updateTAZID(region, "_households", "hh_id", "hh_taz_id", "hh_key='MID2008_Y2008'", mapping))
//			return;
//		mapping = worker.getNewTAZ(region,"_households", "hh_id", "hh_coordinate", "hh_key='MID2008_Y2030'");
//		if(!worker.updateTAZID(region, "_households", "hh_id", "hh_taz_id", "hh_key='MID2008_Y2030'", mapping))
//			return;
        mapping = worker.getNewTAZ(region, "_households", "hh_id", "hh_coordinate", "hh_key='MID2005_Y2005'");
        if (!worker.updateTAZID(region, "_households", "hh_id", "hh_taz_id", "hh_key='MID2008_Y2030'", mapping)) return;
        mapping = worker.getNewTAZ(region, "_households", "hh_id", "hh_coordinate", "hh_key='MID2005_Y2030'");
        if (!worker.updateTAZID(region, "_households", "hh_id", "hh_taz_id", "hh_key='MID2008_Y2030'", mapping)) return;
//		for(Entry<Integer,Integer> i :houseHoldMapping.entrySet()){
//			System.out.printf("Household id %7d is in TAZ %5d\n", (int)i.getKey(), (int)i.getValue());
//		}
    }

    private HashMap<Integer, Integer> getNewTAZ(String region, String table, String idColumn, String coordinateColumn, String sqlCondition) {
        HashMap<Integer, Integer> TAZRecalculated = new HashMap<>();


        try {
            Connection con = this.manager.getConnection(this);

            Statement st = con.createStatement();


            String query = "SELECT " + idColumn + " FROM core." + region + table + " WHERE " + sqlCondition;
            ResultSet rs = st.executeQuery(query);

            while (rs.next()) {
                TAZRecalculated.put(rs.getInt(idColumn), -1);
            }
            rs.close();

            query = "SELECT gid, text(the_geom) FROM core." + region + "_taz_multiline ORDER BY gid";
            rs = st.executeQuery(query);
            Statement st2 = con.createStatement();
            ResultSet rs2;
            int taz, count;
            while (rs.next()) {
                taz = rs.getInt("gid");
                count = 0;
                try {
                    query = "SELECT " + idColumn + " FROM core." + region + table + " WHERE within(" +
                            coordinateColumn + ",geometry('" + rs.getString(2) + "')) AND " + sqlCondition;

                    rs2 = st2.executeQuery(query);
                    while (rs2.next()) {
                        TAZRecalculated.put(rs2.getInt(idColumn), taz);
                        count++;
                    }
                    rs2.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                System.out.printf("Table core.%s : %7d enries for TAZ %7d\n", region + table, count, taz);
            }
            rs.close();

            for (Entry<Integer, Integer> i : TAZRecalculated.entrySet()) {
                if (i.getValue() < 0) {
                    query = "SELECT gid FROM core." + region +
                            "_taz_multiline ORDER BY distance_sphere(the_geom, (SELECT " + coordinateColumn +
                            " FROM core." + region + table + " WHERE " + idColumn + " = " + i.getKey() + " AND " +
                            sqlCondition + ")) LIMIT 1";
                    rs2 = st2.executeQuery(query);
                    if (rs2.next()) {
                        taz = rs2.getInt("gid");
                        TAZRecalculated.put(i.getKey(), taz);
                    } else {
                        taz = -1;
                    }
                    System.out.printf("%s: id %7d is outside any borders. Closest border id: %4d\n", region + table,
                            i.getKey(), taz);
                    rs2.close();
                }
            }

            st2.close();
            st.close();
        } catch (SQLException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }


        return TAZRecalculated;
    }

    @SuppressWarnings("unused")
    private boolean updateTAZ(String region, int bbrCathegory) {
        boolean success = true;
        String query;
        try {
            //update old TAZES
            query = "SELECT gid, text(st_centroid(the_geom)) FROM core." + region +
                    "_taz_multiline WHERE gid IN (SELECT taz_id from core." + region + "_taz) ORDER BY gid";
            ResultSet rs = this.manager.executeQuery(query, this);
            while (rs.next()) {
                query = "UPDATE core." + region + "_taz SET taz_coordinate = st_setsrid(geometry('" + rs.getString(2) +
                        "'),4326), taz_bbr_type = " + bbrCathegory + " WHERE taz_id = " + rs.getInt("gid");
                this.manager.execute(query, this);
            }
            rs.close();


            //insert new TAZES
            query = "SELECT gid, text(st_centroid(the_geom)) FROM core." + region +
                    "_taz_multiline WHERE gid NOT IN (SELECT taz_id from core." + region + "_taz) ORDER BY gid";
            rs = this.manager.executeQuery(query, this);
            while (rs.next()) {
                query = "INSERT INTO core." + region + "_taz (taz_id, taz_bbr_type, taz_coordinate) VALUES (" +
                        rs.getInt("gid") + "," + bbrCathegory + ",st_setsrid(geometry('" + rs.getString(2) +
                        "'),4326))";
                this.manager.execute(query, this);
            }
            rs.close();

            //delete old TAZES
            query = "DELETE FROM core." + region + "_taz WHERE taz_id NOT IN (SELECT gid FROM core." + region +
                    "_taz_multiline)";
            this.manager.execute(query, this);

        } catch (SQLException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        return success;
    }

    private boolean updateTAZID(String region, String table, String idColumn, String tazColumn, String sqlCondition, HashMap<Integer, Integer> map) {
        boolean success = true;

        int counter;
        try {
            PreparedStatement st = this.manager.getConnection(this).prepareStatement(
                    "UPDATE core." + region + table + " SET " + tazColumn + " = ? WHERE " + idColumn + " = ? and " +
                            sqlCondition);
            counter = 0;
            for (Entry<Integer, Integer> i : map.entrySet()) {
                st.setInt(1, i.getValue());
                st.setInt(2, i.getKey());
                st.addBatch();
                counter++;
                if (counter % 1024 == 0) {
                    int[] successer = st.executeBatch();
                    for (int value : successer) success &= value == 1;
                }
            }
            int[] successor = st.executeBatch();
            for (int i : successor) success &= i == 1;

        } catch (SQLException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }


        return success;
    }

}
