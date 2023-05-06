/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.tools.OpportunityDistribution;


import java.io.*;


/**
 * Importiert Daten zur Arbeitsplatzverteilung verschiedener Gruppen und Formate und fügt diese der Datenbank zu.
 * <p>
 * Level:
 * <p>
 * 0 - Berlin
 * <p>
 * 5 - PLZ
 * 6 - statistisches Gebiet
 * <p>
 * 10 - TAZ
 *
 * @author boec_pa
 */
public class ImportDistributions{


    /**
     * @param args
     */
    @SuppressWarnings("unused")
    public static void main(String[] args) {

        ImportDistributions impDist = new ImportDistributions();

        String fn = "C:\\Users\\boec_pa\\Documents\\OpportunityDistribution\\geringfügig_beschäftigt.csv";
        GeringfuegigBeschaeftigte gb = impDist.new GeringfuegigBeschaeftigte(fn);
        fn = "C:\\Users\\boec_pa\\Documents\\OpportunityDistribution\\SozVers.csv";
        SozialVersichterte sv = impDist.new SozialVersichterte(fn);


    }

    public class GeringfuegigBeschaeftigte {

        public GeringfuegigBeschaeftigte(String fileName) {

            super();
            File file = new File(fileName);
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
                String line;
                String[] local;

                // {1,2,3} dummy value for loc_code, 5 dummy value for level
                String query = "INSERT INTO core.berlin_opportunity_distribution VALUES " +
                        "('Geringfügig Beschäftigte',%%PLZ%%, %%VOL%%,'{1,2,3}',5,'Postleitzahl zu TVZ')";
                int cnt = 0;
                String plz, vol, curQ;
                while ((line = br.readLine()) != null) {
                    local = line.split(";");
                    //validate data

                    if (local.length == 0) continue;
                    try {
                        Integer.parseInt(local[0]);//plz
                        Integer.parseInt(local[1].replaceAll("\\.", ""));    //volume
                    } catch (NumberFormatException e) {
                        System.out.println("In der folgenden Zeile konnten die Zahlen nicht korrekt geparst werden:");
                        System.out.println(line);
                        continue;
                    }

                    plz = local[0];
                    vol = local[1].replaceAll("\\.", "");

                    curQ = query.replaceAll("%%PLZ%%", plz).replaceAll("%%VOL%%", vol);

                    //dbCon.execute(curQ, this);
                    cnt++;
                }


                br.close();
                System.out.println("Es wurden " + cnt + " Datensätze erfolgreich eingetragen.");

            } catch (IOException e) {
                //
            }


        }


    }

    public class SozialVersichterte {
        public SozialVersichterte(String fileName) {
            super();
            File file = new File(fileName);
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
                String line;
                String[] local;
                // {1,2,3} dummy value for loc_code, 6 dummy value for level
                String query = "INSERT INTO core.berlin_opportunity_distribution" +
                        " VALUES ('Sozialversicherte', %%AREA%%, %%VOL%%,'{1,2,3}',6,'Statistisches Gebiet zu TVZ')";
                int cnt = 0;
                String gebiet, vol, curQ = "";
                while ((line = br.readLine()) != null) {
                    local = line.split(";");
                    //validate data

                    if (local.length < 5) continue;//empty line
                    if (local[2].length() > 0) continue; //nicht die Übersicht

                    try {


                        Integer.parseInt(local[0]);//Gebietsnummer
                        Integer.parseInt(local[4].replaceAll("\\.", ""));//anzahl beschäftiger
                    } catch (NumberFormatException e) {
                        System.out.println("In der folgenden Zeile konnten die Zahlen nicht korrekt geparst werden:");
                        System.out.println(line);
                        continue;
                    }

                    gebiet = local[0];
                    vol = local[4].replaceAll("\\.", "");

                    curQ = query.replaceAll("%%AREA%%", gebiet).replaceAll("%%VOL%%", vol);

                    //dbCon.execute(curQ, this);
                    cnt++;
                }


                br.close();
                System.out.println("Es wurden " + cnt + " Datensätze erfolgreich eingetragen.");


            } catch (IOException e) {
                //
            }

        }
    }

}
