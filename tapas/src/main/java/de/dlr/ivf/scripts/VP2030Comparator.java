/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.scripts;


import java.sql.Connection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;

public class VP2030Comparator {
    private final Supplier<Connection> connectionSupplier;
    Map<Integer, Integer[]> vp2taz = new HashMap<>();

    public VP2030Comparator(Supplier<Connection> connectionSupplier){
        this.connectionSupplier = connectionSupplier;
    }

    public static void main(String[] args) {
        if (args.length != 4) {
            System.out.println(
                    "Parameters: <region> <tablename> <boolean: use only destination> <boolean: use only source>");
            return;
        }
        VP2030Comparator worker = new VP2030Comparator(null);
        String region = args[0];
        worker.loadVP2TAZValues(region);
        int num = 0;
        for (Entry<Integer, Integer[]> e : worker.vp2taz.entrySet()) {
            num += e.getValue().length;
        }
        System.out.println(region + ":");
        System.out.println("Total number of cells: " + num);
        List<VP2030Data> cells = worker.loadTrafficVolumes(args[1], Boolean.parseBoolean(args[2]),
                Boolean.parseBoolean(args[3]));
        double[][] weight = new double[Modus.values().length][Wegzweck.values().length];
        //aggregate all
        for (VP2030Data e : cells) {
            for (Modus m : Modus.values()) {
                for (Wegzweck z : Wegzweck.values()) {
                    weight[m.ordinal()][z.ordinal()] += e.modusZweck[m.ordinal()][z.ordinal()];
                }
            }
        }
        //aggregate mode
        double sumMode = 0;
        double[] weightMode = new double[Modus.values().length];
        for (Modus m : Modus.values()) {
            for (Wegzweck z : Wegzweck.values()) {
                if (z.equals(Wegzweck.EINKAUF) || z.equals(Wegzweck.PRIVAT) || z.equals(Wegzweck.BILDUNG)) {
                    weightMode[m.ordinal()] += weight[m.ordinal()][z.ordinal()];
                    sumMode += weight[m.ordinal()][z.ordinal()];
                }
            }
        }

        for (Modus m : Modus.values()) {
            System.out.format("All volumes\t%s\t%10.0f\n", m, weightMode[m.ordinal()]);
            System.out.format("All shares \t%s\t%05.2f\n", m, weightMode[m.ordinal()] * 100 / sumMode);
        }

        //aggregate zweck
        System.out.println();
        for (Wegzweck z : Wegzweck.values()) {
            double sumZweck = 0;
            double[] weightZweck = new double[Modus.values().length];
            for (Modus m : Modus.values()) {
                weightZweck[m.ordinal()] += weight[m.ordinal()][z.ordinal()];
                sumZweck += weight[m.ordinal()][z.ordinal()];
            }
            if (sumZweck > 0) {
                for (Modus m : Modus.values()) {
                    System.out.format("Volume %s\t%s\t%10.0f\n", z, m, weightZweck[m.ordinal()]);
                    System.out.format("Share  %s\t%s\t%05.2f\n", z, m, weightZweck[m.ordinal()] * 100 / sumZweck);
                }
                System.out.println();
            }
        }


        System.out.format("Volumen Bus:  %10.0f\n", weight[Modus.OESP.ordinal()][Wegzweck.ALL.ordinal()]);
        System.out.format("Volumen Bahn: %10.0f\n", weight[Modus.BAHN.ordinal()][Wegzweck.ALL.ordinal()]);


    }

    public List<VP2030Data> loadTrafficVolumes(String table, boolean useOnlyDestination, boolean useOnlySource) {
//        String query = "";
//        List<VP2030Data> cells = new ArrayList<>();
//        try {
//            ResultSet rs;
//            query = "SELECT quelle, ziel," + "bahn_ber, bahn_ausb, bahn_eink, bahn_gesch, bahn_url," +
//                    "bahn_priv, miv_ber, miv_ausb, miv_eink, miv_gesch, miv_url, miv_priv," +
//                    "luft_ber, luft_ausb, luft_eink, luft_gesch, luft_url, luft_priv," +
//                    "oespv_ber, oespv_ausb, oespv_eink, oespv_gesch, oespv_url, oespv_priv," +
//                    "rad_ber, rad_ausb, rad_eink, rad_gesch, rad_url, rad_priv, fuss_ber," +
//                    "fuss_ausb, fuss_eink, fuss_gesch, fuss_url, fuss_priv, bahn_all," +
//                    "miv_all, luft_all, oespv_all, rad_all, fuss_all, filter_, rnb_raum_first " + "FROM " + table;
//
//            rs = this.dbCon.executeQuery(query, this);
//            while (rs.next()) {
//                VP2030Data e = new VP2030Data();
//                e.quelle = rs.getInt("quelle");
//                e.ziel = rs.getInt("ziel");
//                //check if this entry should be added
//                if (useOnlyDestination && !this.vp2taz.containsKey(e.ziel)) continue;
//                if (useOnlySource && !this.vp2taz.containsKey(e.quelle)) continue;
//                e.modusZweck[Modus.BAHN.ordinal()][Wegzweck.BERUF.ordinal()] = rs.getInt("bahn_ber");
//                e.modusZweck[Modus.BAHN.ordinal()][Wegzweck.BILDUNG.ordinal()] = rs.getInt("bahn_ausb");
//                e.modusZweck[Modus.BAHN.ordinal()][Wegzweck.EINKAUF.ordinal()] = rs.getInt("bahn_eink");
//                e.modusZweck[Modus.BAHN.ordinal()][Wegzweck.DIENST.ordinal()] = rs.getInt("bahn_gesch");
//                e.modusZweck[Modus.BAHN.ordinal()][Wegzweck.URLAUB.ordinal()] = rs.getInt("bahn_url");
//                e.modusZweck[Modus.BAHN.ordinal()][Wegzweck.PRIVAT.ordinal()] = rs.getInt("bahn_priv");
//                e.modusZweck[Modus.BAHN.ordinal()][Wegzweck.ALL.ordinal()] = rs.getInt("bahn_ber");
//
//                e.modusZweck[Modus.MIV.ordinal()][Wegzweck.BERUF.ordinal()] = rs.getInt("miv_ber");
//                e.modusZweck[Modus.MIV.ordinal()][Wegzweck.BILDUNG.ordinal()] = rs.getInt("miv_ausb");
//                e.modusZweck[Modus.MIV.ordinal()][Wegzweck.EINKAUF.ordinal()] = rs.getInt("miv_eink");
//                e.modusZweck[Modus.MIV.ordinal()][Wegzweck.DIENST.ordinal()] = rs.getInt("miv_gesch");
//                e.modusZweck[Modus.MIV.ordinal()][Wegzweck.URLAUB.ordinal()] = rs.getInt("miv_url");
//                e.modusZweck[Modus.MIV.ordinal()][Wegzweck.PRIVAT.ordinal()] = rs.getInt("miv_priv");
//                e.modusZweck[Modus.MIV.ordinal()][Wegzweck.ALL.ordinal()] = rs.getInt("miv_ber");
//
//                e.modusZweck[Modus.LUFT.ordinal()][Wegzweck.BERUF.ordinal()] = rs.getInt("luft_ber");
//                e.modusZweck[Modus.LUFT.ordinal()][Wegzweck.BILDUNG.ordinal()] = rs.getInt("luft_ausb");
//                e.modusZweck[Modus.LUFT.ordinal()][Wegzweck.EINKAUF.ordinal()] = rs.getInt("luft_eink");
//                e.modusZweck[Modus.LUFT.ordinal()][Wegzweck.DIENST.ordinal()] = rs.getInt("luft_gesch");
//                e.modusZweck[Modus.LUFT.ordinal()][Wegzweck.URLAUB.ordinal()] = rs.getInt("luft_url");
//                e.modusZweck[Modus.LUFT.ordinal()][Wegzweck.PRIVAT.ordinal()] = rs.getInt("luft_priv");
//                e.modusZweck[Modus.LUFT.ordinal()][Wegzweck.ALL.ordinal()] = rs.getInt("luft_ber");
//
//                e.modusZweck[Modus.OESP.ordinal()][Wegzweck.BERUF.ordinal()] = rs.getInt("oespv_ber");
//                e.modusZweck[Modus.OESP.ordinal()][Wegzweck.BILDUNG.ordinal()] = rs.getInt("oespv_ausb");
//                e.modusZweck[Modus.OESP.ordinal()][Wegzweck.EINKAUF.ordinal()] = rs.getInt("oespv_eink");
//                e.modusZweck[Modus.OESP.ordinal()][Wegzweck.DIENST.ordinal()] = rs.getInt("oespv_gesch");
//                e.modusZweck[Modus.OESP.ordinal()][Wegzweck.URLAUB.ordinal()] = rs.getInt("oespv_url");
//                e.modusZweck[Modus.OESP.ordinal()][Wegzweck.PRIVAT.ordinal()] = rs.getInt("oespv_priv");
//                e.modusZweck[Modus.OESP.ordinal()][Wegzweck.ALL.ordinal()] = rs.getInt("oespv_ber");
//
//                e.modusZweck[Modus.RAD.ordinal()][Wegzweck.BERUF.ordinal()] = rs.getInt("rad_ber");
//                e.modusZweck[Modus.RAD.ordinal()][Wegzweck.BILDUNG.ordinal()] = rs.getInt("rad_ausb");
//                e.modusZweck[Modus.RAD.ordinal()][Wegzweck.EINKAUF.ordinal()] = rs.getInt("rad_eink");
//                e.modusZweck[Modus.RAD.ordinal()][Wegzweck.DIENST.ordinal()] = rs.getInt("rad_gesch");
//                e.modusZweck[Modus.RAD.ordinal()][Wegzweck.URLAUB.ordinal()] = rs.getInt("rad_url");
//                e.modusZweck[Modus.RAD.ordinal()][Wegzweck.PRIVAT.ordinal()] = rs.getInt("rad_priv");
//                e.modusZweck[Modus.RAD.ordinal()][Wegzweck.ALL.ordinal()] = rs.getInt("rad_ber");
//
//                e.modusZweck[Modus.FUSS.ordinal()][Wegzweck.BERUF.ordinal()] = rs.getInt("fuss_ber");
//                e.modusZweck[Modus.FUSS.ordinal()][Wegzweck.BILDUNG.ordinal()] = rs.getInt("fuss_ausb");
//                e.modusZweck[Modus.FUSS.ordinal()][Wegzweck.EINKAUF.ordinal()] = rs.getInt("fuss_eink");
//                e.modusZweck[Modus.FUSS.ordinal()][Wegzweck.DIENST.ordinal()] = rs.getInt("fuss_gesch");
//                e.modusZweck[Modus.FUSS.ordinal()][Wegzweck.URLAUB.ordinal()] = rs.getInt("fuss_url");
//                e.modusZweck[Modus.FUSS.ordinal()][Wegzweck.PRIVAT.ordinal()] = rs.getInt("fuss_priv");
//                e.modusZweck[Modus.FUSS.ordinal()][Wegzweck.ALL.ordinal()] = rs.getInt("fuss_ber");
//
//                cells.add(e);
//            }
//            rs.close();
//
//        } catch (SQLException e) {
//            // TODO Auto-generated catch block
//            System.out.println("SQL error! " + query);
//            e.printStackTrace();
//            e.getNextException().printStackTrace();
//        }
        return null;
    }

    public void loadVP2TAZValues(String region) {
        String query = "";
//        try {
//            query = "select vp_id, tapas_id from quesadillas.vp2030_kreise where tapas_region ='" + region + "'";
//            ResultSet rs = this.dbCon.executeQuery(query, this);
//            while (rs.next()) {
//                Integer vpID = rs.getInt("vp_id");
//                Integer[] tazArray = (Integer[]) rs.getArray("tapas_id").getArray();
//                vp2taz.put(vpID, tazArray);
//            }
//            rs.close();
//        } catch (SQLException e) {
//            // TODO Auto-generated catch block
//            System.out.println("SQL error! " + query);
//            e.printStackTrace();
//            e.getNextException().printStackTrace();
//        }
    }

    enum Wegzweck {BERUF, BILDUNG, EINKAUF, DIENST, URLAUB, PRIVAT, ALL}

    enum Modus {BAHN, MIV, LUFT, OESP, RAD, FUSS}

    class VP2030Data {
        int quelle = -1;
        int ziel = -1;
        double[][] modusZweck = new double[Modus.values().length][Wegzweck.values().length];
    }

}
