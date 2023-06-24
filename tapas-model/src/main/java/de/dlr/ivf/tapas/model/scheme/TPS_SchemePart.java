/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.model.scheme;

import de.dlr.ivf.tapas.logger.legacy.LogHierarchy;
import de.dlr.ivf.tapas.logger.legacy.HierarchyLogLevel;
import de.dlr.ivf.tapas.util.ExtendedWritable;
import de.dlr.ivf.tapas.model.parameter.TPS_ParameterClass;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This class implements the basic aspects of a scheme part. It provides a list of all episodes in the part.
 *
 * @author mark_ma
 */
@LogHierarchy(hierarchyLogLevel = HierarchyLogLevel.PLAN)
public abstract class TPS_SchemePart implements Iterable<TPS_Episode>, ExtendedWritable {

    /**
     * Chronological list with all episodes of this scheme part
     */
    protected List<TPS_Episode> episodes;
    /**
     * original start of the scheme part
     */

    protected double originalSchemePartStart = 24 * 60 * 60;
    /**
     * original end of the scheme part
     */

    protected double originalSchemePartEnd = 0.0;
    /**
     * scheme part id
     */
    private final int id;
    /**
     * Reference to the scheme
     */
    private TPS_Scheme scheme;

    /**
     * Sets the id and builds the list for the episodes.
     *
     * @param id
     */
    public TPS_SchemePart(int id) {
        this.id = id;
        this.episodes = new ArrayList<>();
    }

    /**
     * This method adds an episode to the list and sets its reference to a scheme part to this.
     *
     * @param episode
     * @return true if added, false otherwise
     */
    protected boolean addEpisode(TPS_Episode episode) {
        this.originalSchemePartStart = Math.min(this.originalSchemePartStart, episode.getOriginalStart());
        this.originalSchemePartEnd = Math.max(this.originalSchemePartEnd, episode.getOriginalEnd());
        episode.setSchemePart(this);
        return episodes.add(episode);
    }

    /**
     * This method adds a stay to the list and sets its reference to a scheme part to this.
     *
     * @param stay
     * @param parameterClass reference - which is not used in here, only in the subclass
     * @return true if added, false otherwise
     */
    public boolean addStay(TPS_Stay stay, TPS_ParameterClass parameterClass) {
        return this.addEpisode(stay);
    }

    /**
     * Returns the first episode of the scheme part
     *
     * @return first episode
     */
    public TPS_Episode getFirstEpisode() {
        return episodes.get(0);
    }

    /**
     * Returns the id of the scheme part
     *
     * @return id
     */
    public int getId() {
        return id;
    }

    /**
     * Returns the last episode of the scheme part
     *
     * @return last episode
     */
    public TPS_Episode getLastEpisode() {

        return episodes.get(this.episodes.size() - 1);
    }

    /**
     * This method returns the next episode to this episode. If there is no next episode, the first episode of the next
     * scheme part in the scheme is returned. If there is no next episode at all the return value is null;
     *
     * @param episode
     * @return next episode in this or the next scheme part or null
     */
    public TPS_Episode getNextEpisode(TPS_Episode episode) {
        int index = this.episodes.indexOf(episode);
        if (index + 1 < this.episodes.size()) {
            return (this.episodes.get(index + 1));
        }
        TPS_SchemePart sp = this.scheme.getNextSchemePart(this);
        if (sp != null) {
            return sp.getFirstEpisode();
        }
        return null;
    }

    /**
     * This method returns the next episode to this episode. If there is no next episode, the return value is null;
     *
     * @param episode
     * @return next episode in this part or null
     */
    public TPS_Episode getNextEpisodeFromThisPart(TPS_Episode episode) {
        int index = this.episodes.indexOf(episode);
        if (index + 1 < this.episodes.size()) {
            return (this.episodes.get(index + 1));
        }
        return null;
    }

    /**
     * This method returns the next stay to this episode. If there is no further stay, the first stay of the next scheme part
     * in the scheme is returned.
     *
     * @param episode
     * @return next stay in this or the next scheme part
     */
    public TPS_Stay getNextStay(TPS_Episode episode) {
        int index = this.episodes.indexOf(episode);
        TPS_Stay stay = null;
        for (++index; index < this.episodes.size(); index++) {
            if (this.episodes.get(index).isStay()) {
                stay = (TPS_Stay) this.episodes.get(index);
                break;
            }
        }
        TPS_SchemePart sp = this.scheme.getNextSchemePart(this);
        if (stay == null && sp != null) {
            if (sp.getFirstEpisode().isStay()) {
                stay = (TPS_Stay) sp.getFirstEpisode();
            } else {
                stay = sp.getNextStay(sp.getFirstEpisode());
            }
        }
        return stay;
    }

    /**
     * @return the originalSchemePartEnd
     */
    public double getOriginalSchemePartEnd() {
        return originalSchemePartEnd;
    }

    /**
     * @return the originalSchemePartStart
     */
    public double getOriginalSchemePartStart() {
        return originalSchemePartStart;
    }

    /**
     * This method returns the previous episode to this episode. If there is no previous episode, the last episode of the previous
     * scheme part in the scheme is returned. If there is no previous episode at all the return value is null;
     *
     * @param episode
     * @return previous episode in this or the previous scheme part or null
     */
    public TPS_Episode getPreviousEpisode(TPS_Episode episode) {
        int index = this.episodes.indexOf(episode);
        if (index > 0) {
            return (this.episodes.get(index - 1));
        }
        TPS_SchemePart sp = this.scheme.getPreviousSchemePart(this);
        if (sp != null) {

            return sp.getLastEpisode();
        }
        return null;
    }

    /**
     * This method returns the previous episode to this episode. If there is no previous episode, the return value is null;
     *
     * @param episode
     * @return previous episode in this or null
     */
    public TPS_Episode getPreviousEpisodeFromThisPart(TPS_Episode episode) {
        int index = this.episodes.indexOf(episode);
        if (index > 0) {
            return (this.episodes.get(index - 1));
        }
        return null;
    }

    /**
     * This method returns the previous stay to this episode. If there is no previous stay, the last stay of the previous
     * scheme part in the scheme is returned.
     *
     * @param episode
     * @return previous stay in this or the previous scheme part
     */
    public TPS_Stay getPreviousStay(TPS_Episode episode) {
        TPS_Stay stay = null;
        int index = this.episodes.indexOf(episode);
        for (--index; index >= 0; index--) {
            if (this.episodes.get(index).isStay()) {
                stay = (TPS_Stay) this.episodes.get(index);
                break;
            }
        }
        TPS_SchemePart sp = this.scheme.getPreviousSchemePart(this);
        if (stay == null && sp != null) {
            if (sp.getLastEpisode().isStay()) {
                stay = (TPS_Stay) sp.getLastEpisode();
            } else {
                stay = sp.getPreviousStay(sp.getLastEpisode());
            }
        }
        return stay;
    }

    /**
     * Returns a reference to the scheme to scheme part belongs to
     *
     * @return reference to the scheme
     */
    public TPS_Scheme getScheme() {
        return scheme;
    }

    /**
     * Sets the reference of the scheme
     *
     * @param scheme
     */
    void setScheme(TPS_Scheme scheme) {
        this.scheme = scheme;
    }

    /**
     * This method returns an iterator over all stays of this scheme part
     *
     * @return an instance of {@link StayIterator}
     */
    public Iterable<TPS_Stay> getStayIterator() {
        return new StayIterator();
    }

    /**
     * Checks whether the given episode is the first episode of the scheme part
     *
     * @param episode
     * @return true if the given episode is the first one, false otherwise
     */
    public boolean isFirst(TPS_Episode episode) {
        return this.getFirstEpisode().equals(episode);
    }

    /**
     * Flag indicating whether this scheme part is a home part, i.e. all stays are at home and the scheme part does not
     * contain a trip.
     *
     * @return true if the scheme part is a home part
     */
    public abstract boolean isHomePart();

    /**
     * Checks whether the episode given is the last episode of the scheme part
     *
     * @param episode
     * @return true if the given episode is the last one, false otherwise
     */
    public boolean isLast(TPS_Episode episode) {
        return this.getLastEpisode().equals(episode);
    }

    /**
     * Flag indicating whether the scheme part is a tour part, i.e. the scheme part contains trips, in detail it has to start
     * and end with a trip. Every change of a location in the stays in the scheme part is separated by a further trip.
     *
     * @return true if the scheme part is a tour part, false otherwise
     */
    public abstract boolean isTourPart();

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Iterable#iterator()
     */
    public Iterator<TPS_Episode> iterator() {
        return this.episodes.iterator();
    }


    public List<TPS_Episode> getEpisodes(){
        return this.episodes;
    }

    /**
     * Returns the number of all episodes in the scheme part
     *
     * @return number of all episodes
     */
    public int size() {
        return episodes.size();
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
    public abstract String toString(String prefix);

    /**
     * This iterator allows to iterate over all stays of the scheme part by skipping the trips
     *
     * @author mark_ma
     */
    private class StayIterator implements Iterator<TPS_Stay>, Iterable<TPS_Stay> {

        /**
         * Internal iterator over all episodes
         */
        private final Iterator<TPS_Episode> it;

        /**
         * next stay which will be returned
         */
        private TPS_Stay stay;

        /**
         * Initialises the next stay which will be returned and the internal iterator.
         */
        public StayIterator() {
            this.it = episodes.iterator();
            this.next();
        }

        /*
         * (non-Javadoc)
         *
         * @see java.util.Iterator#hasNext()
         */
        public boolean hasNext() {
            return this.stay != null;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.lang.Iterable#iterator()
         */
        public Iterator<TPS_Stay> iterator() {
            return this;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.util.Iterator#next()
         */
        public TPS_Stay next() {
            TPS_Stay temp = this.stay;
            this.stay = null;
            TPS_Episode e;
            while (this.it.hasNext() && this.stay == null) {
                e = it.next();
                if (e.isStay()) {
                    this.stay = (TPS_Stay) e;
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
