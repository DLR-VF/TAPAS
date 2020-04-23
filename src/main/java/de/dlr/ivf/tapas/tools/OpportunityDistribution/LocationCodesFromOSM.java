package de.dlr.ivf.tapas.tools.OpportunityDistribution;

import de.dlr.ivf.tapas.tools.persitence.db.TPS_BasicConnectionClass;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

public class LocationCodesFromOSM extends TPS_BasicConnectionClass {

    /**
     * @param args
     */
    public static void main(String[] args) {
        String fileName = "C:\\Users\\boec_pa\\Documents\\OpportunityDistribution\\OSM2Locations.csv";
        File file = new File(fileName);

        HashMap<String, HashMap<String, HashSet<String>>> map = new HashMap<>();

        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            String line;
            String[] local;
            String locName, locTag;
            String[] locKeyWords;
            String pattern = "(?=([^\"]*\"[^\"]*\")*[^\"]*$)";// look ahead,
            // find non
            // quoted!

            while ((line = br.readLine()) != null) {
                if (line.startsWith("#")) continue; // commented line
                local = line.split(";" + pattern);

                if (local.length != 3) {
                    System.err.println("In der folgenden Zeile konnten die Zahlen nicht korrekt geparst werden:");
                    System.err.println(line);
                    continue;
                }
                locName = local[0];
                locTag = local[1];
                locKeyWords = local[2].split("," + pattern);

                if (!map.containsKey(locName)) map.put(locName, new HashMap<>());
                if (!map.get(locName).containsKey(locTag)) map.get(locName).put(locTag.replaceAll("\"", ""),
                        new HashSet<>());
                for (String key : locKeyWords)
                    map.get(locName).get(locTag).add(key.replaceAll("\"", ""));

            }

            br.close();
        } catch (IOException e) {
            System.err.println("Error reading file " + fileName);
            return;
        }
        if (map.size() == 0) {
            System.out.println("No entries read in " + fileName);
            return;
        }

        // Build queries
        file = new File(file.getParent() + "\\queries.sql");
        int cnt = 0;
        try {
            file.createNewFile();
            BufferedWriter bf = new BufferedWriter(new FileWriter(file));
            // planet_osm_point
            String queryTemplate =
                    "SELECT ARRAY['%TABLE%','%FIELD%'] AS source, osm_id, way AS the_geom, %VOL% AS volume," +
                            " '%TYPE%' AS type, ARRAY[name,%FIELD%] AS description FROM %TABLE%\n";
            String volume = "ST_AREA(geography(way))";
            for (Entry<String, HashMap<String, HashSet<String>>> type : map.entrySet()) {
                for (Entry<String, HashSet<String>> field : type.getValue().entrySet()) {
                    StringBuilder whereClause = new StringBuilder(" WHERE ");
                    for (String value : field.getValue()) {
                        whereClause.append("OR ").append(field.getKey()).append("='").append(value).append("' ");
                    }
                    whereClause = new StringBuilder(whereClause.toString().replaceFirst("OR", "")); // Delete
                    // first
                    // OR

                    String currentQuery = queryTemplate.replaceAll("%VOL%", "'-1'").replaceAll("%TABLE%",
                            "planet_osm_point").replaceAll("%FIELD%", field.getKey()).replaceAll("%TYPE%",
                            type.getKey()).concat(whereClause + " \nUNION\n").concat(queryTemplate).replaceAll("%VOL%",
                            volume).replaceAll("%TABLE%", "planet_osm_polygon").replaceAll("%FIELD%", field.getKey())
                                                       .replaceAll("%TYPE%", type.getKey()).concat(
                                    whereClause.toString());
                    bf.write("INSERT INTO osm2location " + currentQuery + ";\n\n");
                    cnt++;
                }

            }
            bf.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Successfully written " + cnt + " queries to " + file.getPath());
    }

}
