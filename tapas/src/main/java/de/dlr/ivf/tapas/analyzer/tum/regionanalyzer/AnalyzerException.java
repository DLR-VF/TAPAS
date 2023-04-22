/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.analyzer.tum.regionanalyzer;

import de.dlr.ivf.tapas.analyzer.inputfileconverter.TapasTrip;

public class AnalyzerException extends Exception {

    /**
     *
     */
    private static final long serialVersionUID = 2044139247609629757L;

    private final TapasTrip trip;
    private final String message;

    public AnalyzerException(TapasTrip trip, String message) {
        this.trip = trip;
        this.message = message;
    }

    public AnalyzerException(TapasTrip trip) {
        this.trip = trip;
        this.message = "";

    }

    @Override
    public String getMessage() {
        return message;
    }

    public TapasTrip getTrip() {
        return trip;
    }


}
