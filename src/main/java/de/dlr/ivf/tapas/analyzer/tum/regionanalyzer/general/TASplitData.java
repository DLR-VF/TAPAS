/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.analyzer.tum.regionanalyzer.general;

public class TASplitData {
    private double split;
    private long cntTrips;
    private double avgLength;

    public TASplitData(double split, long cntTrips, double avgLength) {
        super();
        this.split = split;
        this.cntTrips = cntTrips;
        this.avgLength = avgLength;
    }

    public double getAvgLength() {
        return avgLength;
    }

    public void setAvgLength(double avgLength) {
        this.avgLength = avgLength;
    }

    public long getCntTrips() {
        return cntTrips;
    }

    public void setCntTrips(long cntTrips) {
        this.cntTrips = cntTrips;
    }

    public double getSplit() {
        return split;
    }

    public void setSplit(double split) {
        this.split = split;
    }


}
