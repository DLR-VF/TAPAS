/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.tools;

import de.dlr.ivf.tapas.tools.persitence.db.TPS_BasicConnectionClass;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

public class TPS_FillLocationRepresentatives extends TPS_BasicConnectionClass {

    List<Integer> tazIds = new LinkedList<>();
    List<Representative> representatives = new LinkedList<>();

    public TPS_FillLocationRepresentatives(String string) {
        super(string);
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        if (args.length != 4) {
            System.out.println(
                    "Usage: <taz multiline table> <address table> <output: representatives table> <samples per taz>");
            return;
        }
        TPS_FillLocationRepresentatives worker = new TPS_FillLocationRepresentatives(
                "T:\\Simulationen\\runtime_herakles_admin.csv");
        worker.readTazes(args[0]);
        worker.readRepresentatives(args[1], args[0], Integer.parseInt(args[3]));
        worker.createRepresentativesTable(args[2]);
        worker.saveToDB(args[2]);

    }

    public void createRepresentativesTable(String representativesTable) {
        String[] parts = representativesTable.split("\\.");
        String query = "DROP TABLE IF EXISTS " + representativesTable + ";" + "CREATE TABLE " + representativesTable +
                "				(" + "						  id integer NOT NULL," +
                "						  taz_id integer," +
                "						  representative_coordinate geometry," +
                "						  CONSTRAINT " + parts[parts.length - 1] + "_b PRIMARY KEY (id)," +
                "						  CONSTRAINT enforce_dims_representative_coordinate CHECK (st_ndims(representative_coordinate) = 2)," +
                "						  CONSTRAINT enforce_geotype_representative_coordinate CHECK (geometrytype(representative_coordinate) = 'POINT'::text OR representative_coordinate IS NULL)," +
                "						  CONSTRAINT enforce_srid_representative_coordinate CHECK (st_srid(representative_coordinate) = 4326)" +
                "						)" + "						WITH (" +
                "						  OIDS=FALSE" + "						);" +
                "						GRANT ALL ON TABLE " + representativesTable + " TO postgres;" +
                "						GRANT ALL ON TABLE " + representativesTable + " TO tapas_admin_group;" +
                "						GRANT SELECT ON TABLE " + representativesTable + " TO tapas_user_group";
        this.dbCon.execute(query, this);

    }

    public void readRepresentatives(String addressTable, String tazTable, int samples) {
        String query = "";
        try {
            int centroidRepresentatives = 0;
            int processed = 0;
            for (Integer i : tazIds) {
                query = "WITH adr as (SELECT gid, st_transform(geom,4326) as geom FROM " + addressTable + ") " +
                        "SELECT gid, st_X(geom) as x, st_Y(geom) as y FROM adr WHERE st_within(geom, (SELECT the_geom from " +
                        tazTable + " WHERE gid = " + i + ")) order by random() LIMIT " + samples;
                ResultSet rs = this.dbCon.executeQuery(query, this);
                int representativesFound = 0;
                while (rs.next()) {
                    representativesFound++;
                    Representative elem = new Representative();
                    elem.id = rs.getInt("gid");
                    elem.taz = i;
                    elem.x = rs.getDouble("x");
                    elem.y = rs.getDouble("y");
                    representatives.add(elem);
                }
                rs.close();
                if (representativesFound == 0) { // no address in this TAZ
                    centroidRepresentatives++; //one more centroid
                    query = "SELECT st_X(st_transform(st_centroid(the_geom),4326)) as x, st_Y(st_transform(st_centroid(the_geom),4326)) as y FROM " +
                            tazTable + " WHERE gid= " + i;
                    rs = this.dbCon.executeQuery(query, this);
                    while (rs.next()) {
                        Representative elem = new Representative();
                        elem.id = -centroidRepresentatives; //negative ID for centroids
                        elem.taz = i;
                        elem.x = rs.getDouble("x");
                        elem.y = rs.getDouble("y");
                        representatives.add(elem);
                    }
                    rs.close();
                }
                processed++;
                if (processed % 10 == 0) System.out.println("Processed TAZ: " + processed + "/" + tazIds.size());
            }
            System.out.println("Processed TAZ: " + processed + "/" + tazIds.size());
            System.out.println("Added " + centroidRepresentatives + " centroids, because TAZ had no address");
        } catch (SQLException e) {
            System.out.println("SQL error! Query: " + query);
            e.printStackTrace();
        }
    }

    public void readTazes(String table) {
        String query = "";
        try {
            query = "SELECT gid FROM " + table;
            ResultSet rs = this.dbCon.executeQuery(query, this);
            while (rs.next()) {
                tazIds.add(rs.getInt("gid"));
            }
            rs.close();
            //System.out.println("Found "+tazIds.size()+" TAZEs");
        } catch (SQLException e) {
            System.out.println("SQL error! Query: " + query);
            e.printStackTrace();
        }
    }

    public void saveToDB(String representativesTable) {
        String query = "DELETE FROM " + representativesTable;
        this.dbCon.execute(query, this);
        for (Representative i : representatives) {
            query = "INSERT INTO " + representativesTable + " VALUES (" + i.id + "," + i.taz +
                    ",st_setsrid(st_makepoint(" + i.x + "," + i.y + "),4326))";
            this.dbCon.execute(query, this);
        }
    }

    class Representative {
        int id, taz;
        double x, y;
    }

}
