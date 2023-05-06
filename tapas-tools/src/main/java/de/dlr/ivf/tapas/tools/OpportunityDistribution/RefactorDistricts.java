/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.tools.OpportunityDistribution;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Erzeugt Altebezirke aus
 * \\Bafs02.intra.dlr.de\vf-ba\VF_Server_neu\Projekte\PJ_laufend\VEU_Umsetzungsphase\VEU_Inhalte\TP1000_2000\TP1000\AP1200\Raumstruktur
 * und fügt sie in berlin_taz_mapping_values ein
 * <p>
 * Erzeugt außerdem Descriptions aus dieser Datei für Altbezirke, Bezirke und stat. Gebiete
 *
 * @author boec_pa
 */
public class RefactorDistricts {


    static String[] altBezirkNamen = {"Mitte", "Tiergarten", "Wedding", "Prenzlauer Berg", "Friedrichshain", "Kreuzberg", "Charlottenburg", "Spandau", "Wilmersdorf", "Zehlendorf", "Schöneberg", "Steglitz", "Tempelhof", "Neukölln", "Treptow", "Köpenick", "Lichtenberg", "Weißensee", "Pankow", "Reinickendorf", "Marzahn", "Hohenschönhausen", "Hellersdorf"};

    public static String bezirkName(String num) {

        return altBezirkNamen[Integer.parseInt(num) - 1];

    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        RefactorDistricts rd = new RefactorDistricts();

        HashMap<String, HashSet<String>> altbezirke = new HashMap<>(); //number, TVZs

        String fn = "C:\\Users\\boec_pa\\Documents\\OpportunityDistribution\\Berlin_Zuordnung.csv";

        File file = new File(fn);
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            String line;
            String[] local;
            String nName, sName, aNum, nNum, sNum, tvz, locUpdateNeu, locUpdateStatG; //lokale Variablen.

            int cntBezirke = 0, cntStatG = 0;

            String qUpdateNeu = "UPDATE core.berlin_taz_mapping_values SET description = '%%NAME%%' WHERE name = '12 Bezirke' AND map_value = %%NUM%%";
            String qUpdateStatG = "UPDATE core.berlin_taz_mapping_values SET description = '%%NAME%%' WHERE name = 'Statistisches Gebiet zu TVZ' AND map_value = %%NUM%%";
            while ((line = br.readLine()) != null) {
                local = line.split(";");

                if (local.length < 9) continue; //empty line

                nNum = local[0];
                nName = local[1];
                aNum = local[3];
                sNum = local[5];
                sName = local[6];
                tvz = local[9];


                //validate
                try {
                    Integer.parseInt(nNum);
                    Integer.parseInt(aNum);
                    Integer.parseInt(sNum);
                } catch (NumberFormatException e) {
                    System.out.println("Fehler beim Parsen von:");
                    System.out.println(line);
                    continue;
                }

                locUpdateNeu = qUpdateNeu.replaceAll("%%NAME%%", nName).replaceAll("%%NUM%%", nNum);
                locUpdateStatG = qUpdateStatG.replaceAll("%%NAME%%", sName).replaceAll("%%NUM%%", sNum);
//                rd.dbCon.execute(locUpdateNeu, rd);
                cntBezirke++;
//                rd.dbCon.execute(locUpdateStatG, rd);
                cntStatG++;

                //locGebiet = new Gebiet(number, tvz)
                if (!altbezirke.containsKey(aNum)) altbezirke.put(aNum, new HashSet<>());
                altbezirke.get(aNum).add(tvz);

            }
            br.close();

            System.out.println("Es wurden " + cntBezirke + " Bezirke aktualisiert.");
            System.out.println("Es wurden " + cntStatG + " stat. Gebiete aktualisiert.");
        } catch (IOException e) {
            System.out.println("Fehler beim Lesen!");
        }

        String insertAB = "INSERT INTO core.berlin_taz_mapping_values VALUES " + " ('23 Altbezirke', %%NUM%%," +
                " (SELECT array_agg(taz_id) FROM core.berlin_taz WHERE %%WHERECLAUSE%%)," + " '%%NAME%%')";
        String locAB;
        StringBuilder whereclause;
        int cntAB = 0;
        for (String ab : altbezirke.keySet()) {//iterieren durch Altbezirke
            whereclause = new StringBuilder();
            for (String tvz : altbezirke.get(ab)) {//iterieren durch tvzs
                whereclause.append("(taz_num_id%100000) = ").append(tvz).append(" OR ");
            }
            whereclause = new StringBuilder(whereclause.substring(0, whereclause.length() - 4)); //kill last OR


            locAB = insertAB.replaceAll("%%NUM%%", ab).replaceAll("%%NAME%%", bezirkName(ab)).replaceAll(
                    "%%WHERECLAUSE%%", whereclause.toString());

            //System.out.println(locAB + "\n\n\n");
//            rd.dbCon.execute(locAB, rd);
            cntAB++;
        }

        System.out.println("Es wurden " + cntAB + " Altbezirke hinzugefügt.");


    }

}
