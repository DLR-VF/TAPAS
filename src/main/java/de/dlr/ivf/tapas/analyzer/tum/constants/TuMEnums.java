/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.analyzer.tum.constants;

import de.dlr.ivf.tapas.analyzer.inputfileconverter.Activity;
import de.dlr.ivf.tapas.analyzer.inputfileconverter.TapasTrip;
import de.dlr.ivf.tapas.analyzer.tum.regionanalyzer.AnalyzerBase;

import java.util.*;
import java.util.Map.Entry;

public class TuMEnums {

    public enum Categories {
        RegionCode(de.dlr.ivf.tapas.analyzer.tum.constants.TuMEnums.RegionCode.class, null), //
        PersonGroup(de.dlr.ivf.tapas.analyzer.tum.constants.TuMEnums.PersonGroup.class, "person_group"), //
        DistanceCategory(de.dlr.ivf.tapas.analyzer.tum.constants.TuMEnums.DistanceCategory.class,
                "distance_category"), //
        DistanceCategoryDefault(de.dlr.ivf.tapas.analyzer.tum.constants.TuMEnums.DistanceCategoryDefault.class,
                "distance_category"), //
        DistanceCategoryEquiFive(de.dlr.ivf.tapas.analyzer.tum.constants.TuMEnums.DistanceCategoryEquiFive.class,
                null), //
        DistanceCategoryEquiTen(de.dlr.ivf.tapas.analyzer.tum.constants.TuMEnums.DistanceCategoryEquiTen.class,
                null), //
        TripIntention(de.dlr.ivf.tapas.analyzer.tum.constants.TuMEnums.TripIntention.class, "trip_intention"), //
        Mode(de.dlr.ivf.tapas.analyzer.tum.constants.TuMEnums.Mode.class, "trip_mode"), //
        Job(de.dlr.ivf.tapas.analyzer.tum.constants.TuMEnums.Job.class, null), //
        TravelTime(de.dlr.ivf.tapas.analyzer.tum.constants.TuMEnums.TravelTime.class, null);

        private static final HashSet<Class<?>> CATEGORIES;

        static {
            CATEGORIES = new HashSet<>();
            for (Categories c : values()) {
                CATEGORIES.add(c.getEnumeration());
            }
        }

        private final Class<?> theClass;
        private final String calibrationColumn;

        Categories(Class<?> theClass, String calibrationColumn) {
            this.calibrationColumn = calibrationColumn;
            this.theClass = theClass;
        }

        public static Categories getByClass(Class<?> theClass) {
            for (Categories c : values()) {
                if (c.getEnumeration().equals(theClass)) return c;
            }

            throw new IllegalArgumentException("The given class " + theClass.getSimpleName() + " is not valid.");
        }

        /**
         * @param theClass
         * @return a new {@link AnalyzerBase} for the given class.
         */
        @SuppressWarnings("rawtypes")
        public static AnalyzerBase getNewAnalyzer(Class<?> theClass) {
            Categories cat = getByClass(theClass);
            switch (cat) {
                case RegionCode:
                    return new RegionCodeAnalyzer();
                case DistanceCategoryDefault:
                    return new DefaultDistanceCategoryAnalyzer();
                case Mode:
                    return new ModeAnalyzer();
                case PersonGroup:
                    return new PersonGroupAnalyzer();
                case TravelTime:
                    return new TravelTimeAnalyzer();
                case TripIntention:
                    return new TripIntentionAnalyzer();
                default:
                    throw new IllegalArgumentException(
                            "The class " + theClass.getCanonicalName() + " is not supported here.");
            }
        }

        @SuppressWarnings("rawtypes")
        public static boolean isValid(Enum cat) {
            if (cat == null) {
                return false;
            }
            return CATEGORIES.contains(cat.getClass());
        }

        /**
         * @return the column name used to identify this element in the
         * <code>calibration_results</code> table.
         */
        public String getCalibrationColumn() {
            return calibrationColumn;
        }

        /**
         * Returns an the enumeration element of the calling class with the
         * given <code>id</code>.
         *
         * @param id
         * @return
         */
        @SuppressWarnings("rawtypes")
        public Enum getElementById(int id) {
            switch (this) {
                case DistanceCategory:
                    return TuMEnums.DistanceCategory.getById(id);
                case DistanceCategoryDefault:
                    return TuMEnums.DistanceCategoryDefault.getById(id);
                case Mode:
                    return TuMEnums.Mode.getById(id);
                case PersonGroup:
                    return TuMEnums.PersonGroup.getById(id);
                case Job:
                    return TuMEnums.Job.getById(id);
                case RegionCode:
                    return TuMEnums.RegionCode.getById(id);
                case TripIntention:
                    return TuMEnums.TripIntention.getById(id);
                default:
                    throw new IllegalStateException("Error in implementation!");
            }
        }

        /**
         * @return the enumeration class of this element.
         */
        public Class<?> getEnumeration() {
            return theClass;
        }

    }

    public enum RegionCode {
        REGION_0(0, "Gesamt"), //
        REGION_1(1, "Agglomeration"), //
        REGION_2(2, "städtisch"), //
        REGION_3(3, "wenig verstädtert"), //
        REGION_4(4, "ländlich"), //
        REGION_5(5, "sehr ländlich");

        private final int id;
        private final String desc;

        RegionCode(int id, String desc) {
            this.id = id;
            this.desc = desc;
        }

        /**
         * @param id
         * @return
         * @throws IllegalArgumentException if the <code>id</code> cannot be assigned.
         */
        public static RegionCode getById(int id) {
            for (RegionCode rc : values()) {
                if (rc.getId() == id) {
                    return rc;
                }
            }
            System.err.println("The region id " + id + " is unknown.");
            throw new IllegalArgumentException();
//			return values()[0];
        }

        /**
         * Returns the maximal number of characters the names can have.
         *
         * @return
         */
        public static int maxNameWidth() {
            int w = 0;
            for (RegionCode rc : values())
                w = Math.max(rc.getName().length(), w);

            return w;
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return desc;
        }

        @Override
        public String toString() {
            return desc;
        }

    }

    public enum TripIntention {
        // Wegezweck

        TRIP_31(0, "Schule", generateSet(Activity.ACT_410, Activity.ACT_413, Activity.ACT_499)), //
        TRIP_38(1, "Studium", generateSet(Activity.ACT_411, Activity.ACT_412, Activity.ACT_414)), //
        TRIP_32(2, "Arbeit", generateSet(Activity.ACT_62, Activity.ACT_211, Activity.ACT_212, Activity.ACT_213)), //
        TRIP_33(3, "Private Erledigungen",
                generateSet(Activity.ACT_10, Activity.ACT_12, Activity.ACT_32, Activity.ACT_522, Activity.ACT_611,
                        Activity.ACT_631)), //
        TRIP_34(4, "Einkauf",
                generateSet(Activity.ACT_50, Activity.ACT_51, Activity.ACT_52, Activity.ACT_53, Activity.ACT_54,
                        Activity.ACT_55)), //
        TRIP_35(5, "Freizeit",
                generateSet(Activity.ACT_231, Activity.ACT_300, Activity.ACT_511, Activity.ACT_512, Activity.ACT_531,
                        Activity.ACT_533, Activity.ACT_640, Activity.ACT_700, Activity.ACT_711, Activity.ACT_720,
                        Activity.ACT_721, Activity.ACT_722, Activity.ACT_723, Activity.ACT_724, Activity.ACT_800,
                        Activity.ACT_880, Activity.ACT_881)), //
        TRIP_36(6, "Sonstige", generateSet(Activity.ACT_740, Activity.ACT_799)), //
        TRIP_37(7, "BackHome", generateSet()), //
        TRIP_MISC(8, "Nicht zugeordnet", generateSet());//

        private final int id;
        private final String caption;
        private final Set<Activity> actingCodes;

        TripIntention(int id, String caption, Set<Activity> actingCodes) {
            this.id = id;
            this.caption = caption;
            this.actingCodes = actingCodes;

        }

        private static Set<Activity> generateSet(Activity... activities) {
            return new HashSet<>(Arrays.asList(activities));
        }

        /**
         * @param act
         * @return den Wegezweck der diese Activity enthält, null wenn die
         * activity null ist oder die Activity keinem Wegezweck
         * zugeordnet ist
         */
        public static TripIntention getByActivity(Activity act) {
            // if (act == null)
            // return null;
            for (TripIntention ti : values()) {
                if (ti.getActingCodes().contains(act)) {
                    return ti;
                }
            }
            return null;
        }

        /**
         * @param id
         * @return den Wegezweck der dieser ID zugeordnet ist
         * @throws IllegalArgumentException geworfen wenn die ID keinem Wegezweck zugeordnet werden
         *                                  konnte
         */
        public static TripIntention getById(int id) throws IllegalArgumentException {
            for (TripIntention ti : values()) {
                if (ti.getId() == id) {
                    return ti;
                }
            }

            throw new IllegalArgumentException();
        }

        /**
         * Returns the maximal number of characters the names can have.
         *
         * @return
         */
        public static int maxNameWidth() {
            int w = 0;
            for (TripIntention ti : values())
                w = Math.max(ti.getName().length(), w);

            return w;
        }

        public Set<Activity> getActingCodes() {
            return actingCodes;
        }

        public String getCaption() {
            return caption;
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return caption;
        }

        @Override
        public String toString() {
            return caption;
        }
    }

    public enum DistanceCategory {
        CAT_1, CAT_2, CAT_3, CAT_4, CAT_5, CAT_6, CAT_7, CAT_8;

        public static String NOT_USED = "Nicht zugeordnet.";

        /**
         * Creates new distances from a filter according to the default classes
         * in {@link DistanceCategory}.<br>
         * Example for default classes
         * <code>[<1, 1-3,3-5, 5-7,7-10,10-25,25-100,>= 100]</code> and filter
         * <code>[F,F,T,F,F,T,F,F]</code> becomes
         * <code>[<5, 5-25, >= 25,-,-,-,-,-]</code>
         *
         * @param filter
         * @return Spare categories will be filled with
         * <code>Double.MaxValue</code>.
         * @throws IllegalArgumentException if the length of the filter does not coincide with the
         *                                  number of categories in {@link DistanceCategory}.
         */
        public static double[] createFilteredDistances(boolean[] filter) {

            int len = filter.length;

            if (len != DistanceCategoryDefault.values().length) throw new IllegalArgumentException(
                    "The filter length must coincide with the number of distance categories");

            double[] result = new double[len];
            int idxF = 0;
            int idxR = 0;

            while (idxF < len) {
                if (filter[idxF]) {
                    result[idxR++] = DistanceCategoryDefault.values()[idxF].getMaxDistance();
                }

                idxF++;
            }

            while (idxR < len) {
                result[idxR++] = Double.MAX_VALUE;
            }

            return result;
        }

        public static DistanceCategory getById(int id) {
            return values()[id];
        }

    }

    public enum DistanceCategoryDefault {
        CAT_1(0, 1000, "< 1 km"), //
        CAT_2(1, 3000, "1-3 km"), //
        CAT_3(2, 5000, "3-5 km"), //
        CAT_4(3, 7000, "5-7 km"), //
        CAT_5(4, 10000, "7-10 km"), //
        CAT_6(5, 25000, "10-25 km"), //
        CAT_7(6, 100000, "25-100 km"), //
        CAT_8(7, Double.MAX_VALUE, ">=100 km");

        private final int id;
        private final double maxDistance;
        private final String description;

        /**
         * @param id          die ID der Kategorie
         * @param maxDistance die maximale Distanz (ausschließlich, der angegebene Wert
         *                    ist nicht mehr gültig)
         */
        DistanceCategoryDefault(int id, double maxDistance, String description) {
            this.id = id;
            this.maxDistance = maxDistance;
            this.description = description;

        }

        /**
         * Erzeugt aus den {@link DistanceCategory} einen einheitlichen Namen.
         * z.B. Wird aus {@link DistanceCategory#CAT_2} und
         * {@link DistanceCategory#CAT_3} = 1-5 km
         *
         * @return
         */
        public static String createDistanceCategoriesID(List<DistanceCategoryDefault> categories) {
            StringBuilder ret = new StringBuilder();
            for (DistanceCategoryDefault cat : categories) {
                ret.append(cat.getId()).append("u");
            }
            return ret.substring(0, ret.length() - 1);
        }

        /**
         * Erzeugt aus den {@link DistanceCategory} einen einheitlichen Namen.
         * z.B. Wird aus {@link DistanceCategory#CAT_2} und
         * {@link DistanceCategory#CAT_3} = 1-5 km
         *
         * @param categories Elemente müssen aufsteigend sortiert sein
         * @return
         */
        public static String createDistanceCategoriesName(List<DistanceCategoryDefault> categories) {
            if (categories.contains(CAT_1) && categories.contains(CAT_8)) {
                return "Beliebig";
            } else if (categories.contains(CAT_1)) {
                // Wenn die erste Kategorie (< 1km) enthalten ist, wird der neue
                // Name: < xkm. Wobei x der größten Zahl in den Kategorien
                // entspricht
                return "<" + categories.get(categories.size() - 1).getDescription().replaceFirst("(<|\\d+-)", "");
            } else if (categories.contains(CAT_8)) {
                return ">=" + categories.get(0).getDescription().replaceFirst("(>|(-\\d+))", "");
            } else {
                String descBeginning = categories.get(0).getDescription();
                String descEnd = categories.get(categories.size() - 1).getDescription();
                String beginning = descBeginning.substring(0, descBeginning.indexOf('-'));
                String end = descEnd.substring(descEnd.indexOf('-') + 1);
                return beginning + "-" + end;
            }
        }

        /**
         * @param dist
         * @return die DistanceCategory dessen obere Distanzgrenze am nächsten
         * an der angebenen größe ist und gleichzeitig größer als die
         * angebene ist. Sollte keine DistanceCategory eine größere
         * Distance haben wird immer die größte zurückgegeben.
         */
        public static DistanceCategoryDefault getByDistance(double dist) {
            for (DistanceCategoryDefault cat : values()) {
                if (dist < cat.getMaxDistance()) {
                    return cat;
                }
            }
            return values()[values().length - 1];
        }

        /**
         * @param id
         * @return die DistanceCategory die die angegebene ID besitzt
         * @throws IllegalArgumentException wenn die ID keiner Kategorie zugeordnet werden kann
         */
        public static DistanceCategoryDefault getById(int id) throws IllegalArgumentException {
            for (DistanceCategoryDefault cat : values()) {
                if (cat.getId() == id) {
                    return cat;
                }
            }
            throw new IllegalArgumentException();
        }

        /**
         * Returns the maximal number of characters the names can have.
         *
         * @return
         */
        public static int maxNameWidth() {
            int w = 0;
            for (DistanceCategoryDefault dc : values())
                w = Math.max(dc.getDescription().length(), w);
            return w;
        }

        public String getDescription() {
            return description;
        }

        public int getId() {
            return id;
        }

        protected double getMaxDistance() {
            return maxDistance;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    /**
     * Distanz-Kategorien in 5km Abstand.
     */
    public enum DistanceCategoryEquiFive {
        CAT_1(0, 5000, "< 5 km"), //
        CAT_2(1, 10000, "5-10 km"), //
        CAT_3(2, 15000, "10-15 km"), //
        CAT_4(3, 20000, "15-20 km"), //
        CAT_5(4, 25000, "20-25 km"), //
        CAT_6(5, 30000, "25-30 km"), //
        CAT_7(6, 350000, "30-35 km"), //
        CAT_8(7, Double.MAX_VALUE, ">=35 km");

        private final int id;
        private final double maxDistance;
        private final String description;

        /**
         * @param id          die ID der Kategorie
         * @param maxDistance die maximale Distanz (ausschließlich, der angegebene Wert
         *                    ist nicht mehr gültig)
         */
        DistanceCategoryEquiFive(int id, double maxDistance, String description) {
            this.id = id;
            this.maxDistance = maxDistance;
            this.description = description;

        }

        /**
         * @param dist
         * @return die DistanceCategory dessen obere Distanzgrenze am nächsten
         * an der angebenen größe ist und gleichzeitig größer als die
         * angebene ist. Sollte keine DistanceCategory eine größere
         * Distance haben wird immer die größte zurückgegeben.
         */
        public static DistanceCategoryEquiFive getByDistance(double dist) {
            for (DistanceCategoryEquiFive cat : values()) {
                if (dist < cat.getMaxDistance()) {
                    return cat;
                }
            }
            return values()[values().length - 1];
        }

        /**
         * @param id
         * @return die DistanceCategory die die angegebene ID besitzt
         * @throws IllegalArgumentException wenn die ID keiner Kategorie zugeordnet werden kann
         */
        public static DistanceCategoryEquiFive getById(int id) throws IllegalArgumentException {
            for (DistanceCategoryEquiFive cat : values()) {
                if (cat.getId() == id) {
                    return cat;
                }
            }
            throw new IllegalArgumentException();
        }

        /**
         * Returns the maximal number of characters the names can have.
         *
         * @return
         */
        public static int maxNameWidth() {
            int w = 0;
            for (DistanceCategoryEquiFive dc : values())
                w = Math.max(dc.getDescription().length(), w);
            return w;
        }

        public String getDescription() {
            return description;
        }

        public int getId() {
            return id;
        }

        protected double getMaxDistance() {
            return maxDistance;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    /**
     * Distanz-Kategorien in 5km Abstand.
     */
    public enum DistanceCategoryEquiTen {
        CAT_1(0, 10000, "< 10 km"), //
        CAT_2(1, 20000, "10-20 km"), //
        CAT_3(2, 30000, "20-30 km"), //
        CAT_4(3, 40000, "30-40 km"), //
        CAT_5(4, 50000, "40-50 km"), //
        CAT_6(5, 60000, "50-60 km"), //
        CAT_7(6, 70000, "60-70 km"), //
        CAT_8(7, Double.MAX_VALUE, ">=70 km");

        private final int id;
        private final double maxDistance;
        private final String description;

        /**
         * @param id          die ID der Kategorie
         * @param maxDistance die maximale Distanz (ausschließlich, der angegebene Wert
         *                    ist nicht mehr gültig)
         */
        DistanceCategoryEquiTen(int id, double maxDistance, String description) {
            this.id = id;
            this.maxDistance = maxDistance;
            this.description = description;

        }

        /**
         * @param dist
         * @return die DistanceCategory dessen obere Distanzgrenze am nächsten
         * an der angebenen größe ist und gleichzeitig größer als die
         * angebene ist. Sollte keine DistanceCategory eine größere
         * Distance haben wird immer die größte zurückgegeben.
         */
        public static DistanceCategoryEquiTen getByDistance(double dist) {
            for (DistanceCategoryEquiTen cat : values()) {
                if (dist < cat.getMaxDistance()) {
                    return cat;
                }
            }
            return values()[values().length - 1];
        }

        /**
         * @param id
         * @return die DistanceCategory die die angegebene ID besitzt
         * @throws IllegalArgumentException wenn die ID keiner Kategorie zugeordnet werden kann
         */
        public static DistanceCategoryEquiTen getById(int id) throws IllegalArgumentException {
            for (DistanceCategoryEquiTen cat : values()) {
                if (cat.getId() == id) {
                    return cat;
                }
            }
            throw new IllegalArgumentException();
        }

        /**
         * Returns the maximal number of characters the names can have.
         *
         * @return
         */
        public static int maxNameWidth() {
            int w = 0;
            for (DistanceCategoryEquiTen dc : values())
                w = Math.max(dc.getDescription().length(), w);
            return w;
        }

        public String getDescription() {
            return description;
        }

        public int getId() {
            return id;
        }

        protected double getMaxDistance() {
            return maxDistance;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    public enum TravelTime {

        CAT1(15, "< 15m"), //
        CAT2(30, "15m - 30m"), //
        CAT3(45, "30m - 45m'"), //
        CAT4(60, "45m - 1h"), //
        CAT5(75, "< 1h15m"), //
        CAT6(90, "1h15m - 1h30m"), //
        CAT7(105, "1h30m - 1h45m"), //
        CAT8(120, "1h45m - 2h"), //
        CAT9(135, "2h - 2h15m"), //
        CAT10(150, "2h15m - 2h30m"), //
        CAT11(165, "2h30m - 2h45m"), //
        CAT12(180, "2h45m < 3h"), //
        CAT13(Integer.MAX_VALUE, "> 3h");

        private final String name;
        private final int maxTime;

        TravelTime(int time, String name) {
            this.name = name;
            this.maxTime = time;
        }

        public static TravelTime getById(int id) {
            return values()[id];
        }

        /**
         * Every negative time will result in {@link TravelTime#CAT1}.
         */
        public static TravelTime getByTime(double d) {
            for (TravelTime cat : values()) {
                if (d < cat.maxTime) {
                    return cat;
                }
            }
            return values()[values().length - 1];
        }

        public int getMaxTime() {
            return maxTime;
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return name;
        }

    }

    public enum PersonGroup {
        PG_1(0, "Vollzeit/Teilzeit Frau mit Pkw", generateSet(Job.JOB_5, Job.JOB_11, Job.JOB_19, Job.JOB_36, Job.JOB_44, Job.JOB_52, Job.JOB_60)), //
        PG_2(1, "Vollzeit/Teilzeit Frau ohne Pkw", generateSet(Job.JOB_12, Job.JOB_20, Job.JOB_37, Job.JOB_45, Job.JOB_53, Job.JOB_61)), //

        PG_3(2, "Vollzeit/Teilzeit Mann mit Pkw", generateSet(Job.JOB_3, Job.JOB_9, Job.JOB_17, Job.JOB_34, Job.JOB_42, Job.JOB_50, Job.JOB_58)), //
        PG_4(3, "Vollzeit/Teilzeit Mann ohne Pkw", generateSet(Job.JOB_10, Job.JOB_18, Job.JOB_35, Job.JOB_43, Job.JOB_51, Job.JOB_59)), //

        PG_5(4, "nicht erwerbstätig Frau mit Pkw", generateSet(Job.JOB_8, Job.JOB_15, Job.JOB_23, Job.JOB_40, Job.JOB_48, Job.JOB_56, Job.JOB_64)), //
        PG_6(5, "nicht erwerbstätig Frau ohne Pkw", generateSet(Job.JOB_16, Job.JOB_24, Job.JOB_41, Job.JOB_49, Job.JOB_57, Job.JOB_65)), //
        PG_7(6, "nicht erwerbstätig Mann mit Pkw", generateSet(Job.JOB_6, Job.JOB_13, Job.JOB_21, Job.JOB_38, Job.JOB_46, Job.JOB_54, Job.JOB_62)), //
        PG_8(7, "nicht erwerbstätig Mann ohne Pkw", generateSet(Job.JOB_14, Job.JOB_22, Job.JOB_39, Job.JOB_47, Job.JOB_55, Job.JOB_63)), //
        PG_9(8, "Rentner mit Pkw", generateSet(Job.JOB_25, Job.JOB_27, Job.JOB_29, Job.JOB_31)), //
        PG_10(9, "Rentner ohne Pkw", generateSet(Job.JOB_26, Job.JOB_28, Job.JOB_30, Job.JOB_32)), //
        PG_11(10, "Schüler/Student", generateSet(Job.JOB_1, Job.JOB_2)), //
        PG_12(11, "Kinder", generateSet()), //
        PG_13(12, "4 und 7: junge Erwachsene bis 24 Jahre mit und ohne PKW, erwerbstätig und nichterwerbstätig",
                generateSet(Job.JOB_4, Job.JOB_7));

        private final int id;
        private final String desc;
        private final Set<Job> jobSet;

        PersonGroup(int id, String desc, Set<Job> jobSet) {
            this.id = id;
            this.jobSet = jobSet;
            this.desc = desc;
        }

        private static Set<Job> generateSet(Job... jobs) {
            return new HashSet<>(Arrays.asList(jobs));
        }

        /**
         * @param id
         * @return den Wegezweck der dieser ID zugeordnet ist
         * @throws IllegalArgumentException geworfen wenn die ID keinem Wegezweck zugeordnet werden
         *                                  konnte
         */
        public static PersonGroup getById(int id) throws IllegalArgumentException {
            for (PersonGroup pg : values()) {
                if (pg.getId() == id) {
                    return pg;
                }
            }

            throw new IllegalArgumentException();
        }

        public static PersonGroup getByJob(Job job) {
            for (PersonGroup group : values()) {
                if (group.getJobSet().contains(job)) {
                    return group;
                }
            }

            throw new IllegalArgumentException("Job is not assigned to any personGroup");

        }

        /**
         * Returns the maximal number of characters the names can have.
         *
         * @return
         */
        public static int maxNameWidth() {
            int w = 0;
            for (PersonGroup pg : values())
                w = Math.max(pg.getName().length(), w);

            return w;
        }

        public int getId() {
            return id;
        }

        public Set<Job> getJobSet() {
            return jobSet;
        }

        public String getName() {
            return desc;
        }

        @Override
        public String toString() {
            return desc;
        }
    }

    public enum Mode {
        WALK(0, "Fuß"), BIKE(1, "Rad"), MIV(2, "Pkw"), MIV_PASS(3, "PkwMf"), TAXI(4, "Taxi"), PT(5, "ÖV"), TRAIN(6,
                "Zug");

        private final int id;
        private final String description;

        Mode(int id, String description) {
            this.id = id;
            this.description = description;
        }

        /**
         * @param id
         * @return den Mode der dieser ID zugeordnet ist
         * @throws IllegalArgumentException geworfen wenn die ID keinem Wegezweck zugeordnet werden
         *                                  konnte
         */
        public static Mode getById(int id) throws IllegalArgumentException {
            for (Mode mode : values()) {
                if (mode.getId() == id) {
                    return mode;
                }
            }

            throw new IllegalArgumentException();
        }

        /**
         * Returns the maximal number of characters the names can have.
         *
         * @return
         */
        public static int maxNameWidth() {
            int w = 0;
            for (Mode mo : values())
                w = Math.max(mo.toString().length(), w);

            return w;
        }

        public String getDescription() {
            return description;
        }

        public int getId() {
            return id;
        }

        public String toString() {
            return description;
        }
    }

    public enum Job {
        JOB_1(1), JOB_2(2), JOB_3(3), JOB_4(4), JOB_5(5), JOB_6(6), JOB_7(7), JOB_8(8), JOB_9(9), JOB_10(10), JOB_11(
                11), JOB_12(12), JOB_13(13), JOB_14(14), JOB_15(15), JOB_16(16), JOB_17(17), JOB_18(18), JOB_19(
                19), JOB_20(20), JOB_21(21), JOB_22(22), JOB_23(23), JOB_24(24), JOB_25(25), JOB_26(26), JOB_27(
                27), JOB_28(28), JOB_29(29), JOB_30(30), JOB_31(31), JOB_32(32), JOB_34(34), JOB_35(35), JOB_36(
                36), JOB_37(37), JOB_38(38), JOB_39(39), JOB_40(40), JOB_41(41), JOB_42(42), JOB_43(43), JOB_44(
                44), JOB_45(45), JOB_46(46), JOB_47(47), JOB_48(48), JOB_49(49), JOB_50(50), JOB_51(51), JOB_52(
                52), JOB_53(53), JOB_54(54), JOB_55(55), JOB_56(56), JOB_57(57), JOB_58(58), JOB_59(59), JOB_60(
                60), JOB_61(61), JOB_62(62), JOB_63(63), JOB_64(64), JOB_65(65);

        private final int id;

        Job(int id) {
            this.id = id;
        }

        public static Job getById(int job) {
            for (Job j : values()) {
                if (j.getId() == job) {
                    return j;
                }
            }
            throw new IllegalArgumentException();
        }

        /**
         * Returns the maximal number of characters the names can have.
         *
         * @return
         */
        public static int maxNameWidth() {
            int w = 0;
            for (Job ti : values())
                w = Math.max(ti.toString().length(), w);

            return w;
        }

        public int getId() {
            return id;
        }
    }

    /**
     * When created with reference to other analyzers, the PersonGroupAnalyzer
     * keeps the number of persons for each {@link CategoryCombination}.
     */
    @SuppressWarnings("rawtypes")
    public static class PersonGroupAnalyzer extends AnalyzerBase<PersonGroup> {

        private final HashMap<CategoryCombination, HashSet<Integer>> personSets;
        private final AnalyzerBase[] analyzers;
        private boolean keepsPersonCnt = false;

        public PersonGroupAnalyzer() {
            this(new AnalyzerBase[0]);
        }

        public PersonGroupAnalyzer(AnalyzerBase... analyzers) {
            super();

            this.analyzers = analyzers.clone();
            personSets = new HashMap<>();

            if (analyzers.length > 0) {
                keepsPersonCnt = true;
            }
        }

        @Override
        public PersonGroup assignTrip(TapasTrip trip) {
            if (keepsPersonCnt) {
                Enum[] cats = new Enum[analyzers.length + 1];
                for (int i = 0; i < analyzers.length; ++i) {
                    cats[i] = analyzers[i].assignTrip(trip);
                }
                cats[analyzers.length] = PersonGroup.getByJob(Job.getById(trip.getJob()));

                CategoryCombination cc = new CategoryCombination(cats);
                if (personSets.containsKey(cc)) {
                    personSets.get(cc).add(trip.getIdPers());
                } else {
                    HashSet<Integer> hs = new HashSet<>();
                    hs.add(trip.getIdPers());
                    personSets.put(cc, hs);
                }

            }
            return PersonGroup.getByJob(Job.getById(trip.getJob()));
        }

        /**
         * @return -1 if the information is not available.
         */
        public int getCntPersons(CategoryCombination cc) {
            if (keepsPersonCnt && personSets.containsKey(cc)) {
                return personSets.get(cc).size();
            } else if (keepsPersonCnt) {
                return 0;
            } else {
                return -1;
            }
        }

        public boolean keepsCnt() {
            return keepsPersonCnt;
        }
    }

    public static class TapasPersonGroupAnalyzer extends AnalyzerBase<Job> {
        @Override
        public Job assignTrip(TapasTrip trip) {
            return Job.getById(trip.getJob());
        }
    }

    public static class DefaultDistanceCategoryAnalyzer extends AnalyzerBase<DistanceCategoryDefault> {
        @Override
        public DistanceCategoryDefault assignTrip(TapasTrip trip) {
            return DistanceCategoryDefault.getByDistance(trip.getDistNet());
        }
    }

    public static class EquiFiveDistanceCategoryAnalyzer extends AnalyzerBase<DistanceCategoryEquiFive> {
        @Override
        public DistanceCategoryEquiFive assignTrip(TapasTrip trip) {
            return DistanceCategoryEquiFive.getByDistance(trip.getDistNet());
        }
    }

    public static class EquiTenDistanceCategoryAnalyzer extends AnalyzerBase<DistanceCategoryEquiTen> {
        @Override
        public DistanceCategoryEquiTen assignTrip(TapasTrip trip) {
            return DistanceCategoryEquiTen.getByDistance(trip.getDistNet());
        }
    }

    public static class TravelTimeAnalyzer extends AnalyzerBase<TravelTime> {
        @Override
        public TravelTime assignTrip(TapasTrip trip) {
            return TravelTime.getByTime(trip.getTT());
        }
    }

    public static class DistanceCategoryAnalyzer extends AnalyzerBase<DistanceCategory> {

        private final double[] distances;

        EnumMap<DistanceCategory, String> nameMap;

        /**
         * @param distances in meters. Must be in strict ascending order.
         * @throws IllegalArgumentException if the length of distances does not coincide with the
         *                                  number of categories or if there is no distance different
         *                                  from <code>Double.MaxValue</code>.
         */
        public DistanceCategoryAnalyzer(double[] distances) {
            if (distances.length != DistanceCategory.values().length) {
                throw new IllegalArgumentException("There must be a distance given for each category.");
            }
            if (distances[0] == Double.MAX_VALUE) throw new IllegalArgumentException(
                    "Please provide at least one real distance.");

            this.distances = distances;
            buildNameMap();
        }

        @Override
        public DistanceCategory assignTrip(TapasTrip trip) {

            double dist = trip.getDistNet();

            if (dist < distances[0]) return DistanceCategory.CAT_1;

            int idx = 1;

            while (dist >= distances[idx] && idx < distances.length - 1) idx++;

            if (distances[idx] == Double.MAX_VALUE) return DistanceCategory.CAT_8;
            else return DistanceCategory.values()[idx];
        }

        private void buildNameMap() {
            nameMap = new EnumMap<>(DistanceCategory.class);

            nameMap.put(DistanceCategory.CAT_1, "< " + String.format("%d", (int) distances[0] / 1000) + " km");

            int idx = 1;
            double oldD = distances[0];
            double newD;
            while (idx < distances.length - 1) {
                newD = distances[idx];
                if (newD == Double.MAX_VALUE) {
                    nameMap.put(DistanceCategory.values()[idx], DistanceCategory.NOT_USED);
                } else {

                    String s = String.format("%d-%d km", (int) oldD / 1000, (int) newD / 1000);
                    nameMap.put(DistanceCategory.values()[idx], s);
                    oldD = newD;
                }
                idx++;
            }
            String s = String.format(">= %d km", (int) oldD / 1000);
            nameMap.put(DistanceCategory.CAT_8, s);
        }

        public String getCategoryName(DistanceCategory dc) {
            return nameMap.get(dc);
        }

        /**
         * Returns all active DistanceCategories.
         */
        public ArrayList<DistanceCategory> getDistanceCategories() {

            ArrayList<DistanceCategory> result = new ArrayList<>();

            for (Entry<DistanceCategory, String> e : nameMap.entrySet()) {
                if (!e.getValue().equals(DistanceCategory.NOT_USED)) result.add(e.getKey());
            }

            return result;
        }
    }

    public static class TripIntentionAnalyzer extends AnalyzerBase<TripIntention> {
        @Override
        public TripIntention assignTrip(TapasTrip trip) {
            TripIntention ti = TripIntention.getByActivity(Activity.getById(trip.getActCode()));
            if (trip.isBackHome()) ti = TripIntention.TRIP_37;
            return ti;
        }
    }

    public static class ModeAnalyzer extends AnalyzerBase<Mode> {
        @Override
        public Mode assignTrip(TapasTrip trip) {
            return Mode.getById(trip.getIdMode());
        }
    }

    public static class RegionCodeAnalyzer extends AnalyzerBase<RegionCode> {
        @Override
        public RegionCode assignTrip(TapasTrip trip) {
            return RegionCode.getById(trip.getBbrTypeHome());
        }
    }

    public static class RegionCodeIgnored extends AnalyzerBase<RegionCode> {
        @Override
        public RegionCode assignTrip(TapasTrip trip) {
            return RegionCode.REGION_0;
        }
    }

}
