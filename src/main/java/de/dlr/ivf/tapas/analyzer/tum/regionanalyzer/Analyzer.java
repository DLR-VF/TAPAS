package de.dlr.ivf.tapas.analyzer.tum.regionanalyzer;

import de.dlr.ivf.tapas.analyzer.inputfileconverter.TapasTrip;
import de.dlr.ivf.tapas.analyzer.tum.constants.CategoryCombination;
import de.dlr.ivf.tapas.analyzer.tum.constants.TuMEnums.*;
import de.dlr.ivf.tapas.analyzer.tum.regionanalyzer.general.TASplitData;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * This class combines a number of {@link AnalyzerBase} instances. The split
 * will by made by applying every {@link AnalyzerBase} to the trips.
 *
 * @author boec_pa
 */
@SuppressWarnings("rawtypes")
public class Analyzer {

    private final AnalyzerBase[] analyzers;
    private final int numberOfAnalyzers;
    private int idxPersonGroup = -1;

    private final HashMap<CategoryCombination, TripStats> statsMap = new HashMap<>();

    /**
     * The order of the {@link AnalyzerBase} matters if you use
     * {@link Analyzer#getSplit(Enum...)}.
     *
     * @param analyzers
     */
    public Analyzer(AnalyzerBase... analyzers) {
        this.analyzers = analyzers;
        numberOfAnalyzers = analyzers.length;

        // check if person group has statistics
        for (int i = 0; i < numberOfAnalyzers; i++) {
            if (analyzers[i] instanceof PersonGroupAnalyzer) {
                if (((PersonGroupAnalyzer) analyzers[i]).keepsCnt()) {
                    idxPersonGroup = i;
                }
            }
        }
    }

    /**
     * Adds the {@link TapasTrip} to the statistics.
     *
     * @param trip
     */
    public void addTrip(TapasTrip trip) {
        if (trip != null) {    //avoids {@link NullPointerException} from being thrown when
            CategoryCombination cc = assignTrip(trip);
            if (statsMap.containsKey(cc)) statsMap.get(cc).add(trip);
            else statsMap.put(cc, new TripStats(trip));
        }
    }

    private CategoryCombination assignTrip(TapasTrip trip) {
        Enum[] cats = new Enum[numberOfAnalyzers];
        for (int iA = 0; iA < numberOfAnalyzers; iA++) {
            cats[iA] = analyzers[iA].assignTrip(trip);
        }
        return new CategoryCombination(cats);
    }

    /**
     * Returns the {@link Categories} of this {@link Analyzer}.
     */
    public Categories[] getCategories() {
        Categories[] cats = new Categories[analyzers.length];
        for (int i = 0; i < analyzers.length; ++i) {
            cats[i] = analyzers[i].getCategories();
        }
        return cats;
    }

    /**
     * @return -1 if no information is available
     */
    public int getCntPersons(Enum[] e) {
        if (idxPersonGroup == -1) {
            return -1;
        }
        CategoryCombination cc = new CategoryCombination(e);

        return ((PersonGroupAnalyzer) analyzers[idxPersonGroup]).getCntPersons(cc);

    }

    /**
     * Gets the accumulated distance for a combination of categories. An example
     * would be
     * <code>[{@link RegionCode#REGION_0}, {@link PersonGroup#PG_6}]</code>. The
     * order has to be the same as the order at construction of this
     * {@link Analyzer}.
     */
    public double getDistance(Enum[] e) {
        TripStats ts = getTripStats(e);
        if (ts != null) {
            return ts.getDist();
        } else return -1;
    }

    /**
     * Gets the total of trips for a combination of categories. An example would
     * be <code>[{@link RegionCode#REGION_0}, {@link PersonGroup#PG_6}]</code>.
     * The order has to be the same as the order at construction of this
     * {@link Analyzer}.
     */
    public double getDuration(Enum[] e) {
        TripStats ts = getTripStats(e);
        if (ts != null) {
            return ts.getDur();
        } else return -1;
    }

    public int getNumberOfAnalyzers() {
        return numberOfAnalyzers;
    }

    /**
     * <p>
     * Calculates the split along the last {@link Categories} of the analyzer
     * for the given categories.
     * </p>
     * <p>
     * Example: The analyzer is <code>[Pg, Ti, Mo]</code>. A call could be
     * <code>c={ {@link PersonGroup#PG_1}, {@link TripIntention#TRIP_35} }</code>
     * which gives a modal split for these categories.
     * </p>
     *
     * @param c the categories for all but the last category.
     */
    public ArrayList<TASplitData> getSplit(Enum... c) {

        boolean empty = true;
        ArrayList<CategoryCombination> allCat = CategoryCombination.listAllCombinations(getCategories());

        // split category:
        int idxSplit = analyzers.length - 1;
        Categories catSplit = analyzers[idxSplit].getCategories();
        ArrayList<TripStats> stats = new ArrayList<>();
        for (int i = 0; i < catSplit.getEnumeration().getEnumConstants().length; ++i) {
            stats.add(new TripStats(0, 0, 0));
        }

        TripStats total = new TripStats(0, 0, 0);

        for (CategoryCombination cc : allCat) {
            boolean skip = false;
            for (int i = 0; i < c.length; ++i) {
                if (cc.getCategories()[i] != c[i]) {
                    skip = true;
                    break;
                }
            }
            if (skip) {
                continue;
            }
            int curIdx = cc.getCategory(catSplit).ordinal();
            TripStats ts = getTripStats(cc.getCategories());

            stats.get(curIdx).add(ts);
            total.add(ts);
        }

        ArrayList<TASplitData> result = new ArrayList<>();

        for (TripStats ts : stats) {
            if (ts.getCntTrips() == 0) {
                result.add(new TASplitData(0, 0, 0));
            } else {
                result.add(new TASplitData((double) ts.getCntTrips() / total.getCntTrips(), ts.getCntTrips(),
                        ts.getDist() / ts.getCntTrips()));
                empty = false;
            }
        }

        if (empty) return null;
        return result;
    }

    /**
     * Gets the number of trips for a combination of categories. An example
     * would be
     * <code>[{@link RegionCode#REGION_0}, {@link PersonGroup#PG_6}]</code>. The
     * order has to be the same as the order at construction of this
     * {@link Analyzer}.
     */
    public long getTripCnt(Enum[] e) {
        TripStats ts = getTripStats(e);
        if (ts != null) {
            return ts.getCntTrips();
        } else return -1;
    }

    /**
     * Gets the {@link TripStats} for a combination of categories. An example
     * would be
     * <code>[{@link RegionCode#REGION_0}, {@link PersonGroup#PG_6}]</code>. The
     * order has to be the same as the order at construction of this
     * {@link Analyzer}.
     */
    public TripStats getTripStats(Enum[] e) {
        CategoryCombination cc = new CategoryCombination(e);

        if (statsMap.containsKey(cc)) {
            return statsMap.get(cc);
        } else return new TripStats(0, 0, 0);
    }

    public boolean hasPersonGroupStatistics() {
        return idxPersonGroup > -1;
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder("Analyzers:[");
        for (AnalyzerBase a : analyzers) {
            s.append(a.toString()).append(",");
        }
        s.setCharAt(s.length() - 1, ']');// kill last comma and replace with closing bracket
        return s.toString();
    }
}
