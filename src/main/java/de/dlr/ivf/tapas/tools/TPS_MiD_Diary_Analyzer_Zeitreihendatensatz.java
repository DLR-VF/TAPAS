/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.tools;

import de.dlr.ivf.tapas.tools.persitence.db.TPS_BasicConnectionClass;
import org.apache.commons.lang3.tuple.ImmutableTriple;

import java.io.PrintWriter;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.Map.Entry;

public class TPS_MiD_Diary_Analyzer_Zeitreihendatensatz extends TPS_BasicConnectionClass {

    static final int ONE_DAY = 24 * 60;
    static final int ANY_DIARY_GROUP = -1;
    static final int ANY_TRIP_PURPOSE_DETAIL = -1;
    static final int ERHEBUNG = 1;
    //    static final String groupCol = "TBG_23";
//    static final String groupCol_name = "TBG_23";
    static final String diaryGroupCol = "tbg";
    static final String diaryGroupColName = "tbg";
    static final String pgTapasCol = "pg_tapas33";
    static final String tableKey = "MID17_33";

    //static final int kindergartengruppe = 91;

    Map<String, Diary> diaryMap = new HashMap<>();
    Map<Integer, Integer> accompanyStat = new HashMap<>();
    Map<Integer, Integer> UncodedAccompanyStat = new HashMap<>();
    Map<Integer, Integer> purposeNonHomeEndTrip = new HashMap<>();
    Map<Integer, Integer> purposeNonHomeStartTrip = new HashMap<>();
    Map<Integer, Map<Integer, Integer>> diaryStatistics = new HashMap<>();
    Map<Integer, Map<Integer, Integer>> diarySumStatistics = new HashMap<>();
    Map<Integer, Map<Integer, Integer>> personGroupDistribution = new HashMap<>();
    Set<Integer> activities = new TreeSet<>();
    Set<Integer> diaryGroups = new TreeSet<>();

    int recoded = 0;
    int notRecoded = 0;
    int numOfDoubleWays = 0;
    int numOfExactDoubleWays = 0;
    int numOfDiariesStartingWithATrip = 0;
    int numOfDiariesNotEndingAtHome = 0;

    public static void main(String[] args) {
        TPS_MiD_Diary_Analyzer_Zeitreihendatensatz worker = new TPS_MiD_Diary_Analyzer_Zeitreihendatensatz();
        HashMap<String, String> times = new HashMap<>();
		times.put("true", "_Mo-So");
        times.put("st_wotag = ANY(ARRAY[1,2,3,4,5])","_Mo-Fr");
        times.put("st_wotag = ANY(ARRAY[2,3,4])", "_Di-Do");
        times.put("st_wotag = ANY(ARRAY[6,7])","_Sa-So");
        times.put("st_wotag = ANY(ARRAY[5, 6,7])","_Fr-So");
		times.put("st_wotag = 1","_Mo");
		times.put("st_wotag = 2","_Di");
		times.put("st_wotag = 3","_Mi");
		times.put("st_wotag = 4","_Do");
		times.put("st_wotag = 5","_Fr");
		times.put("st_wotag = 6","_Sa");
		times.put("st_wotag = 7","_So");
        HashMap<String, String> regions = new HashMap<>();

		regions.put("true", "");
//		regions.put("rtyp = 1", "_RTYP1");
//		regions.put("rtyp = 2", "_RTYP2");
//		regions.put("rtyp = 3", "_RTYP3");
		regions.put("polgk =1", "_PolGK1");
		regions.put("polgk =2", "_PolGK2");
		regions.put("polgk =3", "_PolGK3");
		regions.put("polgk =4", "_PolGK4");
		regions.put("polgk =5", "_PolGK5");
        regions.put("polgk =6", "_PolGK6");


        for (Entry<String, String> t : times.entrySet()) {
            for (Entry<String, String> r : regions.entrySet()) {
                System.out.println(pgTapasCol);
                System.out.println(t);
                System.out.println(r);
                worker.readMIDDiary("public.MiD2002-2008-2017_Wege", t.getKey() + " and " + r.getKey());
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
                worker.printSchemeClassSQLInserts("core.global_scheme_classes", printOnScreen);
                worker.printDiariesSQLInserts("core.global_episodes", "core.global_schemes",
                printOnScreen);
//                worker.printDistributionVectors();
                worker.printDistributionVectorSQLInserts("core.global_scheme_class_distributions",
                        "MID_2017_" + diaryGroupColName + t.getValue() + r.getValue(), printOnScreen);
                worker.clearEverything();
            }
        }
    }


    public void calcDiaryStatistics() {
        Map<Integer, Integer> groupStatistics;
        Map<Integer, Integer> groupSumStatistics;
        Map<Integer, Integer> groupDistribution;
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
                    groupDistribution.put(i, 0);
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
            countGroup = groupDistribution.get(e.group);
            groupDistribution.put(e.group, countGroup + 1);
        }
    }

    public void cleanUpDB(String table) {
        String query = "DELETE FROM " + table + "WHERE key = '" + tableKey + "';";
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
        //triple consists of trip purpose/trip purpose detail/diary group
        Map<ImmutableTriple<Integer, Integer, Integer>, Integer> activityMapping = new HashMap<>();
        //build the code mapping
        activityMapping.put(new ImmutableTriple<>(0, 0, ANY_DIARY_GROUP), 10); //stay at home

        activityMapping.put(new ImmutableTriple<>(1, ANY_TRIP_PURPOSE_DETAIL, ANY_DIARY_GROUP), 211); //unspecified workers->WORKING
        activityMapping.put(new ImmutableTriple<>(1, ANY_TRIP_PURPOSE_DETAIL, 1), 212); //full time workers->WORKING FULL TIME
        activityMapping.put(new ImmutableTriple<>(1, ANY_TRIP_PURPOSE_DETAIL, 2), 212); //full time workers->WORKING FULL TIME
        activityMapping.put(new ImmutableTriple<>(1, ANY_TRIP_PURPOSE_DETAIL, 3), 212); //full time workers->WORKING FULL TIME
        activityMapping.put(new ImmutableTriple<>(1, ANY_TRIP_PURPOSE_DETAIL, 4), 212); //full time workers->WORKING FULL TIME
        activityMapping.put(new ImmutableTriple<>(1, ANY_TRIP_PURPOSE_DETAIL, 5), 212); //full time workers->WORKING FULL TIME

        activityMapping.put(new ImmutableTriple<>(1, ANY_TRIP_PURPOSE_DETAIL, 6), 213); //part time workers->WORKING PART TIME
        activityMapping.put(new ImmutableTriple<>(1, ANY_TRIP_PURPOSE_DETAIL, 7), 213); //part time workers->WORKING PART TIME
        activityMapping.put(new ImmutableTriple<>(1, ANY_TRIP_PURPOSE_DETAIL, 8), 213); //part time workers->WORKING PART TIME
        activityMapping.put(new ImmutableTriple<>(1, ANY_TRIP_PURPOSE_DETAIL, 9), 213); //part time workers->WORKING PART TIME
        activityMapping.put(new ImmutableTriple<>(1, ANY_TRIP_PURPOSE_DETAIL, 10), 213); //part time workers->WORKING PART TIME


        activityMapping.put(new ImmutableTriple<>(2, ANY_TRIP_PURPOSE_DETAIL, ANY_DIARY_GROUP), 211); //Business trip->WORKING

        activityMapping.put(new ImmutableTriple<>(3, ANY_TRIP_PURPOSE_DETAIL, ANY_DIARY_GROUP), 413); //Bildungsweg->Kindergarden TODO WHY? Kindergarden
        activityMapping.put(new ImmutableTriple<>(3, ANY_TRIP_PURPOSE_DETAIL, 1), 412); //Vollzeit berufstätig->SCHOOL TRAINEE
        activityMapping.put(new ImmutableTriple<>(3, ANY_TRIP_PURPOSE_DETAIL, 2), 412); //Vollzeit berufstätig->SCHOOL TRAINEE
        activityMapping.put(new ImmutableTriple<>(3, ANY_TRIP_PURPOSE_DETAIL, 3), 412); //Vollzeit berufstätig->SCHOOL TRAINEE
        activityMapping.put(new ImmutableTriple<>(3, ANY_TRIP_PURPOSE_DETAIL, 4), 412); //Vollzeit berufstätig->SCHOOL TRAINEE
        activityMapping.put(new ImmutableTriple<>(3, ANY_TRIP_PURPOSE_DETAIL, 5), 412); //Vollzeit berufstätig->SCHOOL TRAINEE
        activityMapping.put(new ImmutableTriple<>(3, ANY_TRIP_PURPOSE_DETAIL, 6), 412); //Teilzeit berufstätig->SCHOOL TRAINEE
        activityMapping.put(new ImmutableTriple<>(3, ANY_TRIP_PURPOSE_DETAIL, 7), 412); //Teilzeit berufstätig->SCHOOL TRAINEE
        activityMapping.put(new ImmutableTriple<>(3, ANY_TRIP_PURPOSE_DETAIL, 8), 412); //Teilzeit berufstätig->SCHOOL TRAINEE
        activityMapping.put(new ImmutableTriple<>(3, ANY_TRIP_PURPOSE_DETAIL, 9), 412); //Teilzeit berufstätig->SCHOOL TRAINEE
        activityMapping.put(new ImmutableTriple<>(3, ANY_TRIP_PURPOSE_DETAIL, 10), 412); //Teilzeit berufstätig->SCHOOL TRAINEE

        activityMapping.put(new ImmutableTriple<>(3, ANY_TRIP_PURPOSE_DETAIL, 24), 412); //Azubi->SCHOOL TRAINEE
        activityMapping.put(new ImmutableTriple<>(3, ANY_TRIP_PURPOSE_DETAIL, 12), 410); //Pupil ->SCHOOL
        activityMapping.put(new ImmutableTriple<>(3, ANY_TRIP_PURPOSE_DETAIL, 11), 411); //Bildungsweg, Student->University

        activityMapping.put(new ImmutableTriple<>(4, 501, ANY_DIARY_GROUP), 51);  //Shopping short term: Täglicher Bedarf->SHOPPING SHORT TERM
        activityMapping.put(new ImmutableTriple<>(4, 502, ANY_DIARY_GROUP), 53);  //Shopping long term: sonstige Ware (Kleidung, Möbel, Hausrat)->SHOPPING LONG TERM
        activityMapping.put(new ImmutableTriple<>(4, 503, ANY_DIARY_GROUP), 50);  //Shopping Allgemeiner Einkaufsbummel->SHOPPING
        activityMapping.put(new ImmutableTriple<>(4, 504, ANY_DIARY_GROUP), 52);  //Shopping mid term: Dienstleistung (Friseur Schuster)->SHOPPING MID TERM
        activityMapping.put(new ImmutableTriple<>(4, 505, ANY_DIARY_GROUP), 50);  //Shopping sonstiger Einkaufszweck->SHOPPING
        activityMapping.put(new ImmutableTriple<>(4, 599, ANY_DIARY_GROUP), 50);  //Shopping Einkäufe ohne Angabe zum Detail->SHOPPING
        activityMapping.put(new ImmutableTriple<>(4, 2202, ANY_DIARY_GROUP), 50);  //Shopping keine Angabe->SHOPPING

        activityMapping.put(new ImmutableTriple<>(5, 503, ANY_DIARY_GROUP), 50);  //private Erledigung: Allgemeiner Einkaufsbummel->SHOPPING
        activityMapping.put(new ImmutableTriple<>(5, 504, ANY_DIARY_GROUP), 52);  //private Erledigung: Dienstleistung (Friseur Schuster)-> SHOPPING MID TERM
        activityMapping.put(new ImmutableTriple<>(5, 601, ANY_DIARY_GROUP), 522); //private Erledigung: Arzt->PERSONAL_MATTERS
        activityMapping.put(new ImmutableTriple<>(5, 602, ANY_DIARY_GROUP), 522); //private Erledigung: Amt, Behörde, Post, Bank, Geldautomat->PERSONAL_MATTERS
        activityMapping.put(new ImmutableTriple<>(5, 603, ANY_DIARY_GROUP), 522); //private Erledigung: für andere Person->PERSONAL_MATTERS
        activityMapping.put(new ImmutableTriple<>(5, 604, ANY_DIARY_GROUP), 522); //private Erledigung: sonstiges->PERSONAL_MATTERS
        activityMapping.put(new ImmutableTriple<>(5, 605, ANY_DIARY_GROUP), 522); //private Erledigung: Betreuung->PERSONAL_MATTERS
        activityMapping.put(new ImmutableTriple<>(5, 699, ANY_DIARY_GROUP), 522); //private Erledigung: keine Angabe des Details->PERSONAL_MATTERS
        activityMapping.put(new ImmutableTriple<>(5, 701, ANY_DIARY_GROUP), 631); //private Erledigung: Besuch mit/bei Freunden->VISITING
        activityMapping.put(new ImmutableTriple<>(5, 705, ANY_DIARY_GROUP), 414); //private Erledigung: Weiterbildung ->Weitebrildung
        activityMapping.put(new ImmutableTriple<>(5, 706, ANY_DIARY_GROUP), 720); //private Erledigung: Restaurant->DINING_OR_GOING_OUT
        activityMapping.put(new ImmutableTriple<>(5, 711, ANY_DIARY_GROUP), 722); //private Erledigung: Hund ausführen->PROMENADING
        activityMapping.put(new ImmutableTriple<>(5, 713, ANY_DIARY_GROUP), 522); //private Erledigung: Kirche, Friedhof->PERSONAL_MATTERS
        activityMapping.put(new ImmutableTriple<>(5, 714, ANY_DIARY_GROUP), 300); //private Erledigung: Ehrenamt, Verein, pol. Aufgaben->VOLUNTARY_WORK
        activityMapping.put(new ImmutableTriple<>(5, 715, ANY_DIARY_GROUP), 213); //private Erledigung: Jobben in der Freizeit->WORKING PART TIME
        activityMapping.put(new ImmutableTriple<>(5, 716, ANY_DIARY_GROUP), 881); //private Erledigung: Begleitung Kinder Spielplatz->TRANSPORTING_CHILDREN_LOCATION
        activityMapping.put(new ImmutableTriple<>(5, 717, ANY_DIARY_GROUP), 522); //private Erledigung: Hobby->PERSONAL_MATTERS
        activityMapping.put(new ImmutableTriple<>(5, 2202, ANY_DIARY_GROUP), 522); //private Erledigung: im PAPI nicht->PERSONAL_MATTERS erhoben->PERSONAL_MATTERS Freizeitweg?!?!?

        //TODO keine Aktivität fürs Bringen von allgemeinen Personen? (es gibt Bringen von Kindern in der DB aber
        // nicht in MiD)
        activityMapping.put(new ImmutableTriple<>(6, 2202, ANY_DIARY_GROUP), 740); //Bringen+Holen: keine Angabe ->FREETIME_ANY
        activityMapping.put(new ImmutableTriple<>(6, 7704, ANY_DIARY_GROUP), 799); //Bringen+Holen: kein Einkaufs-, Erledigungs- und Freizeitweg?!?!?->ACTIVITIES_ANY

        activityMapping.put(new ImmutableTriple<>(7, 503, ANY_DIARY_GROUP), 50); //Freizeit: allg. Einkaufsbummel->SHOPPING
        activityMapping.put(new ImmutableTriple<>(7, 603, ANY_DIARY_GROUP), 522); //Freizeit: priv. Erledigung für andere Person->PERSONAL_MATTERS
        activityMapping.put(new ImmutableTriple<>(7, 605, ANY_DIARY_GROUP), 740); //Freizeit: Betreuung Familienmitglieder Bekannter->FREETIME_ANY
        activityMapping.put(new ImmutableTriple<>(7, 701, ANY_DIARY_GROUP), 631); //Freizeit: Besuch/Treffen von Freunden->VISITING
        activityMapping.put(new ImmutableTriple<>(7, 702, ANY_DIARY_GROUP), 640); //Freizeit: Besuch Kulturelle Einrichtung->EXCURSIONS
        activityMapping.put(new ImmutableTriple<>(7, 703, ANY_DIARY_GROUP), 724); //Freizeit: Besuch einer Veranstaltung->BEING_AT_AN_EVENT
        activityMapping.put(new ImmutableTriple<>(7, 704, ANY_DIARY_GROUP), 721); //Freizeit: Sport, Sportverein->SPORTS
        activityMapping.put(new ImmutableTriple<>(7, 705, ANY_DIARY_GROUP), 414); //Freizeit: Weiterbildung->Weiterbildung
        activityMapping.put(new ImmutableTriple<>(7, 706, ANY_DIARY_GROUP), 720); //Freizeit: Restaurant->DINING_OR_GOING_OUT
        activityMapping.put(new ImmutableTriple<>(7, 707, ANY_DIARY_GROUP), 740); //Freizeit: Schrebergarten/Wochenendhaus->FREETIME_ANY
        activityMapping.put(new ImmutableTriple<>(7, 708, ANY_DIARY_GROUP), 640); //Freizeit: Tagesausflug->EXCURSIONS
        activityMapping.put(new ImmutableTriple<>(7, 709, ANY_DIARY_GROUP), 640); //Freizeit: Urlaub->EXCURSIONS
        activityMapping.put(new ImmutableTriple<>(7, 710, ANY_DIARY_GROUP), 722); //Freizeit: Spaziergang->PROMENADING
        activityMapping.put(new ImmutableTriple<>(7, 711, ANY_DIARY_GROUP), 722); //Freizeit: Hund ausführen->PROMENADING
        activityMapping.put(new ImmutableTriple<>(7, 712, ANY_DIARY_GROUP), 721); //Freizeit: Joggen...->SPORTS
        activityMapping.put(new ImmutableTriple<>(7, 713, ANY_DIARY_GROUP), 522); //Freizeit: Kirche und Friedhof->PERSONAL_MATTERS
        activityMapping.put(new ImmutableTriple<>(7, 714, ANY_DIARY_GROUP), 300); //Freizeit: Ehrenamt, Verein, pol. Aufgaben->VOLUNTARY_WORK
        activityMapping.put(new ImmutableTriple<>(7, 715, ANY_DIARY_GROUP), 213); //Freizeit: Jobben i.d. Freizeit->WORKING PART TIME
        activityMapping.put(new ImmutableTriple<>(7, 716, ANY_DIARY_GROUP), 881); //Freizeit: Begleitung von Kindern (Spielplatz)->TRANSPORTING_CHILDREN_LOCATION
        activityMapping.put(new ImmutableTriple<>(7, 717, ANY_DIARY_GROUP), 740); //Freizeit: Hobby->FREETIME_ANY
        activityMapping.put(new ImmutableTriple<>(7, 719, ANY_DIARY_GROUP), 723); //Freizeit: Spielen (Spielplatz)->PLAYING
        activityMapping.put(new ImmutableTriple<>(7, 720, ANY_DIARY_GROUP), 740); //Freizeit: Sonstiges->FREETIME_ANY
        activityMapping.put(new ImmutableTriple<>(7, 799, ANY_DIARY_GROUP), 740); //Freizeit: keine Angabe zum Detail->FREETIME_ANY
        activityMapping.put(new ImmutableTriple<>(7, 2202, ANY_DIARY_GROUP), 740); //Freizeit: keine Angabe->FREETIME_ANY

        activityMapping.put(new ImmutableTriple<>(8, 2202, ANY_DIARY_GROUP), 10); //nach Hause->HOUSEWORK_AT_HOME
        activityMapping.put(new ImmutableTriple<>(8, 7704, ANY_DIARY_GROUP), 10); //nach Hause->HOUSEWORK_AT_HOME
        activityMapping.put(new ImmutableTriple<>(9, 2202, ANY_DIARY_GROUP), 799); //Rückweg vom vorherigen Weg->ACTIVITIES_ANY
        activityMapping.put(new ImmutableTriple<>(9, 7704, ANY_DIARY_GROUP), 799); //Rückweg vom vorherigen Weg->ACTIVITIES_ANY
        activityMapping.put(new ImmutableTriple<>(10, 2202, ANY_DIARY_GROUP), 799); //andere Zweck->ACTIVITIES_ANY
        activityMapping.put(new ImmutableTriple<>(10, 7704, ANY_DIARY_GROUP), 799); //andere Zweck->ACTIVITIES_ANY
        activityMapping.put(new ImmutableTriple<>(11, 2202, ANY_DIARY_GROUP), 410); //(Vor-)Schule->SCHOOL
        activityMapping.put(new ImmutableTriple<>(11, 7704, ANY_DIARY_GROUP), 410); //(Vor-)Schule->SCHOOL
        activityMapping.put(new ImmutableTriple<>(12, 2202, ANY_DIARY_GROUP), 413); //Kita/Kindergarten/Hort->KINDERGARDEN
        activityMapping.put(new ImmutableTriple<>(12, 7704, ANY_DIARY_GROUP), 413); //Kita/Kindergarten/Hort->KINDERGARDEN
        activityMapping.put(new ImmutableTriple<>(13, 2202, ANY_DIARY_GROUP), 799); //Begleitung Erwachsener->ACTIVITIES_ANY
        activityMapping.put(new ImmutableTriple<>(13, 7704, ANY_DIARY_GROUP), 799); //Begleitung Erwachsener->ACTIVITIES_ANY
        activityMapping.put(new ImmutableTriple<>(14, 2202, ANY_DIARY_GROUP), 721); //Sport/Sportverein->Sports
        activityMapping.put(new ImmutableTriple<>(14, 7704, ANY_DIARY_GROUP), 721); //Sport/Sportverein->Sports
        activityMapping.put(new ImmutableTriple<>(15, 2202, ANY_DIARY_GROUP), 631); //Freunde besuchen -> Visiting
        activityMapping.put(new ImmutableTriple<>(15, 7704, ANY_DIARY_GROUP), 631); //Freunde besuchen -> Visiting
        activityMapping.put(new ImmutableTriple<>(16, 2202, ANY_DIARY_GROUP), 414); //Unterricht(nicht Schule)->Weiterbildung
        activityMapping.put(new ImmutableTriple<>(16, 7704, ANY_DIARY_GROUP), 414); //Unterricht(nicht Schule)->Weiterbildung

        activityMapping.put(new ImmutableTriple<>(99, 2202, ANY_DIARY_GROUP), 799); //keine Angabe/im PAPI nicht erhoben->ACTIVITIES_ANY
        activityMapping.put(new ImmutableTriple<>(99, 7705, ANY_DIARY_GROUP), 799); //keine Angabe/Weg ohne Info zum Wegezweck->ACTIVITIES_ANY

        //now we go through the diaries and convert the numbers:
        int schemeID = 1, start, duration, act_code_zbe, tourNumber;
        ImmutableTriple<Integer, Integer, Integer> key;
        boolean home, workchain;

        PrintWriter pw = new PrintWriter(System.out); //needed to get rid of stupid german localization of doubles!
        String tmpString = String.format(Locale.ENGLISH,
                "INSERT INTO %s" + " (scheme_id, start, duration, act_code_zbe, home, tournumber, workchain, key) " +
                        "VALUES (?,?,?,?,?,?,?,'%s');", table_episode, tableKey);

        try {
            PreparedStatement pS = this.dbCon.getConnection(this).prepareStatement(tmpString);
            int batchSize = 0, maxSize = 10000;

            int schemes = 0, diaries = 0;
            for (Entry<Integer, Diary> e : this.diaryMap.entrySet()) {
                Diary tmp = e.getValue();
                tmp.schemeID = schemeID;
                tmpString = String.format(Locale.ENGLISH,
                        "INSERT INTO %s (scheme_id, scheme_class_id, homework, key) VALUES (%d,%d,false,'%s');", table_schemes,
                        tmp.schemeID, tmp.group, tableKey);
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
                    key = new ImmutableTriple<>(d.purpose, d.purposeDetailed, tmp.group);
                    if (!activityMapping.containsKey(key)) {
                        key = new ImmutableTriple<>(d.purpose, d.purposeDetailed, ANY_DIARY_GROUP);
                        if (!activityMapping.containsKey(key)) {
                            key = new ImmutableTriple<>(d.purpose, ANY_TRIP_PURPOSE_DETAIL, tmp.group);
                            if (!activityMapping.containsKey(key)) {
                                key = new ImmutableTriple<>(d.purpose, ANY_TRIP_PURPOSE_DETAIL, ANY_DIARY_GROUP);
                            }
                        }
                    }

                    act_code_zbe = activityMapping.get(key);
                    tourNumber = d.tourNumber;
                    home = d.home && d.stay;
                    workchain = d.workchain;
                    if (!d.stay) act_code_zbe = 80;
                    //System.out.printf("Scheme %6d Start %4d Duration %4d Act %3d tour %2d home %s workchain %s\n", schemeID, start, duration, act_code_zbe, tourNumber, (home?"T":"F"), (workchain?"T":"F"));
                    tmpString = String.format(Locale.ENGLISH, "INSERT INTO %s" +
                                    " (scheme_id, start, duration, act_code_zbe, home, tournumber, workchain, key) " +
                                    "VALUES (%d,%d,%d,%d,%s,%d,%s,'%s'); --hhid: %d, pid: %d", table_episode, tmp.schemeID, start,
                            duration, act_code_zbe, home, tourNumber, workchain, tableKey, tmp.hhID, tmp.pID);
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
        Map<Integer, Integer> groupDistribution;
        Map<Integer, Integer> groupSumCount = new HashMap<>();
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
        Map<Integer, Integer> groupDistribution;
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
            groupCounter.put(i,0);
            groupSumTime.put(i,0);
            groupSumWay.put(i,0);
            groupAvgTime.put(i,0.0);
            groupAvgWay.put(i,0.0);
            groupStdDeviation.put(i,0.0);
            groupWithinStdDeviation.put(i,0.0);
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
            sumways = counter > 0 ? ((double) groupSumWay.get(i)/ (double) counter): 0.0;
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
            stdDev = counter>0 ? (groupStdDeviation.get(i)/ (double) counter) : 0;
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
            if ((e.totalTravelTime - groupAvgTime.get(group)) * (e.totalTravelTime - groupAvgTime.get(group))< stdDev) {
                within += 1;
            }
            groupWithinStdDeviation.put(group, within);
        }
        //normalize
        for (Integer i : this.diaryGroups) {
            counter = groupCounter.get(i);
            within = groupWithinStdDeviation.get(i);
            stdDev = counter>0 ? (within / (double) counter) : 0;
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
                        "INSERT INTO %s (scheme_class_id, avg_travel_time, proz_std_dev, key) VALUES (%d,%f,%f,'%s');\n", table,
                        i, avg, within, tableKey);

            }
            pw.flush();
        } else {
            //there are only a few insert, so we do it directly without prepared Statements
            String query;
            for (Integer i : this.diaryGroups) {
                avg = groupAvgTime.get(i);
                within = groupWithinStdDeviation.get(i);
                query = String.format(Locale.ENGLISH,
                        "INSERT INTO %s (scheme_class_id, avg_travel_time, proz_std_dev, key) VALUES (%d,%f,%f,'%s');", table,
                        i, avg, within, tableKey);
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

    public static String makeDiaryKey(int hhID, int pID, int erhebung) {
        return hhID + "-" + pID + "-" + erhebung;
    }

    public void readMIDDiary(String table, String filter) {
        String query = "";
        Diary actualDiary, lastDiary = null;
        DiaryElement lastActivity = null;
        int doubleReturnDiaries = 0;

        try {
            query = "select h_id, p_id, w_id, hp_taet, w_zweck, w_szs, w_szm, w_azs, w_azm, " +
                    "w_begl_1, w_begl_2, w_begl_3, w_begl_4, w_begl_5, w_begl_6, w_begl_7, w_begl_8, " +
                    "w_folgetag, w_zwd, wegmin, "+ pgTapasCol +", " + diaryGroupCol + " from " + table +
                    " where w_szs != 99 and w_szs != 701 and w_azs != 99 and w_azs != 701 and " +
                    " w_szm != 99 and w_szm != 701 and w_azm != 99 and w_azm != 701 and " +
                    "wegmin != 9994 and " +
                    "wegmin != 9995 and wegmin != 70701 and " + pgTapasCol + " <>-1 and " + filter +
                    " order by hp_id,h_id, p_id, w_id";
            ResultSet rs = this.dbCon.executeQuery(query, this);
            String key, lastKey = "";
            int hpid, hhID, pID, start, end, purpose, purposeDetailed, group, pGroup, personStatus, erhebung;
            boolean clean = true, home, addTripElement;

            while (rs.next()) {
                hhID = rs.getInt("h_id");
                pID = rs.getInt("p_id");
                start = rs.getInt("w_szs") * 60 + rs.getInt("w_szm");
                end = rs.getInt("w_folgetag") * ONE_DAY + rs.getInt("w_azs") * 60 + rs.getInt("w_azm");
                purpose = rs.getInt("w_zweck");
                purposeDetailed = rs.getInt("w_zwd");
                group = rs.getInt(diaryGroupCol);
                pGroup = rs.getInt(pgTapasCol);
                personStatus = rs.getInt("hp_taet");
                home = purpose == 8;
                erhebung = rs.getInt("Erhebung");
                key = makeDiaryKey(hhID, pID, erhebung);
                if (key == lastKey) { //same diary?
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
                    actualDiary = new Diary(hhID, pID, group, pGroup, personStatus);
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
                            if (lastActivity.end_min - lastActivity.start_min >= end - start) {//which trip is the longer one?
                                lastActivity.end_min = start - 5; //reduce the first/prev trip duration
                            } else {
                                start = lastActivity.end_min + 5; //reduce the second/current trip duration
                            }
                        }
                    }
                }
                if (addTripElement){
                    if (start != end) {
                        clean &= actualDiary.addNextElement(start, end, purpose, purposeDetailed, home);
                        int diaryIndex = actualDiary.activities.size() - 1;
                        lastActivity = actualDiary.activities.get(diaryIndex);
                        //check for accompanying persons
                        for (int i = 1; i <= 8; ++i) {
                            String column = "W_BEGL_" + i;
                            if (rs.getInt(column) == 1 && i != pID) {
                                actualDiary.activities.get(diaryIndex).accompanyingPersons.add(hpid);
                            }
                        }
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
            } else {
                boolean ok = true;
                for (DiaryElement d : e.getValue().activities) {
                    if (d.purpose == 11) {
                        ok = false; //set it to false
                        DiaryElement bestHit = null;
                        for (Integer k : d.accompanyingPersons) { // go through accompanying persons 1st time
                            Diary company = this.diaryMap.get(k);
                            if (company != null) { //Diary found?
                                DiaryElement companyAct = company.getClosestTrip(d.start_min);
                                if (companyAct != null) { //found a trip?
                                    //is this trip in the other list and the other trip not a acommpany? (100% hit)
                                    if (companyAct.accompanyingPersons.contains(e.getKey()) &&
                                            companyAct.purpose != 11) {
                                        //d.purpose= companyAct.purpose;
                                        //d.purposeDetailed = companyAct.purposeDetailed;
                                        //d.purposeDescription = "Begleitung bei "+companyAct.purposeDescription;
                                        ok = true;
                                        bestHit = companyAct;
                                        break;
                                    }
                                    if (!ok && companyAct.purpose != 11) { //no 100% hit found jet!
                                        if (bestHit == null || Math.abs(companyAct.start_min - d.start_min) < Math.abs(
                                                bestHit.start_min - d.start_min)) {
                                            bestHit = companyAct;
                                        }
                                    }
                                }
                            }
                        }
                        if (!ok && bestHit != null) {
                            //see if best hit is ok
                            if (Math.abs(bestHit.start_min - d.start_min) < 10) {
                                ok = true;
                            } else {
                                ok = false;
                                bestHit = null;
                            }
                        }
                        this.recodeAcompanyTrip(d, bestHit);
                    }
                }
                if (!ok) {
                    keysToRemove.add(e.getKey());
                }
            }
        }
        //remove bad diaries
        for (String k : keysToRemove) {
            this.diaryMap.remove(k);
        }
        //now scan all diaries for "begleitwege" and find their counterparts
        if (doubleReturnDiaries > 0) System.err.println("Number of two consecutive trips home: " + doubleReturnDiaries);
    }


    private void recodeAcompanyTrip(DiaryElement trip, DiaryElement theOtherOne) {
        if (theOtherOne != null) {
            trip.purpose = theOtherOne.purpose;
            trip.purposeDetailed = theOtherOne.purposeDetailed;
            int purpose = theOtherOne.purpose * 100000 + theOtherOne.purposeDetailed;

            if (accompanyStat.containsKey(purpose)) {
                accompanyStat.put(purpose, 1 + accompanyStat.get(purpose));
            } else {
                accompanyStat.put(purpose, 1);
            }
            recoded++;
        } else {
            int purpose = trip.purpose * 100000 + trip.purposeDetailed;
            if (UncodedAccompanyStat.containsKey(purpose)) {
                UncodedAccompanyStat.put(purpose, 1 + UncodedAccompanyStat.get(purpose));
            } else {
                UncodedAccompanyStat.put(purpose, 1);
            }

            notRecoded++;
        }
    }

    class DiaryElement implements Comparable<DiaryElement> {
        int wsID;
        int tourNumber = 0;
        int start_min;
        int end_min;
        int purpose;
        int purposeDetailed;
        boolean stay;
        boolean home;
        boolean workchain = false; //trip with purpose work
        List<Integer> accompanyingPersons = new ArrayList<>();

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
                            this.purpose + " detailed: " + this.purposeDetailed + " stay: " + this.stay + " home: " +
                            this.home);

        }
    }

    class Diary {
        int schemeID = 0;
        int hhID;
        int pID;
        int group;
        int pGroup;
        int personStatus;
        int totalTravelTime = 0;
        boolean reported;
        List<DiaryElement> activities = new ArrayList<>();

        public Diary(int hhID, int pID, int group, int pGroup, int personStatus) {
            this.hhID = hhID;
            this.pID = pID;
            this.group = group;
            this.pGroup = pGroup;
            this.personStatus = personStatus;
            this.reported = false;
            DiaryElement startElem = new DiaryElement();
            startElem.wsID = -1;
            startElem.start_min = 0;
            startElem.purpose = 0;
            startElem.purposeDetailed = 0;
            startElem.home = true;
            startElem.stay = true;
            this.addDiaryElement(startElem);
        }

        public void addDiaryElement(DiaryElement e) {
            e.wsID = this.activities.size(); //just the index, where it will be inserted
            this.activities.add(e);
        }

        public boolean addNextElement(int start, int end, int purpose, int purposeDetailed, boolean home) {
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
                stay.purposeDetailed = pre.home ? 0 : pre.purposeDetailed;
                stay.home = pre.home;
                stay.stay = true;
                //check for funny double entries!
                if (pre.start_min == start && pre.end_min == end) {
                    numOfDoubleWays++;
                    if (pre.purpose == purpose && pre.purposeDetailed == purposeDetailed) {
                        numOfExactDoubleWays++;
                        return true;
                    }
                    if (pre.purposeDetailed == 7704) {
                        pre.purpose = purpose;
                        pre.purposeDetailed = purposeDetailed;
                        return false;
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
                        purposeDetailed = preStay.purposeDetailed;

                    }
                } else {
                    trip.home = false; // no trip back?!?!
                }
            } else {
                trip.home = home;
            }
            trip.purpose = purpose;
            trip.start_min = start;
            trip.purposeDetailed = purposeDetailed;
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
                    inBetweenTrip.purposeDetailed = purposeDetailed;
                    this.addDiaryElement(inBetweenTrip);
                    //target
                    DiaryElement inBetweenStay = new DiaryElement();
                    inBetweenStay.start_min = middle;
                    inBetweenStay.end_min = middle + 1;
                    inBetweenStay.stay = true;
                    inBetweenStay.home = false;
                    inBetweenStay.purpose = purpose;
                    inBetweenStay.purposeDetailed = purposeDetailed;
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
            //the diary is ok if stay and trip are toggeling every time
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
                stay.purposeDetailed = 0;
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
                    auxStay.purposeDetailed = pre.purposeDetailed;
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
                    pre.purposeDetailed = 7704;
                    pre.home = true;
                    pre.stay = false;
                    this.addDiaryElement(pre);

                }

                stay = new DiaryElement();

                stay.start_min = pre.end_min;
                stay.end_min = Math.max(ONE_DAY, pre.end_min + 1);
                stay.purpose = pre.home ? 0 : pre.purpose;
                stay.purposeDetailed = pre.home ? 0 : pre.purposeDetailed;
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
