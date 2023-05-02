/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.model;

import de.dlr.ivf.tapas.logger.LogHierarchy;
import de.dlr.ivf.tapas.logger.HierarchyLogLevel;
import de.dlr.ivf.tapas.model.distribution.TPS_DiscreteDistribution;
import de.dlr.ivf.tapas.model.mode.TPS_Mode;
import de.dlr.ivf.tapas.model.mode.TPS_ModeChoiceContext;
import de.dlr.ivf.tapas.model.mode.TPS_ModeSet;
import de.dlr.ivf.tapas.model.plan.TPS_Plan;

/**
 * Interface for different utility functions.
 * The utility functions are used to calculate the pivot-point modifications in the scenario case.
 *
 * @author hein_mh
 */
@LogHierarchy(hierarchyLogLevel = HierarchyLogLevel.EPISODE)
public interface TPS_UtilityFunction {

    double walkDistanceBarrier = 3000;
    double walkDistanceShareAfterBarrier = 0.02;
    double walkDistanceBarrierStrong = 5000;
    double walkDistanceShareAfterBarrierStrong = 0.00;
    double minModeProbability = 0; // this is the remaining probability to fetch a "forbidden" mode, e.g. lending a friends car/bike

    //this function calculates the utility factor for the given mode, person and location

    /**
     * This function calculates the deltafactor in the utility values for the given parameters
     *
     * @param mode        The mode to check
     * @param distanceNet The net distance
     * @param plan        The plan we are working on
     * @param mcc
     * @return the delta factor of the utility
     */
    double calculateDelta(TPS_Mode mode, TPS_Plan plan, double distanceNet, TPS_ModeChoiceContext mcc);

    /**
     * * This function gets the distribution set for the utilities of the modes
     *
     * @param modeSet     A link to the calling class to access some data
     * @param plan        the plan we are looking at
     * @param distanceNet the net distance in meters between the locations the
     * @param mcc
     * @return a distributionset, which holds the distribuion of the utilities of the modes
     */
    TPS_DiscreteDistribution<TPS_Mode> getDistributionSet(TPS_ModeSet modeSet, TPS_Plan plan, double distanceNet, TPS_ModeChoiceContext mcc);//TPS_Location locComingFrom, TPS_Location locGoingTo, double startTime, double durationStay, TPS_Car car, boolean fBike, TPS_Stay stay);

    /**
     * Function to set the parameters for the utility function
     *
     * @param mode       the mode we are parametrizing
     * @param parameters the parameters for the mode
     */
    void setParameterSet(TPS_Mode mode, double[] parameters);
}
