/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.environment.gui.graphics.modalsplitchart;

import de.dlr.ivf.tapas.analyzer.tum.constants.CategoryCombination;
import de.dlr.ivf.tapas.analyzer.tum.constants.TuMEnums.Categories;
import de.dlr.ivf.tapas.analyzer.tum.results.CalibrationResultsReader;
import de.dlr.ivf.tapas.environment.gui.graphics.qualitychart.QualityChartData;
import de.dlr.ivf.tapas.environment.gui.graphics.qualitychart.ReferenceDBReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

public class ModalSplitChartDBFetcher {

    /**
     * @param refKey   the key in the <code>reference</code> table (like
     *                 <code>mid2008</code>)
     * @param modelKey the key of the model run in the
     *                 <code>calibration_results</code> table.
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public static ArrayList<ModalSplitData> getModalSplitData(String refKey, String modelKey) throws ClassNotFoundException, IOException {

        CalibrationResultsReader cr = new CalibrationResultsReader(modelKey);
        HashMap<CategoryCombination, Double> model = cr.getAbsoluteSplit(Categories.Mode,
                Categories.DistanceCategoryDefault);

        ReferenceDBReader rr = new ReferenceDBReader(refKey);
        HashMap<CategoryCombination, QualityChartData> reference = rr.getMoDcValues();
        return mergeData(reference, model, cr.getCntTrips(), rr.getCntTrips());
    }

    private static ArrayList<ModalSplitData> mergeData(HashMap<CategoryCombination, QualityChartData> reference, HashMap<CategoryCombination, Double> model, double cntModel, double cntReference) {

        double scale = cntReference / cntModel;

        ArrayList<ModalSplitData> result = new ArrayList<>();

        for (Entry<CategoryCombination, Double> e : model.entrySet()) {
            QualityChartData r = reference.get(e.getKey());

            result.add(new ModalSplitData(e.getKey(), r.getQuality(), e.getValue() * scale, r.getReference()));
        }

        return result;
    }

}
