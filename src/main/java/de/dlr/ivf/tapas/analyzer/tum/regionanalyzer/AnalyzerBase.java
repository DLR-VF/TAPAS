package de.dlr.ivf.tapas.analyzer.tum.regionanalyzer;

import de.dlr.ivf.tapas.analyzer.inputfileconverter.TapasTrip;
import de.dlr.ivf.tapas.analyzer.tum.constants.TuMEnums.Categories;

import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.EnumMap;

/**
 * This class groups an enumeration <code>C</code> with its according split
 * method that assigns a category (member of <code>C</code>) to a
 * {@link TapasTrip}.
 * <p>
 * After adding trips, you can access distance and numberOfTrips by category.
 * <p>
 * Example: enum DistanceCategory = {1-5km, 5-10km, 10-15km,...} assignTrip
 * (trip) if (trip.getDistance < 5km) return DistanceCategory.1-5km; if
 * (trip.getDistance < 10km) return DistanceCategory.5-10km; ...
 *
 * @param <C>
 * @author boec_pa
 */
public abstract class AnalyzerBase<C extends Enum<C>> {

    private final Class<C> categories;
    private final int numberOfCategories;

    private final EnumMap<C, Double> distMap;
    private final EnumMap<C, Long> tripCntMap;

    @SuppressWarnings("unchecked")
    public AnalyzerBase() {
        // reflection to get class of C
        ParameterizedType parameterizedType = (ParameterizedType) getClass().getGenericSuperclass();
        this.categories = (Class<C>) parameterizedType.getActualTypeArguments()[0];
        numberOfCategories = this.categories.getEnumConstants().length;

        distMap = new EnumMap<>(categories);
        tripCntMap = new EnumMap<>(categories);
        for (C category : this.categories.getEnumConstants()) {
            tripCntMap.put(category, 0L);
        }
    }

    public void addTrip(TapasTrip trip) {
        C category = assignTrip(trip);

        distMap.put(category, trip.getDistNet());
        tripCntMap.put(category, tripCntMap.get(category) + 1);
    }

    public void addTrip(Collection<TapasTrip> trips) {
        for (TapasTrip trip : trips) {
            addTrip(trip);
        }
    }

    /**
     * Returns the category the {@link TapasTrip} trip belongs to.
     *
     * @param trip
     * @return
     */
    public abstract C assignTrip(TapasTrip trip);

    protected C assignTrip(TapasTrip trip, boolean[] filter) throws AnalyzerException {
        C cat = assignTrip(trip);

        if (cat == null) {
            throw new AnalyzerException(trip, categories.getSimpleName());
        }
        if (filter[cat.ordinal()]) return cat;
        else return null;
    }

    public Class<C> getAnalyzerClass() {
        return categories;
    }

    public Categories getCategories() {
        return Categories.getByClass(categories);
    }

    public double getDist(C category) {
        return distMap.get(category);
    }

    public int getNumberOfCategories() {
        return numberOfCategories;
    }

    public long getNumberOfTrips(C category) {
        return tripCntMap.get(category);
    }

    @Override
    public String toString() {
        return categories.getSimpleName();
    }

}
