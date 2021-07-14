/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.analyzer.inputfileconverter;

public class InputFileConverterException extends Exception {

    /**
     *
     */
    private static final long serialVersionUID = 693610472233779379L;
    private int intError;

    InputFileConverterException(int intErrNo) {
        intError = intErrNo;
    }

    InputFileConverterException(String strMessage) {
        super(strMessage);
    }

    public String toString() {
        if (intError == 1) {
            return "TripConverterException[MissingTripFiles]";
        } else {
            return "TripConverterException[" + intError + "]";
        }
    }
}
