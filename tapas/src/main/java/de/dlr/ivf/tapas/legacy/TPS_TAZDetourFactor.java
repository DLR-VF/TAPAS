/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.legacy;

import de.dlr.ivf.tapas.model.TPS_RegionResultSet;
import de.dlr.ivf.tapas.model.plan.TPS_LocatedStay;
import de.dlr.ivf.tapas.model.plan.TPS_Plan;
import de.dlr.ivf.tapas.model.plan.TPS_PlanningContext;
import de.dlr.ivf.tapas.model.scheme.TPS_Stay;

import java.util.function.Supplier;


public class TPS_TAZDetourFactor extends TPS_LocationChoiceSet {

    /**
     * Method to return the locations, which are possible to use for the given activity.
     *
     * @param plan
     * @param pc
     * @param locatedStay
     * @return
     */
    @Override
    public TPS_RegionResultSet getLocationRepresentatives(TPS_Plan plan, TPS_PlanningContext pc, TPS_LocatedStay locatedStay, Supplier<TPS_Stay> coming_from, Supplier<TPS_Stay> going_to) {


		/*
		Idea:		
		Calculate the mode-weighted traveltime from each start/inter and inter/end relation.
		Calculate the detourfactor: TT(start, inter)+TT(inter, end)/TT(start,end)
		Incoorperate a min cap of DETECTION_THRESHOLD < TT(start,end) - (TT(start, inter)+TT(inter, end))
		Add only tazes with a detourfactor less than THRESHOLD
		DOMINANCE: If a Location of LocationClass xy (to be generated) is found no else is incoorperated in the TAZ-Weight-computation (closest Aldi dominates all others)
		*/

        return new TPS_RegionResultSet();
    }




}
