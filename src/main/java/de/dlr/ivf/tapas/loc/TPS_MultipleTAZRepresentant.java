/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.loc;

import de.dlr.ivf.tapas.TPS_Main;
import de.dlr.ivf.tapas.constants.TPS_ActivityConstant;
import de.dlr.ivf.tapas.constants.TPS_ActivityConstant.TPS_ActivityCodeType;
import de.dlr.ivf.tapas.log.TPS_Logger;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface.SeverenceLogLevel;
import de.dlr.ivf.tapas.mode.TPS_Mode;
import de.dlr.ivf.tapas.mode.TPS_Mode.ModeType;
import de.dlr.ivf.tapas.persistence.TPS_RegionResultSet;
import de.dlr.ivf.tapas.plan.TPS_LocatedStay;
import de.dlr.ivf.tapas.plan.TPS_Plan;
import de.dlr.ivf.tapas.plan.TPS_PlanningContext;
import de.dlr.ivf.tapas.scheme.TPS_Stay;
import de.dlr.ivf.tapas.scheme.TPS_TourPart;
import de.dlr.ivf.tapas.scheme.TPS_TourPart.TravelDurations;
import de.dlr.ivf.tapas.util.parameters.ParamFlag;
import de.dlr.ivf.tapas.util.parameters.ParamValue;
import de.dlr.ivf.tapas.util.parameters.SimulationType;
import de.dlr.ivf.tapas.util.parameters.TPS_ParameterClass;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class TPS_MultipleTAZRepresentant extends TPS_LocationChoiceSet {
    /// The number of representants to choose
    int numOfTazRepresentants = 3;

    public List<TPS_Location> generateLocationRepr(TPS_ActivityConstant actCode, TPS_TrafficAnalysisZone taz) {
        if (taz.allowsActivity(actCode)) {
            return taz.selectActivityLocations(actCode, numOfTazRepresentants);
        }
        // empty list if the activity is not supported in the TAZ
        return new ArrayList<>();
    }

    /**
     * Method to return the locations, which are possible to use for the given activity.
     *
     * @param plan           the plan to use
     * @param pc             planning context
     * @param locatedStay    the located stay we are coming from
     * @param parameterClass parameter container reference
     * @return A TPS_RegionResultSet of appropriate locations
     */
    public TPS_RegionResultSet getLocationRepresentatives(TPS_Plan plan, TPS_PlanningContext pc, TPS_LocatedStay locatedStay, TPS_ParameterClass parameterClass, Supplier<TPS_Stay> coming_from, Supplier<TPS_Stay> going_to) {

        long time = System.nanoTime();
        TPS_RegionResultSet regionRS = new TPS_RegionResultSet();
        TPS_TourPart tourpart = (TPS_TourPart) locatedStay.getStay().getSchemePart();

        TPS_Stay stay = locatedStay.getStay();
        TPS_ActivityConstant activityCode = locatedStay.getStay().getActCode();
        TPS_Stay comingFrom = coming_from.get();
        TPS_Stay goingTo = going_to.get();
        TPS_Location locComingFrom = plan.getLocatedStay(comingFrom).getLocation();
        TPS_Location locGoingTo = plan.getLocatedStay(goingTo).getLocation();
        TravelDurations td = tourpart.getTravelDurations(stay);
        double arrivalDuration = td.getArrivalDuration(plan.getPM().getParameters());
        double departureDuration = td.getDepartureDuration(plan.getPM().getParameters());

        //store some statistics:
        Integer[] tupel = TPS_Main.actStats.get(activityCode.getCode(TPS_ActivityCodeType.ZBE));

        if (tupel == null) {
            tupel = new Integer[3];
            tupel[0] = 0;
            tupel[1] = 0;
            tupel[2] = 0;
        }

        tupel[0]++;
        tupel[1] += (int) arrivalDuration;
        tupel[2] += (int) departureDuration;

        TPS_Main.actStats.put(activityCode.getCode(TPS_ActivityCodeType.ZBE), tupel);


        int i;
        // normal case
        // speed slices from MAX_SYSTEM_SPEED/ MAX_TRIES_LOCATION_SELECTION to
        // MAX_SYSTEM_SPEED
        double incFactor = 1.0 / parameterClass.getDoubleValue(ParamValue.MAX_TRIES_LOCATION_SELECTION);
        //double incFactorTime = 1.0 / ParamValue.MAX_TRIES_LOCATION_SELECTION.getDoubleValue();
        double arrivalDistance = 0, departureDistance = 0, actDistance;

        // TODO: make this better!
        // some trips start per default at different i and not at i=1!

        switch (activityCode.getCode(TPS_ActivityCodeType.TAPAS)) {
            case 1: // work
                // always look at the maximum range!
                i = parameterClass.getIntValue(ParamValue.MAX_TRIES_LOCATION_SELECTION);
                break;
            case 0:
            case 2: // school
            case 3: // shopping
            case 4: // private matters
            case 5:// free time
            case 6: // other
            case 7: // university
            default:
                i = 1; // start as close as possible
                break;
        }

        for (; i <= parameterClass.getIntValue(ParamValue.MAX_TRIES_LOCATION_SELECTION) + 1 &&
                regionRS.size() < 2 * numOfTazRepresentants; i++) {

            regionRS.clear();

            for (TPS_TrafficAnalysisZone taz : region) {
                if (taz.getData() == null) {
                    // empty taz data
                    continue;
                }
                if (!taz.allowsActivity(activityCode)) {
                    // no activities of this type
                    continue;
                }
                // filter restricted tazes if a restricted car is used
                if (tourpart.getCar() != null && tourpart.isCarUsed() && tourpart.getCar().isRestricted() &&
                        taz.isRestricted()) continue;

                if (parameterClass.isTrue(ParamFlag.FLAG_FILTER_SHOPPING_CHOICE_SET) || !stay.isShopping()) {

                    actDistance = TPS_Mode.get(ModeType.WALK).getDistance(taz, locGoingTo, SimulationType.SCENARIO,
                            null);
                    actDistance += TPS_Mode.get(ModeType.WALK).getDistance(locComingFrom, taz, SimulationType.SCENARIO,
                            null);


                    if (i <= parameterClass.getIntValue(ParamValue.MAX_TRIES_LOCATION_SELECTION)) {
                        arrivalDistance = arrivalDuration * parameterClass.getDoubleValue(ParamValue.MAX_SYSTEM_SPEED) *
                                incFactor * (double) i;
                        departureDistance = departureDuration * parameterClass.getDoubleValue(
                                ParamValue.MAX_SYSTEM_SPEED) * incFactor * (double) i;
                        if (actDistance > (departureDistance + arrivalDistance)) {
                            continue;
                        }
                    } else {
                        // desperate last try: drop distance constraint and keep on going!
                    }
                }


                double weight = taz.getActivityWeightSum(activityCode);
                if (weight <= 0) {
                    // do not process "zero weight"-locations
                    continue;
                }
                List<TPS_Location> locList = generateLocationRepr(activityCode, taz);
                // first sum the weight of the selected representatives (the list may be empty)
                double sumWeight = 0;
                for (TPS_Location loc : locList) {
                    sumWeight += loc.getData().getWeight();
                }
                // now weight the total weight of the cell by the normalized weight of this location
                // in the set of representants
                for (TPS_Location loc : locList) {
                    regionRS.add(taz, loc, weight * loc.getData().getWeight() / sumWeight);
                }
            }
        }

        time = System.nanoTime() - time;
        if (TPS_Logger.isLogging(SeverenceLogLevel.DEBUG)) {
            TPS_Logger.log(SeverenceLogLevel.DEBUG,
                    "Selected traffic analysis zones (size=" + regionRS.size() + ") in " + (time / 1000000.0) + "ms");
        }
        return regionRS;
    }
}
