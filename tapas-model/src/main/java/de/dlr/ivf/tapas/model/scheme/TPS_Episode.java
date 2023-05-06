/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.model.scheme;

import de.dlr.ivf.tapas.model.constants.TPS_ActivityConstant;
import de.dlr.ivf.tapas.model.constants.TPS_ActivityConstant.TPS_ActivityCodeType;
import de.dlr.ivf.tapas.logger.LogHierarchy;
import de.dlr.ivf.tapas.logger.HierarchyLogLevel;

/**
 * This is the basic class for stays and trips. It stores the common member variables of both types of episodes and is extended by TPS_Stay and TPS_Trip
 *
 * @author mark_ma
 */
@LogHierarchy(hierarchyLogLevel = HierarchyLogLevel.PLAN)
public abstract class TPS_Episode {
    private double scaleShift;
    private double scaleStretch;
    /// Variable which determines, if this part is a HomePart
    public boolean isHomePart;
    /// tour number
    public int tourNumber;
    /// id of the episode
    private int id;
    /// activity code of the episode
    private TPS_ActivityConstant actCode;
    /// shift parameter for balancing episode starts: earlier
    private double alphaEarlier;
    /// shift parameter for balancing episode starts: later
    private double alphaLater;
    /// stretch parameter for balancing episode durations: longer
    private double betaLonger;
    /// stretch parameter for balancing episode durations: shorter
    private double betaShorter;
    /// allowed longer duration of the episode in seconds
    private double longerDuration;
    /// original reported duration of the episode in seconds
    private int originalDuration;
    /// original reported start time of the episode in minutes
    private int originalStart;
    /// Each episode is part of a scheme part, this member is the link to the corresponding scheme part
    private TPS_SchemePart schemePart;
    /// allowed shorter duration of the episode in seconds
    private double shorterDuration;
    /// allowed earlier start of the episode in seconds
    private double startEarlier;
    /// allowed later start of the episode in seconds
    private double startLater;

    public TPS_Episode(int id, TPS_ActivityConstant actCode, int start, int duration){}

    /**
     * Constructor
     *
     * @param id              the id for this episode
     * @param actCode         the activity code according to the documentation
     * @param start           the original start for this episode in seconds
     * @param duration        the original duration for this episode in seconds
     * @param startEarlier    the allowed earlier start for this episode in seconds
     * @param startLater      the allowed later start for this episode in seconds
     * @param shorterDuration the allowed shorter duration of this episode in seconds
     * @param longerDuration  the allowed longer duration of this episode in seconds
     */
    public TPS_Episode(int id, TPS_ActivityConstant actCode, int start, int duration, double startEarlier, double startLater,
                       double shorterDuration, double longerDuration,double scaleShift, double scaleStretch) {
        this.id = id;
        this.actCode = actCode;
        this.originalStart = start;
        this.originalDuration = duration;
        this.startEarlier = startEarlier;
        this.startLater = startLater;
        this.alphaEarlier = 1.0;
        this.alphaLater = 1.0;
        this.shorterDuration = shorterDuration;
        this.longerDuration = longerDuration;
        this.betaShorter = 1.0;
        this.betaLonger = (1.0);

        this.scaleShift = scaleShift;
        this.scaleStretch = scaleStretch;

        // Calculation of the alpha and beta values according to

        double eps = 1; // one second
        // alphaEarlier and alphaLater
        double delta = start - startEarlier;
        if (delta < eps) {
            delta = eps;
        }
        delta *= scaleShift;
        alphaEarlier = 1.0 / (delta * delta);

        delta = startLater - start;
        if (delta < eps) {
            delta = eps;
        }
        delta *= scaleShift;
        alphaLater = 1.0 / (delta * delta);

        // betaMinus and betaPlus
        delta = duration - shorterDuration;
        if (delta < eps) {
            delta = eps;
        }
        delta *= scaleStretch;
        betaShorter = 1.0 / (delta * delta);

        delta = longerDuration - duration;
        if (delta < eps) {
            delta = eps;
        }
        delta *= scaleStretch;
        betaLonger = 1.0 / (delta * delta);
    }

    /**
     * Returns the activity code of the episode
     *
     * @return activity code
     */
    public TPS_ActivityConstant getActCode() {
        return actCode;
    }

    /**
     * Setter for the activity code
     *
     * @param actCode The activity code
     */
    public void setActCode(TPS_ActivityConstant actCode) {
        this.actCode = actCode;
    }

    /**
     * @return alpha earlier
     */
    @Deprecated
    public double getAlphaEarlier() {
        return alphaEarlier;
    }

    /**
     * Set the alpha earlier factor. Not used anymore
     *
     * @param alphaEarlier
     */
    @Deprecated
    public void setAlphaEarlier(double alphaEarlier) {
        this.alphaEarlier = alphaEarlier;
    }

    /**
     * @return alpha later
     */
    @Deprecated
    public double getAlphaLater() {
        return alphaLater;
    }

    @Deprecated
    public void setAlphaLater(double alphaLater) {
        this.alphaLater = alphaLater;
    }

    /**
     * @return beta longer
     */
    @Deprecated
    public double getBetaLonger() {
        return betaLonger;
    }

    @Deprecated
    public void setBetaLonger(double betaLonger) {
        this.betaLonger = betaLonger;
    }

    /**
     * @return beta shorter
     */
    @Deprecated
    public double getBetaShorter() {
        return betaShorter;
    }

    @Deprecated
    public void setBetaShorter(double betaShorter) {
        this.betaShorter = betaShorter;
    }

    /**
     * Returns the id of the episode
     *
     * @return the episodes' id
     */
    public int getId() {
        return id;
    }

    /**
     * @return Get the longer allowed duration
     */
    @Deprecated
    public double getLongerDuration() {
        return longerDuration;
    }

    @Deprecated
    public void setLongerDuration(double longerDuration) {
        this.longerDuration = longerDuration;
    }

    /**
     * @return Get the original duration in minutes after midnight
     */
    public int getOriginalDuration() {
        return originalDuration;
    }

    /**
     * Method to set the original duration in minutes after midnight
     *
     * @param originalDuration
     */
    public void setOriginalDuration(int originalDuration) {
        this.originalDuration = originalDuration;
    }

    /**
     * @return Get the original end in minutes after midnight
     */
    public int getOriginalEnd() {
        return (originalStart + originalDuration );
    }

    /**
     * @return Get the original start in minutes after midnight
     */
    public int getOriginalStart() {
        return originalStart;
    }

    /**
     * Method to set the original start in minutes after midnight
     *
     * @param originalStart
     */
    public void setOriginalStart(int originalStart) {
        this.originalStart = originalStart;
    }

    /**
     * @return Get the associated TPS_SchemePart
     */
    public TPS_SchemePart getSchemePart() {
        return schemePart;
    }

    /**
     * Sets the scheme part for this episode
     *
     * @param schemePart
     */
    void setSchemePart(TPS_SchemePart schemePart) {
        this.schemePart = schemePart;
    }

    /**
     * @return Get the shortest allowed duration
     */
    @Deprecated
    public double getShorterDuration() {
        return shorterDuration;
    }

    @Deprecated
    public void setShorterDuration(double shorterDuration) {
        this.shorterDuration = shorterDuration;
    }

    /**
     * @return Get the earliest possible start
     */
    @Deprecated
    public double getStartEarlier() {
        return startEarlier;
    }

    @Deprecated
    public void setStartEarlier(double startEarlier) {
        this.startEarlier = startEarlier;
    }

    /**
     * @return Get the latest possible start
     */
    @Deprecated
    public double getStartLater() {
        return startLater;
    }

    @Deprecated
    public void setStartLater(double startLater) {
        this.startLater = startLater;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return id;
    }

    /**
     * Comparing two episodes
     *
     * @param episode The episode to compare
     * @return true if this original duration is longer than the episode's one
     */
    public boolean isLonger(TPS_Episode episode) {
        return this.getOriginalDuration() > episode.getOriginalDuration();
    }

    /**
     * @return true if this is a TPS_Stay
     */
    public abstract boolean isStay();

    /**
     * @return true if this is a TPS_Trip
     */
    public boolean isTrip() {
        return !this.isStay();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return this.toString("");
    }

    /*
     * (non-Javadoc)
     *
     * @see de.dlr.de.dlr.ivf.util.tapas.ivf.ExtendedWritable#toString(java.lang.String)
     */
    public String toString(String prefix) {
        return prefix + this.getClass().getSimpleName().substring(4) + " [id=" + this.getId() + ", actCode(ZBE)=" +
                this.getActCode().getCode(TPS_ActivityCodeType.ZBE) + ", start=" + this.getOriginalStart() +
                ", duration=" + this.getOriginalDuration() + "]";
    }

    public abstract EpisodeType getEpisodeType();

}
