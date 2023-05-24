/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.environment.gui.legacy.graphics.modalsplitchart;

import de.dlr.ivf.tapas.analyzer.tum.constants.TuMEnums.Categories;
import de.dlr.ivf.tapas.analyzer.tum.constants.TuMEnums.DistanceCategoryDefault;
import de.dlr.ivf.tapas.analyzer.tum.constants.TuMEnums.Mode;
import de.dlr.ivf.tapas.environment.gui.legacy.graphics.qualitychart.Quality;
import de.dlr.ivf.tapas.environment.gui.legacy.util.MultilanguageSupport;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.LegendItemSource;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.DefaultDrawingSupplier;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.DefaultXYItemRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.data.xy.XYDataset;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;

/**
 * This is a {@link JFreeChart} to compare the generated modal split with a
 * reference.
 *
 * @author boec_pa
 */
public class ModalSplitChart extends JFreeChart {

    private static final long serialVersionUID = 168088681357544692L;
    private EnumMap<Mode, Shape> shapeMap;
    private final HashSet<Integer> selected = new HashSet<>();
    private ArrayList<String> toolTips;
    private ArrayList<String> toolTipsExport;

    /**
     * @param data  the datapoints containing reference and model.
     * @param title the title of the chart.
     */
    public ModalSplitChart(ArrayList<ModalSplitData> data, String title) {
        super(title, DEFAULT_TITLE_FONT,
                new XYPlot(new ModalSplitChartDataset(data), new NumberAxis(), new NumberAxis(),
                        new DefaultXYItemRenderer()), false);
        XYPlot plot = (XYPlot) getPlot();

        MultilanguageSupport.init(ModalSplitChartFrame.class);
        //Reference
        String xAxisLabel = MultilanguageSupport.getString("AXIS_REFERENCE");
        //Model
        String yAxisLabel = MultilanguageSupport.getString("AXIS_MODEL");

        plot.getDomainAxis().setLabel(xAxisLabel);
        plot.getRangeAxis().setLabel(yAxisLabel);

        createShapeMap();
        createToolTips(data);

        plot.setRenderer(new ModalSplitRenderer((ModalSplitChartDataset) plot.getDataset()));

        LegendTitle modalLegend = new LegendTitle(new ModalShapeLegendSource());
        LegendTitle qualityLegend = new LegendTitle(new QualityLegendSource());
        modalLegend.setPosition(RectangleEdge.BOTTOM);
        qualityLegend.setPosition(RectangleEdge.BOTTOM);

        addLegend(modalLegend);
        addLegend(qualityLegend);

    }

    /**
     * Builds a chart from the database.
     *
     * @param reference the key to reference (like <code>mid2008</code>) in
     *                  <code>reference</code>
     * @param model     the key of the model run in the
     *                  <code>calibration_results</code> table.
     * @throws ClassNotFoundException
     * @throws IOException            when database connections go wrong.
     */
    public ModalSplitChart(String reference, String model) throws ClassNotFoundException, IOException {

        this(ModalSplitChartDBFetcher.getModalSplitData(reference, model), reference + " vs " + model);
    }

    /**
     * Deselects all items.
     */
    public void clearSelection() {
        selected.clear();
    }

    private void createShapeMap() {
        shapeMap = new EnumMap<>(Mode.class);
        DefaultDrawingSupplier drawingSupplier = new DefaultDrawingSupplier();

        for (Mode m : Mode.values()) {
            shapeMap.put(m, drawingSupplier.getNextShape());
        }
    }

    private void createToolTips(ArrayList<ModalSplitData> data) {
        // TODO @PB add multilanguage
        toolTips = new ArrayList<>(data.size());
        toolTipsExport = new ArrayList<>(data.size());
        String toolTipTemplate = "<html><div align=\"center\">%s</div>" + "<table>" //
                + "<tr><td>" + MultilanguageSupport.getString("TOOLTIP_REFERENCE") +
                " =</td><td align=\"right\"> %.0f</td></tr>" //
                + "<tr><td>" + MultilanguageSupport.getString("TOOLTIP_MODEL") +
                " =</td><td align=\"right\"> %.0f</td></tr>" //
                + "<tr><td>" + MultilanguageSupport.getString("TOOLTIP_DEVIATION") +
                " =</td><td align=\"right\"> %.2f%%</td></tr>"//
                + "<tr><td>" + MultilanguageSupport.getString("TOOLTIP_DEVIATION") +
                " '%s' =</td><td align=\"right\"> %.2f%%</td></tr>"//
                + "<tr><td>" + MultilanguageSupport.getString("TOOLTIP_DEVIATION") +
                " '%s' =</td><td align=\"right\"> %.2f%%</td></tr></html>";

        String toolTipExportTemplate = "%s\n"//
                + MultilanguageSupport.getString("TOOLTIP_REFERENCE") + " =\t%.0f\n" //
                + MultilanguageSupport.getString("TOOLTIP_MODEL") + " =\t%.0f\n" //
                + MultilanguageSupport.getString("TOOLTIP_DEVIATION") + " =\t%.2f%%\n"//
                + MultilanguageSupport.getString("TOOLTIP_DEVIATION") + " '%s' =\t%.2f%%\n"//
                + MultilanguageSupport.getString("TOOLTIP_DEVIATION") + " '%s' =\t%.2f%%";

        double[][] modeValues = new double[Mode.values().length][2];
        double[][] dcValues = new double[DistanceCategoryDefault.values().length][2];

        // collect data
        for (ModalSplitData d : data) {
            DistanceCategoryDefault dc = (DistanceCategoryDefault) d.getCategories().getCategory(
                    Categories.DistanceCategoryDefault);

            Mode mo = d.getMode();

            modeValues[mo.ordinal()][0] += d.getReference();
            modeValues[mo.ordinal()][1] += d.getModel();

            dcValues[dc.ordinal()][0] += d.getReference();
            dcValues[dc.ordinal()][1] += d.getModel();
        }

        // build tooltips
        for (ModalSplitData d : data) {
            DistanceCategoryDefault dc = (DistanceCategoryDefault) d.getCategories().getCategory(
                    Categories.DistanceCategoryDefault);

            Mode mo = d.getMode();
            toolTips.add(String.format(toolTipTemplate, d.getLabel(), d.getReference(),//
                    d.getModel(),//
                    Math.abs(100 - d.getModel() / d.getReference() * 100),//
                    mo.getDescription(),//
                    Math.abs(100 - modeValues[mo.ordinal()][1] / modeValues[mo.ordinal()][0] * 100),//
                    dc.getDescription(),//
                    Math.abs(100 - dcValues[dc.ordinal()][1] / dcValues[dc.ordinal()][0] * 100)//
            ));
            toolTipsExport.add(String.format(toolTipExportTemplate, d.getLabel(), d.getReference(),//
                    d.getModel(),//
                    Math.abs(100 - d.getModel() / d.getReference() * 100),//
                    mo.getDescription(),//
                    Math.abs(100 - modeValues[mo.ordinal()][1] / modeValues[mo.ordinal()][0] * 100),//
                    dc.getDescription(),//
                    Math.abs(100 - dcValues[dc.ordinal()][1] / dcValues[dc.ordinal()][0] * 100)//
            ));
        }

    }

    /**
     * Exports the tooltips of all selected items
     */
    public String exportSelected() {
        StringBuilder sb = new StringBuilder();

        for (int i : selected) {
            sb.append(toolTipsExport.get(i)).append("\n\n");
        }

        return sb.toString();
    }

    /**
     * Adds or removes the given item to/from the selection.
     *
     * @param item
     */
    public void triggerSelect(int item) {
        if (selected.contains(item)) selected.remove(item);
        else selected.add(item);
    }

    /**
     * Class to create legend items for the different modes.
     */
    private class ModalShapeLegendSource implements LegendItemSource {

        private final LegendItemCollection itemCollection = new LegendItemCollection();

        public ModalShapeLegendSource() {
            for (Mode m : Mode.values()) {
                itemCollection.add(new LegendItem(m.getDescription(), null, null, null, shapeMap.get(m), Color.BLACK));
            }

        }

        @Override
        public LegendItemCollection getLegendItems() {
            return itemCollection;
        }
    }

    /**
     * Class to create legend items for different quality levels
     */
    private class QualityLegendSource implements LegendItemSource {
        // TODO @PB quality legend multilanguage support
        private final LegendItemCollection itemCollection = new LegendItemCollection();

        public QualityLegendSource() {
            for (Quality q : Quality.values()) {
                itemCollection.add(new LegendItem(q.toString(), q.getColor()));
            }
        }

        @Override
        public LegendItemCollection getLegendItems() {
            return itemCollection;
        }
    }

    /**
     * This class plots boxes the options for plotting the scatter plot and
     * additional lines.
     */
    private class ModalSplitRenderer extends DefaultXYItemRenderer {

        private static final long serialVersionUID = 1600719877317415532L;
        private final ModalSplitChartDataset dataset;

        public ModalSplitRenderer(ModalSplitChartDataset dataset) {
            super();
            this.dataset = dataset;

            setDefaultToolTipGenerator(new ModalSplitToolTipGenerator());

            setUseOutlinePaint(true);
            for (int i = 0; i < dataset.getSeriesCount(); ++i) {
                if (dataset.isDataSeries(i)) {
                    setSeriesLinesVisible(i, false);
                    setSeriesShapesVisible(i, true);
                } else {
                    setSeriesLinesVisible(i, true);
                    setSeriesShapesVisible(i, false);
                    setSeriesPaint(i, Color.black);
                }
            }

        }

        @Override
        public Paint getItemOutlinePaint(int row, int column) {
            if (dataset.isDataSeries(row) && selected.contains(column)) {
                return Color.BLACK;
            } else {
                return getItemPaint(row, column);
            }
        }

        @Override
        public Paint getItemPaint(int row, int column) {
            if (!dataset.isDataSeries(row)) return Color.BLACK;
            else return dataset.getQuality(column).getColor();
        }

        @Override
        public Shape getItemShape(int row, int column) {
            if (!dataset.isDataSeries(row)) return null;

            Mode m = dataset.getMode(column);
            if (null == m) return null;

            return shapeMap.get(m);
        }
    }

    /**
     * This class handles the tool tip generation for the data points.
     */
    private class ModalSplitToolTipGenerator implements XYToolTipGenerator {
        @Override
        public String generateToolTip(XYDataset dataset, int series, int item) {
            ModalSplitChartDataset data = (ModalSplitChartDataset) dataset;
            if (data.isDataSeries(series)) {
                return toolTips.get(item);
            }

            return null;
        }
    }
}
