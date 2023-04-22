package de.dlr.ivf.tapas.execution.sequential.action;

import de.dlr.ivf.tapas.constants.TPS_ActivityConstant;
import de.dlr.ivf.tapas.execution.sequential.context.TourContext;
import de.dlr.ivf.tapas.loc.TPS_Location;
import de.dlr.ivf.tapas.log.TPS_Logger;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface;
import de.dlr.ivf.tapas.mode.TazBasedCarSharingDelegator;
import de.dlr.ivf.tapas.mode.TPS_ExtMode;
import de.dlr.ivf.tapas.mode.TPS_Mode;
import de.dlr.ivf.tapas.persistence.TPS_PersistenceManager;
import de.dlr.ivf.tapas.plan.*;
import de.dlr.ivf.tapas.execution.sequential.communication.TPS_HouseholdCarMediator;
import de.dlr.ivf.tapas.execution.sequential.statemachine.TPS_PlanState;
import de.dlr.ivf.tapas.scheme.TPS_Stay;
import de.dlr.ivf.tapas.scheme.TPS_TourPart;
import de.dlr.ivf.tapas.scheme.TPS_Trip;
import de.dlr.ivf.tapas.util.FuncUtils;
import de.dlr.ivf.tapas.util.TPS_AttributeReader;
import de.dlr.ivf.tapas.util.parameters.ParamFlag;


import java.util.Map;
import java.util.Objects;

public class SelectLocationAndModeAction implements TPS_PlanStateAction {

    TPS_TourPart tour_part;
    TPS_Plan plan;
    TPS_Stay departure_stay;
    TPS_Stay arrival_stay;
    TPS_PersistenceManager pm;
    TPS_PlanState trip_state;
    TPS_PlanState post_activity_trip_state;
    TPS_PlanState post_trip_activity_state;
    TPS_HouseholdCarMediator car_mediator;
    Map<Integer, TazBasedCarSharingDelegator> car_sharing_mediators;

    private TourContext tour_context;

    TPS_Trip associated_trip;


    public SelectLocationAndModeAction(TPS_TourPart tp) {
    }

    public SelectLocationAndModeAction(TourContext tour_context){
        Objects.requireNonNull(tour_part);
        Objects.requireNonNull(plan);
        Objects.requireNonNull(arrival_stay);
        Objects.requireNonNull(pm);
        Objects.requireNonNull(trip_state);
        Objects.requireNonNull(post_activity_trip_state);
        Objects.requireNonNull(post_trip_activity_state);
        Objects.requireNonNull(departure_stay);
        Objects.requireNonNull(associated_trip);

        this.tour_context = tour_context;

        this.tour_part = tour_part;
        this.plan = plan;
        this.arrival_stay = arrival_stay;
        this.pm = pm;
        this.trip_state = trip_state;
        this.post_activity_trip_state = post_activity_trip_state;
        this.post_trip_activity_state = post_trip_activity_state;
        this.departure_stay = departure_stay;
        this.associated_trip = associated_trip;
        this.car_mediator = car_mediator;
        this.car_sharing_mediators = car_sharing_mediators;
    }


    @Override
    public void run() {




//        if (tour_part.isBikeUsed()) { //was the bike used before?
//            pc.isBikeAvailable = true;
//        } else if (!pc.influenceBikeUsageInPlan) { // is the bike availability modded outside?
//            pc.isBikeAvailable = plan.getPerson().hasBike();
//        }

       //plan.getAttributes().put(TPS_AttributeReader.TPS_Attribute.PERSON_HAS_BIKE, pc.isBikeAvailable ? 1 : 0);

//        if (pc.carForThisPlan == null) {
//
//
//            plan.getAttributes().put(TPS_AttributeReader.TPS_Attribute.HOUSEHOLD_CARS, 0);
//        } else {
//            plan.getAttributes().put(TPS_AttributeReader.TPS_Attribute.HOUSEHOLD_CARS, plan.getPerson().getHousehold().getCarNumber());
//        }

        plan.getAttributes().put(TPS_AttributeReader.TPS_Attribute.CURRENT_EPISODE_ACTIVITY_CODE_TAPAS,
                arrival_stay.getActCode().getCode(TPS_ActivityConstant.TPS_ActivityCodeType.TAPAS));

        plan.getPerson().estimateAccessibilityPreference(arrival_stay,
                pm.getParameters().isTrue(ParamFlag.FLAG_USE_SHOPPING_MOTIVES));

        TPS_LocatedStay arrival_located_stay = plan.getLocatedStay(arrival_stay);

        if (!arrival_located_stay.isLocated()) {
            plan.setCurrentAdaptedEpisode(arrival_located_stay);
            TPS_ActivityConstant currentActCode = arrival_stay.getActCode();

            // Register locations for activities where the location will be used again.
            // Flag for the case of a activity with unique location.
          //  pc.fixLocationAtBase = currentActCode.isFix() && pm.getParameters().isTrue(
           //         ParamFlag.FLAG_USE_FIXED_LOCS_ON_BASE) && !plan.getFixLocations().containsKey(currentActCode);

            //check for a reusable location
            if (currentActCode.isFix() && plan.getFixLocations().containsKey(currentActCode) &&
                //now we check if the mode is fix and if we can reach the fix location with the fix mode!
                //TODO: check only for restricted cars, but bike could also be fixed and no connection!
                !(plan.getPlanningContext().carForThisPlan != null && // we have a car
                        plan.getPlanningContext().carForThisPlan.isRestricted() && //we have a restricted car
                        plan.getFixLocations().get(currentActCode).getTrafficAnalysisZone()
                                .isRestricted())) // we have a restricted car wanting to go to a restricted area! -> BAD!
                {

                    arrival_located_stay.setLocation(plan.getFixLocations().get(currentActCode));

                if (TPS_Logger.isLogging(TPS_LoggingInterface.HierarchyLogLevel.EPISODE, TPS_LoggingInterface.SeverenceLogLevel.FINE)) {
                    TPS_Logger.log(TPS_LoggingInterface.HierarchyLogLevel.EPISODE, TPS_LoggingInterface.SeverenceLogLevel.FINE,
                            "Set location from fix locations");
                }
            } else {
                //arrival_located_stay.selectLocation(plan, plan.getPlanningContext(), () -> plan.getNextHomeStay(tour_part));
                if (currentActCode.isFix()) {
                    plan.getFixLocations().put(currentActCode, plan.getLocatedStay(arrival_stay).getLocation());
                }
            }

            if (arrival_located_stay.getLocation() == null) {
                TPS_Logger.log(TPS_LoggingInterface.SeverenceLogLevel.ERROR, "No Location found!");
            }
        }

        //do we have a fixed mode from the previous trip?
        TPS_ExtMode departure_mode;
//        if (departure_located_stay.getModeArr() != null && departure_located_stay.getModeArr().isFix()) {
//            departure_mode = departure_located_stay.getModeArr();
//        } else
//            departure_mode = pm.getModeSet().selectDepartureMode(plan, departure_located_stay, arrival_located_stay, pc);
//
//        double travel_distance = TPS_Geometrics.getDistance(departure_located_stay.getLocation(),arrival_located_stay.getLocation(), departure_mode.primary, SimulationType.SCENARIO);
//        checkIfValidModeOrSelectAlternative(departure_mode, pc, departure_located_stay, arrival_located_stay,travel_distance);
//
//        departure_located_stay.setModeDep(departure_mode);


        //after mode selection, set arrival mode to the destination and update the planning context for next mode selection

        //todo needed?
//        if (pc.carForThisPlan == null) {
//            plan.getAttributes().put(TPS_AttributeReader.TPS_Attribute.HOUSEHOLD_CARS, 0);
//        } else {
//            plan.getAttributes().put(TPS_AttributeReader.TPS_Attribute.HOUSEHOLD_CARS,
//                    plan.getPerson().getHousehold().getCarNumber());
//        }
//        plan.getAttributes().put(TPS_AttributeReader.TPS_Attribute.PERSON_HAS_BIKE, pc.isBikeAvailable ? 1 : 0);



        if (TPS_Logger.isLogging(TPS_LoggingInterface.SeverenceLogLevel.DEBUG)) {
            TPS_Logger.log(TPS_LoggingInterface.SeverenceLogLevel.DEBUG,
                    "Selected mode (id=" + arrival_located_stay.getModeArr() == null ? "NULL" :
                            arrival_located_stay.getModeArr().getName() + ") for stay (id=" +
                                    arrival_located_stay.getEpisode().getId() + " in TAZ (id=" +
                                    arrival_located_stay.getLocation().getTrafficAnalysisZone().getTAZId() +
                                    ") in block (id= " +
                                    (arrival_located_stay.getLocation().hasBlock() ? arrival_located_stay
                                            .getLocation().getBlock().getId() : -1) + ") via modes " +
                                    arrival_located_stay.getModeArr().getName() + "/" +
                                    arrival_located_stay.getModeDep().getName());
        }


        //departure_located_stay.setModeDep(departure_mode);
        TPS_PlannedTrip planned_trip = plan.getPlannedTrip(associated_trip);
       // planned_trip.setTravelTime(departure_located_stay, arrival_located_stay);

        //now update travel times and init guards

        //planned_trip.setStart(departure_located_stay.getStart() + departure_located_stay.getDuration());


        arrival_located_stay.setStart(planned_trip.getStart() + planned_trip.getDuration());


        //update the travel durations for this plan
        tour_part.updateActualTravelDurations(plan);

        //add transition handlers
        var trip_to_activity_transition_minute = FuncUtils.secondsToRoundedMinutes.apply(arrival_located_stay.getStart());
       // trip_state.addHandler(TPS_EventType.SIMULATION_STEP, post_trip_activity_state, null, i -> (int) i == trip_to_activity_transition_minute);


//        if(departure_stay.isAtHome()){
//            if(departure_mode.isCarUsed()) {
//                trip_state.addOnEnterAction(new TPS_CarCheckOutAction(this.car_mediator,this.plan.getPerson(),pc.carForThisPlan));
//                //plan.usedCars.add(pc.carForThisPlan); //todo is this really needed?
//                pc.isBikeAvailable = false; // reset the bike for the next mode selection in this tour
//            }
//            else
//                pc.carForThisPlan = null;  // reset the requested car for next mode selection in this tour
//        }else{
//            int time_diff_to_original_stay = arrival_located_stay.getStart() - arrival_stay.getOriginalStart();
//            this.car_mediator.updateNextRequest(plan.getPerson(), (int)tour_part.getOriginalSchemePartEnd() + time_diff_to_original_stay);
//        }
//
//
//
//
//        var activity_to_next_trip_transition_minute = FuncUtils.secondsToRoundedMinutes.apply(arrival_located_stay.getStart() + arrival_located_stay.getDuration());
//
//        //tour part is over and we check in the car (null if we didn't use one)
//        if(pc.carForThisPlan != null) {
//
//            //check if car is part of household
//            if(this.car_mediator.isHouseholdCar(pc.carForThisPlan))
//                if(arrival_stay.isAtHome())
//                    trip_state.addOnExitAction(new TPS_CarCheckInAction(this.car_mediator, pc));
//            else
//                trip_state.addOnExitAction(new TPS_CarSharingCheckInAndUpdateRangeAction(pc.carForThisPlan, travel_distance, car_sharing_mediators.get(arrival_located_stay.getLocation().getTAZId()),departure_located_stay.getLocation().getTAZId()));
//        }
//        post_trip_activity_state.addHandler(TPS_EventType.SIMULATION_STEP, post_activity_trip_state, null, i -> i == activity_to_next_trip_transition_minute);
//
//
//        //check in the occupancy
//        post_trip_activity_state.addOnEnterAction(new TPS_PlanStateUpdateOccupancyAction(arrival_located_stay.getLocation(), -1));
//
//        //check out the occupancy
//        post_trip_activity_state.addOnExitAction(new TPS_PlanStateUpdateOccupancyAction(arrival_located_stay.getLocation(),1));
//
//
//
//        plan.getAttributes().put(TPS_AttributeReader.TPS_Attribute.CURRENT_TAZ_SETTLEMENT_CODE_TAPAS,
//                arrival_located_stay.getLocation().getTrafficAnalysisZone().getBbrType()
//                        .getCode(TPS_SettlementSystem.TPS_SettlementSystemType.TAPAS));

    }

    private void checkIfValidModeOrSelectAlternative(TPS_ExtMode selected_mode, TPS_PlanningContext pc, TPS_LocatedStay departure, TPS_LocatedStay destination, double travel_distance){
        if (selected_mode.primary.isType(TPS_Mode.ModeType.MIT) && pc.carForThisPlan == null) {
            selectCarAlternative(pc, departure.getLocation(), destination.getLocation(), travel_distance);
            if(pc.carForThisPlan == null)
                selected_mode.primary = TPS_Mode.get(TPS_Mode.ModeType.PT);
            else {
                //car sharing car has been taken and we need to add the checkin in the target TAZ
                System.out.println("Carsharing taken");


            }
        }

        if(selected_mode.primary.isType(TPS_Mode.ModeType.MIT_PASS) && !departure.getStay().isAtHome())
            selected_mode.primary = TPS_Mode.get(TPS_Mode.ModeType.PT);
        if (selected_mode.primary.isType(TPS_Mode.ModeType.BIKE) && !pc.isBikeAvailable) {
            selected_mode.primary = TPS_Mode.get(TPS_Mode.ModeType.WALK);
        }
    }

    private void selectCarAlternative(TPS_PlanningContext pc, TPS_Location departure_location, TPS_Location destination_location, double travel_distance){
        if(this.plan.getPerson().isCarPooler()){
            //pc.carForThisPlan = car_sharing_mediators.get(departure_location.getTAZId()).request(travel_distance, destination_location.getTAZId());
        }
    }
}
