/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.model.plan;

import de.dlr.ivf.tapas.logger.LogHierarchy;
import de.dlr.ivf.tapas.logger.HierarchyLogLevel;
import de.dlr.ivf.tapas.model.scheme.TPS_Episode;

/**
 * This is the basic class for an adapted episode. It stores all modified values of an episode in a scheme.
 *
 * @author mark_ma
 */
@LogHierarchy(hierarchyLogLevel = HierarchyLogLevel.PLAN)
public abstract class TPS_AdaptedEpisode {

    protected TPS_Plan plan;
    /**
     * real distance
     */
    private double distance;
    /**
     * beeline distance
     */
    private double distanceBeeline;
    /**
     * net distance
     */
    private double distanceEmptyNet;
    /**
     * duration of the episode
     */
    private int duration;
    /**
     * starting time of the episode in minutes
     */
    private int start;

    /**
     * Builds this instance and calls the TPS_AdaptedEpisode#init Method.
     *
     * @param plan    Reference to the whole plan
     * @param episode Reference to this episode of the plan
     */
    public TPS_AdaptedEpisode(TPS_Plan plan, TPS_Episode episode) {
        this.init(episode);
        this.plan = plan;
    }

    /**
     * @return real distance in meters
     */
    public double getDistance() {
        return distance;
    }

    /**
     * Sets the distance
     *
     * @param distance
     */
    public void setDistance(double distance) {
        this.distance = distance;
    }

    /**
     * @return beeline distance
     */
    public double getDistanceBeeline() {
        return distanceBeeline;
    }

    /**
     * Sets the beeline distance
     *
     * @param distanceBeeline
     */
    public void setDistanceBeeline(double distanceBeeline) {
        this.distanceBeeline = distanceBeeline;
    }

    /**
     * @return net distance
     */
    public double getDistanceEmptyNet() {
        return distanceEmptyNet;
    }

    /**
     * Sets the net distance
     *
     * @param distanceEmptyNet
     */
    public void setDistanceEmptyNet(double distanceEmptyNet) {
        this.distanceEmptyNet = distanceEmptyNet;
    }

    /**
     * @return duration
     */
    public int getDuration() {
        return duration;
    }

    /**
     * Sets the duration
     *
     * @param duration
     */
    public void setDuration(int duration) {
        this.duration = duration;
    }

    /**
     * @return end time
     */
    public int getEnd() {
        return start + duration;
    }

    /**
     * @return episode
     */
    public abstract TPS_Episode getEpisode();

    /**
     * @return start time
     */
    public int getStart() {
        return start;
    }

    /**
     * Sets the start
     *
     * @param start
     */
    public void setStart(int start) {
        this.start = start;
    }

    /**
     * This method initialises the member values by reading the start and duration from the episode.
     *
     * @param episode
     */
    protected void init(TPS_Episode episode) {
        this.setStart(episode.getOriginalStart());
        this.setDuration(episode.getOriginalDuration());
        this.setDistance(0);
        this.setDistanceBeeline(0);
        this.setDistanceEmptyNet(0);
    }

    /**
     * @return true if it is a located stay false otherwise
     */
    public abstract boolean isLocatedStay();

    /**
     * @return true if it is a planned trip false otherwise
     */
    public boolean isPlannedTrip() {
        return !isLocatedStay();
    }
}
