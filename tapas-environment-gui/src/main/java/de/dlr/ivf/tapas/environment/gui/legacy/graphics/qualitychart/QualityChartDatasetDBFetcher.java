/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.environment.gui.legacy.graphics.qualitychart;

import de.dlr.ivf.tapas.analyzer.tum.constants.CategoryCombination;
import de.dlr.ivf.tapas.analyzer.tum.constants.TuMEnums.Categories;
import de.dlr.ivf.tapas.analyzer.tum.results.CalibrationResultsReader;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.title.TextTitle;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

/**
 * This class handles the input from the database (the tables
 * <code>calibration_results</code> and <code>reference</code>) to produce a
 * {@link JFreeChart} with the {@link QualityChartFactory}.
 *
 * @author boec_pa
 */
public class QualityChartDatasetDBFetcher {
    /**
     * For testing purposes only
     *
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public static void main(String[] args) throws ClassNotFoundException, IOException {

        String modelKey = "2013y_03m_07d_16h_43m_41s_859ms";
        String referenceKey = "mid2008";

        JFreeChart chart = makeQSChart(referenceKey, modelKey);

        ChartFrame frame = new ChartFrame("TestChart", chart);
        frame.pack();
        frame.setVisible(true);
    }

    /**
     * @param refKey   the key in the <code>reference</code> table (like
     *                 <code>mid2008</code>)
     * @param modelKey the key of the model run in the
     *                 <code>calibration_results</code> table.
     * @throws ClassNotFoundException
     * @throws IOException
     */
    public static JFreeChart makeQSChart(String refKey, String modelKey) throws ClassNotFoundException, IOException {

        CalibrationResultsReader cr = new CalibrationResultsReader(modelKey);
        HashMap<CategoryCombination, Double> model = cr.getAbsoluteSplit(Categories.Mode,
                Categories.DistanceCategoryDefault);

        ReferenceDBReader rr = new ReferenceDBReader(refKey);
        HashMap<CategoryCombination, QualityChartData> reference = rr.getMoDcValues();

        ArrayList<QualityChartData> data = mergeData(reference, model, cr.getCntTrips(), rr.getCntTrips());

        JFreeChart chart = QualityChartFactory.createChart(data);

        String title = modelKey + " compared to " + refKey;
        Font font = new Font(Font.MONOSPACED, Font.PLAIN, 14);

        chart.setTitle(new TextTitle(title, font));

        return chart;
    }

    /**
     * Takes reference and model and merge them into a list of data points for
     * the {@link QualityChartFactory}. Also scales the model to fit the
     * reference.
     *
     * @param reference
     * @param model
     * @param cntModel     number of trips in the model
     * @param cntReference number of trips in the reference
     * @return
     */
    private static ArrayList<QualityChartData> mergeData(HashMap<CategoryCombination, QualityChartData> reference, HashMap<CategoryCombination, Double> model, double cntModel, double cntReference) {

        /* scale model to fit the number of trips in reference */
        double scale = cntReference / cntModel;

        ArrayList<QualityChartData> data = new ArrayList<>();

        // merge
        for (Entry<CategoryCombination, Double> e : model.entrySet()) {
            QualityChartData r = reference.get(e.getKey());
            data.add(new QualityChartData(e.getValue() * scale, r.getReference(), r.getLabel(), r.getQuality()));
        }

        return data;
    }

}
