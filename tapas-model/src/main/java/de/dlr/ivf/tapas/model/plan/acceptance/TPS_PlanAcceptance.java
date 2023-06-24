/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.model.plan.acceptance;

import de.dlr.ivf.tapas.logger.legacy.LogHierarchy;
import de.dlr.ivf.tapas.logger.legacy.HierarchyLogLevel;
import de.dlr.ivf.tapas.model.plan.TPS_Plan;

/**
 * This interface provides one single method to determine whether a plan is accepted or not.
 *
 * @author mark_ma
 */
@LogHierarchy(hierarchyLogLevel = HierarchyLogLevel.PLAN)
public interface TPS_PlanAcceptance {

    /**
     * This method determines whether this plan is accepted or not. Possible values are e.g. the overall travel time of the
     * plan or the financial costs.
     *
     * @param plan plan which is used to determine acceptance
     * @return true if the plan is accepted, false otherwise
     */
    boolean isPlanAccepted(TPS_Plan plan);
}
