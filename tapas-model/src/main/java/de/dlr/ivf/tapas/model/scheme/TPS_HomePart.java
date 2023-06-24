/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.model.scheme;

import de.dlr.ivf.tapas.logger.legacy.LogHierarchy;
import de.dlr.ivf.tapas.logger.legacy.HierarchyLogLevel;

/**
 * This class represents a home part. It consists of stays which are all done at home.
 *
 * @author mark_ma
 */
@LogHierarchy(hierarchyLogLevel = HierarchyLogLevel.PLAN)
public class TPS_HomePart extends TPS_SchemePart {

    /**
     * Calls super constructor with 0
     */
    public TPS_HomePart() {
        super(0);
    }

    /*
     * (non-Javadoc)
     *
     * @see de.dlr.ivf.tapas.plan.TPS_SchemePart#isHomePart()
     */
    @Override
    public boolean isHomePart() {
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see de.dlr.ivf.tapas.plan.TPS_SchemePart#isTourPart()
     */
    @Override
    public boolean isTourPart() {
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see de.dlr.ivf.tapas.plan.TPS_SchemePart#toString(java.lang.String)
     */
    @Override
    public String toString(String prefix) {
        StringBuilder sb = new StringBuilder();
        sb.append(prefix + "HomePart [id=" + this.getId() + "]\n");
        for (TPS_Episode episode : this) {
            sb.append(episode.toString(prefix + " ") + "\n");
        }
        sb.setLength(sb.length() - 1);
        return sb.toString();
    }
}
