/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.environment.gui.legacy.graphics.activitytimechart;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.StandardCategoryToolTipGenerator;
import org.jfree.chart.plot.PlotOrientation;

import java.awt.*;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

/**
 * This class is a factory to create {@link JFreeChart}s showing the activity
 * distribution of a model and a reference.
 *
 * @author boec_pa
 */
public class ActivityTimeChartFactory {
    // TODO add alternative input possibilities

    public static void categorySeriesTest() {
        HashMap<Date, Number> reference = new HashMap<>();
        HashMap<Date, Number> model = new HashMap<>();

        // Create example data
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 2);
        reference.put(cal.getTime(), 0.3);
        model.put(cal.getTime(), 0.2);
        cal.set(Calendar.HOUR_OF_DAY, 5);
        reference.put(cal.getTime(), 0.5);
        model.put(cal.getTime(), 0.6);
        cal.set(Calendar.HOUR_OF_DAY, 8);
        reference.put(cal.getTime(), 0.3);
        model.put(cal.getTime(), 0.1);
        cal.set(Calendar.HOUR_OF_DAY, 12);
        reference.put(cal.getTime(), 0.2);
        model.put(cal.getTime(), 0.1);

        JFreeChart chart = createChart(reference, model);

        // Show Chart
        ChartFrame frame = new ChartFrame("Activity Time Chart", chart);
        frame.pack();
        frame.setVisible(true);

    }

    public static JFreeChart createChart(HashMap<Date, Number> reference, HashMap<Date, Number> model) {

        ActivityTimeDataset dataset = new ActivityTimeDataset(reference, model);

        JFreeChart chart = ChartFactory.createBarChart(null, null, null, dataset, PlotOrientation.VERTICAL, true, false,
                false);

        chart.getPlot().setBackgroundPaint(Color.WHITE);

        chart.getCategoryPlot().getRenderer().setDefaultToolTipGenerator(new StandardCategoryToolTipGenerator());

        return chart;
    }

    /**
     * For testing purposes only
     */
    public static void main(String[] args) {
        categorySeriesTest();

    }
}
