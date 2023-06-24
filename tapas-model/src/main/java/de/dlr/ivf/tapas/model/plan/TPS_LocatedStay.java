/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.model.plan;

import de.dlr.ivf.tapas.model.location.TPS_Location;
import de.dlr.ivf.tapas.logger.legacy.LogHierarchy;
import de.dlr.ivf.tapas.logger.legacy.HierarchyLogLevel;
import de.dlr.ivf.tapas.model.mode.TPS_ExtMode;
import de.dlr.ivf.tapas.model.scheme.TPS_Episode;
import de.dlr.ivf.tapas.model.scheme.TPS_Stay;
import de.dlr.ivf.tapas.util.ExtendedWritable;

/**
 * @author cyga_ri
 */
@LogHierarchy(hierarchyLogLevel = HierarchyLogLevel.EPISODE)
public class TPS_LocatedStay extends TPS_AdaptedEpisode implements ExtendedWritable {
    // location of the stay
    private TPS_Location location;
    // mode used to arrive at the stay
    private TPS_ExtMode modeArr = null;
    // mode used to leave the stay
    private TPS_ExtMode modeDep = null;
    // the local stay instance without location and mode
    private TPS_Stay stay;
    // reference to the persistence manager


    /**
     * Constructor for a located stay.
     *
     * @param plan reference to the whole plan
     * @param stay reference to the current stay
     */
    public TPS_LocatedStay(TPS_Plan plan, TPS_Stay stay) {
        super(plan, stay);
        this.setStay(stay);
        this.init();
    }


    public TPS_LocatedStay(TPS_LocatedStay locatedStayIn) {
        super(locatedStayIn.plan, locatedStayIn.stay);
        this.setStay(locatedStayIn.stay);
        this.init();
    }


    /**
     * Deletes the arrival and departure mode of the stay
     */
    public void deleteModes() {
        this.setModeArr(null);
        this.setModeDep(null);
    }


    /**
     * getter method for the stay-instance as an episode
     */
    @Override
    public TPS_Episode getEpisode() {
        return this.getStay();
    }


    /**
     * Returns the location of the stay
     *
     * @return the location
     */
    public TPS_Location getLocation() {
        return location;
    }

    /**
     * Sets the location for the stay
     *
     * @param location the location for the stay
     */
    public void setLocation(TPS_Location location) {
        this.location = location;
    }

    /**
     * Returns the arrival mode of the stay
     *
     * @return arrival mode
     */
    public TPS_ExtMode getModeArr() {
        return this.modeArr;
    }

    /**
     * Sets the arrival mode of the stay
     *
     * @param modeArr mode to set
     */
    public void setModeArr(TPS_ExtMode modeArr) {
        this.modeArr = modeArr;
    }

    /**
     * Returns the departure mode of the stay
     *
     * @return departure mode
     */
    public TPS_ExtMode getModeDep() {
        return this.modeDep;
    }

    /**
     * Sets the departure mode of the stay
     *
     * @param modeDep mode to set
     */
    public void setModeDep(TPS_ExtMode modeDep) {
        this.modeDep = modeDep;
    }

    /**
     * getter method for the basic stay
     *
     * @return
     */
    public TPS_Stay getStay() {
        return stay;
    }

    /**
     * Sets the Stay for this class
     *
     * @param stay
     */
    public void setStay(TPS_Stay stay) {
        this.stay = stay;
    }

    /**
     *
     */
    public void init() {
        this.setLocation(null);
        this.deleteModes();
        super.init(this.getStay());
    }

    public boolean isLocated() {
        return location != null;
    }

    @Override
    public boolean isLocatedStay() {
        return true;
    }



    /**
     * Override for standard toString: return with empty string as prefix
     */
    @Override
    public String toString() {
        return this.toString("");
    }


    /**
     * to String Method with a given prefix, right now prefix is ignored
     */
    public String toString(String prefix) {
        return this.getClass().getSimpleName() + "[location=" + location + ", modes: arr=" + modeArr + ", dep=" +
                modeDep + "]";
    }

}