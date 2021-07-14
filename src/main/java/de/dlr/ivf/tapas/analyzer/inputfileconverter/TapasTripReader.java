/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.analyzer.inputfileconverter;

import java.util.Iterator;

public interface TapasTripReader {

    void close();

    Iterator<TapasTrip> getIterator();

    /**
     * Returns an estimated progress between <code>0</code> and <code>100</code>
     * .
     */
    int getProgress();

    /**
     * @return a human readable description of the source of the trips.
     */
    String getSource();

    /**
     * @return (estimated) number of elements to be read.
     */
    long getTotal();

}
