/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.tools.matrixMap;

/**
 * @author Holger Siedel
 */
public enum Terrain {
    PLAIN(0.1), DOWNS(0.1), MOUNTAINOUS(0.1), CITY(0.15), STOPS(0.2);

    private final double correctionTerrain;

    Terrain(double correctionTerrain) {
        this.correctionTerrain = correctionTerrain;
    }

    public double getValue() {
        return this.correctionTerrain;
    }
}
