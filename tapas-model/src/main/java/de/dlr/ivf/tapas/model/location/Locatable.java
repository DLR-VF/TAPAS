/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.model.location;

import de.dlr.ivf.tapas.logger.LogHierarchy;
import de.dlr.ivf.tapas.logger.HierarchyLogLevel;

/**
 * Generic interface for all entities which are located in an coordinate system. These entities are numbered by an
 * identifier and have a single coordinate to determine their location.
 *
 * @author mark_ma
 */
@LogHierarchy(hierarchyLogLevel = HierarchyLogLevel.EPISODE)
public interface Locatable {

    TPS_Block getBlock();

    /**
     * @return coordinate
     */
    TPS_Coordinate getCoordinate();

    /**
     * @return identifier of according
     */
    int getTAZId();

    TPS_TrafficAnalysisZone getTrafficAnalysisZone();

    boolean hasBlock();
}
