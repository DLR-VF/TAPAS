package de.dlr.ivf.tapas.plan.state;

import de.dlr.ivf.tapas.persistence.db.TPS_DB_IOManager;
import de.dlr.ivf.tapas.plan.TPS_Plan;
import de.dlr.ivf.tapas.plan.state.action.TPS_PlanStateNoAction;
import de.dlr.ivf.tapas.plan.state.action.TPS_PlanStatePersistenceAction;
import de.dlr.ivf.tapas.plan.state.event.TPS_PlanEventType;
import de.dlr.ivf.tapas.plan.state.guard.TPS_PlanStateGuard;
import de.dlr.ivf.tapas.plan.state.states.TPS_PlanStateConstantNames;
import de.dlr.ivf.tapas.plan.state.states.TPS_SimplePlanState;
import de.dlr.ivf.tapas.scheme.TPS_TourPart;
import de.dlr.ivf.tapas.scheme.TPS_Trip;
import java.lang.Integer;
import java.util.*;
import java.util.function.Function;

public class TPS_PlanStateMachineFactory {

    public static TPS_PlanStateMachine<TPS_Plan> createTPS_PlanStateMachineWithSimpleStates(TPS_SequentialWorker worker, TPS_Plan plan, Function<Integer, Integer> strategy, TPS_DB_IOManager pm){

        TPS_PlanState previous_state = null;
        TPS_PlanState initialState = new TPS_SimplePlanState(TPS_PlanStateConstantNames.EXECUTION_READY.getName());

        Set<TPS_PlanState> states_set = new HashSet<>();
        states_set.add(initialState);

        plan.balanceStarts();

        TPS_PlanStateMachine<TPS_Plan> stateMachine = new TPS_PlanStateMachine<>(initialState,states_set,"Person: "+plan.getPerson().getId(),plan);

        boolean is_first_trip = true;

        for(TPS_TourPart tp : plan.getScheme().getTourPartIterator()){
            for(TPS_Trip trip : tp.getTripIterator()){

                //we expect the following consecutive order in states after the initial state -> trip -> activity -> trip -> activity... -> end_state
                TPS_PlanStateGuard trip_start_guard = input -> input == strategy.apply(plan.getPlannedTrip(trip).getStart());
                TPS_PlanStateGuard activity_guard = input -> input == strategy.apply(plan.getLocatedStay(tp.getNextStay(trip)).getStart());
                TPS_PlanStateGuard trip_end_guard = input -> input == strategy.apply(plan.getPlannedTrip(trip).getEnd());

                TPS_SimplePlanState trip_start_state = new TPS_SimplePlanState("trip_start_"+trip.getId());

                if(is_first_trip){
                    //set guard to initial state
                    initialState.addHandler(TPS_PlanEventType.SIMULATION_STEP, trip_start_state,new TPS_PlanStateNoAction(),trip_start_guard);
                    is_first_trip = false;
                }else{
                    //set the guard to the previous activity
                    previous_state.addHandler(TPS_PlanEventType.SIMULATION_STEP, trip_start_state, new TPS_PlanStateNoAction(), trip_start_guard);
                }

                TPS_SimplePlanState activity_state = new TPS_SimplePlanState("activity_"+trip.getId());
                TPS_SimplePlanState trip_end_state = new TPS_SimplePlanState("trip_end_"+trip.getId(),stateMachine,new TPS_PlanStateNoAction(), new TPS_PlanStatePersistenceAction(worker, plan, tp, trip));

                trip_start_state.addHandler(TPS_PlanEventType.SIMULATION_STEP,activity_state,new TPS_PlanStateNoAction(),activity_guard);
                activity_state.addHandler(TPS_PlanEventType.SIMULATION_STEP,trip_end_state, new TPS_PlanStateNoAction(), trip_end_guard);

                //note that the last state is an activity state.
                states_set.add(trip_start_state);
                states_set.add(activity_state);
                states_set.add(trip_end_state);
                previous_state = trip_end_state;
            }
        }

        //now add the end state
        TPS_PlanState end_state = new TPS_SimplePlanState(TPS_PlanStateConstantNames.EXECUTION_DONE.getName());
        previous_state.addHandler(TPS_PlanEventType.SIMULATION_STEP,end_state,new TPS_PlanStateNoAction(), input -> true);

        return stateMachine;
    }

}
