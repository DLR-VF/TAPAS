/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.loc;


import de.dlr.ivf.tapas.persistence.TPS_RegionResultSet;
import de.dlr.ivf.tapas.persistence.db.TPS_DB_IOManager;
import de.dlr.ivf.tapas.plan.TPS_LocatedStay;
import de.dlr.ivf.tapas.plan.TPS_Plan;
import de.dlr.ivf.tapas.plan.TPS_PlanningContext;
import de.dlr.ivf.tapas.util.parameters.TPS_ParameterClass;

public abstract class TPS_LocationChoiceSet {

    TPS_DB_IOManager PM = null;
    TPS_Region region = null;

    /**
     * This method checks which traffic analysis zones are reachable and selects one location representant for each zone.
     *
     * @param plan           the plan to use
     * @param pc             planning context
     * @param locatedStay    the located stay we are coming from
     * @param parameterClass parameter container reference
     * @return instance of {@link TPS_RegionResultSet} with all reachable traffic analysis zone and location representants
     */

    abstract public TPS_RegionResultSet getLocationRepresentatives(TPS_Plan plan, TPS_PlanningContext pc, TPS_LocatedStay locatedStay, TPS_ParameterClass parameterClass);

    /**
     * This method sets the needed references to the classes we need
     *
     * @param region The region we are in
     * @param pm     The DB handler to use.
     */
    public void setClassReferences(TPS_Region region, TPS_DB_IOManager pm) {
        this.region = region;
        this.PM = pm;
    }
}
