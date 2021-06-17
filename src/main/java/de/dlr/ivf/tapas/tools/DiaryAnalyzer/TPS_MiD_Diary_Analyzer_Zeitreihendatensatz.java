/*
 * Copyright (c) 2021 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.tools.DiaryAnalyzer;

import de.dlr.ivf.tapas.tools.persitence.db.TPS_BasicConnectionClass;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.io.PrintWriter;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.Map.Entry;

public class TPS_MiD_Diary_Analyzer_Zeitreihendatensatz extends TPS_BasicConnectionClass {

    static final int ONE_DAY = 24 * 60;
    static final int ANY_DIARY_GROUP = -1;
    static int ERHEBUNG = 1;
    static final String diaryGroupCol = "tbg";
    static final String diaryGroupColName = "tbg";
    static final String pgTapasCol = "pg_tapas34";
    static String tableKey = "MiD2002-2008-2017_";
//    static String tableKey = "MiD2002-2008-2017_pg_x_";
    static String pgXColFilter = pgTapasCol + " != -1 ";

    //static final int kindergartengruppe = 91;

    Map<String, Diary> diaryMap = new HashMap<>();
    Map<Integer, Integer> accompanyStat = new HashMap<>();
    Map<Integer, Integer> UncodedAccompanyStat = new HashMap<>();
    Map<Integer, Integer> purposeNonHomeEndTrip = new HashMap<>();
    Map<Integer, Integer> purposeNonHomeStartTrip = new HashMap<>();
    Map<Integer, Map<Integer, Integer>> diaryStatistics = new HashMap<>();
    Map<Integer, Map<Integer, Integer>> diarySumStatistics = new HashMap<>();
    Map<Integer, Map<Integer, Double>> personGroupDistribution = new HashMap<>();
    Set<Integer> activities = new TreeSet<>();
    Set<Integer> diaryGroups = new TreeSet<>();

    int recoded = 0;
    int notRecoded = 0;
    int numOfDoubleWays = 0;
    int numOfExactDoubleWays = 0;
    int numOfDiariesStartingWithATrip = 0;
    int numOfDiariesNotEndingAtHome = 0;

    public static void main(String[] args) {
        ERHEBUNG = Integer.parseInt(args[0]);
        if (args.length == 2) pgXColFilter = pgTapasCol + " in (" + args[1] + ") ";
        tableKey = tableKey + String.valueOf(ERHEBUNG);
        TPS_MiD_Diary_Analyzer_Zeitreihendatensatz worker = new TPS_MiD_Diary_Analyzer_Zeitreihendatensatz();
        HashMap<String, String> times = new HashMap<>();
//		times.put("true", "_Mo-So");
//        times.put("st_wotag = ANY(ARRAY[1,2,3,4,5])","_Mo-Fr");
        times.put("st_wotag = ANY(ARRAY[2,3,4])", "_Di-Do");
//        times.put("st_wotag = ANY(ARRAY[6,7])","_Sa-So");
//        times.put("st_wotag = ANY(ARRAY[5, 6,7])","_Fr-So");
//		times.put("st_wotag = 1","_Mo");
//		times.put("st_wotag = 2","_Di");
//		times.put("st_wotag = 3","_Mi");
//		times.put("st_wotag = 4","_Do");
//		times.put("st_wotag = 5","_Fr");
//		times.put("st_wotag = 6","_Sa");
//		times.put("st_wotag = 7","_So");
        HashMap<String, String> regions = new HashMap<>();

//		regions.put("true", "");
//		regions.put("rtyp = 1", "_RTYP1");
//		regions.put("rtyp = 2", "_RTYP2");
//		regions.put("rtyp = 3", "_RTYP3");
//		regions.put("polgk =1", "_PolGK1");
//		regions.put("polgk =2", "_PolGK2");
//		regions.put("polgk =3", "_PolGK3");
//		regions.put("polgk =4", "_PolGK4");
//		regions.put("polgk =5", "_PolGK5");
        regions.put("polgk =6", "_PolGK6");


        for (Entry<String, String> t : times.entrySet()) {
            for (Entry<String, String> r : regions.entrySet()) {
                System.out.println(ERHEBUNG);
                System.out.println(pgTapasCol);
                System.out.println(pgXColFilter);
                System.out.println(t);
                System.out.println(r);
                worker.readMIDDiary("public.\"MiD2002-2008-2017_Wege\"", t.getKey() + " and " + r.getKey());
                //System.out.println("Read "+worker.diaryMap.size()+" diaries");
                //System.out.println("Found "+worker.numOfDoubleWays+" doublet ways. "+worker.numOfExactDoubleWays+" are exact doublets.");
                worker.calcDiaryStatistics();
                boolean printOnScreen = false;
                boolean delete = false;

                if (!printOnScreen && delete) {
                    worker.cleanUpDB("core.global_scheme_classes");
                    worker.cleanUpDB("core.global_episodes");
                    worker.cleanUpDB("core.global_schemes");
                }
//                worker.printSchemeClassSQLInserts("core.global_scheme_classes", printOnScreen);
//                worker.printDiariesSQLInserts("core.global_episodes", "core.global_schemes",
//                printOnScreen);
//                worker.printDistributionVectors();
                worker.printDistributionVectorSQLInserts("core.global_scheme_class_distributions",
                        "MID_2002-2008-2017-" + diaryGroupColName + t.getValue() + r.getValue(), printOnScreen);
                worker.clearEverything();
            }
        }
    }


    public void calcDiaryStatistics() {
        Map<Integer, Integer> groupStatistics;
        Map<Integer, Integer> groupSumStatistics;
        Map<Integer, Double> groupDistribution;
        int countGroup;
        for (Diary e : this.diaryMap.values()) {

            if (!diaryStatistics.containsKey(e.group)) { // make new group
                groupStatistics = new HashMap<>();
                for (Integer i : this.activities) {//fill with init-values for all activities
                    groupStatistics.put(i, 0);
                }
                diaryStatistics.put(e.group, groupStatistics);
            } else {
                groupStatistics = diaryStatistics.get(e.group);
            }

            if (!diarySumStatistics.containsKey(e.group)) { // make new group
                groupSumStatistics = new HashMap<>();
                for (Integer i : this.activities) {//fill with init-values for all activities
                    groupSumStatistics.put(i, 0);
                }
                diarySumStatistics.put(e.group, groupSumStatistics);
            } else {
                groupSumStatistics = diarySumStatistics.get(e.group);
            }

            if (!personGroupDistribution.containsKey(e.pGroup)) { //make new person group
                groupDistribution = new HashMap<>();
                for (Integer i : this.diaryGroups) {//fill with init-values for all groups
                    groupDistribution.put(i, 0.0);
                }
                personGroupDistribution.put(e.pGroup, groupDistribution);
            } else {
                groupDistribution = personGroupDistribution.get(e.pGroup);
            }
            //now fill the statistics
            for (DiaryElement d : e.activities) {
                if (d.stay && !d.home) {
                    groupStatistics.put(d.purpose, groupStatistics.get(d.purpose) + 1);
                    groupSumStatistics.put(d.purpose, groupSumStatistics.get(d.purpose) + d.getDuration());
                }
            }
            //and count one diary for this persongroup
//            countGroup = groupDistribution.get(e.group);
            groupDistribution.put(e.group, groupDistribution.get(e.group) + e.weight);
        }
    }

    public void cleanUpDB(String table) {
        String query = "DELETE FROM " + table + " WHERE key = '" + tableKey + "'";
        this.dbCon.execute(query, this);
    }

    public void clearEverything() {
        this.diaryMap.clear();
        this.accompanyStat.clear();
        this.UncodedAccompanyStat.clear();
        this.purposeNonHomeEndTrip.clear();
        this.purposeNonHomeStartTrip.clear();
        this.diaryStatistics.clear();
        this.diarySumStatistics.clear();
        this.personGroupDistribution.clear();
        this.activities.clear();
        this.diaryGroups.clear();
        this.recoded = 0;
        this.notRecoded = 0;
        this.numOfDoubleWays = 0;
        this.numOfExactDoubleWays = 0;
        this.numOfDiariesStartingWithATrip = 0;
        this.numOfDiariesNotEndingAtHome = 0;

    }

    public void printDiariesSQLInserts(String table_episode, String table_schemes, boolean print) {
        //triple consists of trip purpose/diary group
        Map<ImmutablePair<Integer, Integer>, Integer> activityMapping = new HashMap<>();
        //build the code mapping
        activityMapping.put(new ImmutablePair<>(0, ANY_DIARY_GROUP), 10); //stay at home

        activityMapping.put(new ImmutablePair<>(1, ANY_DIARY_GROUP), 211); //unspecified workers->WORKING
        activityMapping.put(new ImmutablePair<>(1, 1), 212); //full time workers->WORKING FULL TIME
        activityMapping.put(new ImmutablePair<>(1, 2), 212); //full time workers->WORKING FULL TIME
        activityMapping.put(new ImmutablePair<>(1, 3), 212); //full time workers->WORKING FULL TIME
        activityMapping.put(new ImmutablePair<>(1, 4), 212); //full time workers->WORKING FULL TIME
        activityMapping.put(new ImmutablePair<>(1, 5), 212); //full time workers->WORKING FULL TIME

        activityMapping.put(new ImmutablePair<>(1, 6), 213); //part time workers->WORKING PART TIME
        activityMapping.put(new ImmutablePair<>(1, 7), 213); //part time workers->WORKING PART TIME
        activityMapping.put(new ImmutablePair<>(1, 8), 213); //part time workers->WORKING PART TIME
        activityMapping.put(new ImmutablePair<>(1, 9), 213); //part time workers->WORKING PART TIME
        activityMapping.put(new ImmutablePair<>(1, 10), 213); //part time workers->WORKING PART TIME


        activityMapping.put(new ImmutablePair<>(2, ANY_DIARY_GROUP), 211); //Business trip->WORKING

        activityMapping.put(new ImmutablePair<>(3, ANY_DIARY_GROUP),
                413); //Bildungsweg->Kindergarden TODO WHY? Kindergarden
        activityMapping.put(new ImmutablePair<>(3, 1), 412); //Vollzeit berufstätig->SCHOOL TRAINEE
        activityMapping.put(new ImmutablePair<>(3, 2), 412); //Vollzeit berufstätig->SCHOOL TRAINEE
        activityMapping.put(new ImmutablePair<>(3, 3), 412); //Vollzeit berufstätig->SCHOOL TRAINEE
        activityMapping.put(new ImmutablePair<>(3, 4), 412); //Vollzeit berufstätig->SCHOOL TRAINEE
        activityMapping.put(new ImmutablePair<>(3, 5), 412); //Vollzeit berufstätig->SCHOOL TRAINEE
        activityMapping.put(new ImmutablePair<>(3, 6), 412); //Teilzeit berufstätig->SCHOOL TRAINEE
        activityMapping.put(new ImmutablePair<>(3, 7), 412); //Teilzeit berufstätig->SCHOOL TRAINEE
        activityMapping.put(new ImmutablePair<>(3, 8), 412); //Teilzeit berufstätig->SCHOOL TRAINEE
        activityMapping.put(new ImmutablePair<>(3, 9), 412); //Teilzeit berufstätig->SCHOOL TRAINEE
        activityMapping.put(new ImmutablePair<>(3, 10), 412); //Teilzeit berufstätig->SCHOOL TRAINEE

        activityMapping.put(new ImmutablePair<>(3, 24), 412); //Azubi->SCHOOL TRAINEE
        activityMapping.put(new ImmutablePair<>(3, 12), 410); //Pupil ->SCHOOL
        activityMapping.put(new ImmutablePair<>(3, 11), 411); //Bildungsweg, Student->University

        activityMapping.put(new ImmutablePair<>(4, ANY_DIARY_GROUP), 50);  //Shopping ->SHOPPING

        activityMapping.put(new ImmutablePair<>(5, ANY_DIARY_GROUP), 522); //private Erledigung ->PERSONAL_MATTERS
        activityMapping.put(new ImmutablePair<>(6, ANY_DIARY_GROUP), 740); //hwzweck2 Freizeit ->FREETIME_ANY
        activityMapping.put(new ImmutablePair<>(7, ANY_DIARY_GROUP), 799); //hwzweck2 Begleitung -> ACTIVITIES_ANY

        //TODO keine Aktivität fürs Bringen von allgemeinen Personen? (es gibt Bringen von Kindern in der DB aber
        // nicht in MiD)
//        activityMapping.put(new ImmutablePair<>(6, ANY_DIARY_GROUP),799); //Bringen+Holen: kein Einkaufs-, Erledigungs- und Freizeitweg?!?!?->ACTIVITIES_ANY

//        activityMapping.put(new ImmutablePair<>(7, ANY_DIARY_GROUP), 740); //Freizeit: keine Angabe->FREETIME_ANY
////
//        activityMapping.put(new ImmutablePair<>(8, ANY_DIARY_GROUP), 10); //nach Hause->HOUSEWORK_AT_HOME
//        activityMapping.put(new ImmutablePair<>(9, ANY_DIARY_GROUP), 799); //Rückweg vom vorherigen Weg->ACTIVITIES_ANY
//        activityMapping.put(new ImmutablePair<>(10, ANY_DIARY_GROUP), 799); //andere Zweck->ACTIVITIES_ANY
//        activityMapping.put(new ImmutablePair<>(11, ANY_DIARY_GROUP), 410); //(Vor-)Schule->SCHOOL
//        activityMapping.put(new ImmutablePair<>(12, ANY_DIARY_GROUP), 413); //Kita/Kindergarten/Hort->KINDERGARDEN
//        activityMapping.put(new ImmutablePair<>(13, ANY_DIARY_GROUP), 799); //Begleitung Erwachsener->ACTIVITIES_ANY
//        activityMapping.put(new ImmutablePair<>(14, ANY_DIARY_GROUP), 740); //Sport/Sportverein->Sports
//        activityMapping.put(new ImmutablePair<>(15, ANY_DIARY_GROUP), 740); //Freunde besuchen -> Visiting
//        activityMapping.put(new ImmutablePair<>(16, ANY_DIARY_GROUP), 740); //Unterricht(nicht Schule)->Weiterbildung

//        activityMapping.put(new ImmutablePair<>(8, ANY_DIARY_GROUP), 10); //nach Hause->HOUSEWORK_AT_HOME
//        activityMapping.put(new ImmutablePair<>(9, ANY_DIARY_GROUP), 799); //Rückweg vom vorherigen Weg->ACTIVITIES_ANY
//        activityMapping.put(new ImmutablePair<>(10, ANY_DIARY_GROUP), 799); //andere Zweck->ACTIVITIES_ANY
//        activityMapping.put(new ImmutablePair<>(11, ANY_DIARY_GROUP), 410); //(Vor-)Schule->SCHOOL
//        activityMapping.put(new ImmutablePair<>(12, ANY_DIARY_GROUP), 413); //Kita/Kindergarten/Hort->KINDERGARDEN
//        activityMapping.put(new ImmutablePair<>(13, ANY_DIARY_GROUP), 799); //Begleitung Erwachsener->ACTIVITIES_ANY
//        activityMapping.put(new ImmutablePair<>(14, ANY_DIARY_GROUP), 721); //Sport/Sportverein->Sports
//        activityMapping.put(new ImmutablePair<>(15, ANY_DIARY_GROUP), 631); //Freunde besuchen -> Visiting
//        activityMapping.put(new ImmutablePair<>(16, ANY_DIARY_GROUP), 414); //Unterricht(nicht Schule)->Weiterbildung

        activityMapping.put(new ImmutablePair<>(99, ANY_DIARY_GROUP),
                799); //keine Angabe/im PAPI nicht erhoben->ACTIVITIES_ANY

        //now we go through the diaries and convert the numbers:
        int schemeID = 1, start, duration, act_code_zbe, tourNumber;
        ImmutablePair<Integer, Integer> key;
        boolean home, workchain;

        PrintWriter pw = new PrintWriter(System.out); //needed to get rid of stupid german localization of doubles!
        String tmpString = String.format(Locale.ENGLISH,
                "INSERT INTO %s" + " (scheme_id, start, duration, act_code_zbe, home, tournumber, workchain, key) " +
                        "VALUES (?,?,?,?,?,?,?,'%s');", table_episode, tableKey);

        try {
            PreparedStatement pS = this.dbCon.getConnection(this).prepareStatement(tmpString);
            int batchSize = 0, maxSize = 10000;

            int schemes = 0, diaries = 0;
            for (Entry<String, Diary> e : this.diaryMap.entrySet()) {
                Diary tmp = e.getValue();
                tmp.schemeID = schemeID;
                tmpString = String.format(Locale.ENGLISH,
                        "INSERT INTO %s (scheme_id, scheme_class_id, homework, key) VALUES (%d,%d,false,'%s');",
                        table_schemes, tmp.schemeID, tmp.group, tableKey);
                if (print) {
                    pw.printf(tmpString + "\n");
                } else {
                    this.dbCon.execute(tmpString, this);
                }
                schemes++;

                for (DiaryElement d : tmp.activities) {
                    diaries++;
                    start = d.start_min;
                    duration = d.getDuration();
                    key = new ImmutablePair<>(d.purpose, tmp.group);
                    if (!activityMapping.containsKey(key)) {
                        key = new ImmutablePair<>(d.purpose, ANY_DIARY_GROUP);
                    }

                    act_code_zbe = activityMapping.get(key);
                    tourNumber = d.tourNumber;
                    home = d.home && d.stay;
                    workchain = d.workchain;
                    if (!d.stay) act_code_zbe = 80;
                    //System.out.printf("Scheme %6d Start %4d Duration %4d Act %3d tour %2d home %s workchain %s\n", schemeID, start, duration, act_code_zbe, tourNumber, (home?"T":"F"), (workchain?"T":"F"));
                    tmpString = String.format(Locale.ENGLISH, "INSERT INTO %s" +
                                    " (scheme_id, start, duration, act_code_zbe, home, tournumber, workchain, key) " +
                                    "VALUES (%d,%d,%d,%d,%s,%d,%s,'%s'); --hhid: %d, pid: %d", table_episode, tmp.schemeID,
                            start, duration, act_code_zbe, home, tourNumber, workchain, tableKey, tmp.hhID, tmp.pID);
                    if (print) {
                        pw.printf(tmpString + "\n");
                    } else {
                        int index = 1;
                        pS.setInt(index++, tmp.schemeID);
                        pS.setInt(index++, start);
                        pS.setInt(index++, duration);
                        pS.setInt(index++, act_code_zbe);
                        pS.setBoolean(index++, home);
                        pS.setInt(index++, tourNumber);
                        pS.setBoolean(index++, workchain);
                        pS.addBatch();
                        batchSize++;
                    }
                }
                schemeID++;
                if (batchSize >= maxSize) {
                    pS.executeBatch();
                    batchSize = 0;
                }
            }
            pS.executeBatch();
            pw.flush();
            if (!print) {
                System.out.println("Inserted " + schemes + " schemes with " + diaries + " diaries.");
            }
        } catch (SQLException e) {
            System.err.println("Error in sqlstatement: " + tmpString);
            e.printStackTrace();
        }


    }

    public void printDistributionVectorSQLInserts(String tablename, String name, boolean print) {
        Map<Integer, Double> groupDistribution;
        Map<Integer, Double> groupSumCount = new HashMap<>();
        double count;
        double norm;
        //count the numbers for normalization
        for (Integer pgroup : this.personGroupDistribution.keySet()) {
            groupDistribution = this.personGroupDistribution.get(pgroup);
            count = 0;
            for (Integer dgroup : this.diaryGroups) {
                count += groupDistribution.get(dgroup);
            }
            groupSumCount.put(pgroup, count);
        }

        PrintWriter pw = new PrintWriter(System.out); //needed to get rid of stupid german localization of doubles!
        String query;
        //clean up
        if (!print) {
            query = "DELETE FROM " + tablename + " where name ='" + name + "' AND key = '" + tableKey + "';";
            this.dbCon.execute(query, this);
        }

        for (Integer pgroup : this.personGroupDistribution.keySet()) {
            groupDistribution = this.personGroupDistribution.get(pgroup);
            norm = 1.0 / groupSumCount.get(pgroup);
            for (Integer dgroup : this.diaryGroups) {
                query = String.format(Locale.ENGLISH,
                        "INSERT INTO %s (name, scheme_class_id, person_group, probability, key) VALUES ('%s',%d,%d,%f,'%s');",
                        tablename, name, dgroup, pgroup, groupDistribution.get(dgroup) * norm, tableKey);
                if (print) {
                    pw.printf(query + "\n");
                } else {
                    this.dbCon.execute(query, this);
                }
            }
        }

        pw.flush();
    }

    public void printDistributionVectors() {
        Map<Integer, Double> groupDistribution;
        Map<Integer, Integer> groupSumCount = new HashMap<>();
        System.out.println("PG\tDG\tNum\tProb");
        int count;
        double norm;
        //count the numbers for normalization
        for (Integer pgroup : this.personGroupDistribution.keySet()) {
            groupDistribution = this.personGroupDistribution.get(pgroup);
            count = 0;
            for (Integer dgroup : this.diaryGroups) {
                count += groupDistribution.get(dgroup);
            }
            groupSumCount.put(pgroup, count);
        }

        for (Integer pgroup : this.personGroupDistribution.keySet()) {
            groupDistribution = this.personGroupDistribution.get(pgroup);
            norm = 1.0 / groupSumCount.get(pgroup);
            for (Integer dgroup : this.diaryGroups) {
                System.out.println(pgroup + "\t" + dgroup + "\t" + groupDistribution.get(dgroup) + "\t" +
                        (groupDistribution.get(dgroup) * norm));
            }
        }
    }

    public void printSchemeClassSQLInserts(String table, boolean print) {
        Map<Integer, Integer> groupCounter = new HashMap<>();
        Map<Integer, Integer> groupSumTime = new HashMap<>();
        Map<Integer, Integer> groupSumWay = new HashMap<>();
        Map<Integer, Double> groupAvgTime = new HashMap<>();
        Map<Integer, Double> groupAvgWay = new HashMap<>();
        Map<Integer, Double> groupStdDeviation = new HashMap<>();
        Map<Integer, Double> groupWithinStdDeviation = new HashMap<>();
        int counter, group, time, ways;
        double avg, stdDev, within, sumtime, sumways;
        //set the keys and default values
        for (Integer i : this.diaryGroups) {
            groupCounter.put(i, 0);
            groupSumTime.put(i, 0);
            groupSumWay.put(i, 0);
            groupAvgTime.put(i, 0.0);
            groupAvgWay.put(i, 0.0);
            groupStdDeviation.put(i, 0.0);
            groupWithinStdDeviation.put(i, 0.0);
        }


        //calc sum of times
        for (Diary e : this.diaryMap.values()) {
            group = e.group;
            //fetch old values
            counter = groupCounter.get(group);
            time = groupSumTime.get(group);
            ways = groupSumWay.get(group);

            //calc new values
            counter++;
            time += e.totalTravelTime;
            for (DiaryElement d : e.activities) {
                if (!d.stay) ways++;
            }

            //put new values
            groupCounter.put(group, counter);
            groupSumTime.put(group, time);
            groupSumWay.put(group, ways);
        }

        //calc avg
        for (Integer i : this.diaryGroups) {
            counter = groupCounter.get(i);
            sumtime = counter > 0 ? (groupSumTime.get(i) / (double) counter) : 0.0;
            sumways = counter > 0 ? ((double) groupSumWay.get(i) / (double) counter) : 0.0;
            groupAvgTime.put(i, sumtime);
            groupAvgWay.put(i, sumways);
        }
        //calc standard deviation
        for (Diary e : this.diaryMap.values()) {
            group = e.group;
            if (groupStdDeviation.containsKey(group)) {
                stdDev = groupStdDeviation.get(group);
            } else {
                stdDev = 0;
            }
            //update value
            stdDev += (e.totalTravelTime - groupAvgTime.get(group)) * (e.totalTravelTime - groupAvgTime.get(group));
            groupStdDeviation.put(group, stdDev);
        }
        //normalize stdDev
        for (Integer i : this.diaryGroups) {
            counter = groupCounter.get(i) - 1;
            stdDev = counter > 0 ? (groupStdDeviation.get(i) / (double) counter) : 0;
            groupStdDeviation.put(i, stdDev);
        }

        //now count number of diaries within std deviation
        for (Diary e : this.diaryMap.values()) {
            group = e.group;
            stdDev = groupStdDeviation.get(group);
            if (groupWithinStdDeviation.containsKey(group)) {
                within = groupWithinStdDeviation.get(group);
            } else {
                within = 0;
            }

            //update value
            if ((e.totalTravelTime - groupAvgTime.get(group)) * (e.totalTravelTime - groupAvgTime.get(group)) <
                    stdDev) {
                within += 1;
            }
            groupWithinStdDeviation.put(group, within);
        }
        //normalize
        for (Integer i : this.diaryGroups) {
            counter = groupCounter.get(i);
            within = groupWithinStdDeviation.get(i);
            stdDev = counter > 0 ? (within / (double) counter) : 0;
            groupWithinStdDeviation.put(i, stdDev);
        }

        // puh, now print the whole stuff

        if (print) {
            PrintWriter pw = new PrintWriter(System.out); //needed to get rid of stupid german localization of doubles!


            for (Integer i : this.diaryGroups) {
                avg = groupAvgTime.get(i);
                within = groupWithinStdDeviation.get(i);
                //double ways = groupAvgWay.get(i);
                //pw.printf(Locale.ENGLISH,"%d;%f;%f;%f\n",i,avg,within, ways);
                pw.printf(Locale.ENGLISH,
                        "INSERT INTO %s (scheme_class_id, avg_travel_time, proz_std_dev, key) VALUES (%d,%f,%f,'%s');\n",
                        table, i, avg, within, tableKey);

            }
            pw.flush();
        } else {
            //there are only a few insert, so we do it directly without prepared Statements
            String query;
            for (Integer i : this.diaryGroups) {
                avg = groupAvgTime.get(i);
                within = groupWithinStdDeviation.get(i);
                query = String.format(Locale.ENGLISH,
                        "INSERT INTO %s (scheme_class_id, avg_travel_time, proz_std_dev, key) VALUES (%d,%f,%f,'%s');",
                        table, i, avg, within, tableKey);
                this.dbCon.execute(query, this);

            }
        }
    }

    public void printStatistics() {
        Map<Integer, Integer> groupStatistics;
        Map<Integer, Integer> groupSumStatistics;
        int sumTime, sumActivities, numAct, numDuration;
        int avgDuration;
        System.out.println("Group\tActivity\tNum\tAvg Duration");
        for (Integer group : this.diarySumStatistics.keySet()) {
            sumTime = 0;
            sumActivities = 0;
            groupStatistics = this.diaryStatistics.get(group);
            groupSumStatistics = this.diarySumStatistics.get(group);

            for (Integer act : this.activities) {
                numAct = groupStatistics.get(act);
                numDuration = groupSumStatistics.get(act);
                if (numAct > 0) {
                    avgDuration = numDuration / numAct;
                } else {
                    avgDuration = 0;
                }

                System.out.println(group + "\t" + act + "\t" + numAct + "\t" + avgDuration);
                sumTime += numDuration;
                sumActivities += numAct;
            }
            System.out.println(group + "\t0\t" + sumActivities + "\t" + (sumTime / sumActivities));
        }
    }

    public static String makeDiaryKey(long hhID, int pID, int erhebung) {
        return hhID + "-" + pID + "-" + erhebung;
    }

    public void readMIDDiary(String table, String filter) {
        String query = "";
        Diary actualDiary, lastDiary = null;
        DiaryElement lastActivity = null;
        int doubleReturnDiaries = 0;

        try {
            query = "select \"Erhebung\", h_id, p_id, w_id, hp_taet, hwzweck2 as w_zweck, w_szs, w_szm, w_azs, w_azm, " +
                    "w_folgetag, wegmin, berlin_weight as w_hoch, " + pgTapasCol + ", " + diaryGroupCol + " from " + table +
                    " where useable_diary=true and w_szs != 99 and w_szs != 701 and w_azs != 99 and w_azs != 701 and " +
                    " w_szm != 99 and w_szm != 701 and w_azm != 99 and w_azm != 701 and" + " \"Erhebung\" = " +
                    ERHEBUNG + " and wegmin != 9994 and " + pgXColFilter + " and " +
                    filter + " order by h_id, p_id, w_id";
            ResultSet rs = this.dbCon.executeQuery(query, this);
            String key, lastKey = "";
            long hhID;
            int pID, start, end, purpose, group, pGroup, personStatus, erhebung;
            double weight;
            boolean clean = true, home, addTripElement;

            while (rs.next()) {
                hhID = rs.getLong("h_id");
                pID = rs.getInt("p_id");
                start = rs.getInt("w_szs") * 60 + rs.getInt("w_szm");
                end = rs.getInt("w_folgetag") * ONE_DAY + rs.getInt("w_azs") * 60 + rs.getInt("w_azm");
                purpose = rs.getInt("w_zweck");
                group = rs.getInt(diaryGroupCol);
                pGroup = rs.getInt(pgTapasCol);
                weight = rs.getDouble("w_hoch");
                personStatus = rs.getInt("hp_taet");
                home = purpose == 8;
                erhebung = rs.getInt("Erhebung");
                key = makeDiaryKey(hhID, pID, erhebung);
                if (key.equals(lastKey)) { //same diary?
                    actualDiary = this.diaryMap.get(key);
                } else {
                    if (clean) {
                        //finish the old one
                        if (lastDiary != null) {
                            lastDiary.finishDiary();
                        }
                    } else {
                        this.diaryMap.remove(lastKey);
                    }
                    clean = true;
                    actualDiary = new Diary(hhID, pID, group, pGroup, personStatus, weight);
                    this.diaryMap.put(key, actualDiary);
                    lastActivity = null;
                }
                addTripElement = true; //default value, only changed if start or end time equal to the prev trip
                if (actualDiary.activities.size() == 1 && (purpose == 8 || purpose == 9)) {
                    numOfDiariesStartingWithATrip++;
                } else if (lastActivity != null && lastActivity.home && purpose == 8) { // two consecutive "trips home"
//                    System.err.println("Diary " + hhID + " pid " + pID + " has two consecutive trips home");
//                    doubleReturnDiaries++;
                    if (lastActivity.start_min == start) { //take the longer trip
                        lastActivity.end_min = Math.max(lastActivity.end_min, end);
                        addTripElement = false; //don't add the current trip element
                    } else if (lastActivity.end_min == end) {// take the longer trip
                        lastActivity.start_min = Math.max(lastActivity.start_min, start);
                        addTripElement = false; //don't add the current trip element
                    } else {
                        lastActivity.purpose = 10; //change prev trip purpose to "anderer Zweck"
                        if (lastActivity.end_min >= start) {//merge the trips and take the first one
                            if (lastActivity.end_min - lastActivity.start_min >=
                                    end - start) {//which trip is the longer one?
                                lastActivity.end_min = start - 5; //reduce the first/prev trip duration
                            } else {
                                start = lastActivity.end_min + 5; //reduce the second/current trip duration
                            }
                        }
                    }
                }
                if (addTripElement) {
                    if (start != end) {
                        clean &= actualDiary.addNextElement(start, end, purpose, home);
                        int diaryIndex = actualDiary.activities.size() - 1;
                        lastActivity = actualDiary.activities.get(diaryIndex);
                    }
                }


                lastDiary = actualDiary;
                activities.add(purpose); //collect all possible activities
                diaryGroups.add(actualDiary.group);
                lastKey = key;
            }
        } catch (SQLException e) {
            System.err.println("Error in SQL statement: " + query);
            e.printStackTrace();
        }
        if (lastDiary != null) {
            lastDiary.finishDiary();
        }

        //check all diaries:
        List<String> keysToRemove = new LinkedList<>();
        for (Entry<String, Diary> e : this.diaryMap.entrySet()) {
            if (!e.getValue().checkDiary()) {
                System.out.println("Diary " + e.getKey() + " is not correct!");
                e.getValue().printDiary();
                keysToRemove.add(e.getKey());
            }
        }
        //remove bad diaries
        for (String k : keysToRemove) {
            this.diaryMap.remove(k);
        }
        //now scan all diaries for "begleitwege" and find their counterparts
        if (doubleReturnDiaries > 0) System.err.println("Number of two consecutive trips home: " + doubleReturnDiaries);
    }


    class DiaryElement implements Comparable<DiaryElement> {
        int wsID;
        int tourNumber = 0;
        int start_min;
        int end_min;
        int purpose;
        boolean stay;
        boolean home;
        boolean workchain = false; //trip with purpose work

        @Override
        public int compareTo(DiaryElement arg0) {
            return arg0.start_min - this.start_min;
        }

        public int getDuration() {
            return end_min - start_min;
        }

        public void printElement() {
            System.out.println(
                    "\tWay ID: " + this.wsID + " start: " + this.start_min + " end: " + this.end_min + " purpose: " +
                            this.purpose + " stay: " + this.stay + " home: " + this.home);

        }
    }

    class Diary {
        int schemeID = 0;
        long hhID;
        int pID;
        int group;
        int pGroup;
        int personStatus;
        int totalTravelTime = 0;
        boolean reported;
        double weight;
        List<DiaryElement> activities = new ArrayList<>();

        public Diary(long hhID, int pID, int group, int pGroup, int personStatus, double weight) {
            this.hhID = hhID;
            this.pID = pID;
            this.group = group;
            this.pGroup = pGroup;
            this.personStatus = personStatus;
            this.reported = false;
            this.weight = weight;
            DiaryElement startElem = new DiaryElement();
            startElem.wsID = -1;
            startElem.start_min = 0;
            startElem.purpose = 0;
            startElem.home = true;
            startElem.stay = true;
            this.addDiaryElement(startElem);
        }

        public void addDiaryElement(DiaryElement e) {
            e.wsID = this.activities.size(); //just the index, where it will be inserted
            this.activities.add(e);
        }

        public boolean addNextElement(int start, int end, int purpose, boolean home) {
            if (start == end) //no duration?!?
                return false;
            DiaryElement pre = this.activities.get(this.activities.size() - 1);
            DiaryElement stay;
            if (pre.stay) {
                stay = pre;
            } else { //if the previous DiaryElement pre was a trip -> create a stay between the pre and current trip
                stay = new DiaryElement();
                stay.start_min = pre.end_min;
                stay.purpose = pre.home ? 0 : pre.purpose;
                stay.home = pre.home;
                stay.stay = true;
                //check for funny double entries!
                if (pre.start_min == start && pre.end_min == end) {
                    numOfDoubleWays++;
                    if (pre.purpose == purpose) {
                        numOfExactDoubleWays++;
                        return true;
                    }
                }
            }
            stay.end_min = start;
            DiaryElement trip = new DiaryElement();
            if (purpose == 9) { // rückweg vom vorherigen weg
                if (this.activities.size() >= 2) { // look if the previous stay was at home
                    DiaryElement preStay = this.activities.get(this.activities.size() - 2);
                    trip.home = preStay.home;
                    if (trip.home) {
                        purpose = 8;
                    } else { //a trip back has the same purpose!
                        purpose = preStay.purpose;
                    }
                } else {
                    trip.home = false; // no trip back?!?!
                }
            } else {
                trip.home = home;
            }
            trip.purpose = purpose;
            trip.start_min = start;
            trip.end_min = end;
            trip.stay = false;
            //now something strange: we have trips "Return to home" starting and ending at home.
            // I assume these are round trips (walks etc..)
            if (stay.home && purpose == 8) {
                //is the trip long enough to split? (3min= 1min trip 1min stay 1min back)
                int middle = start + (end - start) / 2;
                boolean splitIt = true;
                if (start < middle && middle + 1 < end && splitIt) {
                    if (!stay.equals(pre)) this.addDiaryElement(stay);
                    //split the trip!
                    //way to target
                    DiaryElement inBetweenTrip = new DiaryElement();
                    inBetweenTrip.start_min = start;
                    inBetweenTrip.end_min = middle;
                    inBetweenTrip.stay = false;
                    inBetweenTrip.home = false;
                    inBetweenTrip.purpose = purpose;
                    this.addDiaryElement(inBetweenTrip);
                    //target
                    DiaryElement inBetweenStay = new DiaryElement();
                    inBetweenStay.start_min = middle;
                    inBetweenStay.end_min = middle + 1;
                    inBetweenStay.stay = true;
                    inBetweenStay.home = false;
                    inBetweenStay.purpose = purpose;
                    this.addDiaryElement(inBetweenStay);

                    //way back home
                    trip.start_min = middle + 1;
                    this.addDiaryElement(trip);

                    this.totalTravelTime += end - start - 1;
                }

            } else {
                //if (purpose != 8 && purpose != 9) { // 8/9 sind rückwege!
                if (!stay.equals(pre)) this.addDiaryElement(stay);
                //}
                this.addDiaryElement(trip);
                this.totalTravelTime += end - start;
            }
            return true;
        }

        public boolean checkDiary() {
            //the diary is ok if stay and trip are toggling every time
            //this can be done by checking the indices:
            //   all even indices must be stays and all odd ones trips...
            if (this.activities.size() % 2 == 0) return false; //we have to end with an stay! so the number must be odd!
            for (int i = 0; i < this.activities.size(); i++) {
                if (((i % 2) == 0 && !this.activities.get(i).stay) || //even and not a stay: bad!
                        ((i % 2) == 1 && this.activities.get(i).stay) // odd and not a trip: bad!
                ) {
                    return false;
                }
                if (this.activities.get(i).getDuration() <= 0) {
                    if (this.activities.get(i).start_min == 0 && this.activities.get(i).end_min == 0)
                        continue;//starts at midnight are ok!
                    System.out.println("Error in diary!");
                    this.activities.get(i).printElement();
                    return false;
                }
            }
            //now check if the first and last stays are home
            return this.activities.size() > 0 && this.activities.get(0).home && this.activities.get(
                    this.activities.size() - 1).home;
        }

        /**
         * Method to close a Diary with a stay at home.
         */
        public void finishDiary() {
            DiaryElement stay;
            if (this.activities.size() == 1) {
                stay = this.activities.get(0);
                stay.start_min = 0;
                stay.end_min = ONE_DAY;
                stay.purpose = 0;
                stay.home = true;
                stay.stay = true;
                //this.addDiaryElement(stay);
            } else {
                //check if the last trip was going home!
                DiaryElement pre = this.activities.get(this.activities.size() - 1);
                if (!pre.home) {// && !pre.isRoundTrip()){
                    numOfDiariesNotEndingAtHome++;
//					if(purposeNonHomeEndTrip.get(pre.purpose)==null){
//						purposeNonHomeEndTrip.put(pre.purpose,1);
//					}
//					else{
//						purposeNonHomeEndTrip.put(pre.purpose,purposeNonHomeEndTrip.get(pre.purpose)+1);
//					}
                    //System.out.println("Purp: "+pre.purpose+"/"+pre.purposeDetailed+" Dest: "+pre.tripDestination);
                    //oops fit it: Insert a stay (5min) and a trip back home
                    DiaryElement auxStay = new DiaryElement();
                    auxStay.start_min = pre.end_min;
                    auxStay.end_min = auxStay.start_min + 5;
                    auxStay.purpose = pre.purpose;
                    auxStay.home = false;
                    auxStay.stay = true;
                    this.addDiaryElement(auxStay);
                    // now insert a trip home.
                    //Only problem: how long should it be?
                    //Assumption: look back for the last trip from home and take this time!
                    pre = new DiaryElement(); //this will be the preceding trip for the rest of the code!
                    pre.start_min = auxStay.end_min;
                    //now look for the duration of the last trip from home
                    int duration = 5; //safety value
                    for (int i = this.activities.size() - 2; i >= 0; --i) {
                        DiaryElement lastStayHome = this.activities.get(i);
                        if (lastStayHome.home && lastStayHome.stay) {
                            duration = this.activities.get(i + 1).getDuration();
                            break;
                        }
                    }


                    pre.end_min = pre.start_min + duration;
                    pre.purpose = 8;
                    pre.home = true;
                    pre.stay = false;
                    this.addDiaryElement(pre);

                }

                stay = new DiaryElement();

                stay.start_min = pre.end_min;
                stay.end_min = Math.max(ONE_DAY, pre.end_min + 1);
                stay.purpose = pre.home ? 0 : pre.purpose;
                stay.home = true;
                stay.stay = true;
                this.addDiaryElement(stay);

                //Collections.sort(this.activities);

                //now update the tournumbers
                pre = this.activities.get(0);
                int actTourNumber;
                DiaryElement act;
                List<Integer> toursWithWork = new LinkedList<>();
                //first home stays are always tour 0 trips are 1...
                actTourNumber = 1; // start with the first tour
                for (int i = 1; i < this.activities.size(); ++i) {
                    act = this.activities.get(i);
                    if (act.stay) { //a stay allways gets the tournumber of its last trip
                        if (act.home) act.tourNumber = 0;
                        else act.tourNumber = pre.tourNumber;
                    } else {
                        act.tourNumber = actTourNumber; //update the tour number
                    }

                    if (act.purpose == 1 || act.purpose == 2) {
                        toursWithWork.add(actTourNumber);
                    }
                    if (!act.stay && act.home) // a tour ending at home: all succeding trips are a new tour
                    {
                        actTourNumber++;
                    }
                    pre = act;
                }
                //now fix all trips within a workchain
                if (toursWithWork.size() > 0) {
                    for (int i = 1; i < this.activities.size() - 1; ++i) {
                        act = this.activities.get(i);
                        if (!(act.home && act.stay) && toursWithWork.contains(act.tourNumber)) {
                            act.workchain = true;
                        }
                    }
                }

                //now fix 0-min stays:
                this.totalTravelTime = 0;
                int addTime;
                for (int i = 1; i < this.activities.size(); ++i) {
                    act = this.activities.get(i);
                    pre = this.activities.get(i - 1);
                    //fix starts at midnight
                    if (act.start_min == 0) {
                        pre.end_min = act.start_min = 1;
                    }
                    addTime = 1 - act.getDuration();
                    if (act.getDuration() <= 0) {
                        act.end_min += addTime;
                        for (int j = i + 1; j < this.activities.size(); ++j) {
                            this.activities.get(j).start_min += addTime;
                            this.activities.get(j).end_min += addTime;
                        }
                    }
                    if (!act.stay) {
                        this.totalTravelTime += act.getDuration();
                    }
                }
                for (int i = 1; i < this.activities.size(); ++i) {
                    act = this.activities.get(i);
                    pre = this.activities.get(i - 1);
                    if (act.start_min == pre.start_min) System.out.println("argh!");
                }

            }


        }

        public DiaryElement getClosestTrip(int startTime) {
            DiaryElement returnVal = this.activities.get(0);

            for (DiaryElement e : this.activities) {
                //is this trip closer than the actual best hit?
                if (Math.abs(startTime - e.start_min) < Math.abs(startTime - returnVal.start_min) && !e.stay) {
                    returnVal = e;
                }
            }

            if (returnVal.stay) { //found something?
                returnVal = null; //no: set to null
            }

            return returnVal;
        }

        public void printDiary() {
            System.out.println(
                    "Diary household: " + this.hhID + " Person: " + this.pID + " Person group: " + this.pGroup +
                            " Status: " + this.personStatus + " Diary group: " + this.group);
            for (DiaryElement act : this.activities)
                act.printElement();
        }
    }
}
