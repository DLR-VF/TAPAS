/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.tools.tazcalculator;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.dlr.ivf.api.io.configuration.ConnectionDetails;
import de.dlr.ivf.api.io.connection.ConnectionPool;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.function.Supplier;

public class TAZCalculator {

    private final ConnectionPool connectionSupplier;
    Supplier<Connection> manager;

    public TAZCalculator(ConnectionPool connectionSupplier){
        this.connectionSupplier = connectionSupplier;
    }

    /**
     * @param args
     */
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        if (args.length != 1) {
            System.out.println("Usage: TAZCalculator <region>");
            return;
        }

        String region = args[0];

        Path configFile = Paths.get(args[0]);
        if (!Files.isRegularFile(configFile))
            throw new IllegalArgumentException("The provided argument is not a file.");

        ConnectionDetails connector = new ObjectMapper().readValue(configFile.toFile(), ConnectionDetails.class);

        ConnectionPool connectionSupplier = new ConnectionPool(connector);
        TAZCalculator worker = new TAZCalculator(connectionSupplier);
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


        try (Connection connection = connectionSupplier.borrowObject()){
            String query = "SELECT " + idColumn + " FROM core." + region + table + " WHERE " + sqlCondition;

            try(PreparedStatement st = connection.prepareStatement(query);
                ResultSet rs = st.executeQuery()) {


                while (rs.next()) {
                    TAZRecalculated.put(rs.getInt(idColumn), -1);
                }
            }catch (SQLException e){
                e.printStackTrace();
            }

            query = "SELECT gid, text(the_geom) FROM core." + region + "_taz_multiline ORDER BY gid";

            try(PreparedStatement st = connection.prepareStatement(query);
                ResultSet rs = st.executeQuery()) {

                int taz, count;
                while (rs.next()) {
                    taz = rs.getInt("gid");
                    count = 0;

                    String query2 = "SELECT " + idColumn + " FROM core." + region + table + " WHERE within(" +
                            coordinateColumn + ",geometry('" + rs.getString(2) + "')) AND " + sqlCondition;
                    try (PreparedStatement st2 = connection.prepareStatement(query2);
                         ResultSet rs2 = st2.executeQuery()) {

                        while (rs2.next()) {
                            TAZRecalculated.put(rs2.getInt(idColumn), taz);
                            count++;
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    System.out.printf("Table core.%s : %7d enries for TAZ %7d\n", region + table, count, taz);
                }

                for (Entry<Integer, Integer> i : TAZRecalculated.entrySet()) {
                    if (i.getValue() < 0) {
                        query = "SELECT gid FROM core." + region +
                                "_taz_multiline ORDER BY distance_sphere(the_geom, (SELECT " + coordinateColumn +
                                " FROM core." + region + table + " WHERE " + idColumn + " = " + i.getKey() + " AND " +
                                sqlCondition + ")) LIMIT 1";
                        try (PreparedStatement st2 = connection.prepareStatement(query);
                             ResultSet rs2 = st2.executeQuery()) {

                            if (rs2.next()) {
                                taz = rs2.getInt("gid");
                                TAZRecalculated.put(i.getKey(), taz);
                            } else {
                                taz = -1;
                            }
                            System.out.printf("%s: id %7d is outside any borders. Closest border id: %4d\n", region + table,
                                    i.getKey(), taz);
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }catch (SQLException e){
                e.printStackTrace();
            }
            connectionSupplier.returnObject(connection);
        } catch (SQLException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }


        return TAZRecalculated;
    }

    private boolean updateTAZID(String region, String table, String idColumn, String tazColumn, String sqlCondition, HashMap<Integer, Integer> map) {
        boolean success = true;

        int counter;
        String query = "UPDATE core." + region + table + " SET " + tazColumn + " = ? WHERE " + idColumn + " = ? and " + sqlCondition;
        try(Connection connection = connectionSupplier.borrowObject();
            PreparedStatement st = connection.prepareStatement(query)){

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
            connectionSupplier.returnObject(connection);
        } catch (SQLException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        return success;
    }

}
