package de.dlr.ivf.tapas.choice;

import de.dlr.ivf.tapas.logger.legacy.HierarchyLogLevel;
import de.dlr.ivf.tapas.logger.legacy.SeverityLogLevel;
import de.dlr.ivf.tapas.logger.legacy.TPS_Logger;
import de.dlr.ivf.tapas.model.TPS_AttributeReader;
import de.dlr.ivf.tapas.model.constants.TPS_ActivityConstant;
import de.dlr.ivf.tapas.model.constants.TPS_DrivingLicenseInformation;
import de.dlr.ivf.tapas.model.location.TPS_Location;
import de.dlr.ivf.tapas.model.mode.TPS_ExtMode;
import de.dlr.ivf.tapas.model.parameter.ParamFlag;
import de.dlr.ivf.tapas.model.parameter.ParamValue;
import de.dlr.ivf.tapas.model.parameter.TPS_ParameterClass;
import de.dlr.ivf.tapas.model.scheme.*;
import de.dlr.ivf.tapas.model.vehicle.CarController;
import de.dlr.ivf.tapas.model.plan.TPS_LocatedStay;
import de.dlr.ivf.tapas.model.plan.TPS_Plan;
import de.dlr.ivf.tapas.model.plan.TPS_PlanningContext;
import de.dlr.ivf.tapas.model.vehicle.TPS_Car;

import java.util.List;
import java.util.function.Supplier;


/**
 * temporary class extracting "selectLocationAndModesAndTravelTimes from the TPS_Plan class
 */
public class LocationAndModeChooser {

    private final TPS_ParameterClass parameterClass;
    private final boolean useShoppingMotives;
    private final boolean useFixLocs;
    private final LocationSelector locationSelector;
    private final ModeSelector modeSelector;
    private final int automaticVehicleMinDriverAge;
    private final int automaticVehicleLevel;

    public LocationAndModeChooser(TPS_ParameterClass parameterClass, LocationSelector locationSelector, ModeSelector modeSelector) {
        this.parameterClass = parameterClass;
        this.useShoppingMotives = parameterClass.isTrue(ParamFlag.FLAG_USE_SHOPPING_MOTIVES);
        this.useFixLocs = parameterClass.isTrue(ParamFlag.FLAG_USE_FIXED_LOCS_ON_BASE);
        this.locationSelector = locationSelector;
        this.modeSelector = modeSelector;
        this.automaticVehicleMinDriverAge = parameterClass.getIntValue(ParamValue.AUTOMATIC_VEHICLE_MIN_DRIVER_AGE);
        this.automaticVehicleLevel = parameterClass.getIntValue(ParamValue.AUTOMATIC_VEHICLE_LEVEL);
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

                    CarController tmpCar = availableCars.isEmpty() ? null : availableCars.get(0);
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
                        if (TPS_Logger.isLogging(SeverityLogLevel.DEBUG)) {
                            String s = "gewählte Location zu Stay: " + currentLocatedStay.getEpisode().getId() + ": " +
                                    currentLocatedStay.getLocation().getId() + " in TAZ:" +
                                    currentLocatedStay.getLocation().getTrafficAnalysisZone().getTAZId() + " in block: " +
                                    (currentLocatedStay.getLocation().hasBlock() ? currentLocatedStay.getLocation()
                                            .getBlock()
                                            .getId() : -1) +
                                    " via" + currentLocatedStay.getModeArr().getName() + "/" +
                                    currentLocatedStay.getModeDep().getName();
                            TPS_Logger.log(SeverityLogLevel.DEBUG, s);
                            TPS_Logger.log(SeverityLogLevel.DEBUG,
                                    "Selected location (id=" + currentLocatedStay.getLocation().getId() +
                                            ") for stay (id=" + currentLocatedStay.getEpisode().getId() + " in TAZ (id=" +
                                            currentLocatedStay.getLocation().getTrafficAnalysisZone().getTAZId() +
                                            ") in block (id= " +
                                            (currentLocatedStay.getLocation().hasBlock() ? currentLocatedStay.getLocation()
                                                    .getBlock()
                                                    .getId() : -1) +
                                            ") via modes " + currentLocatedStay.getModeArr().getName() + "/" +
                                            currentLocatedStay.getModeDep().getName());
                        }
                    }
                    // fetch previous and next stay
                    TPS_Stay prevStay = tourpart.getStayHierarchy(stay).getPrevStay();
                    Supplier<TPS_Stay> prevStaySupplier = () -> tourpart.getStayHierarchy(stay).getPrevStay();
                    TPS_Stay goingTo = tourpart.getStayHierarchy(stay).getNextStay();
                    Supplier<TPS_Stay> goingToSupplier = () -> tourpart.getStayHierarchy(stay).getNextStay();
                    if (currentLocatedStay.getModeArr() == null || currentLocatedStay.getModeDep() == null) {
                        if (TPS_Logger.isLogging(SeverityLogLevel.FINE)) {
                            TPS_Logger.log(SeverityLogLevel.FINE,
                                    "Start select mode for each stay in tour part (id=" + tourpart.getId() + ")");
                        }
                        //do we have a fixed mode from the previous trip?
                        if (tourpart.isFixedModeUsed()) {
                            currentLocatedStay.setModeArr(tourpart.getUsedMode());
                            currentLocatedStay.setModeDep(tourpart.getUsedMode());
                        } else {
                            TPS_Location pLocGoingTo = plan.getLocatedStay(goingTo).getLocation();
                            TPS_Car tmpCar = pc.carForThisPlan;
                            if (tmpCar != null && tmpCar.isRestricted() &&
                                    (currentLocatedStay.getLocation().getTrafficAnalysisZone().isRestricted() ||
                                            pLocGoingTo.getTrafficAnalysisZone().isRestricted())) {
                                pc.carForThisPlan = null;
                            }
                            this.modeSelector.selectMode(plan, prevStaySupplier, currentLocatedStay, goingToSupplier, pc);
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
                    if (TPS_Logger.isLogging(SeverityLogLevel.DEBUG)) {
                        String s = "Chosen mode of Stay: " + currentLocatedStay.getEpisode().getId() + ": " +
                                currentLocatedStay.getModeArr() == null ? "NULL" :
                                currentLocatedStay.getModeArr().getName() + " in TAZ:" +
                                        currentLocatedStay.getLocation().getTrafficAnalysisZone().getTAZId() +
                                        " in block: " +
                                        (currentLocatedStay.getLocation().hasBlock() ? currentLocatedStay.getLocation()
                                                .getBlock()
                                                .getId() : -1) +
                                        " via" + currentLocatedStay.getModeArr().getName() + "/" +
                                        currentLocatedStay.getModeDep().getName();
                        TPS_Logger.log(SeverityLogLevel.DEBUG, s);
                        TPS_Logger.log(SeverityLogLevel.DEBUG,
                                "Selected mode (id=" + currentLocatedStay.getModeArr() == null ? "NULL" :
                                        currentLocatedStay.getModeArr().getName() + ") for stay (id=" +
                                                currentLocatedStay.getEpisode().getId() + " in TAZ (id=" +
                                                currentLocatedStay.getLocation().getTrafficAnalysisZone().getTAZId() +
                                                ") in block (id= " + (currentLocatedStay.getLocation()
                                                .hasBlock() ? currentLocatedStay.getLocation()
                                                .getBlock()
                                                .getId() : -1) +
                                                ") via modes " + currentLocatedStay.getModeArr().getName() + "/" +
                                                currentLocatedStay.getModeDep().getName());
                    }

                    //set travel time for the arriving mode
                    if (!tourpart.isFirst(stay)) {
                        plan.getPlannedTrip(tourpart.getPreviousTrip(stay)).setTravelTime(plan.getLocatedStay(prevStay),
                                plan.getLocatedStay(stay));
                    }
                    //set travel time for the departure mode
                    if (!tourpart.isLast(stay)) {
                        plan.getPlannedTrip(tourpart.getNextTrip(stay)).setTravelTime(plan.getLocatedStay(stay),
                                plan.getLocatedStay(goingTo));
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
//                        //todo need a fix
//                        prevLocatedStay.setModeDep(TPS_ExtMode.simpleWalk);
//                        locatedStay.setModeArr(TPS_ExtMode.simpleWalk);
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
