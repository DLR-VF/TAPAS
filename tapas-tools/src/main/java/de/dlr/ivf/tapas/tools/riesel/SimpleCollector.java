/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.tools.riesel;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.dlr.ivf.api.io.configuration.ConnectionDetails;
import de.dlr.ivf.api.io.connection.ConnectionPool;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * This class distributes a given number of inhabitants on addresses found within the geometries.<br>
 * It distributes them equally among all addresses found. <br>
 * Hence it is only useful for relatively small geometries with little diversity.
 *
 * @author boec_pa
 */
public class SimpleCollector implements Runnable {

    private static final int QUEUE_SIZE = 10000;

    private final ConnectionPool dbCon;

    private final ArrayBlockingQueue<AddressPojo> queue;

    private final SimpleCollectorWriter writer;

    public SimpleCollector(ConnectionPool dbCon, String outputFilePath) throws IOException {
        super();
        this.dbCon = dbCon;
        queue = new ArrayBlockingQueue<>(QUEUE_SIZE);
        writer = new SimpleCollectorWriter(outputFilePath, queue);
        new Thread(writer).start();
    }

    /**
     * Start here.
     */
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        String loginInfo = "T:\\Simulationen\\runtime_perseus.csv";

        Path configFile = Paths.get(args[0]);
        if (!Files.isRegularFile(configFile))
            throw new IllegalArgumentException("The provided argument is not a file.");

        ConnectionDetails connector = new ObjectMapper().readValue(configFile.toFile(), ConnectionDetails.class);

        SimpleCollector sc = new SimpleCollector(new ConnectionPool(connector),"D:\\tmp\\berlin_blocks.csv");

        sc.run();
    }

    private List<AddressPojo> distributeInhabitants(ArrayList<Integer> addresses, long inh) {
        ArrayList<AddressPojo> result = new ArrayList<>();

        for (Integer addKey : addresses) {
            result.add(new AddressPojo(addKey, 0));
        }

        while (--inh >= 0) {
            int i = (int) (Math.random() * (addresses.size()));
            result.get(i).incInhabitants();
        }

        return result;
    }

    private ArrayList<Integer> getAddresses(EWZPojo ewz) throws SQLException {
        String query = "SELECT add.id " + " FROM core.berlin_blocks_multiline AS ewz " +
                " JOIN core.berlin_address_bkg AS add " +
                " ON ST_WITHIN(ST_TRANSFORM(add.the_geom,4326),ewz.the_geom) " + " WHERE ewz.gid = " + ewz.key;

        ArrayList<Integer> buildings = new ArrayList<>();

        try(Connection connection = dbCon.borrowObject();
            PreparedStatement st = connection.prepareStatement(query);
            ResultSet rs = st.executeQuery()) {

            while (rs.next()) {
                buildings.add(rs.getInt("id"));
            }

            dbCon.returnObject(connection);
        }catch (SQLException e){
            e.printStackTrace();
        }

        return buildings;
    }

    private List<EWZPojo> getGeometries() throws SQLException {

        String query = "SELECT gid, insgesamt FROM core.berlin_blocks_multiline " + " WHERE netzf = 'Block'";

        ArrayList<EWZPojo> result = new ArrayList<>();
        try(Connection connection = dbCon.borrowObject();
            PreparedStatement st = connection.prepareStatement(query);
            ResultSet rs = st.executeQuery()) {

            while (rs.next()) {
                EWZPojo ewz = new EWZPojo(rs.getInt("gid"), rs.getInt("insgesamt"));
                result.add(ewz);
            }
            dbCon.returnObject(connection);
        }catch (SQLException e){
            e.printStackTrace();
        }

        return result;
    }

    @Override
    public void run() {
        System.out.println("Starting Collection");
        List<EWZPojo> geometries;
        try {
            geometries = getGeometries();
            // System.out.println("Got Geometries");
            for (EWZPojo ewz : geometries) {
                ArrayList<Integer> addresses = getAddresses(ewz);

                if (addresses.size() == 0 && ewz.inhabitants > 0) {
                    System.err.println("No addresses in " + ewz.key + ". " + ewz.inhabitants +
                            " inhabitants could not be distributed.");
                    continue;
                }

                List<AddressPojo> inhs = distributeInhabitants(addresses, ewz.inhabitants);

                queue.addAll(inhs);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            queue.add(AddressPojo.POISON_ELEMENT);
        }
    }

    private class EWZPojo {
        int key;
        long inhabitants;

        public EWZPojo(int key, long inhabitants) {
            this.key = key;
            this.inhabitants = inhabitants;
        }

        @Override
        public String toString() {
            return "[" + key + ", " + inhabitants + "]";
        }

    }
}
