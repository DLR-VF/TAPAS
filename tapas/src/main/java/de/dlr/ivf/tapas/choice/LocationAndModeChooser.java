package de.dlr.ivf.tapas.choice;

import de.dlr.ivf.tapas.logger.legacy.HierarchyLogLevel;
import de.dlr.ivf.tapas.logger.legacy.SeverityLogLevel;
import de.dlr.ivf.tapas.logger.legacy.TPS_Logger;
import de.dlr.ivf.tapas.mode.CostCalculator;
import de.dlr.ivf.tapas.mode.cost.MNLFullComplexContext;
import de.dlr.ivf.tapas.model.TPS_AttributeReader;
import de.dlr.ivf.tapas.model.TPS_Geometrics;
import de.dlr.ivf.tapas.model.constants.TPS_ActivityConstant;
import de.dlr.ivf.tapas.model.constants.TPS_DrivingLicenseInformation;
import de.dlr.ivf.tapas.model.location.TPS_Location;
import de.dlr.ivf.tapas.model.location.TPS_TrafficAnalysisZone;
import de.dlr.ivf.tapas.model.mode.TPS_ExtMode;
import de.dlr.ivf.tapas.model.mode.TPS_Mode;
import de.dlr.ivf.tapas.model.parameter.ParamFlag;
import de.dlr.ivf.tapas.model.parameter.ParamValue;
import de.dlr.ivf.tapas.model.parameter.TPS_ParameterClass;
import de.dlr.ivf.tapas.model.plan.TPS_PlannedTrip;
import de.dlr.ivf.tapas.model.scheme.*;
import de.dlr.ivf.tapas.model.vehicle.CarController;
import de.dlr.ivf.tapas.model.plan.TPS_LocatedStay;
import de.dlr.ivf.tapas.model.plan.TPS_Plan;
import de.dlr.ivf.tapas.model.plan.TPS_PlanningContext;
import de.dlr.ivf.tapas.model.vehicle.TPS_Car;
import lombok.Getter;

import java.util.List;
import java.util.function.Supplier;


/**
 * temporary class extracting "selectLocationAndModesAndTravelTimes from the TPS_Plan class
 */
public class LocationAndModeChooser {

    private final TPS_ParameterClass parameterClass;
    private final boolean useShoppingMotives;
    private final boolean useFixLocs;
    @Getter
    private final LocationSelector locationSelector;
    private final ModeSelector modeSelector;
    private final int automaticVehicleMinDriverAge;
    private final int automaticVehicleLevel;
    private final TravelDistanceCalculator distanceCalculator;

    private final TravelTimeCalculator travelTimeCalculator;

    private final CostCalculator costCalculator;

    public LocationAndModeChooser(TPS_ParameterClass parameterClass, LocationSelector locationSelector, ModeSelector modeSelector,
                                  TravelDistanceCalculator distanceCalculator, TravelTimeCalculator travelTimeCalculator,
                                  CostCalculator costCalculator) {
        this.parameterClass = parameterClass;
        this.useShoppingMotives = parameterClass.isTrue(ParamFlag.FLAG_USE_SHOPPING_MOTIVES);
        this.useFixLocs = parameterClass.isTrue(ParamFlag.FLAG_USE_FIXED_LOCS_ON_BASE);
        this.locationSelector = locationSelector;
        this.modeSelector = modeSelector;
        this.automaticVehicleMinDriverAge = parameterClass.getIntValue(ParamValue.AUTOMATIC_VEHICLE_MIN_DRIVER_AGE);
        this.automaticVehicleLevel = parameterClass.getIntValue(ParamValue.AUTOMATIC_VEHICLE_LEVEL);
        this.distanceCalculator = distanceCalculator;
        this.travelTimeCalculator = travelTimeCalculator;
        this.costCalculator = costCalculator;
    }

    /**
     * Initiates the determination of locations and modes for stays and trips of the scheme
     */
    public void selectLocationsAndModesAndTravelTimes(TPS_PlanningContext pc, TPS_Scheme scheme, TPS_Plan plan) {

        long start = System.currentTimeMillis();

        // We need several loops to resolve the hierarchy of episodes.
        // No we can use the priority of the stays to solve all locations searches in one loop
        for (TPS_SchemePart schemePart : scheme) {
            if(schemePart instanceof TPS_TourPart tourpart) {

                // check mobility options
                if (tourpart.isCarUsed()) {
                    pc.carForThisPlan = tourpart.getCar();
                } else if (!pc.influenceCarUsageInPlan) {

                    pc.carForThisPlan = null; // will be overwritten, if a car can be used
                    int carUsageDuration = (int) tourpart.getOriginalSchemePartEnd() - (int) tourpart.getOriginalSchemePartStart();

                    List<CarController> availableCars = plan.getPerson().getHousehold().getCarFleetManager().getAvailableCarsNonRestrictedFirst((int) tourpart.getOriginalSchemePartStart(), carUsageDuration);

                    CarController tmpCar = availableCars.isEmpty() ? null : availableCars.getFirst();
                    if (tmpCar != null) {
                        if (pc.getPerson().mayDriveACar(tmpCar.getCar(), automaticVehicleMinDriverAge, automaticVehicleLevel)) {
                            pc.carForThisPlan = tmpCar.getCar(); // this person can use this car
                            pc.addAttribute(TPS_AttributeReader.TPS_Attribute.PERSON_DRIVING_LICENSE_CODE,
                                    TPS_DrivingLicenseInformation.CAR.getCode());   //update this attribute because
                            //an automated car may have changed it
                        }
                    }
                }

                if (tourpart.isBikeUsed()) { //was the bike used before?
                    pc.isBikeAvailable = true;
                } //todo check if this is still needed
                // else if (!pc.influenceBikeUsageInPlan) { // is the bike availability modded outside?
                   // pc.isBikeAvailable = pc.getPerson().hasBike();
                //}
                pc.addAttribute(TPS_AttributeReader.TPS_Attribute.PERSON_HAS_BIKE, pc.isBikeAvailable ? 1 : 0);

                if (pc.carForThisPlan == null) {
                    pc.addAttribute(TPS_AttributeReader.TPS_Attribute.HOUSEHOLD_CARS, 0);
                } else {
                    pc.addAttribute(TPS_AttributeReader.TPS_Attribute.HOUSEHOLD_CARS, pc.numHouseholdCars());
                }

                MNLFullComplexContext mnlContext = new MNLFullComplexContext(plan.getPerson(),tourpart);

                for (TPS_Stay stay : tourpart.getPriorisedStayIterable()) {
                    plan.getAttributes().put(TPS_AttributeReader.TPS_Attribute.CURRENT_EPISODE_ACTIVITY_CODE_TAPAS,
                            stay.getActCode().getCode(TPS_ActivityConstant.TPS_ActivityCodeType.TAPAS));

                    pc.pe.getPerson().estimateAccessibilityPreference(stay, useShoppingMotives);

                    TPS_LocatedStay currentLocatedStay = plan.getLocatedStay(stay);
                    if (!currentLocatedStay.isLocated()) {
                        plan.setCurrentAdaptedEpisode(currentLocatedStay);
                        TPS_ActivityConstant currentActCode = stay.getActCode();
                        // Register locations for activities where the location will be used again.
                        // Flag for the case of a activity with unique location.
                        pc.fixLocationAtBase = currentActCode.isFix() && useFixLocs && !plan.getFixLocations().containsKey(currentActCode);

                        // when all tour parts are correctly instantiated the else case will never happen, because every
                        // tour part starts with a trip. In the current episode file there exist tour parts with no first
                        // trip (e.g. shopping in the same building where you live)
                        //TPS_Trip previousTrip = null;
                        if (!tourpart.isFirst(stay)) {
                            pc.previousTrip = tourpart.getPreviousTrip(stay);
                        } else {
                            pc.previousTrip = new TPS_Trip(-999, TPS_ActivityConstant.DUMMY, -999, 0);
                        }

                        //First execution: fix locations will be set (ELSE branch)
                        //Further executions: fix locations are set already, take locations from map (IF branch)
                        //Non fix location, i.e. everything except home and work: also ELSE branch
                        //E.g.: Provides a work location, when ActCode is working in ELSE

                        if (currentActCode.isFix() && plan.getFixLocations().containsKey(currentActCode)) {
                            //now we check if the mode is fix and if we can reach the fix location with the fix mode!
                            //TODO: check only for restricted cars, but bike could also be fixed and no connection!
                            if (pc.carForThisPlan != null && // we have a car
                                    pc.carForThisPlan.isRestricted() && //we have a restricted car
                                    plan.getFixLocations().get(currentActCode).getTrafficAnalysisZone()
                                            .isRestricted()) // we have a restricted car wanting to go to a restricted area! -> BAD!
                            {
                                locationSelector.selectLocation(plan, pc, () -> tourpart.getStayHierarchy(stay).getPrevStay(), () -> tourpart.getStayHierarchy(stay).getNextStay(), stay);
                                if (currentActCode.isFix()) {
                                    plan.getFixLocations().put(currentActCode, plan.getLocatedStay(stay).getLocation());
                                }
                            } else {
                                currentLocatedStay.setLocation(plan.getFixLocations().get(currentActCode));
                            }

                            if (TPS_Logger.isLogging(HierarchyLogLevel.EPISODE, SeverityLogLevel.FINE)) {
                                TPS_Logger.log(HierarchyLogLevel.EPISODE, SeverityLogLevel.FINE,
                                        "Set location from fix locations");
                            }
                        } else {
                            locationSelector.selectLocation(plan, pc, () -> tourpart.getStayHierarchy(stay).getPrevStay(), () -> tourpart.getStayHierarchy(stay).getNextStay(), stay);
                            if (currentActCode.isFix()) {
                                plan.getFixLocations().put(currentActCode, plan.getLocatedStay(stay).getLocation());
                            }
                        }

                        if (currentLocatedStay.getLocation() == null) {
                            TPS_Logger.log(SeverityLogLevel.ERROR, "No Location found!");
                        }
                    }
                    // fetch previous and next stay
                    TPS_Stay prevStay = tourpart.getStayHierarchy(stay).getPrevStay();
                    Supplier<TPS_Stay> prevStaySupplier = () -> tourpart.getStayHierarchy(stay).getPrevStay();
                    TPS_Stay goingTo = tourpart.getStayHierarchy(stay).getNextStay();
                    Supplier<TPS_Stay> goingToSupplier = () -> tourpart.getStayHierarchy(stay).getNextStay();

                    TPS_Mode chosenMode;
                    if (currentLocatedStay.getModeArr() == null || currentLocatedStay.getModeDep() == null) {

                        //do we have a fixed mode from the previous trip?
                        if (tourpart.isFixedModeUsed()) {
                            currentLocatedStay.setModeArr(tourpart.getUsedMode());
                            currentLocatedStay.setModeDep(tourpart.getUsedMode());
                            chosenMode = tourpart.getUsedMode().primary;
                        } else {
                            TPS_Location pLocGoingTo = plan.getLocatedStay(goingTo).getLocation();
                            TPS_Car tmpCar = pc.carForThisPlan;
                            if (tmpCar != null && tmpCar.isRestricted() &&
                                    (currentLocatedStay.getLocation().getTrafficAnalysisZone().isRestricted() ||
                                            pLocGoingTo.getTrafficAnalysisZone().isRestricted())) {
                                pc.carForThisPlan = null;
                            }
                            //double distanceNet = distanceCalculator.getDistance(pLocComingFrom, pLocGoingTo, TPS_Mode.ModeType.WALK)
                           // chosenMode = this.modeSelector.selectMode(plan, prevStaySupplier, currentLocatedStay, goingToSupplier, pc);
                            pc.carForThisPlan = tmpCar;
                            //set the mode and car (if used)
                            tourpart.setUsedMode(currentLocatedStay.getModeDep(), pc.carForThisPlan);

                            //set some variables for the fixed modes
                            TPS_ExtMode em = tourpart.getUsedMode();
                            plan.usesBike = em.isBikeUsed();
                            if (em.isCarUsed()) {
                                plan.usedCars.add(pc.carForThisPlan);
                            }

                            //set variables for fixed modes:
                            pc.carForThisPlan = tourpart.getCar();
                            pc.isBikeAvailable = tourpart.isBikeUsed();
                            if (pc.carForThisPlan == null) {
                                plan.getAttributes().put(TPS_AttributeReader.TPS_Attribute.HOUSEHOLD_CARS, 0);
                            } else {
                                plan.getAttributes().put(TPS_AttributeReader.TPS_Attribute.HOUSEHOLD_CARS,
                                        plan.getPerson().getHousehold().getNumberOfCars());
                            }
                            plan.getAttributes().put(TPS_AttributeReader.TPS_Attribute.PERSON_HAS_BIKE, pc.isBikeAvailable ? 1 : 0);
                        }
                    }

                    TPS_LocatedStay pComingFrom;
                    TPS_LocatedStay pGoingTo;
                    TPS_Location pLocComingFrom;
                    TPS_Location pLocGoingTo;
                    TPS_PlannedTrip plannedTrip;
                    TPS_TrafficAnalysisZone comingFromTaz;
                    TPS_TrafficAnalysisZone goingToTaz;

                    //set travel time for the arriving mode
                    if (!tourpart.isFirst(stay)) {
                        pComingFrom = plan.getLocatedStay(prevStay);
                        pGoingTo = plan.getLocatedStay(stay);
                        pLocComingFrom = pComingFrom.getLocation();
                        pLocGoingTo = pGoingTo.getLocation();
                        plannedTrip =  plan.getPlannedTrip(tourpart.getPreviousTrip(stay));
                        comingFromTaz = pLocComingFrom.getTrafficAnalysisZone();
                        goingToTaz = pLocGoingTo.getTrafficAnalysisZone();

                        plannedTrip.setDistanceEmptyNet(distanceCalculator.getDistance(pLocComingFrom, pLocGoingTo, TPS_Mode.ModeType.WALK));
                        plannedTrip.setDistanceBeeline(TPS_Geometrics.getDistance(pLocComingFrom, pLocGoingTo, parameterClass.getDoubleValue(ParamValue.MIN_DIST)));
                        plannedTrip.setTravelTime(pComingFrom,pGoingTo);

                        TPS_Mode mode = plannedTrip.getMode().primary;
                        double tripDuration = travelTimeCalculator.getTravelTime(
                                mode, pLocComingFrom, pLocGoingTo, plannedTrip.getStart());

                        plannedTrip.setDuration((int) tripDuration);
                       // plannedTrip.setCost(costCalculator.calculateCost(goingToTaz,comingFromTaz,tripDuration,));
                    }
                    //set travel time for the departure mode
                    if (!tourpart.isLast(stay)) {
                        pComingFrom = plan.getLocatedStay(stay);
                        pGoingTo = plan.getLocatedStay(goingTo);
                        pLocComingFrom = pComingFrom.getLocation();
                        pLocGoingTo = pGoingTo.getLocation();
                        plannedTrip = plan.getPlannedTrip(tourpart.getNextTrip(stay));

                        plannedTrip.setDistanceEmptyNet(distanceCalculator.getDistance(pLocComingFrom, pLocGoingTo, TPS_Mode.ModeType.WALK));
                        plannedTrip.setDistanceBeeline(TPS_Geometrics.getDistance(pLocComingFrom, pLocGoingTo, parameterClass.getDoubleValue(ParamValue.MIN_DIST)));
                        plannedTrip.setTravelTime(pComingFrom,pGoingTo);

                        TPS_Mode mode = plannedTrip.getMode().primary;
                        plannedTrip.setDuration((int) travelTimeCalculator
                                .getTravelTime(mode, pLocComingFrom, pLocGoingTo, plannedTrip.getStart()));
                    }

                    //update the travel durations for this plan
                    tourpart.updateActualTravelDurations(plan);
                    plan.getAttributes().put(TPS_AttributeReader.TPS_Attribute.CURRENT_TAZ_SETTLEMENT_CODE_TAPAS,
                            currentLocatedStay.getLocation().getTrafficAnalysisZone().getBbrType());
                }//end for tourpart
            }
        }

        /* At this point everything should be ready, but:
         * locations within the same location group (malls etc.) should be accessed by foot.
         * You should not drive through a mall with an SUV, unless you play cruel video games.
         *
         * So check, if two adjacent locations are within the same group and adopt the mode accordingly.
         * This is done AFTER mode estimation, this might result in slightly wrong travel times within a mall.
         * TODO: Check if this inaccuracy is acceptable.
         */
        //tourpart.setUsedMode(null, null);

        TPS_LocatedStay prevLocatedStay = null;
        for (TPS_LocatedStay locatedStay : plan.getLocatedStays()) {
            if (prevLocatedStay != null) { // not for the first stay, which starts at home
                if (locatedStay.isLocated() && prevLocatedStay.isLocated()) {
                    if (locatedStay.getLocation().isSameLocationGroup(prevLocatedStay.getLocation())) {
                        TPS_ExtMode modeWalk = new TPS_ExtMode(modeSelector.getModes().getModeByName(TPS_Mode.ModeType.WALK.name()),null);
                        prevLocatedStay.setModeDep(modeWalk);
                        locatedStay.setModeArr(modeWalk);
                    }
                } else {
                    if (TPS_Logger.isLogging(SeverityLogLevel.WARN)) {
                        TPS_Logger.log(SeverityLogLevel.WARN, "One location is null");
                    }
                }
            }
            prevLocatedStay = locatedStay;
        }

        //now we set the final travel times
        for (TPS_SchemePart schemePart : plan.getScheme()) {
            if (schemePart.isTourPart()) {
                TPS_TourPart tourpart = (TPS_TourPart) schemePart;
                //update the travel durations for this plan
                tourpart.updateActualTravelDurations(plan);
            }
        }

        TPS_Logger.log(SeverityLogLevel.DEBUG,
                "Selected all locations in " + (System.currentTimeMillis() - start) + "ms");
    }
}
