package de.dlr.ivf.tapas.plan.sequential.statemachine;

import de.dlr.ivf.tapas.log.TPS_Logger;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface;
import de.dlr.ivf.tapas.mode.TPS_CarSharingMediator;
import de.dlr.ivf.tapas.persistence.db.TPS_DB_IOManager;
import de.dlr.ivf.tapas.persistence.db.TPS_TripWriter;
import de.dlr.ivf.tapas.plan.TPS_Plan;
import de.dlr.ivf.tapas.plan.sequential.action.TPS_PlanStatePersistenceAction;
import de.dlr.ivf.tapas.plan.sequential.action.TPS_PlanStateSelectLocationAndModeAction;
import de.dlr.ivf.tapas.plan.sequential.event.TPS_PlanEventType;
import de.dlr.ivf.tapas.scheme.*;
import java.util.*;

public class TPS_PlanStateMachineFactory {

    public static TPS_PlanStateMachine createTPS_PlanStateMachineWithSimpleStates(TPS_Plan plan, TPS_TripWriter writer, TPS_DB_IOManager pm, Map<Integer, TPS_CarSharingMediator> car_sharing_mediators){

        List<TPS_PlanState> all_states = new ArrayList<>();

        List<TPS_Stay> all_stays = new ArrayList<>();
        List<TPS_Trip> all_trips = new ArrayList<>();

        TPS_PlanState end_state = new TPS_SimplePlanState(TPS_PlanStateConstantNames.FINISHING_DAY.getName()+"_"+plan.getPerson());

        TPS_PlanStateMachine stateMachine = new TPS_PlanStateMachine("Person: "+plan.getPerson().getId(),plan);

        int simulation_entry_time = (int) (plan.getScheme()
                                               .getSchemeParts()
                                               .get(0)
                                               .getFirstEpisode()
                                               .getOriginalDuration() * 1.66666666e-2 + 0.5);

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
                        TPS_PlanState trip_state = new TPS_SimplePlanState(TPS_PlanStateConstantNames.ON_TRIP.getName() + "_" + index, stateMachine);
                        all_states.add(trip_state);
                        //when we exit an activity state, the trip has been fulfilled and can be written to the database
                        trip_state.addOnExitAction(new TPS_PlanStatePersistenceAction(writer, plan, (TPS_TourPart) sp, (TPS_Trip) episode));
                    } else {
                        all_stays.add((TPS_Stay) episode);
                        all_states.add(new TPS_SimplePlanState(TPS_PlanStateConstantNames.ON_ACTIVITY.getName() + "_" +  index, stateMachine));

                    }
                }
            }
        }
        all_states.add(end_state); //add the *magic* end_state at the end of the state list
        //we should have twice the states than stays
        if((all_states.size() ) / 2 != all_stays.size()){
            throw new IllegalArgumentException("we got "+all_states.size()+" states against "+all_stays.size()+" stays... States count must be (2 * stays count)-1");
        }

        //we also should have n-stays - 1 trips because the first home stay is not preceded by a trip
        if(all_stays.size()-1 != all_trips.size()){
            throw new IllegalArgumentException("we got "+all_trips.size()+" trips against "+all_stays.size()+" stays... trips count must be stays count - 1");
        }

        for(int i = 0; i < all_trips.size(); i++){

            TPS_Stay departure_stay = all_stays.get(i);
            TPS_Trip associated_trip = all_trips.get(i);
            TPS_Stay arrival_stay = all_stays.get(i+1);

            //odd indexes are trip states and even indexes are activity states.
            TPS_PlanState activity_state = all_states.get(i*2);
            TPS_PlanState trip_state = all_states.get(i*2+1);
            TPS_PlanState post_trip_activity_state = all_states.get(i*2+2);
            TPS_PlanState post_activity_trip_state = all_states.get(i*2+3); // in case of the last stay this is the *magic* end_state



            activity_state.addOnExitAction(new TPS_PlanStateSelectLocationAndModeAction(plan, (TPS_TourPart) associated_trip.getSchemePart(), associated_trip, departure_stay, arrival_stay, pm, trip_state, post_trip_activity_state, post_activity_trip_state, plan.getPerson().getHousehold().getCarMediator(),car_sharing_mediators));


        }// day finished

        //now set up the mandatory parameters of the state machine
        stateMachine.setAllStates(all_states);
        stateMachine.setInitialStateAndReset(all_states.get(0));
        stateMachine.setEndState(end_state);
        all_states.get(0).addHandler(TPS_PlanEventType.SIMULATION_STEP, all_states.get(1), null, input -> (int) input == simulation_entry_time);

        return stateMachine;
    }
}
