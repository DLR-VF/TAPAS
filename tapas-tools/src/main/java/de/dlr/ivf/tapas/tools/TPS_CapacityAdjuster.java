/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.dlr.ivf.api.io.ConnectionProvider;
import de.dlr.ivf.api.io.JdbcConnectionProvider;
import de.dlr.ivf.api.io.configuration.model.ConnectionDetails;
import de.dlr.ivf.api.io.configuration.model.RemoteDataSource;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.*;
import java.util.Map.Entry;

public class TPS_CapacityAdjuster{

    private final ConnectionProvider<Connection> connectionProvider;
    private final ConnectionDetails connector;
    String keyVal = "MID2008-Y2005";
    String scheme_class_distribution = "idspsg_shdg_14159_ward_PG32_BBR_RegTyp1_Di_Do";
    String region = "berlin";
    String locationTable = "berlin_locations";
    Map<Integer, List<Integer>> act2Loc = new HashMap<>();
    Map<Integer, Double> actOverloadFactor = new HashMap<>();
    Map<Integer, Double> locTypeCapGiven = new HashMap<>();
    Map<Integer, Double> locTypeCapDemanded = new HashMap<>();
    Map<Integer, Integer> personGroupCount = new HashMap<>();
    Map<Integer, ValueDistribution> schemeClassDistribution = new HashMap<>();
    Map<Integer, ValueDistribution> schemeClassActivityDistribution = new HashMap<>();
    Map<Integer, List<Triple<Integer, Double, Double>>> locCodeToTazIdAndXYGeoList = new HashMap<>();

    public TPS_CapacityAdjuster(ConnectionProvider<Connection> connectionProvider, ConnectionDetails connector){
        this.connectionProvider = connectionProvider;
        this.connector = connector;
    }

    public static void main(String[] args) throws IOException {

        Path configFile = Paths.get(args[0]);
        if (!Files.isRegularFile(configFile))
            throw new IllegalArgumentException("The provided argument is not a file.");

        ConnectionDetails connector = new ObjectMapper().readValue(configFile.toFile(), ConnectionDetails.class);

        TPS_CapacityAdjuster worker = new TPS_CapacityAdjuster(JdbcConnectionProvider.newJdbcConnectionProvider(), connector);

        worker.keyVal = "WISTA_scen2030a";
        worker.scheme_class_distribution = "MID_2008_TBG_7_Mo-So";
        worker.region = "berlin";
        worker.locationTable = "berlin_locations";
        worker.loadDBEntries();
        worker.calcLocDemand();
        worker.printNewDemand();
        worker.updateNewDemand();
//        worker.createNewMinimalLocationTable();
    }

    public void calcLocDemand() {

        //over all person groups
        for (Entry<Integer, Integer> entryPG : this.personGroupCount.entrySet()) {
            int pg = entryPG.getKey();
            int pgCount = entryPG.getValue();
            if (pg == 0) { //children below 6
                continue;
            }
            //get the scheme class distribution
            ValueDistribution schemeClassDist = this.schemeClassDistribution.get(pg);
            //over all scheme classes

            for (Entry<Integer, Double> entrySC : schemeClassDist.map.entrySet()) {
                int sc = entrySC.getKey();
                double weightSC = entrySC.getValue();
                if (sc == 21) { //scheme class 21 has no diaries!
                    continue;
                }
                if (weightSC > 0) {
                    //get the activity distribution
                    ValueDistribution activityDist = this.schemeClassActivityDistribution.get(sc);
                    for (Entry<Integer, Double> entryAct : activityDist.map.entrySet()) {
                        int actCode = entryAct.getKey();
                        double weightAct = entryAct.getValue();
                        double overloadFactor = actOverloadFactor.get(actCode);
                        //get the total location capacity for this activity type
                        List<Integer> locForThisAct = this.act2Loc.get(actCode);
                        int sumOfLoc = 0;
                        for (Integer locType : locForThisAct) {
                            if (this.locTypeCapGiven.get(locType) != null) {
                                sumOfLoc += this.locTypeCapGiven.get(locType);
                            }
                        }
                        if (sumOfLoc > 0) {
                            //now calc the demand
                            double demand = overloadFactor * pgCount * weightSC * weightAct;

                            //make it a factor
                            demand /= sumOfLoc;

                            //store the new val
                            for (Integer locType : locForThisAct) {
                                if (this.locTypeCapDemanded.get(locType) != null) {
                                    double newVal = this.locTypeCapDemanded.get(locType) + (this.locTypeCapGiven.get(
                                            locType) * demand);
                                    this.locTypeCapDemanded.put(locType, newVal);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public void calculateGenerationRates() {

        RemoteDataSource schemesDataSource = new RemoteDataSource(connector, "core.global_schemes");
        RemoteDataSource episodesDataSource = new RemoteDataSource(connector, "core.global_episodes");

        String query = """
                        WITH
                        episode_to_schemeclass AS(
                          SELECT a.*, b.*
                          FROM %1$s a
                          INNER JOIN %2$s b
                          ON a.scheme_id = b.scheme_id),
                        diarycount AS(
                          SELECT COUNT(*) diary_cnt, scheme_class_id
                          FROM %1$s
                          GROUP BY scheme_class_id),
                        act_cnt_scheme_class AS(
                          SELECT COUNT(*) act_cnt, act_code_zbe, scheme_class_id
                          FROM episode_to_schemeclass
                          WHERE home=FALSE AND act_code_zbe!= 80
                          GROUP BY act_code_zbe, scheme_class_id)
                        SELECT a.*, b.diary_cnt
                        FROM act_cnt_scheme_class a
                        INNER JOIN diarycount b
                        ON a.scheme_class_id = b.scheme_class_id
                        """.formatted(
                                schemesDataSource.getUri(),
                                episodesDataSource.getUri()
                            );

        try(Connection connection = connectionProvider.get(schemesDataSource.getConnector())) {
            try (PreparedStatement st = connection.prepareStatement(query);
                 ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    //store the average frequency
                    int act = rs.getInt("act_code_zbe");
                    int freq = rs.getInt("act_cnt");
                    int diaryCount = rs.getInt("diary_cnt");
                    int schemeClassId = rs.getInt("scheme_class_id");
                    ValueDistribution val;
                    //get/create the distribution
                    if (this.schemeClassActivityDistribution.containsKey(schemeClassId)) {
                        val = this.schemeClassActivityDistribution.get(schemeClassId);
                    } else {
                        val = new ValueDistribution();
                        this.schemeClassActivityDistribution.put(schemeClassId, val);
                    }
                    //store average number
                    val.map.put(act, (double) freq / (double) diaryCount);
                }
            }catch (SQLException e) {
                e.printStackTrace();
            }
        }catch (SQLException e){
            e.printStackTrace();
        }


//        String query = "";
//        ResultSet rs;
//        try {
//            //get the scheme classes
//            List<Integer> schemeClasses = new ArrayList<>();
//            query = "SELECT distinct scheme_class_id from core.global_schemes";
//            rs = dbCon.executeQuery(query, this);
//            while (rs.next()) {
//                schemeClasses.add(rs.getInt("scheme_class_id"));
//            }
//            rs.close();
//
//            for (Integer i : schemeClasses) {
//                //get the number of diaries
//                int sumDiaries = 0;
//                query = "SELECT count(*) as num from core.global_schemes where scheme_class_id =" + i;
//                rs = dbCon.executeQuery(query, this);
//                if (rs.next()) {
//                    sumDiaries = rs.getInt("num");
//                }
//                rs.close();
//
//                //get the frequency for each activity in this scheme class
//                query = "select count(*) as freq, act_code_zbe from core.global_episodes where home=FALSE and act_code_zbe!= 80 and scheme_id = any (SELECT scheme_id from core.global_schemes where scheme_class_id =" +
//                        i + ") group by act_code_zbe";
//                rs = dbCon.executeQuery(query, this);
//                while (rs.next()) {
//                    //store the average frequency
//                    int act = rs.getInt("act_code_zbe");
//                    int freq = rs.getInt("freq");
//                    ValueDistribution val;
//                    //get/create the distribution
//                    if (this.schemeClassActivityDistribution.containsKey(i)) {
//                        val = this.schemeClassActivityDistribution.get(i);
//                    } else {
//                        val = new ValueDistribution();
//                        this.schemeClassActivityDistribution.put(i, val);
//                    }
//                    //store average number
//                    val.map.put(act, (double) freq / (double) sumDiaries);
//                }
//                rs.close();
//            }
//
//        } catch (SQLException e) {
//            System.err.println(this.getClass().getCanonicalName() + " loadDB: SQL-Error during statement: " + query);
//            e.printStackTrace();
//        }



    }

    public void fillOverloadFactors() {

            RemoteDataSource dataSource = new RemoteDataSource(connector,"core." + region + "_act_2_loc_code");
            //load act2loc
            String query = "select distinct act_code from "+dataSource.getUri();

            try(Connection connection = connectionProvider.get(connector)){
                try (PreparedStatement st = connection.prepareStatement(query);
                     ResultSet rs = st.executeQuery()) {
                    while (rs.next()) {

                        int actCode = rs.getInt("act_code");
                        double factor;
                        //for now hard coded values
                        switch (actCode) {
                            case 10: //HOUSEWORK_AT_HOME
                                factor = 1.0;
                                break;
                            case 12: //"E_COMMERCE_OUT_OF_HOME"
                                factor = 2.0;
                                break;
                            case 32: //"HOUSEWORK_OUT_OF_HOME"
                                factor = 2.0;
                                break;
                            case 50: //"SHOPPING"
                                factor = 10.0;
                                break;
                            case 62: //"JOB_SEEKING"
                                factor = 1.0;
                                break;
                            case 211: //"WORKING"
                                factor = 1.1;
                                break;
                            case 231: //"CONVERSATION_ABOUT_WORK"
                                factor = 2.0;
                                break;
                            case 299: //"FREETIME_ACTIVE_AT_HOME"
                                factor = 1.0;
                                break;
                            case 300: //"VOLUNTARY_WORK"
                                factor = 1.5;
                                break;
                            case 410: //"SCHOOL"
                                factor = 1.1;
                                break;
                            case 411: //"UNIVERSITY"
                                factor = 1.1;
                                break;
                            case 499: //"LUNCH_BREAK_PUPILS"
                                factor = 1.0;
                                break;
                            case 511: //"SLEEPING"
                                factor = 1.0;
                                break;
                            case 512: //"RELAXING"
                                factor = 1.0;
                                break;
                            case 522: //"PERSONAL_MATTERS"
                                factor = 1.1;
                                break;
                            case 531: //"EATING_AT_HOME"
                                factor = 1.0;
                                break;
                            case 611: //"CONVERSATION_ABOUT_PERSONAL_MATTER"
                                factor = 1.1;
                                break;
                            case 631: //"VISITING"
                                factor = 10.0;
                                break;
                            case 640: //"EXCURSIONS"
                                factor = 4.0;
                                break;
                            case 700: //"FREETIME_AT_HOME"
                                factor = 1.0;
                                break;
                            case 711: //"TELEVIEWING"
                                factor = 4.0;
                                break;
                            case 720: //"DINING_OR_GOING_OUT"
                                factor = 2.0;
                                break;
                            case 721: //"SPORTS"
                                factor = 2.0;
                                break;
                            case 722: //"PROMENADING"
                                factor = 10.0;
                                break;
                            case 723: //"PLAYING"
                                factor = 2.0;
                                break;
                            case 724: //"BEING_AT_AN_EVENT"
                                factor = 2.0;
                                break;
                            case 740: //"FREETIME_ANY"
                                factor = 1.0;
                                break;
                            case 799: //"ACTIVITIES_ANY"
                                factor = 1.0;
                                break;
                            case 800: //"LEARNING_WITH_CHILDREN"
                                factor = 1.0;
                                break;
                            case 880: //"TRANSPORTING_CHILDREN"
                                factor = 1.0;
                                break;
                            default:
                                factor = 1.0;
                                break;
                        }
                        actOverloadFactor.put(actCode, factor);
                    }
                }catch (SQLException e){
                    e.printStackTrace();
                }
            }catch (SQLException e){
                e.printStackTrace();
            }
    }

    public void createNewMinimalLocationTable(){

        RemoteDataSource adressenBkgTable = new RemoteDataSource(connector, "public.adressen_bkg_2018");
        RemoteDataSource tazTable = new RemoteDataSource(connector, "core.berlin_taz_1223_multiline");
        RemoteDataSource locCodeTable = new RemoteDataSource(connector, "core.global_location_codes");
        RemoteDataSource locTable = new RemoteDataSource(connector, "core.berlin_locations_baz");

        int tazNumId;
        double x;
        double y;

        //get all location codes and write into a set
        String query = "select code_tapas from " + locCodeTable.getUri();

        try(Connection connection = connectionProvider.get(connector)){

            HashSet<Integer> locCodeSet = new HashSet<>();

            try (PreparedStatement st = connection.prepareStatement(query);
                 ResultSet rs = st.executeQuery()) {

                while (rs.next()) {
                    locCodeSet.add(rs.getInt("code_tapas"));
                }
            }catch (SQLException e){
                e.printStackTrace();
            }

            query = "with p as (select st_transform(geom,4326) as loc_coordinate from " + adressenBkgTable +
                    " where land_id=11 order by random() limit 10) " +
                    "select btm.taz_num_id, st_x(p.loc_coordinate), st_y(p.loc_coordinate) " + "from " + tazTable +
                    " btm join p on st_within(p.loc_coordinate, btm.the_geom)";

            for (Integer locCode : locCodeSet) {

                try (PreparedStatement st = connection.prepareStatement(query);
                     ResultSet rs = st.executeQuery()) {

                    locCodeToTazIdAndXYGeoList.put(locCode, new ArrayList<>());
                    while (rs.next()) {
                        tazNumId = rs.getInt("taz_num_id");
                        x = rs.getInt("st_x");
                        y = rs.getInt("st_y");
                        locCodeToTazIdAndXYGeoList.get(locCode).add(new ImmutableTriple<>(tazNumId, x, y));
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

            //write loc Table
            try(Statement statement = connection.createStatement()){
                int locId = 0;
                for (Integer locCode : locCodeSet){
                    for (Triple<Integer, Double, Double> e : locCodeToTazIdAndXYGeoList.get(locCode)) {
                        locId++;
                        tazNumId = e.getLeft();
                        x = e.getMiddle();
                        y = e.getRight();
                        if (tazNumId == 0) continue;
                        query = "Insert into "+ locTable
                                + " (loc_id, loc_blk_id, loc_taz_id, loc_code, loc_capacity, loc_coordinate)"
                                + "values ("
                                + locId + ", 0, " + tazNumId + ", " + locCode + ", 1, st_setsrid(st_makepoint("
                                + x + ", " + y + "), 4326));";
                        statement.executeUpdate(query);
                    }
                }
            }catch (SQLException e){
                e.printStackTrace();
            }
        }catch (SQLException e) {
            System.err.println(this.getClass().getCanonicalName() + " loadDB: SQL-Error during statement: " + query);
            e.printStackTrace();
        }
    }

    public void loadDBEntries() {

        try(Connection connection = connectionProvider.get(connector)){
            //load act2loc
            String query = "select act_code, loc_code from core." + region + "_act_2_loc_code order by act_code";
            try(PreparedStatement st = connection.prepareStatement(query);
                ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    int actCode = rs.getInt("act_code");
                    int locCode = rs.getInt("loc_code");
                    if (this.act2Loc.containsKey(actCode)) {
                        this.act2Loc.get(actCode).add(locCode);
                    } else {
                        List<Integer> loc = new ArrayList<>();
                        loc.add(locCode);
                        this.act2Loc.put(actCode, loc);
                    }
                }
            }catch (SQLException e){
                    e.printStackTrace();
            }

            //load locTypeCapGiven
            query = "select loc_code, sum(loc_capacity) as sum_cap from core." + locationTable +
                    " group by loc_code order by loc_code";

            try(PreparedStatement st = connection.prepareStatement(query);
                ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    int locCode = rs.getInt("loc_code");
                    int sumCap = rs.getInt("sum_cap");
                    locTypeCapGiven.put(locCode, (double) sumCap);
                    locTypeCapDemanded.put(locCode, 0.0);
                }
            }catch (SQLException e){
                e.printStackTrace();
            }

            //person groups
            query = "select p_group, count(p_group) as count_p from core." + region + "_persons where p_key='" +
                    this.keyVal + "' group by p_group order by p_group";

            try(PreparedStatement st = connection.prepareStatement(query);
                ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    int group = rs.getInt("p_group");
                    int sumGroup = rs.getInt("count_p");
                    personGroupCount.put(group, sumGroup);
                }
            }catch (SQLException e){
                e.printStackTrace();
            }

            //scheme class distribution
            query = "select person_group, scheme_class_id, probability from core.global_scheme_class_distributions where name='" +
                    scheme_class_distribution + "'";

            try(PreparedStatement st = connection.prepareStatement(query);
                ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    int pGroup = rs.getInt("person_group");
                    int schemeClass = rs.getInt("scheme_class_id");
                    double probability = rs.getDouble("probability");
                    if (probability > 0) {
                        if (this.schemeClassDistribution.containsKey(pGroup)) {
                            this.schemeClassDistribution.get(pGroup).map.put(schemeClass, probability);
                        } else {
                            ValueDistribution val = new ValueDistribution();
                            val.map.put(schemeClass, probability);
                            this.schemeClassDistribution.put(pGroup, val);
                        }
                    }
                }
            }catch (SQLException e){
                e.printStackTrace();
            }

            //schemeClassActivityFrequency
            this.calculateGenerationRates();

            this.fillOverloadFactors();

        } catch (SQLException e) {
            System.err.println(this.getClass().getCanonicalName() + " loadDB: SQL-Error during statement: ");
            e.printStackTrace();
        }
    }

    public void printNewDemand() {
        for (Integer key : this.locTypeCapDemanded.keySet()) {
            System.out.println("Loc type " + key + " stored cap: " + this.locTypeCapGiven.get(key) + " demanded cap: " +
                    this.locTypeCapDemanded.get(key));
        }

        for (Integer key : this.locTypeCapDemanded.keySet()) {
            if (this.locTypeCapGiven.get(key) > 0) {
                System.out.printf("Loc type %2d load factor: %5.3f\n", key,
                        (this.locTypeCapDemanded.get(key) / this.locTypeCapGiven.get(key)));
            } else {
                System.out.printf("Loc type %2d load factor: zero given\n", key);
            }
        }

    }

    public void updateNewDemand() {
        String query;

        try(Connection connection = connectionProvider.get(connector);
            Statement st = connection.createStatement()) {

            for (Integer key : this.locTypeCapDemanded.keySet()) {
                if (this.locTypeCapDemanded.get(key) > 0 && this.locTypeCapGiven.get(key) > 0) {
                    double capFactor = (this.locTypeCapDemanded.get(key) / this.locTypeCapGiven.get(key));
                    query = "UPDATE core." + locationTable + " set loc_capacity = " + capFactor +
                            "*loc_capacity WHERE loc_code = " + key + " and " + capFactor + "*loc_capacity >= 1";
                    st.executeUpdate(query);
                    query = "UPDATE core." + locationTable + " set loc_capacity = 1 WHERE loc_code = " + key + " and " +
                            capFactor + "*loc_capacity < 1";
                    st.executeUpdate(query);
                }
            }
        }catch (SQLException e){
            e.printStackTrace();
        }
    }

    class ValueDistribution {
        public Map<Integer, Double> map = new HashMap<>();
    }

}
