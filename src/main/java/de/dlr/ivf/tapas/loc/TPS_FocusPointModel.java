/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.loc;

import de.dlr.ivf.tapas.constants.TPS_ActivityConstant;
import de.dlr.ivf.tapas.log.TPS_Logger;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface.SeverenceLogLevel;
import de.dlr.ivf.tapas.plan.TPS_Plan;
import de.dlr.ivf.tapas.scheme.TPS_Stay;

public class TPS_FocusPointModel {

    static boolean STATUS = true;

    TPS_Plan plan;

    /**
     * Default constructor with a plan reference.
     *
     * @param plan the plan, which holds person, household and other information
     */
    public TPS_FocusPointModel(TPS_Plan plan) {
        this.plan = plan;
    }

    /**
     * Function to select the appropriate focus point
     *
     * @param from the previous stay
     * @param to   the next stay
     * @param code the activity code
     * @return the reference to the TPS_Stay, which is the focus point.
     */
    public TPS_Stay getFocusPoint(TPS_Stay from, TPS_Stay to, TPS_ActivityConstant code) {
        if (STATUS) {
            if (TPS_Logger.isLogging(
                    SeverenceLogLevel.DEBUG)) { // TODO: what's this double-check stuff? Is STATUS needed?
                TPS_Logger.log(SeverenceLogLevel.DEBUG, "You want me to do the Fancy Chicken?");
            }
        }

        if (from.getActCode().equals(
                code)) { //if the activity at the last location is the same e.g. shopping than this is the focus point
            return from;
        } else if (to.isAtHome()) { //if the destination is the home location, this is the focus point
            return to;
        } else if (plan.getPerson().getWorkLocationID() == plan.getLocatedStay(to).getLocation().getId()) { // if the to
            // destination is the primary location (work) this is the focus point
            return to;
        }

        if (STATUS) {
            STATUS = false;
            if (TPS_Logger.isLogging(SeverenceLogLevel.DEBUG)) {
                TPS_Logger.log(SeverenceLogLevel.DEBUG, "Lets do the Fancy Chicken!");
            }
        }

        return from;
    }

}
