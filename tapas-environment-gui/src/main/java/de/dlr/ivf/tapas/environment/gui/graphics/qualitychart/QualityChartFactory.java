/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.environment.gui.graphics.qualitychart;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.XYSeriesLabelGenerator;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;

import de.dlr.ivf.tapas.environment.gui.graphics.qualitychart.QualityChartDataset.Key;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;

/**
 * Factory to create a {@link JFreeChart} for quality assessment of traffic
 * simulations.
 * <p>
 * This class handles the specifics of the {@link JFreeChart} library.
 *
 * @author boec_pa
 */
public class QualityChartFactory {

    private static final String TOOL_TIP_TEMPLATE = "<html><div align=\"center\">%s<br />(%.3f;%.3f)</div></html>";

    /**
     * @param data
     * @return
     */
    public static JFreeChart createChart(ArrayList<QualityChartData> data) {

        // TODO delete EnumSet
        EnumSet<Key> es = EnumSet.allOf(Key.class);
        // es.remove(Key.UPPER_GEH5);
        // es.remove(Key.LOWER_GEH5);

        QualityChartDataset dataset = new QualityChartDataset(data, es);

        JFreeChart chart = ChartFactory.createXYLineChart(null, null, null, dataset, PlotOrientation.HORIZONTAL, true,
                false, false);

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        ((XYPlot) chart.getPlot()).setRenderer(renderer);
        chart.getPlot().setBackgroundPaint(Color.WHITE);

        renderer.setLegendItemLabelGenerator(new QualityChartLegendGerator());
        // High quality data
        int i = dataset.indexOf(Key.DATA_HIGH);
        renderer.setSeriesLinesVisible(i, false);
        renderer.setSeriesShapesVisible(i, true);
        renderer.setSeriesVisibleInLegend(i, true);
        renderer.setSeriesPaint(i, Quality.GOOD.getColor());
        renderer.setSeriesToolTipGenerator(i, new XYToolTipGenerator() {
            @Override
            public String generateToolTip(XYDataset dataset, int series, int item) {
                return String.format(TOOL_TIP_TEMPLATE, ((QualityChartDataset) dataset).getLabel(series, item),
                        dataset.getXValue(series, item), dataset.getYValue(series, item));
            }
        });

        // Medium quality data
        i = dataset.indexOf(Key.DATA_MED);
        renderer.setSeriesLinesVisible(i, false);
        renderer.setSeriesShapesVisible(i, true);
        renderer.setSeriesVisibleInLegend(i, true);
        renderer.setSeriesPaint(i, Quality.MEDIUM.getColor());
        renderer.setSeriesToolTipGenerator(i, new XYToolTipGenerator() {
            @Override
            public String generateToolTip(XYDataset dataset, int series, int item) {
                return String.format(TOOL_TIP_TEMPLATE, ((QualityChartDataset) dataset).getLabel(series, item),
                        dataset.getXValue(series, item), dataset.getYValue(series, item));
            }
        });

        // low quality data
        i = dataset.indexOf(Key.DATA_LOW);
        renderer.setSeriesLinesVisible(i, false);
        renderer.setSeriesShapesVisible(i, true);
        renderer.setSeriesVisibleInLegend(i, true);
        renderer.setSeriesPaint(i, Quality.BAD.getColor());
        renderer.setSeriesToolTipGenerator(i, new XYToolTipGenerator() {
            @Override
            public String generateToolTip(XYDataset dataset, int series, int item) {
                return String.format(TOOL_TIP_TEMPLATE, ((QualityChartDataset) dataset).getLabel(series, item),
                        dataset.getXValue(series, item), dataset.getYValue(series, item));
            }
        });

        // confidence interval
        Stroke dashed = new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{10.0f},
                0.0f);

        i = dataset.indexOf(Key.LOWER_CONFIDENCE);
        renderer.setSeriesLinesVisible(i, true);
        renderer.setSeriesShapesVisible(i, false);
        renderer.setSeriesVisibleInLegend(i, true);
        renderer.setSeriesPaint(i, Color.BLACK);
        renderer.setSeriesStroke(i, dashed);

        i = dataset.indexOf(Key.UPPER_CONFIDENCE);
        renderer.setSeriesLinesVisible(i, true);
        renderer.setSeriesShapesVisible(i, false);
        renderer.setSeriesVisibleInLegend(i, false);
        renderer.setSeriesPaint(i, Color.BLACK);
        renderer.setSeriesStroke(i, dashed);

        // optimal line
        i = dataset.indexOf(Key.OPTIMAL);
        renderer.setSeriesLinesVisible(i, true);
        renderer.setSeriesShapesVisible(i, false);
        renderer.setSeriesVisibleInLegend(i, false);
        renderer.setSeriesPaint(i, Color.BLACK);

        // GEH5
        i = dataset.indexOf(Key.LOWER_GEH5);
        renderer.setSeriesLinesVisible(i, true);
        renderer.setSeriesShapesVisible(i, false);
        renderer.setSeriesVisibleInLegend(i, true);
        renderer.setSeriesPaint(i, Color.BLACK);

        i = dataset.indexOf(QualityChartDataset.Key.UPPER_GEH5);
        renderer.setSeriesLinesVisible(i, true);
        renderer.setSeriesShapesVisible(i, false);
        renderer.setSeriesVisibleInLegend(i, false);
        renderer.setSeriesPaint(i, Color.BLACK);

        return chart;
    }

    /**
     * For testing purposes only
     *
     * @param args
     */
    public static void main(String[] args) {

        ArrayList<QualityChartData> data = new ArrayList<>();

        data.add(new QualityChartData(0.01, 0.01, "MO1 x PG1"));
        // data.add(new QualityChartData(0.02, 0.04, "MO2 x PG1",
        // Quality.GOOD));
        // data.add(new QualityChartData(0.02, 0.03, "MO3 x PG1",
        // Quality.GOOD));
        data.add(new QualityChartData(0.02, 0.04, "MO2 x PG1"));
        data.add(new QualityChartData(0.02, 0.03, "MO3 x PG1"));
        data.add(new QualityChartData(0.025, 0.02, "MO1 x PG2"));
        data.add(new QualityChartData(0.054, 0.06, "MO2 x PG2"));
        data.add(new QualityChartData(0.065, 0.058, "MO3 x PG2"));

        JFreeChart chart = QualityChartFactory.createChart(data);

        // save chart to png
        try {
            ChartUtils.saveChartAsPNG(new File("chartExport.png"), chart, 1000, 1000);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Show Chart
        ChartFrame frame = new ChartFrame("TestChart", chart);
        frame.pack();
        frame.setVisible(true);

    }

    private static class QualityChartLegendGerator implements XYSeriesLabelGenerator {

        @Override
        public String generateLabel(XYDataset dataset, int series) {
            if (!(dataset instanceof QualityChartDataset)) {
                throw new IllegalArgumentException("This dataset is not supported.");
            }

            QualityChartDataset qcd = (QualityChartDataset) dataset;

            Key key = (Key) qcd.getSeriesKey(series);

            if (QualityChartDataset.DATASERIES.contains(key)) {
                return Key.getQuality(key).toString();
            } else if (key == Key.LOWER_CONFIDENCE) {
                return "Confidence Interval";
            } else if (key == Key.LOWER_GEH5) {
                return "GEH5";
            }
            return "";
        }

    }
}
