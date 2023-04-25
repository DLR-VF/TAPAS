/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.plan;

import de.dlr.ivf.tapas.constants.TPS_ActivityConstant;
import de.dlr.ivf.tapas.constants.TPS_ActivityConstant.TPS_ActivityCodeType;
import de.dlr.ivf.tapas.constants.TPS_ActivityConstant.TPS_ActivityConstantAttribute;
import de.dlr.ivf.tapas.loc.TPS_Location;
import de.dlr.ivf.tapas.loc.TPS_Region;
import de.dlr.ivf.tapas.logger.LogHierarchy;
import de.dlr.ivf.tapas.logger.TPS_Logger;
import de.dlr.ivf.tapas.logger.HierarchyLogLevel;
import de.dlr.ivf.tapas.logger.SeverityLogLevel;
import de.dlr.ivf.tapas.mode.TPS_ExtMode;
import de.dlr.ivf.tapas.mode.TPS_Mode;
import de.dlr.ivf.tapas.mode.TPS_Mode.ModeType;
import de.dlr.ivf.tapas.persistence.TPS_PersistenceManager;
import de.dlr.ivf.tapas.scheme.TPS_Episode;
import de.dlr.ivf.tapas.scheme.TPS_Stay;
import de.dlr.ivf.tapas.scheme.TPS_TourPart;
import de.dlr.ivf.tapas.util.ExtendedWritable;
import de.dlr.ivf.tapas.util.TPS_AttributeReader.TPS_Attribute;
import de.dlr.ivf.tapas.parameter.SimulationType;

import java.util.function.Supplier;

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
    private final TPS_PersistenceManager PM;


    /**
     * Constructor for a located stay.
     *
     * @param plan reference to the whole plan
     * @param stay reference to the current stay
     */
    public TPS_LocatedStay(TPS_Plan plan, TPS_Stay stay) {
        super(plan, stay);
        this.setStay(stay);
        this.PM = plan.getPM();
        this.init();
    }


    public TPS_LocatedStay(TPS_LocatedStay locatedStayIn) {
        super(locatedStayIn.plan, locatedStayIn.stay);
        this.setStay(locatedStayIn.stay);
        this.PM = locatedStayIn.plan.getPM();
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
     * Selects a location for the stay
     *
     * @param plan day plan to be executed
     * @param pc
     */
    public void selectLocation(TPS_Plan plan, TPS_PlanningContext pc, Supplier<TPS_Stay> coming_from, Supplier<TPS_Stay> going_to) {
        if (TPS_Logger.isLogging(SeverityLogLevel.DEBUG)) {
            TPS_Logger.log(SeverityLogLevel.DEBUG,
                    "Start select procedure for stay (id=" + this.getStay().getId() + ")");
        }

        TPS_ActivityConstant currentActCode = this.stay.getActCode();
        plan.setAttributeValue(TPS_Attribute.CURRENT_EPISODE_ACTIVITY_CODE_MCT,
                currentActCode.getCode(TPS_ActivityCodeType.MCT));
        plan.setAttributeValue(TPS_Attribute.CURRENT_EPISODE_ACTIVITY_CODE_VOT,
                currentActCode.getCode(TPS_ActivityCodeType.VOT));
        plan.setAttributeValue(TPS_Attribute.CURRENT_EPISODE_ACTIVITY_CODE_TAPAS,
                currentActCode.getCode(TPS_ActivityCodeType.TAPAS));

        // can never happen:
        if (this.isLocated()) {
            throw new RuntimeException(
                    "The location should not be set because in a previous call, because we only call this method once for every TPS_LocatedStay");
        } else if (this.stay.isAtHome()) {
            throw new RuntimeException(
                    "The initialisation of the home parts should be done in the constructor of TPS_Plan");
        }

        // Has to be a tour part because all home parts are at home; home parts will never reach this method
        TPS_TourPart tourpart = (TPS_TourPart) this.stay.getSchemePart();
        TPS_Stay comingFrom = coming_from.get();
        TPS_Stay goingTo = going_to.get();


        if (!plan.isLocated(comingFrom) || !plan.isLocated(goingTo)) {
            // this case should be impossible because we now iterate over the priorised stays,
            // so every stay where we can come from or where we go to is located
            throw new IllegalStateException("Found no location for a higher priorised stay");
        }

        if (currentActCode.hasAttribute(TPS_ActivityConstantAttribute.E_COMMERCE_OUT_OF_HOME)) {
            // Is this an activity that (if it not takes place at home anyway) should be executed in the very
            // vicinity of the home residence?
            if (TPS_Logger.isLogging(SeverityLogLevel.DEBUG)) {
                TPS_Logger.log(SeverityLogLevel.DEBUG,
                        "Activity: " + currentActCode + " assumed to be performed at home");
            }
            this.setLocation(plan.getPerson().getHousehold().getLocation());
        } else {
            TPS_Region region = PM.getRegion();
            this.setLocation(region.selectLocation(plan, pc, this, coming_from,going_to));
            if (this.getLocation() == null) {
                TPS_Logger.log(SeverityLogLevel.ERROR,
                        "End select procedure for stay (id=" + this.getStay().getId() + ") with no location");
                //throw new RuntimeException("End select procedure for stay (id=" + this.getStay().getId() + ") with no location");
            } else {
                if (TPS_Logger.isLogging(SeverityLogLevel.DEBUG)) {
                    TPS_Logger.log(SeverityLogLevel.DEBUG,
                            "End select procedure for stay (id=" + this.getStay().getId() + ") with location (id=" +
                                    this.getLocation().getId() + ")");
                }
            }
        }

        // Get distance from home The MIV-mode is used to get distances on the net.
        this.setDistance(TPS_Mode.get(ModeType.MIT)
                .getDistance(plan.getLocatedStay(comingFrom).getLocation(), this.getLocation(),
                        SimulationType.SCENARIO, null));
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