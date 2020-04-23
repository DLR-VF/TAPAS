package de.dlr.ivf.tapas.loc;

import de.dlr.ivf.tapas.log.LogHierarchy;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface.HierarchyLogLevel;

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
