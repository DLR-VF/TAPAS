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
import de.dlr.ivf.tapas.analyzer.tum.constants.TuMEnums.Mode;
import de.dlr.ivf.tapas.environment.gui.graphics.qualitychart.Quality;

public class ModalSplitData {

    private final CategoryCombination categoryCombination;
    private final Quality quality;
    private final double model;
    private final double reference;

    public ModalSplitData(CategoryCombination cc, Quality q, double model, double reference) {

        if (!cc.contains(Categories.Mode)) {
            throw new IllegalArgumentException("The CategoryCombination must contain MODE");
        }

        categoryCombination = new CategoryCombination(cc.getCategories());

        quality = q;
        this.model = model;
        this.reference = reference;
    }

    public CategoryCombination getCategories() {
        return categoryCombination;
    }

    public String getLabel() {
        return categoryCombination.toString();
    }

    /**
     * @return the {@link Mode} of this data point.
     */
    @SuppressWarnings("rawtypes")
    public Mode getMode() {
        for (Enum e : categoryCombination.getCategories()) {
            if (e instanceof Mode) return (Mode) e;
        }
        return null;
    }

    public double getModel() {
        return model;
    }

    public Quality getQuality() {
        return quality;
    }

    public double getReference() {
        return reference;
    }

    @Override
    public String toString() {
        return "[" + getLabel() + ",ref=" + reference + ",mod=" + model + "]";
    }

}
