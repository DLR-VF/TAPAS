/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.model.location;

import de.dlr.ivf.tapas.model.TPS_RegionResultSet;
import de.dlr.ivf.tapas.model.plan.TPS_LocatedStay;
import de.dlr.ivf.tapas.model.plan.TPS_Plan;
import de.dlr.ivf.tapas.model.plan.TPS_PlanningContext;
import de.dlr.ivf.tapas.model.scheme.TPS_Stay;

import java.util.function.Supplier;


public abstract class TPS_LocationSelectModel {

    TPS_Region region = null;

    /**
     * The interface method to select a location from the given choice set
     *
     * @param choiceSet the choice set to scan
     * @return the picked location.
     */

    abstract public TPS_Location selectLocationFromChoiceSet(TPS_RegionResultSet choiceSet, TPS_Plan plan, TPS_PlanningContext pc, TPS_LocatedStay locatedStay, Supplier<TPS_Stay> coming_from, Supplier<TPS_Stay> going_to);

    /**
     * This method sets the needed references to the classes we need
     *
     * @param region The region we are in
     */
    public void setClassReferences(TPS_Region region) {
        this.region = region;
    }
}
