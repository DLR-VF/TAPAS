/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.tools;

import de.dlr.ivf.tapas.tools.persitence.db.TPS_BasicConnectionClass;

import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;


/**
 * This program scans for locations at the exact same position and bundles them to a group.
 * Doing so avoids later on "driving through a mall with your suv"-effects
 */
public class TPS_LocationGroupCreator extends TPS_BasicConnectionClass {


    /**
     * @param args
     */
    public static void main(String[] args) {
        TPS_LocationGroupCreator worker = new TPS_LocationGroupCreator();
        worker.groupLocations("core.berlin_locations_urmo_ref2015");
        worker.groupLocations("core.berlin_locations_urmo_ref2030");
        worker.groupLocations("core.berlin_locations_urmo_ec2030");
        worker.groupLocations("core.berlin_locations_urmo_ec2030_extreme");
//		worker.groupLocations("core.berlin_locations_1223_urmo_ref2015");
//		worker.groupLocations("core.berlin_locations_1223_urmo_ref2030");
//		worker.groupLocations("core.berlin_locations_1223_urmo_ec2030");
//		worker.groupLocations("core.berlin_locations_1223_urmo_ec2030_extreme");

    }

    public void groupLocations(String table) {
        String query = "";
        try {
            int groupId = 1;

            query = "SELECT max(loc_group_id) as max FROM " + table;
            ResultSet rs = dbCon.executeQuery(query, this);
            while (rs.next()) {
                if (rs.getInt("max") > -1) {
                    groupId = rs.getInt("max") + 1;
                }
            }
            rs.close();

            Map<String, Array> coordinates = new HashMap<>();
            query = "SELECT loc_coordinate, array_agg(loc_id) as ids, count(loc_id) as num FROM " + table +
                    " WHERE loc_group_id<0 GROUP BY loc_coordinate";
            rs = dbCon.executeQuery(query, this);
            while (rs.next()) {
                Array array = rs.getArray("ids");


                if (rs.getInt("num") > 1) {
                    coordinates.put(rs.getString("loc_coordinate"), array);
                }
            }
            rs.close();
            query = "UPDATE " + table + " SET loc_group_id=? WHERE loc_id = any(?)";
            PreparedStatement pS = dbCon.getConnection(this).prepareStatement(query);
            for (Array idSet : coordinates.values()) {
                pS.setInt(1, groupId);

                pS.setArray(2, dbCon.getConnection(this).createArrayOf("integer", (Object[]) idSet.getArray()));
                pS.addBatch();
                groupId++;
                if (groupId % 1000 == 0) {
                    pS.executeBatch();
                }
            }
            pS.executeBatch();

        } catch (SQLException e) {
            System.err.println(
                    this.getClass().getCanonicalName() + " groupLocations: SQL-Error during statement: " + query);
            e.printStackTrace();
        }
    }
}
