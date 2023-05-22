/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.analyzer.tum.regionanalyzer;

import de.dlr.ivf.tapas.analyzer.gui.AbstractCoreProcess;
import de.dlr.ivf.tapas.analyzer.inputfileconverter.TapasTrip;
import de.dlr.ivf.tapas.analyzer.tum.constants.TuMEnums.*;
import de.dlr.ivf.tapas.analyzer.tum.results.DatabaseSummaryExport;
import de.dlr.ivf.tapas.model.parameter.TPS_ParameterClass;

import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * <p>
 * This module analyzes a number of {@link TapasTrip}s according to the
 * distribution within different {@link Categories}.<br>
 * After adding all trips it is used as a data module for different exports like
 * {@link LegacyExcelWriter}.
 * </p>
 * <p>
 * It extends the {@link AbstractCoreProcess} to be used as a module in the
 * TapasAnalyzer.
 * </p>
 *
 * @author boec_pa
 */
@SuppressWarnings("rawtypes")
public class RegionAnalyzer extends AbstractCoreProcess {

    public ArrayList<EnumSet<Categories>> allCategoryCombinations;
    // AbstractCoreProcess
    private StyledDocument console;
    private String outputPath;
    private String source = "";
    private final EnumMap<Categories, AnalyzerBase> baseAnalyzers;
    private final HashMap<EnumSet<Categories>, Analyzer> analyzers;
    private final boolean differentiateRegion;
    private final EnumMap<RegionCode, GlobalAnalysis> globalAnalysis = new EnumMap<>(RegionCode.class);

    /**
     * @param distanceCategoryFilter must have the same length as {@link DistanceCategory}.
     *                               <code>true</code> means, that the category is handled
     *                               separately.
     * @param differentiateRegion    <code>true</code> if all occurring regions should be treated
     *                               separately.
     */
    @SuppressWarnings("unchecked")
    public RegionAnalyzer(boolean[] distanceCategoryFilter, boolean differentiateRegion) {

        // this.distanceCategoryFilter = distanceCategoryFilter;
        this.differentiateRegion = differentiateRegion;

        baseAnalyzers = new EnumMap<>(Categories.class);
        baseAnalyzers.put(Categories.RegionCode, new RegionCodeIgnored());

        if (this.differentiateRegion) {
            baseAnalyzers.put(Categories.RegionCode, new RegionCodeAnalyzer());
        }
        baseAnalyzers.put(Categories.PersonGroup, new PersonGroupAnalyzer());

        // baseAnalyzers.put(Categories.DistanceCategory,
        // new DistanceCategoryAnalyzer());
        //
        baseAnalyzers.put(Categories.DistanceCategory,
                new DistanceCategoryAnalyzer(DistanceCategory.createFilteredDistances(distanceCategoryFilter)));

        baseAnalyzers.put(Categories.TripIntention, new TripIntentionAnalyzer());
        baseAnalyzers.put(Categories.Mode, new ModeAnalyzer());

        baseAnalyzers.put(Categories.Job, new TapasPersonGroupAnalyzer());

        this.analyzers = new HashMap<>();

        allCategoryCombinations = createAllCombinations();

        for (EnumSet<Categories> es : allCategoryCombinations) {

            es.add(Categories.RegionCode);

            ArrayList<AnalyzerBase> locAnalyzers = new ArrayList<>();

            for (Categories c : es) {
                locAnalyzers.add(baseAnalyzers.get(c));
            }

            Analyzer tmpAnalyzer = new Analyzer(locAnalyzers.toArray(new AnalyzerBase[0]));

            analyzers.put(es, tmpAnalyzer);
        }

        globalAnalysis.put(RegionCode.REGION_0,
                new GlobalAnalysis(RegionCode.REGION_0, baseAnalyzers.get(Categories.PersonGroup)));

    }

    /**
     * @return <code>true</code> if the source is updated.
     */
    private boolean addSource(String source) {
        if (!Arrays.asList(this.source.split(";")).contains(source)) {
            if (this.source.equals("")) {
                this.source = source;
            } else {
                this.source += ";" + source;
            }
            return true;
        }
        return false;

    }

    /**
     * Creates a list of all {@link Analyzer}s for easy use and readability.
     *
     * @return
     */
    private ArrayList<EnumSet<Categories>> createAllCombinations() {
        ArrayList<EnumSet<Categories>> al = new ArrayList<>();

        al.add(EnumSet.of(Categories.Mode));
        al.add(EnumSet.of(Categories.PersonGroup));
        al.add(EnumSet.of(Categories.Job));
        al.add(EnumSet.of(Categories.DistanceCategory));
        al.add(EnumSet.of(Categories.TripIntention));
        al.add(EnumSet.of(Categories.RegionCode));

        al.add(EnumSet.of(Categories.Mode, Categories.PersonGroup));
        al.add(EnumSet.of(Categories.Mode, Categories.Job));
        al.add(EnumSet.of(Categories.Mode, Categories.DistanceCategory));
        al.add(EnumSet.of(Categories.Mode, Categories.TripIntention));
        al.add(EnumSet.of(Categories.PersonGroup, Categories.DistanceCategory));
        al.add(EnumSet.of(Categories.Job, Categories.DistanceCategory));
        al.add(EnumSet.of(Categories.PersonGroup, Categories.TripIntention));
        al.add(EnumSet.of(Categories.Job, Categories.TripIntention));
        al.add(EnumSet.of(Categories.DistanceCategory, Categories.TripIntention));

        al.add(EnumSet.of(Categories.Mode, Categories.PersonGroup, Categories.DistanceCategory));
        al.add(EnumSet.of(Categories.Mode, Categories.Job, Categories.DistanceCategory));
        al.add(EnumSet.of(Categories.Mode, Categories.PersonGroup, Categories.TripIntention));
        al.add(EnumSet.of(Categories.Mode, Categories.Job, Categories.TripIntention));
        al.add(EnumSet.of(Categories.Mode, Categories.DistanceCategory, Categories.TripIntention));
        al.add(EnumSet.of(Categories.PersonGroup, Categories.DistanceCategory, Categories.TripIntention));
        al.add(EnumSet.of(Categories.Job, Categories.DistanceCategory, Categories.TripIntention));

        al.add(EnumSet
                .of(Categories.Mode, Categories.PersonGroup, Categories.DistanceCategory, Categories.TripIntention));
        al.add(EnumSet.of(Categories.Mode, Categories.Job, Categories.DistanceCategory, Categories.TripIntention));

        return al;
    }

    @Override
    public boolean finish() throws BadLocationException {
        // TODO clean up resources
        // TODO new Excel, (text) and new database export

        if (null != console) {
            console.insertString(console.getLength(),
                    "Region Auswertung beendet. Ergebnisse werden nach " + outputPath + " exportiert\n", null);
            console.insertString(console.getLength(), "Es wurden " + getCntTrips() + " Trips analysiert\n", null);
        }

        LegacyExcelWriter excelWriter;
        try {
            excelWriter = new LegacyExcelWriter(outputPath, this);
            excelWriter.writeStatistics();
            if (null != console) console.insertString(console.getLength(), "Excel export successful.\n", null);
        } catch (IOException e) {
            if (null != console) {
                console.insertString(console.getLength(), "Excel export failed.\n", null);
            } else {
                System.err.println("Excel export failed.\n");
            }
            e.printStackTrace();
        }

        LegacyTextWriter textWriter = new LegacyTextWriter(outputPath);
        textWriter.process(this);

        try {
            DatabaseSummaryExport dbWriter = new DatabaseSummaryExport(this);
            if (dbWriter.writeSummary()) {
                if (null != console) console.insertString(console.getLength(), "Database export successful.\n", null);
            } else {
                if (null != console) console.insertString(console.getLength(), "Database export failed.\n", null);
                else System.err.println("Database export failed.\n");
            }
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
        }

        return true;
    }

    @Override
    public boolean finish(File exportfile) {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Get the average distance for a specific instance (e.g. bike) of a
     * category (e.g. mode)
     *
     * @param category
     * @param instance must be from the category given as parameter
     * @throws IllegalArgumentException if there is no Analyzer considering <code>cat</code>.
     */
    public double getAvgDistByTrip(Categories category, Enum instance) {
        Categories[] categories = {category};
        Enum[] instances = {instance};
        return getAvgDistByTrip(categories, instances);
    }

    /**
     * Get the average distance for a specific combination of instances (e.g.
     * [bike, shopping]) of categories (e.g. [mode, trip intention])
     *
     * @param categories
     * @param instances  must be the same length as <code>categories</code> and every
     *                   element must be from the according category.
     * @throws IllegalArgumentException if the combination of <code>categories</code> is not found.
     */
    public double getAvgDistByTrip(Categories[] categories, Enum[] instances) {
        TripStats ts = getTripStats(categories, instances);
        return saveDiv(ts.getDist(), ts.getCntTrips());
    }

    public Set<EnumSet<Categories>> getCategories() {
        return analyzers.keySet();
    }

    // Access for exporters

    public int getCntPersons(RegionCode region) {
        return globalAnalysis.get(region).getCntPersons();
    }

    public int getCntPersons(PersonGroup pg, RegionCode region) {
        return globalAnalysis.get(region).getCntPersons(pg);
    }

    public int getCntPersons() {
        return getCntPersons(RegionCode.REGION_0);
    }

    public int getCntPersons(PersonGroup pg) {
        return getCntPersons(pg, RegionCode.REGION_0);
    }

    public long getCntTrips(RegionCode region) {
        return globalAnalysis.get(region).getCntTrips();
    }

    public long getCntTrips() {
        return getCntTrips(RegionCode.REGION_0);
    }

    /**
     * Get the number of trips for a specific instance (e.g. bike) of a category
     * (e.g. mode)
     *
     * @param category
     * @param instance must be from the category given as parameter
     * @return
     * @throws IllegalArgumentException if there is no Analyzer considering <code>cat</code>.
     */
    public long getCntTrips(Categories category, Enum instance) {
        Categories[] catArray = {category};
        Enum[] enumArray = {instance};
        return getTripStats(catArray, enumArray).getCntTrips();
    }

    /**
     * Get the number of trips for a specific combination of instances (e.g.
     * [bike, shopping]) of categories (e.g. [mode, trip intention])
     *
     * @param categories
     * @param instances  must be the same length as <code>categories</code> and every
     *                   element must be from the according category.
     * @throws IllegalArgumentException if the combination of <code>categories</code> is not found.
     */
    public long getCntTrips(Categories[] categories, Enum[] instances) {
        return getTripStats(categories, instances).getCntTrips();
    }

    /**
     * Get the accumulated distance for a specific instance (e.g. bike) of a
     * category (e.g. mode)
     *
     * @param category
     * @param instance must be from the category given as parameter
     * @throws IllegalArgumentException if there is no Analyzer considering <code>cat</code>.
     */
    public double getDist(Categories category, Enum instance) {
        Categories[] catArray = {category};
        Enum[] enumArray = {instance};
        return getTripStats(catArray, enumArray).getDist();
    }

    /**
     * Get the accumulated distance for a specific combination of instances
     * (e.g. [bike, shopping]) of categories (e.g. [mode, trip intention])
     *
     * @param categories
     * @param instances  must be the same length as <code>categories</code> and every
     *                   element must be from the according category.
     * @throws IllegalArgumentException if the combination of <code>categories</code> is not found.
     */
    public double getDist(Categories[] categories, Enum[] instances) {
        return getTripStats(categories, instances).getDist();
    }

    /**
     * @return all active distance categories (after filtering).
     */
    public ArrayList<DistanceCategory> getDistanceCategories() {
        return ((DistanceCategoryAnalyzer) baseAnalyzers.get(Categories.DistanceCategory)).getDistanceCategories();
    }

    /**
     * @return the description of a distance category after filtering, e.g.
     * 1-10km.
     */
    public String getDistanceCategoryDescription(DistanceCategory dc) {
        return ((DistanceCategoryAnalyzer) baseAnalyzers.get(Categories.DistanceCategory)).getCategoryName(dc);
    }

    /**
     * Get the accumulated duration for a specific instance (e.g. bike) of a
     * category (e.g. mode)
     *
     * @param category
     * @param instance must be from the category given as parameter
     * @throws IllegalArgumentException if there is no Analyzer considering <code>cat</code>.
     */
    public double getDur(Categories category, Enum instance) {
        Categories[] catArray = {category};
        Enum[] enumArray = {instance};
        return getTripStats(catArray, enumArray).getDur();
    }

    /**
     * Get the accumulated duration for a specific combination of instances
     * (e.g. [bike, shopping]) of categories (e.g. [mode, trip intention])
     *
     * @param categories
     * @param instances  must be the same length as <code>categories</code> and every
     *                   element must be from the according category.
     * @throws IllegalArgumentException if the combination of <code>categories</code> is not found.
     */
    public double getDur(Categories[] categories, Enum[] instances) {
        return getTripStats(categories, instances).getDur();
    }

    public double getMaxDuration(RegionCode region) {
        return globalAnalysis.get(region).getMaxDuration();
    }

    public double getMaxDuration() {
        return getMaxDuration(RegionCode.REGION_0);
    }

    public double getMaxTripLength(RegionCode region) {
        return globalAnalysis.get(region).getMaxTripLength();
    }

    public double getMaxTripLength() {
        return getMaxTripLength(RegionCode.REGION_0);
    }

    public double getMinDuration(RegionCode region) {
        return globalAnalysis.get(region).getMinDuration();
    }

    public double getMinDuration() {
        return getMinDuration(RegionCode.REGION_0);
    }

    public double getMinTripLength(RegionCode region) {
        return globalAnalysis.get(region).getMinTripLength();
    }

    public double getMinTripLength() {
        return getMinTripLength(RegionCode.REGION_0);
    }

    public String getSource() {
        return this.source;
    }

    public double getTotalDuration(RegionCode region) {
        return globalAnalysis.get(region).getTotalDuration();
    }

    public double getTotalDuration() {
        return getTotalDuration(RegionCode.REGION_0);
    }

    public double getTotalTripLength(RegionCode region) {
        return globalAnalysis.get(region).getTotalTripLength();
    }

    // default region access
    public double getTotalTripLength() {
        return getTotalTripLength(RegionCode.REGION_0);
    }

    /**
     * Main access method to get information from {@link Analyzer}.
     *
     * @param cat e.g. [Categories.RegionCode, Categories.TripIntention]
     * @param c   e.g. [RegionCode.REGION_0, TripIntention.TRIP_31]
     * @return {@link TripStats}
     */
    private TripStats getTripStats(Categories[] cat, Enum[] c) {

        EnumSet<Categories> catsEnum = EnumSet.copyOf(Arrays.asList(cat));
        Analyzer a = analyzers.get(catsEnum);
        return a.getTripStats(c);
    }

    /**
     * @param console my be <code>null</code>. Then, only error messages will be
     *                printed in the error stream.
     * @throws BadLocationException
     */
    @Override
    public boolean init(String outputPath, StyledDocument console, boolean clearSources) throws BadLocationException {

        this.console = console;
        this.outputPath = outputPath;

        if (null != console) console.insertString(console.getLength(), "Region Analysis started.\n", null);
        return true;
    }

    @Override
    public boolean init(boolean clearSources) {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isRegionDifferentiated() {
        return differentiateRegion;
    }

    @Override
    public boolean prepare(String source, TapasTrip trip, TPS_ParameterClass parameterClass) throws BadLocationException {
        // TODO error handling invalid trips

        if (addSource(source) && null != console) {
            console.insertString(console.getLength(), "The following source was added: " + source + "\n", null);
        }
        for (Analyzer a : analyzers.values()) {
            a.addTrip(trip);
        }
        updateGlobalStatistics(trip);

        return true;
    }

    /**
     * @return 0.0 if <code>d == 0</code> and <code>n/d</code> else
     */
    private double saveDiv(double n, double d) {
        return d != 0 ? n / d : 0.0;
    }

    /**
     * Covers all statistics that are not specifically for one {@link Analyzer}
     * like total trip length or the number of unique persons.
     *
     * @param tt
     */
    @SuppressWarnings("unchecked")
    private void updateGlobalStatistics(TapasTrip tt) {

        RegionCode region = (RegionCode) baseAnalyzers.get(Categories.RegionCode).assignTrip(tt);

        if (globalAnalysis.containsKey(region)) {
            globalAnalysis.get(region).addTrip(tt);
        } else {
            globalAnalysis.put(region, new GlobalAnalysis(region, baseAnalyzers.get(Categories.PersonGroup)));
        }

        if (differentiateRegion) {
            globalAnalysis.get(RegionCode.REGION_0).addTrip(tt);
        }
    }

}
