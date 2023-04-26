/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.analyzer.core;


import de.dlr.ivf.tapas.analyzer.inputfileconverter.*;
import de.dlr.ivf.tapas.analyzer.tum.RegionPOJO;
import de.dlr.ivf.tapas.analyzer.tum.RegionPOJO.ModalSplitForDistanceCategory;
import de.dlr.ivf.tapas.analyzer.tum.RegionPOJO.ModalSplitForDistanceCategory.ModalSplitForDistanceCategoryElement;
import de.dlr.ivf.tapas.analyzer.tum.RegionPOJO.ModalSplitForDistanceCategoryAndTripIntention;
import de.dlr.ivf.tapas.analyzer.tum.RegionPOJO.ModalSplitForDistanceCategoryAndTripIntention.ModalSplitForDistanceCategoryAndTripIntentionElement;
import de.dlr.ivf.tapas.analyzer.tum.RegionPOJO.RegionCode;
import de.dlr.ivf.tapas.analyzer.tum.StatisticPOJO;
import org.apache.commons.collections4.map.MultiKeyMap;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartTheme;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.AbstractDataset;
import org.jfree.data.general.DefaultPieDataset;

import javax.print.attribute.standard.MediaSize.Other;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Factory Class for creating the Graphics {@link JFreeChart}s from given {@link StatisticPOJO}'s
 */

// These warnings are disabled, because MultiKey and results need too much casting...
@SuppressWarnings({"rawtypes", "unchecked"})
public class TapasChartFactory {

    /**
     * is partly used as an additional key
     */
    private static final String OTHER = "other";

    private static int countFalses(boolean[] modalSplitForDistanceCatFilter) {
        int counter = 0;
        for (boolean val : modalSplitForDistanceCatFilter)
            if (!val) counter++;
        return counter;
    }

    /**
     * creates a bar-chart for the given {@link DefaultCategoryDataset}.
     *
     * @param caption the chart title ({@code null} permitted)
     * @param dataset the dataset for the chart ({@code null} permitted).
     * @param theme   chart theme
     * @return the created bar chart object
     */
    private static JFreeChart createChartBar(String caption, DefaultCategoryDataset dataset, String horizontalCaption, String verticalCaption, ChartTheme theme) {
        JFreeChart chart = ChartFactory.createBarChart(caption, horizontalCaption, verticalCaption, dataset,
                PlotOrientation.VERTICAL, true, true, false);
        theme.apply(chart);

        return chart;
    }

    /**
     * creates a pie-chart for the given {@link DefaultPieDataset}
     *
     * @param caption the chart title ({@code null} permitted)
     * @param dataset the dataset for the chart ({@code null} permitted).
     * @param theme   chart theme
     * @return the created pie chart object
     */
    private static JFreeChart createChartPie(String caption, DefaultPieDataset dataset, ChartTheme theme) {

        JFreeChart chart = ChartFactory.createPieChart(caption, // chart title
                dataset, // data
                true, // include legend
                true, false);
        PiePlot plot = (PiePlot) chart.getPlot();
        plot.setLabelGenerator(new StandardPieSectionLabelGenerator("{2}"));
        theme.apply(chart);
        return chart;
    }

    /**
     * creates the modal split separated by region as pie or bar
     *
     * @param statistics {@link StatisticPOJO} to obtain a list of {@link RegionPOJO}s
     * @param asPie      if true creates a DefaultPieDataset, otherwise DefaultCategoryDataset
     * @return a {@link Map} with a regionCode as Key and a {@link JFreeChart} as Value
     */
    public static Map<RegionCode, JFreeChart> createModalSplit(StatisticPOJO statistics, boolean asPie, ChartTheme theme) {
        Map<RegionCode, JFreeChart> map = new HashMap<>();
        // every region
        for (RegionPOJO region : statistics.getAnalyses()) {
            JFreeChart chart = null;
            if (asPie) {
                boolean hasData = false;
                DefaultPieDataset dataset = new DefaultPieDataset();
                // all modes
                for (Mode m : Mode.values()) {
                    double modalSplit = region.getModalSplit(m);
                    if (modalSplit != 0) hasData = true;
                    dataset.setValue(m.getDescription(), modalSplit);
                }
                if (hasData) chart = createChartPie("ModalSplit - " + region.getRegionCode().getDescription(), dataset,
                        theme);
            } else {
                boolean hasData = false;
                DefaultCategoryDataset dataset = new DefaultCategoryDataset();

                // all modes
                for (Mode m : Mode.values()) {
                    double modalSplit = region.getModalSplit(m);
                    if (modalSplit != 0) hasData = true;
                    dataset.addValue(modalSplit, m.getDescription(), region.getRegionCode().getDescription());
                }
                if (hasData) chart = createModalSplitChartBar("ModalSplit - " + region.getRegionCode().getDescription(),
                        dataset, theme);
            }
            if (chart != null) map.put(region.getRegionCode(), chart);
        }
        return map;
    }

    private static JFreeChart createModalSplitChartBar(String caption, DefaultCategoryDataset dataset, ChartTheme theme) {
        return createChartBar(caption, dataset, "Modus", "% der Wege", theme);
    }

    /**
     * Creates the graphics and returns them in a multi key map
     * the keys are {@link RegionCode} and a list of {@link DistanceCategory}
     *
     * @param statistics                     {@link StatisticPOJO} to obtain a list of {@link RegionPOJO}s
     * @param modalSplitForDistanceCatFilter
     * @param asPie                          if true creates a DefaultPieDataset, otherwise DefaultCategoryDataset
     * @param theme                          chart theme
     * @return
     */

    public static MultiKeyMap createModalSplitDistanceCategorySeparated(StatisticPOJO statistics, boolean[] modalSplitForDistanceCatFilter, boolean asPie, ChartTheme theme) {
        MultiKeyMap result = new MultiKeyMap();
        int maxDistanceCategoryGroup = modalSplitForDistanceCatFilter.length - countFalses(
                modalSplitForDistanceCatFilter);
        for (RegionPOJO region : statistics.getAnalyses()) {
            Map<Mode, ModalSplitForDistanceCategory> modalSplits = region.getModalSplitForDistanceCategory(
                    modalSplitForDistanceCatFilter);

            for (int currentDistanceCategoryGroup = 0;
                 currentDistanceCategoryGroup < maxDistanceCategoryGroup; currentDistanceCategoryGroup++) {
                AbstractDataset dataSet = asPie ? new DefaultPieDataset() : new DefaultCategoryDataset();

                String distanceGroupName = null;
                List<DistanceCategory> categories = null;
                for (Mode mode : Mode.values()) {

                    ModalSplitForDistanceCategory modalSplit = modalSplits.get(mode);
                    ModalSplitForDistanceCategoryElement element = modalSplit.getElements().get(
                            currentDistanceCategoryGroup);
                    if (element.getModalSplit() > 0) {
                        if (distanceGroupName == null)
                            distanceGroupName = DistanceCategory.createDistanceCategoriesName(
                                    element.getDistanceCategories());
                        if (categories == null) categories = element.getDistanceCategories();

                        if (asPie) {
                            ((DefaultPieDataset) dataSet).setValue(mode.getDescription(), element.getModalSplit());
                        } else {
                            ((DefaultCategoryDataset) dataSet).addValue(element.getModalSplit(), mode.getDescription(),
                                    region.getRegionCode().getDescription());
                        }
                    }
                }

                JFreeChart chart;
                if (distanceGroupName != null && categories != null) {
                    if (asPie) chart = createChartPie(
                            "Modalsplit - " + region.getRegionCode().getDescription() + " - " + distanceGroupName,
                            (DefaultPieDataset) dataSet, theme);
                    else chart = createModalSplitChartBar(
                            "Modalsplit - " + region.getRegionCode().getDescription() + " - " + distanceGroupName,
                            (DefaultCategoryDataset) dataSet, theme);
                    result.put(region.getRegionCode(), categories, chart);
                }
            }
        }
        return result;
    }

    /**
     * @param statistics                                            {@link StatisticPOJO} to obtain a list of {@link RegionPOJO}s
     * @param modalSplitForDistanceCatAndTripIntentionFilterTripInt
     * @param modalSplitForDistanceCatAndTripIntentionFilterDistCat
     * @param asPie                                                 if true creates a DefaultPieDataset, otherwise DefaultCategoryDataset
     * @param theme                                                 chart theme
     * @return Map mit {@link RegionCode}, {@link DistanceCategory}, {@link TripIntention} als Key. Zusätzlich noch {@link Other} statt {@link TripIntention}
     */
    public static MultiKeyMap createModalSplitTripIntentionDistanceCategorySeparated(StatisticPOJO statistics, boolean[] modalSplitForDistanceCatAndTripIntentionFilterTripInt, boolean[] modalSplitForDistanceCatAndTripIntentionFilterDistCat, boolean asPie, ChartTheme theme) {
        MultiKeyMap result = new MultiKeyMap();
        int maxDistanceCategegoryGroup = modalSplitForDistanceCatAndTripIntentionFilterDistCat.length - countFalses(
                modalSplitForDistanceCatAndTripIntentionFilterDistCat);
        for (RegionPOJO region : statistics.getAnalyses()) {
            Map<Mode, ModalSplitForDistanceCategoryAndTripIntention> modalSplits = region
                    .getModalSplitForDistanceCategoryAndTripIntention(
                            modalSplitForDistanceCatAndTripIntentionFilterDistCat,
                            modalSplitForDistanceCatAndTripIntentionFilterTripInt);

            for (int currentDistanceCategoryGroup = 0;
                 currentDistanceCategoryGroup < maxDistanceCategegoryGroup; currentDistanceCategoryGroup++) {

                String distanceGroupName = null;
                List<DistanceCategory> categories = null;
                for (TripIntention ti : TripIntention.values()) {
                    AbstractDataset dataSet = asPie ? new DefaultPieDataset() : new DefaultCategoryDataset();
                    boolean hasData = false;
                    for (Mode mode : Mode.values()) {

                        ModalSplitForDistanceCategoryAndTripIntention modalSplit = modalSplits.get(mode);
                        ModalSplitForDistanceCategoryAndTripIntentionElement element = modalSplit.getElements().get(
                                currentDistanceCategoryGroup);
                        if (element.containsModalSplitTripIntentionSeparated(ti)) {
                            if (element.getModalSplitTripIntentionSeparated(ti) > 0) hasData = true;

                            if (distanceGroupName == null)
                                distanceGroupName = DistanceCategory.createDistanceCategoriesName(
                                        element.getDistanceCategories());
                            if (categories == null) categories = element.getDistanceCategories();

                            if (asPie) {
                                ((DefaultPieDataset) dataSet).setValue(mode.getDescription(),
                                        element.getModalSplitTripIntentionSeparated(ti));
                            } else {
                                ((DefaultCategoryDataset) dataSet).addValue(
                                        element.getModalSplitTripIntentionSeparated(ti), mode.getDescription(),
                                        region.getRegionCode().getDescription());
                            }
                        }
                    }

                    JFreeChart chart;
                    if (hasData && distanceGroupName != null && categories != null) {
                        if (asPie) chart = createChartPie(
                                "Modalsplit - " + region.getRegionCode().getDescription() + " - " + distanceGroupName +
                                        " - " + ti.getCaption(), (DefaultPieDataset) dataSet, theme);
                        else chart = createModalSplitChartBar(
                                "Modalsplit - " + region.getRegionCode().getDescription() + " - " + distanceGroupName +
                                        " - " + ti.getCaption(), (DefaultCategoryDataset) dataSet, theme);
                        result.put(region.getRegionCode(), categories, ti, chart);
                    }
                }

                // the others
                AbstractDataset dataSet = asPie ? new DefaultPieDataset() : new DefaultCategoryDataset();
                boolean hasData = false;
                for (Mode mode : Mode.values()) {

                    ModalSplitForDistanceCategoryAndTripIntention modalSplit = modalSplits.get(mode);
                    ModalSplitForDistanceCategoryAndTripIntentionElement element = modalSplit.getElements().get(
                            currentDistanceCategoryGroup);
                    if (element.getModalSplitOther() > 0) {
                        hasData = true;
                    }
                    if (distanceGroupName == null) distanceGroupName = DistanceCategory.createDistanceCategoriesName(
                            element.getDistanceCategories());
                    if (categories == null) categories = element.getDistanceCategories();

                    if (asPie) {
                        ((DefaultPieDataset) dataSet).setValue(mode.getDescription(), element.getModalSplitOther());
                    } else {
                        ((DefaultCategoryDataset) dataSet).addValue(element.getModalSplitOther(), mode.getDescription(),
                                region.getRegionCode().getDescription());
                    }
                }
                if (hasData) {
                    JFreeChart chart;
                    if (distanceGroupName != null && categories != null) {
                        if (asPie) chart = createChartPie(
                                "Modalsplit - " + region.getRegionCode().getDescription() + " - " + distanceGroupName +
                                        " - " + "Alle anderen", (DefaultPieDataset) dataSet, theme);
                        else chart = createModalSplitChartBar(
                                "Modalsplit - " + region.getRegionCode().getDescription() + " - " + distanceGroupName +
                                        " - " + "Alle anderen", (DefaultCategoryDataset) dataSet, theme);
                        result.put(region.getRegionCode(), categories, OTHER, chart);
                    }
                }
            }
        }
        return result;
    }

    /**
     * creates the modal split separated by region and {@link TripIntention} as a pie or bar
     *
     * @param statistics {@link StatisticPOJO} to obtain a list of {@link RegionPOJO}s
     * @param asPie      if true creates a DefaultPieDataset, otherwise DefaultCategoryDataset
     * @return a {@link MultiKeyMap} with regionCode and {@link TripIntention} as key and {@link JFreeChart} as value
     */
    public static MultiKeyMap createModalSplitTripIntentionSeparated(StatisticPOJO statistics, boolean asPie, ChartTheme theme) {
        MultiKeyMap map = new MultiKeyMap();
        for (RegionPOJO region : statistics.getAnalyses()) {
            for (TripIntention ti : TripIntention.values()) {
                JFreeChart chart = null;
                if (asPie) {
                    boolean hasData = false;
                    DefaultPieDataset dataSet = new DefaultPieDataset();
                    for (Mode m : Mode.values()) {
                        double modalSplitForTripIntention = region.getModalSplitForTripIntention(m,
                                ti);// 0.6320970029397677+0.2526509565985104+0.3944736125329902+0.3475124681838247
                        if (modalSplitForTripIntention != 0) hasData = true;
                        dataSet.setValue(m.getDescription(), modalSplitForTripIntention);
                    }
                    if (hasData) chart = createChartPie(
                            "Modalsplit - " + region.getRegionCode().getDescription() + " - " + ti.getCaption(),
                            dataSet, theme);
                } else {
                    boolean hasData = false;
                    DefaultCategoryDataset categoryDataset = new DefaultCategoryDataset();
                    for (Mode m : Mode.values()) {
                        double modalSplitForTripIntention = region.getModalSplitForTripIntention(m, ti);
                        if (modalSplitForTripIntention != 0) hasData = true;
                        categoryDataset.addValue(modalSplitForTripIntention, m.getDescription(),
                                region.getRegionCode().getDescription());
                    }
                    if (hasData) chart = createModalSplitChartBar(
                            "Modalsplit - " + region.getRegionCode().getDescription() + " - " + ti.getCaption(),
                            categoryDataset, theme);
                }
                if (chart != null) map.put(region.getRegionCode(), ti, chart);

            }
        }

        return map;
    }

    /**
     * creates the modal split separated by region, {@link TripIntention} and {@link Job} as a pie or bar
     *
     * @param statistics {@link StatisticPOJO} to obtain a list of {@link RegionPOJO}s
     * @param asPie      if true creates a DefaultPieDataset, otherwise DefaultCategoryDataset
     * @return a {@link MultiKeyMap} with regionCode and {@link TripIntention} and {@link Job} as key and {@link JFreeChart} as value
     */
    public static MultiKeyMap createModalSplitTripIntentionTapasPersonGroupSeparated(StatisticPOJO statistics, boolean asPie, ChartTheme theme) {
        MultiKeyMap map = new MultiKeyMap();
        for (RegionPOJO region : statistics.getAnalyses()) {
            for (TripIntention ti : TripIntention.values()) {
                for (Job pg : Job.values()) {
                    JFreeChart chart = null;
                    if (asPie) {
                        boolean hasData = false;
                        DefaultPieDataset categoryDataset = new DefaultPieDataset();
                        for (Mode m : Mode.values()) {
                            double modalSplitForTripIntentionAndPersonGroupTapas = region
                                    .getModalSplitForTripIntentionAndPersonGroupTapas(m, ti, pg);
                            if (modalSplitForTripIntentionAndPersonGroupTapas != 0) hasData = true;
                            categoryDataset.setValue(m.getDescription(), modalSplitForTripIntentionAndPersonGroupTapas);
                        }
                        if (hasData) chart = createChartPie(
                                "Modalsplit - " + region.getRegionCode().getDescription() + " - " + ti.getCaption() +
                                        " - PG:" + pg.getId(), categoryDataset, theme);

                    } else {
                        boolean hasData = false;
                        DefaultCategoryDataset categoryDataset = new DefaultCategoryDataset();
                        for (Mode m : Mode.values()) { // 0.4104, 0.1785, 0.1916, 0.2658, 0.0085, 0.2912, 0.0
                            double modalSplitForTripIntentionAndPersonGroupTapas = region
                                    .getModalSplitForTripIntentionAndPersonGroupTapas(m, ti, pg);
                            if (modalSplitForTripIntentionAndPersonGroupTapas != 0) hasData = true;
                            categoryDataset.addValue(modalSplitForTripIntentionAndPersonGroupTapas, m.getDescription(),
                                    region.getRegionCode().getDescription());
                        }
                        if (hasData) chart = createModalSplitChartBar(
                                "Modalsplit - " + region.getRegionCode().getDescription() + " - " + ti.getCaption() +
                                        " - PG:" + pg.getId(), categoryDataset, theme);
                    }
                    if (chart != null) map.put(region.getRegionCode(), ti, pg, chart);
                }
            }
        }
        return map;
    }

    /**
     * creates the modal split separated by region and {@link PersonGroup} as pie or bar
     *
     * @param statistics {@link StatisticPOJO} to obtain a list of {@link RegionPOJO}s
     * @param asPie      if true creates a DefaultPieDataset, otherwise DefaultCategoryDataset
     * @return a {@link MultiKeyMap} with regionCode and {@link TripIntention} and {@link PersonGroup} as key and {@link JFreeChart} as value
     */
    public static MultiKeyMap createModalSplitTripIntentionVisevaPersonGroupSeparated(StatisticPOJO statistics, boolean asPie, ChartTheme theme) {
        MultiKeyMap map = new MultiKeyMap();
        for (RegionPOJO region : statistics.getAnalyses()) {
            for (TripIntention ti : TripIntention.values()) {
                for (PersonGroup pg : PersonGroup.values()) {
                    JFreeChart chart = null;
                    if (asPie) {
                        boolean hasData = false;
                        DefaultPieDataset categoryDataset = new DefaultPieDataset();
                        for (Mode m : Mode.values()) {
                            double modalSplitForTripIntentionAndPersonGroupViseva = region
                                    .getModalSplitForTripIntentionAndPersonGroupViseva(m, ti, pg);
                            if (modalSplitForTripIntentionAndPersonGroupViseva != 0) hasData = true;
                            categoryDataset.setValue(m.getDescription(),
                                    modalSplitForTripIntentionAndPersonGroupViseva);
                        }
                        if (hasData) chart = createChartPie(
                                "Modalsplit - " + region.getRegionCode().getDescription() + " - " + ti.getCaption() +
                                        " - PG:" + pg.getId(), categoryDataset, theme);

                    } else {
                        boolean hasData = false;
                        DefaultCategoryDataset categoryDataset = new DefaultCategoryDataset();
                        for (Mode m : Mode.values()) {
                            double modalSplitForTripIntentionAndPersonGroupViseva = region
                                    .getModalSplitForTripIntentionAndPersonGroupViseva(m, ti, pg);
                            if (modalSplitForTripIntentionAndPersonGroupViseva != 0) hasData = true;
                            categoryDataset.addValue(modalSplitForTripIntentionAndPersonGroupViseva, m.getDescription(),
                                    region.getRegionCode().getDescription());
                        }
                        if (hasData) chart = createModalSplitChartBar(
                                "Modalsplit - " + region.getRegionCode().getDescription() + " - " + ti.getCaption() +
                                        " - PG:" + pg.getId(), categoryDataset, theme);
                    }
                    if (chart != null) map.put(region.getRegionCode(), ti, pg, chart);
                }
            }
        }
        return map;
    }

    /**
     * @param statistics {@link StatisticPOJO} to obtain a list of {@link RegionPOJO}s
     * @param asPie      if true creates a DefaultPieDataset, otherwise DefaultCategoryDataset
     * @param theme      chart theme
     * @return
     */
    public static Map<RegionCode, JFreeChart> createTripLength(StatisticPOJO statistics, boolean asPie, ChartTheme theme) {
        Map<RegionCode, JFreeChart> map = new HashMap<>();
        // every region
        for (RegionPOJO region : statistics.getAnalyses()) {
            JFreeChart chart = null;
            if (asPie) {
                boolean hasData = false;
                DefaultPieDataset dataset = new DefaultPieDataset();
                // for all modes
                for (DistanceCategory cat : DistanceCategory.values()) {
                    double tripLengthPercentage = region.getPercentageOfTriplengthAllocationForDistanceCategory(cat);
                    if (tripLengthPercentage != 0) {
                        hasData = true;
                        dataset.setValue(cat.getDescription(), tripLengthPercentage);
                    }
                }
                if (hasData) chart = createChartPie("Wegelänge - " + region.getRegionCode().getDescription(), dataset,
                        theme);
            } else {
                boolean hasData = false;
                DefaultCategoryDataset dataset = new DefaultCategoryDataset();

                // every mode
                for (DistanceCategory cat : DistanceCategory.values()) {
                    double tripLengthPercentage = region.getPercentageOfTriplengthAllocationForDistanceCategory(cat);
                    if (tripLengthPercentage != 0) {
                        hasData = true;
                        dataset.addValue(tripLengthPercentage, cat.getDescription(),
                                region.getRegionCode().getDescription());
                    }
                }
                if (hasData) chart = createModalSplitChartBar("Wegelänge - " + region.getRegionCode().getDescription(),
                        dataset, theme);
            }
            if (chart != null) map.put(region.getRegionCode(), chart);
        }
        return map;
    }

    /**
     * @param statistics {@link StatisticPOJO} to obtain a list of {@link RegionPOJO}s
     * @param asPie      if true creates a DefaultPieDataset, otherwise DefaultCategoryDataset
     * @param theme      chart theme
     * @return
     */
    public static MultiKeyMap createTripLengthModeSeparated(StatisticPOJO statistics, boolean asPie, ChartTheme theme) {
        MultiKeyMap map = new MultiKeyMap();
        for (RegionPOJO region : statistics.getAnalyses()) {
            for (Mode m : Mode.values()) {
                JFreeChart chart = null;
                if (asPie) {
                    boolean hasData = false;
                    DefaultPieDataset dataSet = new DefaultPieDataset();
                    for (DistanceCategory cat : DistanceCategory.values()) {
                        double triplengthForTripIntention = region
                                .getPercentageOfTriplengthAllocationForDistanceCategoryAndMode(m, cat);
                        if (triplengthForTripIntention != 0) {
                            hasData = true;
                            dataSet.setValue(cat.getDescription(), triplengthForTripIntention);
                        }
                    }
                    if (hasData) chart = createChartPie(
                            "Wegelänge - " + region.getRegionCode().getDescription() + " - " + m.getDescription(),
                            dataSet, theme);
                } else {
                    boolean hasData = false;
                    DefaultCategoryDataset categoryDataset = new DefaultCategoryDataset();
                    for (DistanceCategory cat : DistanceCategory.values()) {
                        double triplengthForTripIntention = region
                                .getPercentageOfTriplengthAllocationForDistanceCategoryAndMode(m, cat);
                        if (triplengthForTripIntention != 0) {
                            hasData = true;
                            categoryDataset.addValue(triplengthForTripIntention, cat.getDescription(),
                                    region.getRegionCode().getDescription());
                        }
                    }
                    if (hasData) chart = createChartBar(
                            "Wegelänge - " + region.getRegionCode().getDescription() + " - " + m.getDescription(),
                            categoryDataset, "Distanzkategorie", "Anzahl Trips %", theme);
                }
                if (chart != null) map.put(region.getRegionCode(), m, chart);

            }
        }

        return map;
    }

    /**
     * @param statistics {@link StatisticPOJO} to obtain a list of {@link RegionPOJO}s
     * @param asPie      if true creates a DefaultPieDataset, otherwise DefaultCategoryDataset
     * @param theme      chart theme
     * @return
     */
    public static MultiKeyMap createTripLengthTripIntentionSeparated(StatisticPOJO statistics, boolean asPie, ChartTheme theme) {
        MultiKeyMap map = new MultiKeyMap();
        for (RegionPOJO region : statistics.getAnalyses()) {
            for (TripIntention ti : TripIntention.values()) {
                JFreeChart chart = null;
                if (asPie) {
                    boolean hasData = false;
                    DefaultPieDataset dataSet = new DefaultPieDataset();
                    for (DistanceCategory cat : DistanceCategory.values()) {
                        double triplengthForTripIntention = region
                                .getPercentageOfTriplengthAllocationForDistanceCategoryAndTripIntention(ti, cat);
                        if (triplengthForTripIntention != 0) {
                            hasData = true;
                            dataSet.setValue(cat.getDescription(), triplengthForTripIntention);
                        }
                    }
                    if (hasData) chart = createChartPie(
                            "Wegelänge - " + region.getRegionCode().getDescription() + " - " + ti.getCaption(), dataSet,
                            theme);
                } else {
                    boolean hasData = false;
                    DefaultCategoryDataset categoryDataset = new DefaultCategoryDataset();
                    for (DistanceCategory cat : DistanceCategory.values()) {
                        double triplengthForTripIntention = region
                                .getPercentageOfTriplengthAllocationForDistanceCategoryAndTripIntention(ti, cat);
                        if (triplengthForTripIntention != 0) {
                            hasData = true;
                            categoryDataset.addValue(triplengthForTripIntention, cat.getDescription(),
                                    region.getRegionCode().getDescription());
                        }
                    }
                    if (hasData) chart = createChartBar(
                            "Wegelänge - " + region.getRegionCode().getDescription() + " - " + ti.getCaption(),
                            categoryDataset, "Distanzkategorie", "Anzahl Trips %", theme);
                }
                if (chart != null) map.put(region.getRegionCode(), ti, chart);
            }
        }
        return map;
    }
}
