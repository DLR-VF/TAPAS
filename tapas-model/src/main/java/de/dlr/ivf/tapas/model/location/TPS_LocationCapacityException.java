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
