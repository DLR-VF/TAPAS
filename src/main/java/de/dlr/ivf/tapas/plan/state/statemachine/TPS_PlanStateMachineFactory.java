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
import java.util.stream.IntStream;

public class TPS_PlanStateMachineFactory {

    public static TPS_PlanStateMachine createTPS_PlanStateMachineWithSimpleStates(TPS_Plan plan, TPS_TripWriter writer, TPS_DB_IOManager pm, TPS_PlansExecutor executor){

        List<TPS_PlanState> all_states = new ArrayList<>();

        List<TPS_Stay> all_stays = new ArrayList<>();
        List<TPS_Trip> all_trips = new ArrayList<>();

        //the initial state of the state machine
        //Note: that we need to set the owning state machine after we instantiated the state machine
        TPS_PlanState initial_state;
        TPS_PlanState end_state = new TPS_SimplePlanState(TPS_PlanStateConstantNames.FINISHING_DAY.getName()+"_"+plan.getPerson());

        TPS_PlanStateMachine stateMachine = new TPS_PlanStateMachine("Person: "+plan.getPerson().getId(),plan);

        Map<TPS_Stay,TPS_TourPart> stays_to_tourparts = new HashMap<>();

        int simulation_entry_time = (int) (plan.getScheme().getSchemeParts().get(0).getFirstEpisode().getOriginalDuration() * 1.66666666e-2 + 0.5);

        end_state.setStateMachine(stateMachine);


       // all_states.add(initial_state);

        if (TPS_Logger.isLogging(TPS_LoggingInterface.SeverenceLogLevel.DEBUG)) {
            TPS_Logger.log(TPS_LoggingInterface.SeverenceLogLevel.DEBUG,
                    "Initializing state machine for plan with scheme (id=" + plan.getScheme().getId() + ")");
        }

         //we do expect the scheme parts to be chronologically ordered, tour parts always starting and ending with trip as well as the episodes inside a tour part being chronologically ordered
        int index = 0;
        for(TPS_SchemePart sp : plan.getScheme()){
            index++;
            if(sp.isHomePart()){
                all_stays.add((TPS_Stay) sp.getFirstEpisode());
                all_states.add(new TPS_SimplePlanState(TPS_PlanStateConstantNames.AT_HOME.getName() + "_" + index, stateMachine));
            }else{
                List<TPS_Episode> episodes = sp.getEpisodes();
                for (TPS_Episode episode : episodes) {
                    if (episode instanceof TPS_Trip) {
                        all_trips.add((TPS_Trip) episode);
                        all_states.add(new TPS_SimplePlanState(TPS_PlanStateConstantNames.ON_TRIP.getName() + "_" + index, stateMachine));
                    } else {
                        all_stays.add((TPS_Stay) episode);
                        all_states.add(new TPS_SimplePlanState(TPS_PlanStateConstantNames.ON_ACTIVITY.getName() + "_" +  index, stateMachine));
                        stays_to_tourparts.put((TPS_Stay) episode, (TPS_TourPart) sp);
                    }
                }
            }
        }


        //every stay will consist of 2 states (1: trip state, 2: activity state) inside the state machine.
        //the very first stay (at home) is preceded by the initial state, so think of the initial state as a pseudo trip state



        //we skip the first home stay
       // TPS_PlanState trip_state; //we are at home so no trip yet.
        //TPS_PlanState activity_state = new TPS_SimplePlanState(TPS_PlanStateConstantNames.AT_HOME.getName(),stateMachine);
        //all_states.add(activity_state);


//        for(int i = 1; i < all_stays.size(); i++){
//            TPS_Stay stay = all_stays.get(i);
//            index++;
//
//            trip_state = new TPS_SimplePlanState(TPS_PlanStateConstantNames.ON_TRIP.getName() + "_" + index, stateMachine);
//            if(stay.isAtHome())
//                activity_state = new TPS_SimplePlanState(TPS_PlanStateConstantNames.AT_HOME.getName() + "_" + index, stateMachine);
//            else
//                activity_state = new TPS_SimplePlanState(TPS_PlanStateConstantNames.ON_ACTIVITY.getName() + "_" +  index, stateMachine);
//
//            all_states.add(trip_state);
//            all_states.add(activity_state);
//        }

        //we should have twice the states than stays
        if((all_states.size() + 1) / 2 != all_stays.size()){
            throw new IllegalArgumentException("we got "+all_states.size()+" states against "+all_stays.size()+" stays... States count must be (2 * stays count)-1");
        }

        //we also should have n-stays - 1 trips because the first home stay is not preceded by a trip
        if(all_stays.size()-1 != all_trips.size()){
            throw new IllegalArgumentException("we got "+all_trips.size()+" trips against "+all_stays.size()+" stays... trips count must be stays count - 1");
        }

        boolean is_first_trip = true;
        //now we add the appropriate enter/exit actions to every state
        //we will skip the first trip because we set the handler manually at the end
        for(int i = 0; i < all_trips.size(); i++){

            TPS_Stay departure_stay = all_stays.get(i);
            TPS_Trip associated_trip = all_trips.get(i);
            TPS_Stay arrival_stay = all_stays.get(i+1);

            TPS_TourPart tour_part = stays_to_tourparts.get(departure_stay);
            if(tour_part == null) // can happen if current stay is at home
                tour_part = stays_to_tourparts.get(arrival_stay); // so get tour part of next stay

            //we can safely start the stream at i+1 because 'i' won't exceed all_stays.size - 1
            //TPS_Stay next_fix_stay = IntStream.range(i+1,all_stays.size()).mapToObj(position -> all_stays.get(position)).filter(stay -> plan.getFixLocations().get(stay.getActCode()) != null).findFirst().get();
            TPS_Stay next_fix_stay = plan.getNextHomeStay(tour_part);

            //odd indexes are trip states (except the initial state which will be our pseudo trip state) and even indexes are activity states.
            //TPS_PlanState calling_activity_state = all_states.get(i*2-1);
            TPS_PlanState trip_state = all_states.get(i*2+1);
            TPS_PlanState post_trip_activity_state = all_states.get(i*2+2);
            TPS_PlanState post_activity_trip_state;


            if(i*2+3 < all_states.size()) { //we are still in the predefined states area
                post_activity_trip_state = all_states.get(i * 2 + 3);
            }else{ //no target state for the last activity thus we finish our day...
                post_activity_trip_state = end_state;
                all_states.add(end_state); //be careful here, this should only be executed during the very last iteration
            }


            //when we exit an activity state, the trip has been fulfilled and can be written to the database
            post_trip_activity_state.setOnEnterAction(new TPS_PlanStatePersistenceAction(writer, plan, tour_part, associated_trip));

            //when a home or activity state is being entered, we start looking for the next location and transport mode and set the appropriate guard conditions
            //for the first trip part, travel time must be subtracted from the start of the first stay to determine when we need to leave the house to get to the first location in time

            trip_state.setOnEnterAction(new TPS_PlanStateSelectLocationAndModeAction(plan, tour_part, associated_trip, departure_stay, arrival_stay,next_fix_stay, pm, trip_state, post_trip_activity_state, post_activity_trip_state, executor));

        }// day finished

        //now set up the mandatory parameters of the state machine
        stateMachine.setAllStates(all_states);
        stateMachine.setInitialStateAndReset(all_states.get(0));
        stateMachine.setEndState(end_state);
        all_states.get(0).addHandler(TPS_PlanEventType.SIMULATION_STEP, all_states.get(1), StateMachineUtils.NoAction(), input -> (int) input == simulation_entry_time);

        return stateMachine;
    }
}
