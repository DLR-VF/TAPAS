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

    private final TPS_DB_Connector dbCon;

    private final ArrayBlockingQueue<AddressPojo> queue;

    private final SimpleCollectorWriter writer;

    public SimpleCollector(TPS_DB_Connector dbCon, String outputFilePath) throws IOException {
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

        TPS_ParameterClass parameterClass = new TPS_ParameterClass();
        parameterClass.loadRuntimeParameters(new File(loginInfo));
        parameterClass.setValue("DB_DBNAME", "tapas");
        TPS_DB_Connector dbCon = new TPS_DB_Connector(parameterClass);

        SimpleCollector sc = new SimpleCollector(dbCon, "D:\\tmp\\berlin_blocks.csv");

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

        ResultSet rs = dbCon.executeQuery(query, this);

        ArrayList<Integer> buildings = new ArrayList<>();
        while (rs.next()) {
            buildings.add(rs.getInt("id"));
        }

        rs.close();

        return buildings;
    }

    private List<EWZPojo> getGeometries() throws SQLException {

        String query = "SELECT gid, insgesamt FROM core.berlin_blocks_multiline " + " WHERE netzf = 'Block'";

        ResultSet rs = dbCon.executeQuery(query, this);

        ArrayList<EWZPojo> result = new ArrayList<>();
        while (rs.next()) {
            EWZPojo ewz = new EWZPojo(rs.getInt("gid"), rs.getInt("insgesamt"));
            result.add(ewz);
        }
        rs.close();

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
