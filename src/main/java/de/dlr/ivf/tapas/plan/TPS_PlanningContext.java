/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.plan;

import de.dlr.ivf.tapas.person.TPS_Car;
import de.dlr.ivf.tapas.scheme.TPS_Trip;

public class TPS_PlanningContext {
    // environment
    public TPS_PlanEnvironment pe;

    // plan
    public boolean influenceCarUsageInPlan;
    public TPS_Car carForThisPlan;
    public boolean influenceBikeUsageInPlan;
    public boolean isBikeAvailable;

    public TPS_Trip previousTrip = null;
    public boolean fixLocationAtBase = false;


    //
    public TPS_PlanningContext(TPS_PlanEnvironment _pe, TPS_Car _car, boolean _isBikeAvailable) {
        pe = _pe;
        carForThisPlan = _car;
        influenceCarUsageInPlan = _car != null;
        influenceBikeUsageInPlan = false;
        isBikeAvailable = _isBikeAvailable;
    }


    public boolean needsOtherModeAlternatives(TPS_Plan plan) {
        if (plan.usesCar()) {            //check if we need to rerun this plan with no car
            influenceCarUsageInPlan = true;
            carForThisPlan = null; //forbid car for next run
        } else if (plan.usesBike) {            //check if we need to rerun this plan with no car and no bike
            influenceBikeUsageInPlan = true;
            isBikeAvailable = false; //forbid bike for next run
        } else { //found a plan with no fixed modes!
            return false;
        }
        return true;
    }
}
