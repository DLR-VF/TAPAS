package de.dlr.ivf.tapas.plan.state.statemachine;

import de.dlr.ivf.tapas.log.TPS_Logger;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface;
import de.dlr.ivf.tapas.persistence.db.TPS_DB_IOManager;
import de.dlr.ivf.tapas.persistence.db.TPS_TripToDbWriter;
import de.dlr.ivf.tapas.plan.TPS_Plan;
import de.dlr.ivf.tapas.plan.state.action.TPS_PlanStatePersistenceAction;
import de.dlr.ivf.tapas.plan.state.action.TPS_PlanStateSelectLocationAndModeAction;
import de.dlr.ivf.tapas.scheme.TPS_SchemePart;
import de.dlr.ivf.tapas.scheme.TPS_Stay;
import de.dlr.ivf.tapas.scheme.TPS_TourPart;

import java.util.*;

public class TPS_PlanStateMachineFactory {

    public static TPS_PlanStateMachine<TPS_Plan> createTPS_PlanStateMachineWithSimpleStates(TPS_Plan plan, TPS_TripToDbWriter writer, TPS_DB_IOManager pm){

        // so we have a plan with our fix locations and some stays that neither have a selected location nor the used mode.
        //  -
        //
        Set<TPS_PlanState> states_set = new HashSet<>();


        int i = 0;
        int machine_start_time;
        boolean very_first_trip = true;
        TPS_PlanState initialState = new TPS_SimplePlanState(TPS_PlanStateConstantNames.EXECUTION_READY.getName());
        TPS_PlanStateMachine<TPS_Plan> stateMachine = new TPS_PlanStateMachine<>(initialState,states_set,"Person: "+plan.getPerson().getId(),plan);



        states_set.add(initialState);

        if (TPS_Logger.isLogging(TPS_LoggingInterface.SeverenceLogLevel.DEBUG)) {
            TPS_Logger.log(TPS_LoggingInterface.SeverenceLogLevel.DEBUG,
                    "Initializing state machine for plan with scheme (id=" + plan.getScheme().getId() + ")");
        }

        TPS_PlanState current_state = initialState;

        for (TPS_SchemePart schemePart : plan.getScheme()) {

            if (schemePart.isHomePart()) {
                // Home Parts are already set
                // create home state
                continue;
                //System.out.println("homestate created");
                //current_state = new TPS_SimplePlanState(TPS_PlanStateConstantNames.AT_HOME.getName());
                //states_set.add(current_state);
                //if (TPS_Logger.isLogging(TPS_LoggingInterface.SeverenceLogLevel.FINE)) {
                 //   TPS_Logger.log(TPS_LoggingInterface.SeverenceLogLevel.FINE, "Home part state (id=" + schemePart.getId() + ") created.");
                //}
             }else{

            TPS_TourPart tour_part = (TPS_TourPart) schemePart;

            if (TPS_Logger.isLogging(TPS_LoggingInterface.SeverenceLogLevel.FINE)) {
                TPS_Logger.log(TPS_LoggingInterface.SeverenceLogLevel.FINE,
                        "Start select location for each stay in tour part (id=" + tour_part.getId() + ")");
            }


            //when we loop over every stay we create an ON_MOVE and ON_ACTIVITY state without guards
            // - the current state will select during its onEnter-action the location and mode for the stay and also calculate travel times.
            // - since we now have the travel time, we can add new states with their respective guards
            for (TPS_Stay stay : tour_part.getPriorisedStayIterable()) {
                System.out.println("creating tourparts");
                TPS_PlanState move_state = new TPS_SimplePlanState(TPS_PlanStateConstantNames.ON_MOVE.getName());
                TPS_PlanState activity_state = new TPS_SimplePlanState(TPS_PlanStateConstantNames.ON_ACTIVITY.getName());

                states_set.add(move_state);
                states_set.add(activity_state);

                //when we exit an activity state, the trip has been fulfilled and can be written to the database
                activity_state.setOnExitAction(new TPS_PlanStatePersistenceAction(writer, plan, tour_part, stay));


                //inside the location and mode selection action, a handler is added to the current state that will transition into the ON_MOVE state
                //additionally a handler will be added to the ON_MOVE state targeting the activity_state
                //at the end our current state will be set to the activity state as we need to choose a new location as well as the mode to use during next iteration
                current_state.setOnEnterAction(new TPS_PlanStateSelectLocationAndModeAction(plan, tour_part, stay, pm, current_state, move_state, activity_state));
                current_state = activity_state;
            }}
        }
        initialState.enter();
        return stateMachine;
    }



//    boolean is_first_trip = true;
//
//        for(TPS_TourPart tp : plan.getScheme().getTourPartIterator()){
//        for(TPS_Trip trip : tp.getTripIterator()){
//
//            //we expect the following consecutive order in states after the initial state -> trip -> activity -> trip -> activity... -> end_state
//            TPS_PlanStateGuard trip_start_guard = input -> input == strategy.apply(plan.getPlannedTrip(trip).getStart());
//            TPS_PlanStateGuard activity_guard = input -> input == strategy.apply(plan.getLocatedStay(tp.getNextStay(trip)).getStart());
//            TPS_PlanStateGuard trip_end_guard = input -> input == strategy.apply(plan.getPlannedTrip(trip).getEnd());
//
//            TPS_SimplePlanState trip_start_state = new TPS_SimplePlanState("trip_start_"+trip.getId());
//
//            if(is_first_trip){
//                //set guard to initial state
//                initialState.addHandler(TPS_PlanEventType.SIMULATION_STEP, trip_start_state,new TPS_PlanStateNoAction(),trip_start_guard);
//                is_first_trip = false;
//            }else{
//                //set the guard to the previous activity
//                previous_state.addHandler(TPS_PlanEventType.SIMULATION_STEP, trip_start_state, new TPS_PlanStateNoAction(), trip_start_guard);
//            }
//
//            TPS_SimplePlanState activity_state = new TPS_SimplePlanState("activity_"+trip.getId());
//            TPS_SimplePlanState trip_end_state = new TPS_SimplePlanState("trip_end_"+trip.getId(),stateMachine,new TPS_PlanStateNoAction(), new TPS_PlanStatePersistenceAction(worker, plan, tp, trip));
//
//            trip_start_state.addHandler(TPS_PlanEventType.SIMULATION_STEP,activity_state,new TPS_PlanStateNoAction(),activity_guard);
//            activity_state.addHandler(TPS_PlanEventType.SIMULATION_STEP,trip_end_state, new TPS_PlanStateNoAction(), trip_end_guard);
//
//            //note that the last state is an activity state.
//            states_set.add(trip_start_state);
//            states_set.add(activity_state);
//            states_set.add(trip_end_state);
//            previous_state = trip_end_state;
//        }
//    }

}
