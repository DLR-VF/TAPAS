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
import de.dlr.ivf.tapas.model.parameter.TPS_ParameterClass;
import de.dlr.ivf.tapas.model.person.TPS_PreferenceParameters.*;

/**
 * A Stay indicates an episode at one specific place. A stay can be part of a home part or a tour part.
 */
@LogHierarchy(hierarchyLogLevel = HierarchyLogLevel.PLAN)
public class TPS_Stay extends TPS_Episode implements Comparable<TPS_Stay> {
    public ShoppingPreferenceAccessibility locationChoiceMotive = ShoppingPreferenceAccessibility.Sonstige;
    public ShoppingPreferenceSupply locationChoiceMotiveSupply = ShoppingPreferenceSupply.Sonstige;
    boolean isShopping = false;
    boolean hasDetailedShoppingInformation = false;
    /**
     * priority in tour - stays at home have always priority 0; stays in trips have priorities from 0 to infinity
     * (Highest: 0, Lowest: Infinity)
     */
    private int priority;

    /**
     * Calls the super constructor with all parameters
     *
     * @param id             ID of the stay
     * @param actCode        activity code of the stay
     * @param start          original start time of the stay in seconds
     * @param duration       original duration of the stay in seconds
     * @param startEarlier
     * @param startLater
     * @param durationMinus
     * @param durationPlus
     */
    public TPS_Stay(int id, TPS_ActivityConstant actCode, int start, int duration, double startEarlier, double startLater,
                    double durationMinus, double durationPlus, double scaleShift, double scaleStretch) {
        super(id, actCode, start, duration, startEarlier, startLater, durationMinus, durationPlus, scaleShift, scaleStretch);
        this.setActCode(actCode);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(TPS_Stay stay) {
        int deltaPriority = stay.getPriority() - this.getPriority(); // higher priority -> first in list
        if (deltaPriority == 0) {
            int deltaDuration = this.getOriginalDuration() -
                    stay.getOriginalDuration(); // same priority: longer duration -> first in list
            int deltaStart = stay.getOriginalStart() -
                    this.getOriginalStart(); // same priority: lower Start Time -> first in list
            if (deltaDuration == 0) return deltaStart;
            else return deltaDuration;
        }
        return deltaPriority;
    }

    /**
     * Returns the priority of the stay
     *
     * @return priority
     */
    public int getPriority() {
        return priority;
    }

    /**
     * Determines the type of shopping for a shopping stay from a fixed coding.
     * 51 is short term shopping
     * 54 is mid term shopping
     * 53 is long term shopping
     * the coding is due to historic reasons and makes no sense, except the leading 5
     *
     * @return The shopping class or null if no class available
     */
    public ShoppingClass getTypeOfShoppingGoods() {
        ShoppingClass typeOfGoods = null;
        int actCode = this.getActCode().getCode(TPS_ActivityCodeType.ZBE);
        switch (actCode) {
            case 51: //short term
                typeOfGoods = ShoppingClass.NUG;
                break;
            case 54: //mid term
                typeOfGoods = ShoppingClass.UEL;
                break;
            case 55: // long term
                typeOfGoods = ShoppingClass.TEX;
                break;
            default: //all others
                break;
        }

        return typeOfGoods;
    }

    /**
     * This function checks if the activity has some detailed shopping information available. (short, mid, long term shopping)
     *
     * @return Returns ture if the activity is shopping and detailed information is available. False otherwise
     */
    public boolean hasDetailedShoppingInformation() {
        return hasDetailedShoppingInformation;
    }

    /**
     * Flag indicating whether the stay is part of a home based episode, i.e. the stay is located at home -> housework,
     * telework, watching tv, etc.
     *
     * @return true if the stay is part of a home part, false otherwise
     */
    public boolean isAtHome() {
        return this.getSchemePart().isHomePart();
    }

    public boolean isShopping() {
        return isShopping;
    }

    /*
     * (non-Javadoc)
     *
     * @see de.dlr.ivf.tapas.plan.TPS_Episode#isStay()
     */
    @Override
    public boolean isStay() {
        return true;
    }

    /**
     * Setter for the activity code
     *
     * @param actCode The activity code
     */
    @Override
    public void setActCode(TPS_ActivityConstant actCode) {
        super.setActCode(actCode);
        this.priority = actCode.getCode(TPS_ActivityCodeType.PRIORITY);
        int actCodeInt = actCode.getCode(TPS_ActivityCodeType.ZBE);
        hasDetailedShoppingInformation = actCodeInt == 51 || actCodeInt == 54 || actCodeInt == 55;
        isShopping = actCodeInt >= 50 && actCodeInt <= 55;
    }

    /*
     * (non-Javadoc)
     *
     * @see de.dlr.de.dlr.ivf.scheme.tapas.ivf.TPS_Episode#toString(java.lang.String)
     */
    @Override
    public String toString(String prefix) {
        StringBuilder sb = new StringBuilder(super.toString(prefix));
        sb.setLength(sb.length() - 1);
        sb.append(", priority=" + priority + "]");
        return sb.toString();
    }

    @Override
    public EpisodeType getEpisodeType() {
        return isAtHome() ? EpisodeType.HOME : EpisodeType.ACTIVITY;
    }
}
