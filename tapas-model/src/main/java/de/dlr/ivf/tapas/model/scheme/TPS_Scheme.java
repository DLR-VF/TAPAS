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
import de.dlr.ivf.tapas.logger.legacy.LogHierarchy;
import de.dlr.ivf.tapas.logger.legacy.HierarchyLogLevel;
import de.dlr.ivf.tapas.util.ExtendedWritable;
import de.dlr.ivf.tapas.util.NestedIterator;
import de.dlr.ivf.tapas.model.parameter.ParamValue;
import de.dlr.ivf.tapas.model.parameter.TPS_ParameterClass;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This class provides a scheme. All schemes are singleton instances, so that the plans have just a reference to the scheme
 * but doesn't manipulate it. A scheme is divided into tour and home parts. Tour parts consists of stays and trips and
 * home parts does only contain of stays which are located at home.
 */
@LogHierarchy(hierarchyLogLevel = HierarchyLogLevel.PLAN)
public class TPS_Scheme implements Iterable<TPS_SchemePart>, ExtendedWritable {

    /**
     * id of the scheme
     */
    private final int id;
    /**
     * original travel duration of this scheme in seconds
     */
    private double originalTravelDuration;
    /**
     * Reference to the corresponding scheme class
     */
    private TPS_SchemeClass schemeClass;
    /**
     * List of all scheme parts
     */
    private final List<TPS_SchemePart> schemeParts;
    /**
     * this method returns the number of scheme parts in this scheme
     */

    private final TPS_ParameterClass parameterClass;

    /**
     * New scheme for a given id and a parameter class reference
     *
     * @param id             scheme id
     * @param parameterClass parameter class reference
     */
    public TPS_Scheme(int id, TPS_ParameterClass parameterClass) {
        this.id = id;
        this.schemeParts = new ArrayList<>();
        this.originalTravelDuration = 0.0;
        this.parameterClass = parameterClass;
    }

    /**
     * This method adds the given episode to the last scheme part of the scheme. When there is no scheme part a home part is
     * created (a scheme starts at home). When the last scheme past does not fit to the tour number or the fact that it is a
     * home part a new part is created. At last the episode is added to the last scheme part.
     *
     * @param episode the episode to add
     * @return true if the episode was added successful, false otherwise
     */
    public boolean addEpisode(TPS_Episode episode) {
        TPS_SchemePart sp;
        if (this.schemeParts.isEmpty()) {
            sp = new TPS_HomePart();
            sp.setScheme(this);
            this.schemeParts.add(sp);
        } else {
            sp = this.schemeParts.get(this.schemeParts.size() - 1);
        }

        if (episode.isHomePart && sp.isTourPart()) {
            if (sp.getLastEpisode().isStay()) {
                sp.getLastEpisode().setOriginalDuration(
                        Math.min(this.parameterClass.getIntValue(ParamValue.SEC_TIME_SLOT),
                                sp.getLastEpisode().getOriginalDuration() - this.parameterClass
                                        .getIntValue(ParamValue.SEC_TIME_SLOT))); //make the last stay 5 min shorter

                //double startEarlier, double startLater,
                //                    double durationMinus, double durationPlus, double scaleShift, double scaleStretch


                TPS_ActivityConstant activity = TPS_ActivityConstant.getActivityCodeByTypeAndCode(
                        TPS_ActivityCodeType.ZBE, 80); // activity: moving
                TPS_Trip takeMeHome = new TPS_Trip(sp.getId(), activity, (int) sp.getLastEpisode().getOriginalEnd(), 5); // insert a "take me home in 5 minutes"-trip
                ((TPS_TourPart) sp).addTrip(takeMeHome);
            }
            sp = new TPS_HomePart();
            sp.setScheme(this);
            this.schemeParts.add(sp);
        } else if (episode.tourNumber > 0 && sp.isHomePart()) {
            sp = new TPS_TourPart(episode.tourNumber);
            sp.setScheme(this);
            this.schemeParts.add(sp);
        }

        try {
            if (episode.isStay()) {
                sp.addStay((TPS_Stay) episode, parameterClass);
            } else {
                ((TPS_TourPart) sp).addTrip((TPS_Trip) episode);
            }
        } catch (ClassCastException e) {
            System.err.println("Scheme: " + sp.getScheme().getId());
            System.err.println(episode.toString());
            e.printStackTrace();
            return false;
        }
        return true;
    }


    /**
     * @return An instance of EpisodeIterator
     */
    public Iterable<TPS_Episode> getEpisodeIterator() {
        return new NestedIterator<>(this.schemeParts);
    }

    /**
     * Returns the id of the scheme
     *
     * @return id
     */
    public int getId() {
        return id;
    }

    /**
     * Returns the subsequent scheme part to the one handed over
     *
     * @param schemePart current scheme part
     * @return next scheme part to the given one
     */
    public TPS_SchemePart getNextSchemePart(TPS_SchemePart schemePart) {
        int index = this.schemeParts.indexOf(schemePart);
        TPS_SchemePart nextSchemePart = null;
        if (index + 1 < this.schemeParts.size()) {
            nextSchemePart = this.schemeParts.get(index + 1);
        }
        return nextSchemePart;
    }

    /**
     * @return complete travel duration of all trips of the scheme
     */
    public double getOriginalTravelDuration() {
        return this.originalTravelDuration;
    }

    /**
     * Returns the previous scheme part to the one handed over
     *
     * @param schemePart current scheme part
     * @return previous scheme part to the given one
     */
    TPS_SchemePart getPreviousSchemePart(TPS_SchemePart schemePart) {
        int index = this.schemeParts.indexOf(schemePart);
        TPS_SchemePart prevSchemePart = null;
        if (index > 0) {
            prevSchemePart = this.schemeParts.get(index - 1);
        }
        return prevSchemePart;
    }

    /**
     * Returns the reference to the scheme class / diary class
     *
     * @return reference to the scheme class
     */
    TPS_SchemeClass getSchemeClass() {
        return schemeClass;
    }

    /**
     * Sets the reference to the scheme class
     *
     * @param schemeClass scheme class
     */
    void setSchemeClass(TPS_SchemeClass schemeClass) {
        this.schemeClass = schemeClass;
    }

    /**
     * Method to get the according scheme part for a given episode. Returns null if this episode is not part of this scheme
     *
     * @param episode the episode to look for
     * @return the scheme part, which contains the episode or null, if the episode is not part of this scheme
     */
    public TPS_SchemePart getSchemePart(TPS_Episode episode) {

        for (TPS_SchemePart parts : this.schemeParts) {
            if (parts.episodes.indexOf(episode) >= 0) return parts;
        }

        return null;
    }

    public List<TPS_SchemePart> getSchemeParts() {
        return this.schemeParts;
    }

    /**
     * Returns the average travel time expenditure in the diary class the scheme belongs to
     *
     * @return average travel time expenditures in minutes
     */
    public double getTimeUsageAVG() {
        return this.schemeClass.getMean();
    }

    /**
     * Returns the standard deviance of the travel time in the diary class the scheme belongs to
     *
     * @return the standard deviance of the average as ratio of the average travel time expenditure
     */
    public double getTimeUsageSTD() {
        return this.schemeClass.getDeviation();
    }

    /**
     * This method returns an iterator over all trips in this scheme.
     *
     * @return an instance of TourPartIterator
     */
    public Iterable<TPS_TourPart> getTourPartIterator() {
        return new TourPartIterator();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return this.id;
    }

    /**
     * Initialises the hierarchies in the tours and calculates the complete travel duration.
     */
    public void init() {
        for (TPS_SchemePart sp : this.schemeParts) {
            if (sp.isTourPart()) {
                ((TPS_TourPart) sp).initHierarchy();
                //todo need a work around
                //((TPS_TourPart) sp).setInitialTravelDurations(this.parameterClass);
            }
        }
        this.originalTravelDuration = 0;
        for (TPS_SchemePart sp : this.schemeParts) {
            if (sp.isTourPart()) {
                this.originalTravelDuration += ((TPS_TourPart) sp).getOriginalTripDuration();
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Iterable#iterator()
     */
    public Iterator<TPS_SchemePart> iterator() {
        return this.schemeParts.iterator();
    }

    /**
     * Returns the number of episodes the scheme consists of
     *
     * @return the number of episodes
     */
    public int size() {
        int sum = 0;
        for (TPS_SchemePart sp : this) {
            sum += sp.size();
        }
        return sum;
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
        StringBuilder sb = new StringBuilder();
        sb.append(prefix + "TAPAS Scheme [id=" + this.id + "]\n");
        for (TPS_SchemePart schemePart : this.schemeParts) {
            sb.append(schemePart.toString(prefix + " ") + "\n");
        }
        sb.setLength(sb.length() - 1);
        return sb.toString();
    }

    /**
     * This iterator allows to iterate over all trips of the scheme part by skipping the stays
     */
    private class TourPartIterator implements Iterator<TPS_TourPart>, Iterable<TPS_TourPart> {

        /**
         * Internal iterator over all scheme parts
         */
        private final Iterator<TPS_SchemePart> it;

        /**
         * next stay which will be returned
         */
        private TPS_TourPart tourpart;

        /**
         * Initializes the next stay which will be returned and the internal iterator.
         */
        public TourPartIterator() {
            this.it = schemeParts.iterator();
            this.next();
        }

        /*
         * (non-Javadoc)
         *
         * @see java.util.Iterator#hasNext()
         */
        public boolean hasNext() {
            return this.tourpart != null;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.lang.Iterable#iterator()
         */
        public Iterator<TPS_TourPart> iterator() {
            return this;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.util.Iterator#next()
         */
        public TPS_TourPart next() {
            TPS_TourPart temp = this.tourpart;
            this.tourpart = null;
            TPS_SchemePart e;
            while (this.it.hasNext() && this.tourpart == null) {
                e = it.next();
                if (e.isTourPart()) {
                    this.tourpart = (TPS_TourPart) e;
                }
            }
            return temp;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.util.Iterator#remove()
         */
        public void remove() {
            throw new RuntimeException("This method is not implemented");
        }
    }
}
