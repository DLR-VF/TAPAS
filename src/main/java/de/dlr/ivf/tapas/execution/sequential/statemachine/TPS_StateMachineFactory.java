package de.dlr.ivf.tapas.execution.sequential.statemachine;

import de.dlr.ivf.tapas.execution.sequential.action.ActionProvider;
import de.dlr.ivf.tapas.execution.sequential.action.TPS_PlanStateAction;
import de.dlr.ivf.tapas.execution.sequential.choice.LocationContext;
import de.dlr.ivf.tapas.execution.sequential.communication.SharingMediator;
import de.dlr.ivf.tapas.execution.sequential.context.PlanContext;
import de.dlr.ivf.tapas.execution.sequential.guard.Guard;
import de.dlr.ivf.tapas.mode.TPS_ModeValidator;
import de.dlr.ivf.tapas.mode.TazBasedCarSharingDelegator;
import de.dlr.ivf.tapas.persistence.db.TPS_DB_IOManager;
import de.dlr.ivf.tapas.persistence.db.TPS_TripWriter;
import de.dlr.ivf.tapas.person.TPS_Car;
import de.dlr.ivf.tapas.person.TPS_Household;
import de.dlr.ivf.tapas.plan.TPS_Plan;
import de.dlr.ivf.tapas.execution.sequential.event.TPS_EventType;
import de.dlr.ivf.tapas.scheme.TPS_Episode;
import de.dlr.ivf.tapas.util.FuncUtils;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Supplier;


public class TPS_StateMachineFactory {

    private ActionProvider transition_actions_provider;
    private final TPS_SimplePlanState error_state = new TPS_SimplePlanState("error", null);
    private int immediately_finished_sm_cnt = 0;
    public TPS_StateMachineFactory (ActionProvider transition_actions_provider){

        this.transition_actions_provider = transition_actions_provider;
    }

    public TPS_StateMachine createStateMachineWithSimpleStates(TPS_Plan plan){

        String identifier = Integer.toString(plan.getPerson().getId());

        TPS_StateMachine stateMachine = new TPS_StateMachine("Person: "+identifier);

        TPS_PlanState end_state = new TPS_SimplePlanState("END_" + identifier, stateMachine);
        stateMachine.setEndState(end_state);
        stateMachine.setErrorState(error_state);

        if(plan.getScheme().getSchemeParts().size() == 1) {
            stateMachine.setInitialStateAndReset(end_state);
            immediately_finished_sm_cnt++;
            return stateMachine;
        }

        TPS_Household hh = plan.getPerson().getHousehold();
        LocationContext location_context = new LocationContext(hh.getLocation());
        PlanContext plan_context = new PlanContext(plan, hh.getCarMediator(), location_context);

        TPS_PlanState activity_state = new TPS_SimplePlanState("activity_" + identifier, stateMachine);
        TPS_PlanState trip_state = new TPS_SimplePlanState("trip_" + identifier, stateMachine);

        stateMachine.setAllStates(List.of(activity_state, trip_state, end_state, error_state));
        stateMachine.setInitialStateAndReset(activity_state);

        Guard activity_to_trip_guard = new Guard(getSimulationEntryTime(plan));
        Guard trip_to_activity_guard = new Guard(Integer.MAX_VALUE);

        //add guard and action supplier for activity to trip state transition
        Supplier<List<TPS_PlanStateAction>> act2trip_actions = () -> transition_actions_provider.getActivityToTripActions(plan_context,trip_to_activity_guard, stateMachine);
        activity_state.addHandler(TPS_EventType.SIMULATION_STEP, trip_state, act2trip_actions, activity_to_trip_guard);

        Supplier<List<TPS_PlanStateAction>> trip2act_actions = () -> transition_actions_provider.getTripToActivityActions(plan_context,activity_to_trip_guard, stateMachine);
        trip_state.addHandler(TPS_EventType.SIMULATION_STEP, activity_state, trip2act_actions, trip_to_activity_guard);

        return stateMachine;
    }

    private Map<EpisodeType, TPS_PlanState> generateStateMappings(TPS_StateMachine state_machine, String identifier) {

        Map<EpisodeType,TPS_PlanState> state_mappings = new EnumMap<>(EpisodeType.class);

        state_mappings.put(EpisodeType.HOME, new TPS_SimplePlanState("home_"+identifier, state_machine));
        state_mappings.put(EpisodeType.TRIP, new TPS_SimplePlanState("trip_"+identifier, state_machine));
        state_mappings.put(EpisodeType.ACTIVITY, new TPS_SimplePlanState("activity_"+identifier, state_machine));

        return state_mappings;
    }

    public int getImmediatelyFinishedStateMachineCnt(){
        return this.immediately_finished_sm_cnt;
    }

    private int getSimulationEntryTime(TPS_Plan plan){

        return FuncUtils.secondsToRoundedMinutes.apply(
                plan.getScheme()
                        .getSchemeParts()
                        .get(0)
                        .getFirstEpisode()
                        .getOriginalDuration()
        );
    }
}
