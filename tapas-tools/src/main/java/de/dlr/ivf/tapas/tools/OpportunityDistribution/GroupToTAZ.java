/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.tools.OpportunityDistribution;

import de.dlr.ivf.tapas.persistence.db.TPS_DB_Connector;
import de.dlr.ivf.tapas.persistence.db.TPS_DB_IO;
import de.dlr.ivf.tapas.model.parameter.TPS_ParameterClass;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class GroupToTAZ {

    TPS_DB_Connector dbCon = null;
    private final List<Integer> taz = new ArrayList<>();
    private String region;
    private String dbName = "_taz_mapping_values";
    private final HashMap<Integer, int[]> mapping = new HashMap<>();

    /**
     * Stand alone initializator, assumes that global TPS_Parameters contains the login information for the db
     */
    public GroupToTAZ(TPS_ParameterClass parameterClass) {
        try {
            //init db connection assuming that param-values are already set
            dbCon = new TPS_DB_Connector(parameterClass);
            initDB();

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Constructor with a given db-connector to use
     *
     * @param con The Connector to use
     */
    public GroupToTAZ(TPS_DB_Connector con) {
        dbCon = con;
        initDB();
    }

    private boolean flushTAZImportData(String name, String region, int map_id, List<Integer> tazes) {
        StringBuilder query = new StringBuilder(
                "INSERT INTO core." + region + this.dbName + " VALUES ('" + name + "'," + map_id + ", ARRAY[");
        for (int i = 0; i < tazes.size(); ++i) {
            query.append(tazes.get(i));
            if (i < tazes.size() - 1) query.append(",");
        }
        query.append("])");

        dbCon.executeUpdate(query.toString(), this);
        return true;
    }

    /**
     * @return the dbName
     */
    public String getDbName() {
        return dbName;
    }

    /**
     * @param dbName the dbName to set
     */
    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    /**
     * Returns a sorted list of the map-values which are actually present.
     *
     * @return
     */
    public List<Integer> getMappingValues() {
        List<Integer> map = new ArrayList<>(this.mapping.keySet());
        java.util.Collections.sort(map);

        return map;
    }

    /**
     * @return the region
     */
    public String getRegion() {
        return region;
    }

    /**
     * @param region the region to set
     */
    public void setRegion(String region) {
        this.region = region;
    }

    /**
     * Method to return a sql-query which contains the shape of the given mapping group
     *
     * @param map_id the id for tzhis group
     * @return a sql-string or null if group is empty
     */
    public String getShapeSQLQuery(int map_id) {
        int[] tazes = this.mapping.get(map_id);
        if (tazes != null && tazes.length > 0) {
            String taz_shape_db_name = "_taz_multiline";
            StringBuilder query = new StringBuilder(
                    "SELECT ST_Union(the_geom) AS geom FROM core." + this.region + taz_shape_db_name +
                            " WHERE gid = ANY(ARRAY[");
            for (int taze : tazes) {
                query.append(taze).append(",");
            }
            query.deleteCharAt(query.length() - 1); // delete last comma
            query.append("])");
            return query.toString();
        }
        return null;
    }

    /**
     * Method to return an array of tazes for the given map_id.
     *
     * @param map_id The map id
     * @return The sorted array of tazes or an zero-element array if map_id is not found.
     */
    public int[] getTAZes(int map_id) {
        if (this.mapping.containsKey(map_id)) return this.mapping.get(map_id);
        else return new int[0];
    }

    public boolean importData(String name, String region, String csvFile) {
        try {
            int map_id = -1, actID, actTaz;
            List<Integer> tazes = new ArrayList<>();
            FileReader in = new FileReader(csvFile);
            BufferedReader input = new BufferedReader(in);
            input.readLine();//header
            String line = input.readLine();//first data
            while (line != null) {
                String[] array = line.split(";");
                if (array.length == 2) {
                    actID = Integer.parseInt(array[0]);
                    actTaz = Integer.parseInt(array[1]);
                    if (map_id != -1 && map_id != actID) {

                        //flush data!
                        boolean ok = this.flushTAZImportData(name, region, map_id, tazes);
                        if (!ok) {
                            input.close();
                            return false;
                        }
                        //clear list
                        tazes.clear();

                    }
                    tazes.add(actTaz);
                    map_id = actID;
                } else {
                    input.close();
                    return false;
                }
                line = input.readLine();
            }
            //close files
            input.close();
            in.close();
            //flush last group
            boolean ok = this.flushTAZImportData(name, region, map_id, tazes);
            if (!ok) return false;

        } catch (FileNotFoundException e) {
            System.err.println(this.getClass().getCanonicalName() + " importData: FileNotFoundException: " + csvFile);
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            System.err.println(this.getClass().getCanonicalName() + " importData: IOException: " + csvFile);
            e.printStackTrace();
            return false;
        }

        return true;
    }

    private void initDB() {

    }

    /**
     * Method to init the taz info: load all taz ids for this region
     *
     * @return
     */
    public boolean initRegion(String region) {
        this.region = region;
        String query = "";
        try {
            query = "SELECT taz_id FROM core." + this.region + "_taz ORDER BY taz_id";
            ResultSet rs = dbCon.executeQuery(query, this);
            while (rs.next()) {
                this.taz.add(rs.getInt("taz_id"));
            }
            rs.close();
        } catch (SQLException e) {
            System.err.println(
                    this.getClass().getCanonicalName() + " initRegion: SQL-Error during statement: " + query);
            e.printStackTrace();
            return false;
        }
        return taz.size() > 0;
    }

    /**
     * Method to load the mapping infos. Loads the mapping values, checks them and stores them internaly
     *
     * @return true if everything is ok and values are found.
     */
    public boolean loadMapping(String name) {
        String query = "";
        try {
            query = "SELECT map_value, taz_values FROM core." + this.region + this.dbName + " WHERE name='" + name +
                    "'";
            ResultSet rs = dbCon.executeQuery(query, this);
            while (rs.next()) {
                int map_id = rs.getInt("map_value");
                int[] tazes = TPS_DB_IO.extractIntArray(rs, "taz_values");
                boolean tazOK = tazes.length > 0;
                int i;
                for (i = 0; i < tazes.length && tazOK; ++i) {
                    tazOK &= this.taz.contains(tazes[i]);
                }
                if (tazOK) {
                    // sort values for later convenience, see getTAZes
                    Arrays.sort(tazes);
                    this.mapping.put(map_id, tazes);
                } else {
                    if (tazes.length == 0) {
                        System.err.println(
                                this.getClass().getCanonicalName() + "loadMapping: Empty map for map_id: " + map_id);
                    } else {
                        System.err.println(
                                this.getClass().getCanonicalName() + "loadMapping: Unknown TAZ: " + tazes[i - 1]);
                    }
                    return false;
                }
            }
            rs.close();
        } catch (SQLException e) {
            System.err.println(
                    this.getClass().getCanonicalName() + "loadMapping: SQL-Error during statement: " + query);
            e.printStackTrace();
            return false;
        }
        return taz.size() > 0;
    }

//	/**
//	 * Method to return a PGgeometry object of the shape of the mapping group.
//	 * @param map_id the id for the mapping group
//	 * @return a geometry containing the shape or null
//	 */
//	
//	public PGgeometry getShape(int map_id){
//		String query=getShapeSQLQuery(map_id);
//		if(query!=null){
//			try{	
//				ResultSet rs = dbCon.executeQuery(query, this);
//				if(rs.next()){				
//					return (PGgeometry)rs.getObject("geom");
//				}			
//			}catch(SQLException e){
//				System.err.println(this.getClass().getCanonicalName()+" getShape: SQL-Error during statement: "+query);
//				e.printStackTrace();
//			}
//		}
//		return null;
//	}

}
