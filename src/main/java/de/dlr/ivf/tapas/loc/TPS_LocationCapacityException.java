package de.dlr.ivf.tapas.loc;

import de.dlr.ivf.tapas.log.LogHierarchy;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface.HierarchyLogLevel;

/**
 * This exception is used if there occurs an error in the capacity utilization.
 *
 * @author mark_ma
 */
@LogHierarchy(hierarchyLogLevel = HierarchyLogLevel.EPISODE)
public class TPS_LocationCapacityException extends Exception {

    /**
     * Serial version id
     */
    private static final long serialVersionUID = -956841802785324114L;

    /**
     * Calls super constructor with the given message
     *
     * @param msg
     */
    public TPS_LocationCapacityException(String msg) {
        super(msg);
    }

}
