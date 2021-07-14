/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.tools.OpportunityDistribution;

import de.dlr.ivf.tapas.tools.persitence.db.TPS_BasicConnectionClass;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Updates all household positions from block centroid to actual buildings. The
 * households are distributed mainly on residential buildings.
 *
 * @author boec_pa
 */
public class HouseholdDistribution extends TPS_BasicConnectionClass {

    static final double MIN_PROB = 1E-5;
    static final int RESIDENTIAL = 0;// 2111;
    static final int MIXED = 1; // 2113;
    static final int SPECIAL = 2; // 2114;
    static final int INDUSTRIAL = 3; // 2112;
    static final double[] DEFAULT_DISTRIBUTION = {0.6, 0.39, 0.1, MIN_PROB};
    private final int BATCH_SIZE = 10000;
    private final ArrayList<Integer> blocks = new ArrayList<>();
    private final ArrayList<String> keys = new ArrayList<>();

    private long cntHouseholdsUpdated = 0;
    private long cntHouseholdsNoBuilding = 0;

    private final ArrayList<Integer> emptyBlocks = new ArrayList<>();

    private PreparedStatement updateHouseholds;
    private PreparedStatement updateHouseholdsNoBuilding;
    private PreparedStatement fetchHouseholds;
    private PreparedStatement fetchBuildings;
    private PreparedStatement fetchBuildingsSimple;

    /**
     * Fetches all blocks (15k) and fetches household keys (4) Prepared all sql
     * statements
     */
    public HouseholdDistribution() {
        // get all blocks
        String query = "SELECT DISTINCT blk_id AS blk" + " FROM core.berlin_blocks";
        try {
            System.out.println(
                    "Starting to fetch all blocks and their number of households." + "\nThis may take a while...");
            ResultSet rs = dbCon.executeQuery(query, this);
            while (rs.next()) {
                blocks.add(rs.getInt("blk"));
            }
            rs.close();
            System.out.println("I found " + blocks.size() + " blocks.");
        } catch (SQLException e) {
            System.err.println(this.getClass().getCanonicalName() + query);
            e.printStackTrace();
            return;
        }

        // get all household keys
        query = "SELECT DISTINCT hh_key AS key FROM core.berlin_households";
        try {
            ResultSet rs = dbCon.executeQuery(query, this);
            while (rs.next()) keys.add(rs.getString("key"));
            rs.close();

        } catch (SQLException e) {
            System.err.println(this.getClass().getCanonicalName() + query);
            e.printStackTrace();
            return;
        }

        try {
            updateHouseholds = dbCon.getConnection(this).prepareStatement(
                    "UPDATE core.berlin_households AS hh " + " SET hh_coordinate_old = b.the_geom " +
                            " FROM core.berlin_buildings AS b WHERE " + " hh.hh_id = ? AND hh_key = ? AND b.id = ?");

            updateHouseholdsNoBuilding = dbCon.getConnection(this).prepareStatement(
                    "UPDATE core.berlin_households AS hh " + " SET hh_coordinate_old = b.blk_coordinate " +
                            " FROM core.berlin_blocks AS b " +
                            " WHERE ST_DWITHIN(hh.hh_coordinate,b.blk_coordinate,0.00001) " + " AND b.blk_id = ? ");

            fetchHouseholds = dbCon.getConnection(this).prepareStatement(
                    "SELECT * FROM (SELECT DISTINCT h.hh_id as hh " + " FROM core.berlin_households AS h " +
                            " JOIN core.berlin_blocks AS b " +
                            " ON ST_DWITHIN(h.hh_coordinate,b.blk_coordinate,0.00001) " +
                            " WHERE b.blk_id = ? AND h.hh_key = ? ) AS t ORDER BY RANDOM()");

            fetchBuildings = dbCon.getConnection(this).prepareStatement(
                    "WITH dlmblocks AS " + "(SELECT ST_TRANSFORM(d.the_geom,4326) AS geom, d.objart " +
                            " FROM core.berlin_blocks_multiline AS b " + " JOIN core.gis_dlm_fot AS d " +
                            " ON ST_INTERSECTS(d.the_geom,ST_TRANSFORM(b.the_geom, 31467)) " +
                            " WHERE b.netzf = 'Block' " + " AND b.blocknr = ? " +
                            " AND objart= ANY(ARRAY[2111,2112, 2113, 2114])" +
                            " AND area(ST_INTERSECTION(ST_TRANSFORM(b.the_geom, 31467),d.the_geom))/area(d.the_geom) > 0.1 )" +
                            " SELECT bld.id AS building, dlm.objart AS type" + " FROM core.berlin_buildings AS bld " +
                            " JOIN dlmblocks AS dlm " + " ON ST_WITHIN(bld.the_geom, dlm.geom)");
            fetchBuildingsSimple = dbCon.getConnection(this).prepareStatement(
                    "SELECT bld.id AS building" + " FROM core.berlin_buildings AS bld " +
                            " JOIN core.berlin_blocks_multiline AS block " +
                            " ON ST_WITHIN(bld.the_geom,block.the_geom) " +
                            " WHERE bld.bundesland='11' AND block.blocknr = ?");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static int getLanduseFromCode(int landuseCode) {
        switch (landuseCode) {
            case 2111:
                return RESIDENTIAL;
            case 2112:
                return INDUSTRIAL;
            case 2113:
                return MIXED;
            default:
                return SPECIAL;
        }
    }

    public static void main(String[] args) {

        HouseholdDistribution hd = new HouseholdDistribution();
        hd.updateHouseholds(true);
        System.out.println("The number of households to be updated is " + hd.getHouseholdCnt());
        System.out.println("The number of blocks without buildings is " + hd.getEmptyBlocks().size());

        File file = new File("C:\\Users\\boec_pa\\Documents\\HouseholdDistribution\\emptyBlocks.dat");

        try {
            BufferedWriter bf = new BufferedWriter(new FileWriter(file));

            ArrayList<Integer> blks = hd.getEmptyBlocks();
            for (Integer b : blks) {
                bf.write(b + "\n");
            }
            bf.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void addNoBuildingUpdate(int block) {

        if (block == -1) { // update rest
            try {
                updateHouseholdsNoBuilding.executeBatch();
            } catch (SQLException e) {
                System.err.println("Error when trying to update last empty blocks.");
                e.printStackTrace();
            }
            return;
        }

        cntHouseholdsNoBuilding++;
        try {
            updateHouseholdsNoBuilding.setInt(1, block);
            updateHouseholdsNoBuilding.addBatch();

            if (cntHouseholdsNoBuilding % BATCH_SIZE == 0) {
                int[] successer = updateHouseholdsNoBuilding.executeBatch();
                int cntFailed = 0;
                for (int i : successer) {
                    if (i == PreparedStatement.EXECUTE_FAILED) {
                        cntFailed++;
                    }
                    System.err.println(cntFailed + " updates (buildingless) have failed at block " + block);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void addRegularUpdate(int building, int household, String key, boolean verbose) {

        if (building == -1) {// update rest
            try {
                updateHouseholds.executeBatch();
            } catch (SQLException e) {
                System.err.println("Error while updating last households.");
                e.printStackTrace();
            }

        }

        cntHouseholdsUpdated++;
        try {
            updateHouseholds.setInt(1, household);
            updateHouseholds.setString(2, key);
            updateHouseholds.setInt(3, building);
            updateHouseholds.addBatch();

            if (cntHouseholdsUpdated % BATCH_SIZE == 0) {
                int[] successer = updateHouseholds.executeBatch();
                int cntFailed = 0;
                for (int i : successer) {
                    if (i == PreparedStatement.EXECUTE_FAILED) {
                        cntFailed++;
                    }
                    if (cntFailed > 0) {
                        System.err.println(
                                cntFailed + " updates have failed at household " + household + " of " + key + ".");
                    }
                }

                if (verbose) {
                    System.out.println("So far I have updated \t" + cntHouseholdsUpdated);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    /**
     * Example: There are no MIXED buildings Default is [0.6,0.39,0.01,0.0]
     * Result is (roughly) [0.98,0.0, 0.2,0.0] After that, we weight the
     * distribution with the number of buildings in each category
     *
     * @param buildings
     * @return
     * @throws DistributionException
     */
    private double[] getDistribution(HashMap<Integer, ArrayList<Integer>> buildings) throws DistributionException {
        double[] result = DEFAULT_DISTRIBUTION.clone();

        for (int i = 0; i < DEFAULT_DISTRIBUTION.length; i++) {
            if (buildings.get(i).size() == 0 && result[i] > 0) {
                for (int j = 0; j < DEFAULT_DISTRIBUTION.length; j++) {
                    if (i != j) {
                        result[j] = result[j] + result[j] * result[i] / (1 - result[i]);
                    }
                }
                result[i] = 0;
            }
        }

        double sum = 0;
        for (int i = 0; i < DEFAULT_DISTRIBUTION.length; i++) {
            result[i] *= buildings.get(i).size();
            sum += result[i];
        }
        for (int i = 0; i < DEFAULT_DISTRIBUTION.length; i++) {
            result[i] /= sum;
        }

        // validate numbers

        sum = 0;
        for (double d : result) {
            if (Double.isNaN(d)) throw new DistributionException();
            sum += d;
        }

        if (Double.compare(Math.abs(sum - 1), MIN_PROB) > 0) throw new DistributionException();

        return result;
    }

    public ArrayList<Integer> getEmptyBlocks() {
        return emptyBlocks;
    }

    public long getHouseholdCnt() {
        return cntHouseholdsUpdated;
    }

    private int getLanduse(double[] locDist) {
        double r = Math.random();
        if (r < locDist[RESIDENTIAL]) return RESIDENTIAL;
        else if (r < locDist[RESIDENTIAL] + locDist[MIXED]) return MIXED;
        else if (r < locDist[RESIDENTIAL] + locDist[MIXED] + locDist[SPECIAL]) return SPECIAL;
        else return INDUSTRIAL;
    }

    /**
     * Retrieves 4 lists of buildings in the given <code>block</code> organised
     * in a {@link Map} with the landuse as key.
     *
     * @param block
     * @return
     */
    private HashMap<Integer, ArrayList<Integer>> retrieveBuildingsByLanduse(int block) {

        HashMap<Integer, ArrayList<Integer>> buildings = new HashMap<>();
        try {
            fetchBuildings.setInt(1, block);
            ResultSet rs = fetchBuildings.executeQuery();

            buildings.put(RESIDENTIAL, new ArrayList<>());
            buildings.put(MIXED, new ArrayList<>());
            buildings.put(SPECIAL, new ArrayList<>());
            buildings.put(INDUSTRIAL, new ArrayList<>());

            while (rs.next()) {
                int type = getLanduseFromCode(rs.getInt("type"));

                int building = rs.getInt("building");
                buildings.get(type).add(building);
            }

            rs.close();
        } catch (SQLException e) {
            System.err.println("Error on fetchBuildings with block " + block);
            System.err.println(this.getClass().getCanonicalName());
            e.printStackTrace();
        }

        return buildings;

    }

    private ArrayList<Integer> retrieveBuildingsSimple(int block) {
        ArrayList<Integer> buildings = new ArrayList<>();

        try {
            fetchBuildingsSimple.setInt(1, block);
            ResultSet rs = fetchBuildingsSimple.executeQuery();

            while (rs.next()) {
                int building = rs.getInt("building");
                buildings.add(building);
            }
        } catch (SQLException e) {
            System.err.println("Error on fetchBuildings with block " + block);
            System.err.println(this.getClass().getCanonicalName());
            e.printStackTrace();
        }
        return buildings;
    }

    /**
     * Retrieves a list of all households in the given <code>block</code> with
     * the given <code>key</code>.
     *
     * @param key
     * @param block
     * @return <code>null</code> on SQL exceptions.
     */
    private ArrayList<Integer> retrieveHouseholds(String key, int block) {
        ArrayList<Integer> households = new ArrayList<>();

        try {
            fetchHouseholds.setInt(1, block);
            fetchHouseholds.setString(2, key);
            ResultSet rs = fetchHouseholds.executeQuery();
            while (rs.next()) {
                households.add(rs.getInt("hh"));
            }
            rs.close();

        } catch (SQLException e) {
            System.err.println(this.getClass().getCanonicalName() + "\n Parameters were " + key + " and " + block);
            e.printStackTrace();
            return null;
        }

        return households;
    }

    /**
     * Updates all household positions from block centroid to actual buildings.
     * The households are distributed mainly on residential buildings. 3 stage
     * algorithm:
     * <ol>
     * <li>Try to match dlm blocks to blocks and find buildings in there</li>
     * <li>If that fails, try find buildings just in blocks</li>
     * <li>If that fails as well, update households to block centroid instead</li>
     * </ol>
     *
     * @param verbose If <code>true</code>, a status report for every block is given
     *                to track progress.
     */
    public void updateHouseholds(boolean verbose) {
        for (int block : blocks) {
            HashMap<Integer, ArrayList<Integer>> buildings = retrieveBuildingsByLanduse(block);
            double[] locDist = DEFAULT_DISTRIBUTION;

            boolean foundAny = false;
            for (ArrayList<Integer> al : buildings.values()) {
                foundAny |= al.size() > 0;
            }
            if (!foundAny) {// no buildings found by landuse
                // use simple approach
                ArrayList<Integer> buildingsSimple = retrieveBuildingsSimple(block);

                if (buildingsSimple.size() == 0) {// block is empty
                    emptyBlocks.add(block);
                    addNoBuildingUpdate(block);
                } else {// simple update
                    for (String key : keys) {
                        ArrayList<Integer> households = retrieveHouseholds(key, block);
                        for (int household : households) {
                            int building = buildingsSimple.get((int) (Math.random() * buildingsSimple.size()));
                            addRegularUpdate(building, household, key, verbose);
                        }
                    }
                }
            } else {// buildings found in normal approach
                try {
                    locDist = getDistribution(buildings);
                } catch (DistributionException e) {
                    // should not happen
                    System.err.println("Something went wrong while calculating distributions for block " + block);
                    continue;
                }

                for (String key : keys) {
                    ArrayList<Integer> households = retrieveHouseholds(key, block);
                    for (int household : households) {
                        int type = getLanduse(locDist);
                        int building = buildings.get(type).get((int) (Math.random() * buildings.get(type).size()));
                        addRegularUpdate(building, household, key, verbose);
                    }
                }
            }
        }
        // update the rest
        addNoBuildingUpdate(-1);
        addRegularUpdate(-1, -1, null, true);
    }

    private class DistributionException extends Exception {

        /**
         *
         */
        private static final long serialVersionUID = 1L;

    }
}
