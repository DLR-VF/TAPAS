/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.tools.OpportunityDistribution;

import de.dlr.ivf.tapas.tools.persitence.db.TPS_BasicConnectionClass;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

public class UniqueStreetIdentifier extends TPS_BasicConnectionClass {

    private static final double MAXDIST = 2E3;

    private static HashMap<HashSet<String>, HashSet<String>> combineHsn(HashMap<String, HashSet<String>> hsn, ArrayList<ArrayList<String>> streets) {

        HashMap<HashSet<String>, HashSet<String>> result = new HashMap<>();

        for (ArrayList<String> al : streets) {
            HashSet<String> plzs = new HashSet<>();
            HashSet<String> hn = new HashSet<>();

            for (String plz : al) {
                plzs.add(plz);
                hn.addAll(hsn.get(plz));
            }

            result.put(plzs, hn);

        }

        return result;
    }

    private static ArrayList<ArrayList<String>> combineStreets(HashMap<PlzPair, Double> input) {

        ArrayList<ArrayList<String>> result = new ArrayList<>();

        for (Entry<PlzPair, Double> e : input.entrySet()) {
            boolean touches = e.getValue() <= MAXDIST;

            // find first entry
            int idxLeft = findIdx(e.getKey().getLeft(), result);
            int idxRight = findIdx(e.getKey().getRight(), result);

            if (idxLeft == -1 && idxRight == -1) { // both don't exist
                if (touches) {
                    ArrayList<String> list = new ArrayList<>();
                    list.add(e.getKey().getLeft());
                    list.add(e.getKey().getRight());
                    result.add(list);
                } else {
                    ArrayList<String> list = new ArrayList<>();
                    list.add(e.getKey().getLeft());
                    result.add(list);
                    ArrayList<String> list2 = new ArrayList<>();
                    list2.add(e.getKey().getRight());
                    result.add(list2);
                }
            } else if (idxLeft == -1) {// left does not exist, right does
                if (touches) {
                    result.get(idxRight).add(e.getKey().getLeft());
                } else {
                    ArrayList<String> list = new ArrayList<>();
                    list.add(e.getKey().getLeft());
                    result.add(list);
                }

            } else if (idxRight == -1) {// left exists, right doesnt
                if (touches) {
                    result.get(idxLeft).add(e.getKey().getRight());
                } else {
                    ArrayList<String> list = new ArrayList<>();
                    list.add(e.getKey().getRight());
                    result.add(list);
                }
            } else { // both exist
                if (touches) {
                    if (idxLeft == idxRight) continue;// should not happen
                    move(idxRight, idxLeft, result);
                }
            }
        }
        return result;
    }

    private static int findIdx(String s, ArrayList<ArrayList<String>> list) {
        int idx = -1;
        for (int i = 0; i < list.size(); i++) {
            for (int j = 0; j < list.get(i).size(); j++) {
                if (list.get(i).contains(s)) {
                    return i;
                }
            }
        }
        return idx;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        UniqueStreetIdentifier ui = new UniqueStreetIdentifier();
        HashMap<String, HashMap<HashSet<String>, HashSet<String>>> strPlzHsn = new HashMap<>();
        try {
            PreparedStatement getStreetParts = ui.dbCon.getConnection(ui).prepareStatement(
                    "WITH strassen AS ( " + " SELECT strasse,plz,ARRAY_AGG(DISTINCT hausnummer) AS hsn, " +
                            " ST_CONVEXHULL(ST_COLLECT(the_geom)) AS the_geom " + " FROM core.berlin_buildings " +
                            " WHERE bundesland = '11' AND strasse = ? " + " GROUP BY strasse,plz) " +
                            " SELECT s1.strasse, s1.plz AS plz1, s2.plz AS plz2, s1.hsn AS hsn1, s2.hsn AS hsn2,  " +
                            " ST_DISTANCE(s1.the_geom,s2.the_geom, false) AS dist " + " FROM strassen s1 " +
                            " JOIN strassen s2 ON s1.plz < s2.plz");

            ResultSet rs = ui.dbCon.executeQuery(
                    "SELECT strasse FROM core.berlin_buildings " + " WHERE bundesland = '11' " + " GROUP BY strasse " +
                            " HAVING COUNT(DISTINCT plz) > 1", ui);

            ArrayList<String> strassen = new ArrayList<>();

            while (rs.next()) {
                strassen.add(rs.getString("strasse"));
            }
            rs.close();

            for (String str : strassen) {
//				if (verbose)
//					System.out.println(str);

                getStreetParts.setString(1, str);
                ResultSet rs2 = getStreetParts.executeQuery();

                HashMap<PlzPair, Double> input = new HashMap<>();

                HashMap<String, HashSet<String>> plzHsn = new HashMap<>();

                while (rs2.next()) {

                    String plz1 = rs2.getString("plz1");
                    String plz2 = rs2.getString("plz2");
                    String[] hsn1 = (String[]) rs2.getArray("hsn1").getArray();
                    String[] hsn2 = (String[]) rs2.getArray("hsn2").getArray();

                    HashSet<String> hn1 = new HashSet<>(Arrays.asList(hsn1));
                    HashSet<String> hn2 = new HashSet<>(Arrays.asList(hsn2));

                    if (!plzHsn.containsKey(plz1)) plzHsn.put(plz1, hn1);
                    if (!plzHsn.containsKey(plz2)) plzHsn.put(plz2, hn2);

                    double dist = rs2.getDouble("dist");

                    input.put(new PlzPair(plz1, plz2), dist);

                }
                rs2.close();

                ArrayList<ArrayList<String>> streets = combineStreets(input);


                HashMap<HashSet<String>, HashSet<String>> plzHsnCombined = combineHsn(plzHsn, streets);

                if (plzHsnCombined.size() > 1) {//mehr als eine echte Strasse
                    for (Entry<HashSet<String>, HashSet<String>> e : plzHsnCombined.entrySet()) {
                        if (!e.getValue().contains("1")) {
                            System.out.println(str + e.getKey());
                        }
                    }
                }


                strPlzHsn.put(str, plzHsnCombined);


//				for (ArrayList<String> al : streets) {
//					for (String s : al) {
//						System.out.print(s + ", ");
//					}
//					System.out.println();
//				}
//				System.out.println();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    private static void move(int from, int to, ArrayList<ArrayList<String>> list) {

        list.get(to).addAll(list.get(from));
        list.remove(from);

    }

    public static class PlzPair {
        private final String left;
        private final String right;

        public PlzPair(String left, String right) {
            this.left = left;
            this.right = right;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null) return false;
            if (!(o instanceof PlzPair)) return false;
            PlzPair pairo = (PlzPair) o;
            return this.left.equals(pairo.getLeft()) && this.right.equals(pairo.getRight());
        }

        public String getLeft() {
            return left;
        }

        public String getRight() {
            return right;
        }

        @Override
        public int hashCode() {
            return left.hashCode() ^ right.hashCode();
        }

        @Override
        public String toString() {
            return "{" + left + "," + right + "}";
        }

    }

}
