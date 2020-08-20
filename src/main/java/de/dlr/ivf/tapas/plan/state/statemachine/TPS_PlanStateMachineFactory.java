package de.dlr.ivf.tapas.plan.state.statemachine;

import de.dlr.ivf.tapas.log.TPS_Logger;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface;
import de.dlr.ivf.tapas.persistence.db.TPS_DB_IOManager;
import de.dlr.ivf.tapas.persistence.db.TPS_TripWriter;
import de.dlr.ivf.tapas.plan.StateMachineUtils;
import de.dlr.ivf.tapas.plan.TPS_Plan;
import de.dlr.ivf.tapas.plan.state.TPS_PlansExecutor;
import de.dlr.ivf.tapas.plan.state.action.TPS_PlanStatePersistenceAction;
import de.dlr.ivf.tapas.plan.state.action.TPS_PlanStateSelectLocationAndModeAction;
import de.dlr.ivf.tapas.plan.state.event.TPS_PlanEventType;
import de.dlr.ivf.tapas.scheme.*;
import java.util.*;

public class TPS_PlanStateMachineFactory {

    public static TPS_PlanStateMachine<TPS_Plan> createTPS_PlanStateMachineWithSimpleStates(TPS_Plan plan, TPS_TripWriter writer, TPS_DB_IOManager pm, TPS_PlansExecutor executor){

        List<TPS_PlanState> all_states = new ArrayList<>();

        List<TPS_Stay> all_stays = new ArrayList<>();
        List<TPS_Trip> all_trips = new ArrayList<>();

        //the initial state of the state machine
        //Note: that we need to set the owning state machine after we instantiated the state machine
        TPS_PlanState initial_state = new TPS_SimplePlanState(TPS_PlanStateConstantNames.INITIALIZING.getName());
        TPS_PlanState end_state = new TPS_SimplePlanState(TPS_PlanStateConstantNames.FINISHING_DAY.getName()+"_"+plan.getPerson());

        TPS_PlanStateMachine<TPS_Plan> stateMachine = new TPS_PlanStateMachine<>(initial_state,all_states,end_state,"Person: "+plan.getPerson().getId(),plan);

        Map<TPS_Stay,TPS_TourPart> stays_to_tourparts = new HashMap<>();

        initial_state.setStateMachine(stateMachine);
        end_state.setStateMachine(stateMachine);


        all_states.add(initial_state);

        if (TPS_Logger.isLogging(TPS_LoggingInterface.SeverenceLogLevel.DEBUG)) {
            TPS_Logger.log(TPS_LoggingInterface.SeverenceLogLevel.DEBUG,
                    "Initializing state machine for plan with scheme (id=" + plan.getScheme().getId() + ")");
        }

         //we do expect the scheme parts to be chronologically ordered, tour parts always starting and ending with trip as well as the episodes inside a tour part being chronologically ordered
        for(TPS_SchemePart sp : plan.getScheme()){
            if(sp.isHomePart()){
                all_stays.add((TPS_Stay) sp.getFirstEpisode());
            }else{
                List<TPS_Episode> episodes = sp.getEpisodes();
                for (TPS_Episode episode : episodes) {
                    if (episode instanceof TPS_Trip) {
                        all_trips.add((TPS_Trip) episode);
                    } else {
                        all_stays.add((TPS_Stay) episode);
                        stays_to_tourparts.put((TPS_Stay) episode, (TPS_TourPart) sp);
                    }
                }
            }
        }

        //every stay will consist of 2 states (1: trip state, 2: activity state) inside the state machine.
        //the very first stay (at home) is preceded by the initial state, so think of the initial state as a pseudo trip state

        int index = 0;

        //we skip the first home stay
        TPS_PlanState trip_state; //we are at home so no trip yet.
        TPS_PlanState activity_state = new TPS_SimplePlanState(TPS_PlanStateConstantNames.AT_HOME.getName(),stateMachine);
        all_states.add(activity_state);

        //immediately transition into the first activity state to trigger its on enter action
        initial_state.addHandler(TPS_PlanEventType.INIT_FIRST_STAY, activity_state, StateMachineUtils.NoAction(), input -> true);

        //now create all states for the remaining stays
        for(int i = 1; i < all_stays.size(); i++){
            TPS_Stay stay = all_stays.get(i);
            index++;

            trip_state = new TPS_SimplePlanState(TPS_PlanStateConstantNames.ON_TRIP.getName() + "_" + index, stateMachine);
            if(stay.isAtHome())
                activity_state = new TPS_SimplePlanState(TPS_PlanStateConstantNames.AT_HOME.getName() + "_" + index, stateMachine);
            else
                activity_state = new TPS_SimplePlanState(TPS_PlanStateConstantNames.ON_ACTIVITY.getName() + "_" +  index, stateMachine);

            all_states.add(trip_state);
            all_states.add(activity_state);
        }

        //we should have twice the states than stays
        if(all_states.size() != all_stays.size() * 2){
            throw new IllegalArgumentException("we got "+all_states.size()+" states against "+all_stays.size()+" stays... States count must be 2 * stays count");
        }

        //we also should have n-stays - 1 trips because the first home stay is not preceded by a trip
        if(all_stays.size()-1 != all_trips.size()){
            throw new IllegalArgumentException("we got "+all_trips.size()+" trips against "+all_stays.size()+" stays... trips count must be stays count - 1");
        }

        TPS_PlanState after_activity_state;
        all_states.get(0).addHandler(TPS_PlanEventType.INIT_FIRST_STAY, all_states.get(1), StateMachineUtils.NoAction(), input -> true);
        //now add the appropriate actions
        for(int i = 1; i < all_stays.size(); i++){

            TPS_Stay current_stay = all_stays.get(i-1);
            TPS_Stay next_stay = all_stays.get(i);

            TPS_TourPart tour_part = stays_to_tourparts.get(current_stay);
            if(tour_part == null) // can happen if current stay is at home
                tour_part = stays_to_tourparts.get(next_stay); // so get tour part of next stay

            TPS_Stay next_home_stay = plan.getNextHomeStay(tour_part);

            TPS_Trip previous_trip = null;
            if(i != 1)
                previous_trip = all_trips.get(i-2);

            TPS_Trip associated_trip = all_trips.get(i-1);


            //even indexes are trip states (except the initial state which will be our pseudo trip state) and odd indexes are activity states.
            TPS_PlanState calling_activity_state = all_states.get(i*2-1);
            TPS_PlanState next_trip_state = all_states.get(i*2);
            TPS_PlanState next_activity_state = all_states.get(i*2+1);


            if(i*2+2 < all_states.size()) { //we are still in the predefined states area
                after_activity_state = all_states.get(i * 2 + 2);
            }else{ //no target state for the last activity thus we finish our day...
                after_activity_state = end_state;
                all_states.add(end_state); //be careful here, this should only be executed during the very last iteration
            }


            //when we exit an activity state, the trip has been fulfilled and can be written to the database
            calling_activity_state.setOnExitAction(new TPS_PlanStatePersistenceAction(writer, plan, tour_part, associated_trip));

            //when a home or activity state is being entered, we start looking for the next location and transport mode and set the appropriate guard conditions
            //for the first trip part, travel time must be subtracted from the start of the first stay to determine when we need to leave the house to get to the first location in time

            calling_activity_state.setOnEnterAction(new TPS_PlanStateSelectLocationAndModeAction(plan, tour_part, previous_trip, associated_trip, current_stay, next_stay,next_home_stay, pm, calling_activity_state, next_trip_state, next_activity_state, after_activity_state, executor));

        }// day finished

        return stateMachine;
    }
}
