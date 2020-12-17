/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.runtime.client.Graphics.ModalSplitChart;

import de.dlr.ivf.tapas.analyzer.tum.constants.CategoryCombination;
import de.dlr.ivf.tapas.analyzer.tum.constants.TuMEnums.Mode;
import de.dlr.ivf.tapas.runtime.client.Graphics.QualityChart.Quality;
import org.jfree.data.DomainOrder;
import org.jfree.data.xy.AbstractXYDataset;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;

public class ModalSplitChartDataset extends AbstractXYDataset {

    private static final long serialVersionUID = 7190643396627027541L;

    private final CategoryCombination ccDummy = new CategoryCombination(Mode.BIKE);
    private final EnumMap<Series, ArrayList<ModalSplitData>> seriesCollection = new EnumMap<>(Series.class);

    public ModalSplitChartDataset(ArrayList<ModalSplitData> data) {
        data.sort(Comparator.comparingDouble(ModalSplitData::getReference));

        seriesCollection.put(Series.DATA, data);
        addGEH(data);
        addOptimal(data);
    }

    private void addGEH(ArrayList<ModalSplitData> data) {

        seriesCollection.put(Series.GEH5_HIGH, new ArrayList<>());
        seriesCollection.put(Series.GEH5_LOW, new ArrayList<>());

        for (ModalSplitData d : data) {
            double s = d.getReference();
            double g5p = 0.25 * (4 * s + 5 * Math.sqrt(16 * s + 25) + 25);
            double g5m = 0.25 * (4 * s - 5 * Math.sqrt(16 * s + 25) + 25);

            seriesCollection.get(Series.GEH5_LOW).add(new ModalSplitData(ccDummy, null, g5m, s));
            seriesCollection.get(Series.GEH5_HIGH).add(new ModalSplitData(ccDummy, null, g5p, s));
        }
    }

    private void addOptimal(ArrayList<ModalSplitData> data) {
        seriesCollection.put(Series.OPTIMAL, new ArrayList<>());
        double max = data.get(data.size() - 1).getReference();
        seriesCollection.get(Series.OPTIMAL).add(new ModalSplitData(ccDummy, null, 0, 0));
        seriesCollection.get(Series.OPTIMAL).add(new ModalSplitData(ccDummy, null, max, max));
    }

    public int getDataSeries() {
        return Series.DATA.ordinal();
    }

    @Override
    public DomainOrder getDomainOrder() {
        return DomainOrder.ASCENDING;
    }

    @Override
    public int getItemCount(int series) {
        return seriesCollection.get(Series.getById(series)).size();
    }

    public String getLabel(int item) {
        return seriesCollection.get(Series.DATA).get(item).getLabel();
    }

    public Mode getMode(int item) {
        return seriesCollection.get(Series.DATA).get(item).getMode();
    }

    public Quality getQuality(int item) {
        return seriesCollection.get(Series.DATA).get(item).getQuality();
    }

    @Override
    public int getSeriesCount() {
        return Series.values().length;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Comparable getSeriesKey(int series) {
        return Series.getById(series);
    }

    @Override
    public Number getX(int series, int item) {
        return getXValue(series, item);
    }

    @Override
    public double getXValue(int series, int item) {
        return seriesCollection.get(Series.getById(series)).get(item).getReference();
    }

    @Override
    public Number getY(int series, int item) {
        return getYValue(series, item);
    }

    @Override
    public double getYValue(int series, int item) {
        return seriesCollection.get(Series.getById(series)).get(item).getModel();
    }

    public boolean isDataSeries(int series) {
        return series == Series.DATA.ordinal();
    }

    public enum Series {
        DATA, GEH5_LOW, GEH5_HIGH, OPTIMAL;

        public static Series getById(int series) {
            return Series.values()[series];
        }
    }

}
