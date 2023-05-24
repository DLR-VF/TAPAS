/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.environment.gui.legacy.graphics.qualitychart;

/**
 * This immutable object to collect a model and reference value, a label for
 * this data point and its {@link Quality}. {@link Quality} is assigned by
 * relative error of the data or provided on creation.
 *
 * @author boec_pa
 */
public class QualityChartData {

    /**
     * threshold in percent.
     */
    private final static double lowThreshold = 0.2;
    /**
     * threshold in percent.
     */
    private final static double highThreshold = 0.1;

    private final double model;
    private final double reference;
    private final String label;
    private final Quality quality;

    /**
     * The {@link Quality} is set according to the relative error
     * <ul>
     * <li> {@link Quality#GOOD} if <code>abs(ref-mod)/ref| &lt; 0.1</code></li>
     * <li> {@link Quality#BAD} if <code>abs(ref-mod)/ref| &gt; 0.2</code></li>
     * <li> {@link Quality#MEDIUM} else</li>
     * </ul>
     * <code>ref=0</code> results in {@link Quality#MEDIUM}.
     *
     * @param model
     * @param reference
     * @param label
     */
    public QualityChartData(double model, double reference, String label) {
        Quality q = Quality.MEDIUM;
        if (Math.abs(model - reference) / reference < highThreshold) q = Quality.GOOD;
        else if (Math.abs(model - reference) / reference > lowThreshold) q = Quality.BAD;

        // Quality by GEH
        // double geh = Math.sqrt(2 * (model - reference) * (model - reference)
        // / (model + reference));
        //
        // if (geh < 5)
        // q = Quality.GOOD;
        // else if (geh > 10)
        // q = Quality.BAD;

        // TODO change structure too avoid redundant code?
        this.model = model;
        this.reference = reference;
        this.label = label;
        this.quality = q;
    }

    public QualityChartData(double model, double reference, String label, Quality quality) {
        this.model = model;
        this.reference = reference;
        this.label = label;
        this.quality = quality;
    }

    /**
     * If the relative error of model and reference is smaller than
     * <code>highThreshold</code>, then it is considered {@link Quality#GOOD} .
     */
    public static double getHighThreshold() {
        return highThreshold;
    }

    /**
     * If the relative error of model and reference is bigger than
     * <code>lowThreshold</code>, then it is considered {@link Quality#BAD} .
     */
    public static double getLowThreshold() {
        return lowThreshold;
    }

    public String getLabel() {
        return label;
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
        return "(m=" + model + ",r=" + reference + ",'" + label + "'," + quality + ")";
    }
}
