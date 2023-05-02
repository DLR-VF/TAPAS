/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.tools.riesel;

import de.dlr.ivf.tapas.persistence.db.TPS_DB_Connector;
import de.dlr.ivf.tapas.model.parameter.TPS_ParameterClass;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This tool distributes households given on some geometries on the addresses
 * within these geometries. It uses land use information (given on different
 * geometries) for this distribution.
 *
 * @author boec_pa
 */
public class RieselTool {

    static final double MIN_PROB = 1E-5;
    private final TPS_DB_Connector dbCon;
    private final BlockingQueue<AddressPojo> queue = new LinkedBlockingQueue<>();
    private final UpdateAddressesWorker updateWorker;

    /**
     * @param dbCon
     * @throws SQLException if the database connection cannot be established.
     */
    public RieselTool(TPS_DB_Connector dbCon) throws SQLException {
        this.dbCon = dbCon;

        updateWorker = new UpdateAddressesWorker(queue, dbCon);
        Thread thread = new Thread(updateWorker);
        thread.start();
    }

    /**
     * @param args
     * @throws SQLException
     */
    public static void main(String[] args) throws SQLException {
        String loginInfo = "T:\\Simulationen\\runtime_perseus.csv";
        try {
            TPS_ParameterClass parameterClass = new TPS_ParameterClass();
            parameterClass.loadRuntimeParameters(new File(loginInfo));
            parameterClass.setValue("DB_DBNAME", "dlm");
            TPS_DB_Connector dbCon = new TPS_DB_Connector(parameterClass);
            RieselTool rt = new RieselTool(dbCon);

            rt.updateAll();

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

    }

    /**
     * Lists the ids of all areas where the number of inhabitants is known and
     * the according number.
     *
     * @return
     * @throws SQLException if the query fails which might happen if the database
     *                      connection fails.
     */
    public List<EWZPojo> getAllInhabitants() throws SQLException {

        ArrayList<EWZPojo> result = new ArrayList<>();

        String query = "SELECT rs,ewz FROM vg250_ew_nam WHERE use=6 AND ewz > 0";
        ResultSet rs = dbCon.executeQuery(query, this);
        while (rs.next()) {
            int ewz = rs.getInt("ewz");
            String key = rs.getString("rs");
            result.add(new EWZPojo(key, ewz));
        }
        rs.close();

        return result;
    }

    /**
     * <p>
     * This method retrieves the ids of all buildings in the area identified
     * with <code>rs_key</code>.
     * </p>
     * <p>
     * Note that this method might take a long time and might return a large
     * data set.
     * </p>
     *
     * @return <code>null</code> if no buildings are found.
     * @throws SQLException if the query is not successful. This might happen if the
     *                      database connection is not active.
     */
    public EnumMap<LAND_USE, ArrayList<Long>> getBuildingsForArea(String rs_key) throws SQLException {

        boolean hasBuildings = false;

        // TODO special cases for certain areas possible here
        String query = "WITH geom AS ( SELECT ST_UNION(ST_INTERSECTION(ew.the_geom, fot.the_geom)) AS fot_geom " +
                " FROM fot_landuse AS fot " + " JOIN vg250_ew_f AS ew ON ST_INTERSECTS(ew.the_geom, fot.the_geom)" +
                " WHERE fot.objart = _LU_ AND ew.use=6 AND ew.gf=4 AND ew.rs='" + rs_key +
                "') SELECT id FROM address_bkg AS ad" + " JOIN geom ON ST_WITHIN(ad.the_geom,geom.fot_geom)";

        EnumMap<LAND_USE, ArrayList<Long>> result = new EnumMap<>(LAND_USE.class);

        for (LAND_USE lu : LAND_USE.values()) {
            String luQuery = query.replaceAll("_LU_", String.valueOf(lu.dbKey));
            ResultSet rs = dbCon.executeQuery(luQuery, this);
            ArrayList<Long> luBuildings = new ArrayList<>();
            while (rs.next()) {
                luBuildings.add(rs.getLong("id"));
                hasBuildings = true;
            }
            rs.close();
            result.put(lu, luBuildings);
        }
        if (hasBuildings) return result;
        else return null;
    }

    /**
     * Retrieves all buildings for Berlin (around 2Mio adresses). This is for
     * testing purposes only.
     *
     * @throws SQLException
     */
    public int testPerformance() throws SQLException {
        String query = "WITH geom AS ( " + " SELECT ST_UNION(ST_INTERSECTION(ew.the_geom, fot.the_geom)) AS fot_geom " +
                " FROM fot_landuse AS fot " + " JOIN vg250_ew_f AS ew ON ST_INTERSECTS(ew.the_geom, fot.the_geom) " +
                " WHERE fot.objart = 2111 AND ew.use=6 AND ew.gf=4 AND ew.rs='110000000000')" +
                " SELECT id  FROM address_bkg AS ad " + " JOIN geom ON ST_WITHIN(ad.the_geom,geom.fot_geom)";

        ArrayList<Integer> addr = new ArrayList<>();
        ResultSet rs = dbCon.executeQuery(query, this);
        while (rs.next()) {
            addr.add(rs.getInt("id"));
        }

        return addr.size();
    }

    /**
     * Updates the given addresses with a number of inhabitants according to a
     * distribution.
     *
     * @param addresses database ids of the buildings that are to be updated ordered
     *                  by their {@link LAND_USE}.
     * @param ewz       Number of inhabitants to be distributed among the buildings.
     */
    public void updateAddresses(EnumMap<LAND_USE, ArrayList<Long>> addresses, int ewz) {

        HashMap<Long, Integer> increments = new HashMap<>();
        Distributor dist = new Distributor(addresses);

        while (ewz > 0) {
            long b = dist.getRandomBuilding();
            if (b >= 0) {
                int count = increments.getOrDefault(b, 0);
                increments.put(b, count + 1);
                ewz--;
            }
        }

        for (Entry<Long, Integer> e : increments.entrySet()) {
            queue.add(new AddressPojo(e.getKey(), e.getValue()));
        }
        // System.out.println(increments);

    }

    /**
     * This is the main access method of the class. It will retrieve all areas
     * where the number of inhabitants is known, intersect it with the land use
     * polygons and put them into {@link RieselTool#queue}. The actual update
     * process is done by the {@link UpdateAddressesWorker}.
     */
    public void updateAll() {
        List<EWZPojo> inhabitants;
        try {
            inhabitants = getAllInhabitants();
            System.out.println("List of all regions is created. Starting to update now.");
            System.out.println("I expect to update " + inhabitants.size() + " regions.");

            final AtomicInteger cnt = new AtomicInteger();
            for (final EWZPojo ew : inhabitants) {
                EnumMap<LAND_USE, ArrayList<Long>> buildings = getBuildingsForArea(ew.rs);

                if (buildings != null) {
                    updateAddresses(buildings, ew.ewz);
                } else {
                    System.err.println(
                            "No buildings found in area " + ew.rs + ". " + ew.ewz + " inhabitants not assigned.");
                }

                int current_count = cnt.addAndGet(1);
                if (current_count % 50 == 0) {
                    System.out.println(current_count + " areas updated.");
                }
            }

            queue.add(AddressPojo.POISON_ELEMENT);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * The land use codifies percentage of addresses in a certain area that is
     * likely used for private housing. We expect for example 60% of all
     * addresses in a residential area to be actual private housing.
     */
    private enum LAND_USE {
        RESIDENTIAL(0.6, 2111), //
        MIXED(0.39, 2113), //
        SPECIAL(0.1, 2114), //
        INDUSTRIAL(MIN_PROB, 2112);

        private final double useFactor;
        private final int dbKey;

        LAND_USE(double distribution, int dbKey) {
            this.useFactor = distribution;
            this.dbKey = dbKey;
        }

        public double getFactor() {
            return useFactor;
        }
    }

    /**
     * This method provides a way to randomly pick addresses from the list given
     * when creating an instance. It takes the number of addresses for each
     * {@link LAND_USE} and the encoded land use factor into account.
     */
    private class Distributor {

        private final SortedMap<Double, LAND_USE> finalFactor;
        private final EnumMap<LAND_USE, ArrayList<Long>> addresses;
        private final Random randomGenerator;

        /**
         * @param addresses Empty lists may be omitted in the map.
         */
        public Distributor(EnumMap<LAND_USE, ArrayList<Long>> addresses) {

            LAND_USE.RESIDENTIAL.getFactor();

            this.addresses = addresses;
            double total = 0;
            for (LAND_USE lu : LAND_USE.values()) {

                total += addresses.get(lu).size() * lu.getFactor();
            }

            // create sorted map of factors
            TreeMap<Double, LAND_USE> factor = new TreeMap<>();
            for (LAND_USE lu : LAND_USE.values()) {
                if (addresses.get(lu).size() > 0) {
                    factor.put(addresses.get(lu).size() * lu.getFactor() / total, lu);
                } else {
                    factor.put(0.0, lu);
                }
            }

            // make cumulative map
            finalFactor = new TreeMap<>();

            for (Entry<Double, LAND_USE> e : factor.entrySet()) {
                double t = 0;
                for (Double dt : factor.headMap(e.getKey()).keySet()) {
                    t += dt;
                }
                finalFactor.put(t, e.getValue());
            }

            randomGenerator = new Random();
        }

        /**
         * This method returns the id of a building picked randomly from the
         * lists entered when creating this instance.
         */
        public long getRandomBuilding() {
            double r = Math.random();
            LAND_USE lu = finalFactor.get(finalFactor.headMap(r).lastKey());
            // System.out.println("r = " + r + " lu = " + lu);
            ArrayList<Long> ll = addresses.get(lu);
            if (ll.size() > 0) {
                return ll.get(randomGenerator.nextInt(ll.size()));
            } else {
                return -1;
            }

        }
    }

    /**
     * Result object of a query to get all geometries with number of inhabitants
     */
    private class EWZPojo {
        private final String rs;
        private final int ewz;

        public EWZPojo(String rs, int ewz) {
            super();
            this.rs = rs;
            this.ewz = ewz;
        }

        @Override
        public String toString() {
            return "(" + rs + "," + ewz + ")";
        }
    }
}
