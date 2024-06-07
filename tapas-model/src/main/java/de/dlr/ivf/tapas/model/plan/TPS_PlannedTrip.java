/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.model.plan;


import de.dlr.ivf.tapas.logger.legacy.LogHierarchy;

import de.dlr.ivf.tapas.logger.legacy.HierarchyLogLevel;

import de.dlr.ivf.tapas.model.mode.TPS_ExtMode;

import de.dlr.ivf.tapas.model.scheme.TPS_Episode;

import de.dlr.ivf.tapas.model.scheme.TPS_Trip;


/**
 * @author cyga_ri
 */
@LogHierarchy(hierarchyLogLevel = HierarchyLogLevel.EPISODE)
public class TPS_PlannedTrip extends TPS_AdaptedEpisode {
    // variable costs of the trip in Euro
    private double costs;
    // mode selected for the trip
    private TPS_ExtMode mode;
    // Reference to the trip of this planned trip
    private final TPS_Trip trip;


    /**
     * This constructor calls the super constructor with the trip and sets the internal reference of the trip.
     *
     * @param plan Reference to the whole plan
     * @param trip Reference to the original trip
     */
    public TPS_PlannedTrip(TPS_Plan plan, TPS_Trip trip) {
        super(plan, trip);
        this.trip = trip;
    }

    /**
     * Returns the variable costs of the trip in the given currency
     *
     * @return cost of the trip
     */
    public double getCosts() {
        return costs;
    }

    public void setCost(double cost){
        this.costs = cost;
    }


    /*
     * (non-Javadoc)
     *
     * @see de.dlr.de.dlr.ivf.plan.tapas.ivf.TPS_AdaptedEpisode#getEpisode()
     */
    @Override
    public TPS_Episode getEpisode() {
        return this.getTrip();
    }

    /**
     * Returns the mode selected for the trip
     *
     * @return the mode
     */
    public TPS_ExtMode getMode() {
        return mode;
    }

    /**
     * Sets the selected mode for the trip
     *
     * @param mode
     */
    public void setMode(TPS_ExtMode mode) {
        this.mode = mode;
    }

    /**
     * @return reference to the corresponding trip
     */
    public TPS_Trip getTrip() {
        return trip;
    }

    /*
     * (non-Javadoc)
     *
     * @see de.dlr.de.dlr.ivf.plan.tapas.ivf.TPS_AdaptedEpisode#isLocatedStay()
     */
    @Override
    public boolean isLocatedStay() {
        return false;
    }

    /**
     * Function identifies the modes used for the previous and subsequent stay and determines the modes that can be used
     * for the current change of location. If a previous and subsequent trip exists, one of the modes is chosen and
     * travel time as well as distance are calculated.
     *
     * @param pComingFrom location coming from
     * @param pGoingTo    location going to
     */
    //todo revise
    public boolean setTravelTime(TPS_LocatedStay pComingFrom, TPS_LocatedStay pGoingTo) {
        return false;
    }


}