package de.dlr.ivf.tapas.execution.sequential.statemachine;

import de.dlr.ivf.tapas.execution.sequential.action.ActionProvider;
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


public class TPS_StateMachineFactory {

    private ActionProvider transition_actions_provider;

    public TPS_StateMachineFactory (ActionProvider transition_actions_provider){

        this.transition_actions_provider = transition_actions_provider;
    }

    public TPS_StateMachine createStateMachineWithSimpleStates(TPS_Plan plan){

        String identifier = Integer.toString(plan.getPerson().getId());

        TPS_StateMachine stateMachine = new TPS_StateMachine("Person: "+identifier);

        Map<EpisodeType, TPS_PlanState> state_mappings = generateStateMappings(stateMachine, identifier);

        TPS_Household hh = plan.getPerson().getHousehold();
        LocationContext location_context = new LocationContext(hh.getLocation());
        PlanContext plan_context = new PlanContext(plan, hh.getCarMediator(), location_context);

        TPS_PlanState activity_state = new TPS_SimplePlanState("activity_"+identifier, stateMachine);
        TPS_PlanState trip_state = new TPS_SimplePlanState("trip_"+identifier, stateMachine);
        TPS_PlanState end_state = new TPS_SimplePlanState("END_"+identifier, stateMachine);

        stateMachine.setAllStates( List.of(activity_state, trip_state, end_state) );

        stateMachine.setInitialStateAndReset(activity_state);

        Guard activity_to_trip_guard = new Guard(getSimulationEntryTime(plan));
        Guard trip_to_activity_guard = new Guard(Integer.MAX_VALUE);

        activity_state.addHandler(TPS_EventType.SIMULATION_STEP, trip_state, () -> transition_actions_provider.getActivityToTripActions(plan_context,activity_to_trip_guard, stateMachine), activity_to_trip_guard);
        trip_state.addHandler(TPS_EventType.SIMULATION_STEP, activity_state, () -> transition_actions_provider.getTripToActivityActions(plan_context,trip_to_activity_guard), trip_to_activity_guard);


//
//        Supplier<TPS_PlanStateAction> trip_planning_action_supplier = () -> new TPS_PlanStateSelectLocationAndModeAction();
//
//        Supplier<TPS_ModeChoiceContext> tourpart_mode_initializer = () -> new TPS_ModeChoiceContext(plan.getPerson(), household_sm_controller, car_sharing_mediators)
//                                                                                            .initBikeAvailability()
//                                                                                            .initCarAvailability();
//
//
//        Supplier<TPS_Episode> episode_supplier =
//
//        TPS_PlanState end_state = new TPS_SimplePlanState(EpisodeType.FINISHING_DAY);
//        end_state.setStateMachine(stateMachine);
//
//
//       // all_states.add(initial_state);
//
//        if (TPS_Logger.isLogging(TPS_LoggingInterface.SeverenceLogLevel.DEBUG)) {
//            TPS_Logger.log(TPS_LoggingInterface.SeverenceLogLevel.DEBUG,
//                    "Initializing state machine for plan with scheme (id=" + plan.getScheme().getId() + ")");
//        }
//
//        TPS_PlanState home_state = new TPS_SimplePlanState(EpisodeType.HOME, stateMachine);
//        TPS_PlanState trip_planning_state = new TPS_SimplePlanState(EpisodeType.TRIP_PLANNING, stateMachine);
//
//
//        List<TPS_SchemePart> scheme_parts = plan.getScheme().getSchemeParts();
//        home_state.addHandler(TPS_PlanEventType.SIMULATION_STEP, trip_planning_state, ActionSuppliers.SelectLocationAndMode((TPS_TourPart) scheme_parts.get(0)), sim_time -> (int) sim_time == simulation_entry_time);
//
//        TPS_PlanState trip_state = new TPS_SimplePlanState(EpisodeType.TRIP, stateMachine);
//        TPS_PlanState activity_state = new TPS_SimplePlanState(EpisodeType.ACTIVITY, stateMachine);
//
//
//        ListIterator<TPS_SchemePart> scheme_part_iterator = scheme_parts.listIterator(scheme_parts.size());
//
//
//        scheme_parts.stream().flatMap(List::stream).sorted(Comparator.comparing(TPS_Episode::getOriginalStart)).forEach(episode);
////---------------------------------------------------------------------
//        while(scheme_part_iterator.hasPrevious()){
//
//            TPS_PlanState consecutive_state = null;
//            TPS_Stay consecutive_stay = null;
//
//            TPS_SchemePart scheme_part = scheme_part_iterator.previous();
//
//            if(scheme_part.isHomePart()){
//
//                if(!scheme_part_iterator.hasNext()){ //this is our last home part
//                    all_states.add(end_state);
//                    consecutive_state = end_state;
//                    consecutive_stay  = (TPS_Stay) scheme_part.getFirstEpisode();
//                }else{
//                    TPS_PlanState home_state = new TPS_SimplePlanState(EpisodeType.HOME, stateMachine);
//                    consecutive_state
//                    home_state.addHandler(TPS_PlanEventType.SIMULATION_STEP);
//                    all_states.add(home_state);
//                }
//
//
//            }
//
//        }
//
//         //we do expect the scheme parts to be chronologically ordered, tour parts always starting and ending with trip as well as the episodes inside a tour part being chronologically ordered
//        int index = 0;
//        for(TPS_SchemePart sp : plan.getScheme()){
//            index++;
//            if(sp.isHomePart()){
//                all_stays.add((TPS_Stay) sp.getFirstEpisode());
//                all_states.add(new TPS_SimplePlanState(EpisodeType.HOME, stateMachine));
//            }else{
//                List<TPS_Episode> episodes = sp.getEpisodes();
//
//                for (TPS_Episode episode : episodes) {
//                    if (episode instanceof TPS_Trip) {
//                        all_trips.add((TPS_Trip) episode);
//                        TPS_PlanState trip_state = new TPS_SimplePlanState(EpisodeType.TRIP.getName() + "_" + index, stateMachine);
//                        all_states.add(trip_state);
//                        //when we exit an activity state, the trip has been fulfilled and can be written to the database
//                        trip_state.addOnExitAction(new TPS_PlanStatePersistenceAction(writer, plan, (TPS_TourPart) sp, (TPS_Trip) episode));
//                    } else {
//                        all_stays.add((TPS_Stay) episode);
//                        all_states.add(new TPS_SimplePlanState(EpisodeType.ACTIVITY.getName() + "_" +  index, stateMachine));
//
//                    }
//                }
//            }
//        }
//        all_states.add(end_state); //add the *magic* end_state at the end of the state list
//        //we should have twice the states than stays
//        if((all_states.size() ) / 2 != all_stays.size()){
//            throw new IllegalArgumentException("we got "+all_states.size()+" states against "+all_stays.size()+" stays... States count must be (2 * stays count)-1");
//        }
//
//        //we also should have n-stays - 1 trips because the first home stay is not preceded by a trip
//        if(all_stays.size()-1 != all_trips.size()){
//            throw new IllegalArgumentException("we got "+all_trips.size()+" trips against "+all_stays.size()+" stays... trips count must be stays count - 1");
//        }
//
//        for(int i = 0; i < all_trips.size(); i++){
//
//            TPS_Stay departure_stay = all_stays.get(i);
//            TPS_Trip associated_trip = all_trips.get(i);
//            TPS_Stay arrival_stay = all_stays.get(i+1);
//
//            //odd indexes are trip states and even indexes are activity states.
//            TPS_PlanState activity_state = all_states.get(i*2);
//            TPS_PlanState trip_state = all_states.get(i*2+1);
//            TPS_PlanState post_trip_activity_state = all_states.get(i*2+2);
//            TPS_PlanState post_activity_trip_state = all_states.get(i*2+3); // in case of the last stay this is the *magic* end_state
//
//
//
//            activity_state.addOnExitAction(new TPS_PlanStateSelectLocationAndModeAction(plan, (TPS_TourPart) associated_trip.getSchemePart(), associated_trip, departure_stay, arrival_stay, pm, trip_state, post_trip_activity_state, post_activity_trip_state, plan.getPerson().getHousehold().getCarMediator(),car_sharing_mediators));
//
//
//        }// day finished
//
//        //now set up the mandatory parameters of the state machine
//        stateMachine.setAllStates(all_states);
//        stateMachine.setInitialStateAndReset(all_states.get(0));
//        stateMachine.setEndState(end_state);
//        all_states.get(0).addHandler(TPS_PlanEventType.SIMULATION_STEP, all_states.get(1), null, input -> (int) input == simulation_entry_time);

        return stateMachine;
    }

    private Map<EpisodeType, TPS_PlanState> generateStateMappings(TPS_StateMachine state_machine, String identifier) {

        Map<EpisodeType,TPS_PlanState> state_mappings = new EnumMap<>(EpisodeType.class);

        state_mappings.put(EpisodeType.HOME, new TPS_SimplePlanState("home_"+identifier, state_machine));
        state_mappings.put(EpisodeType.TRIP, new TPS_SimplePlanState("trip_"+identifier, state_machine));
        state_mappings.put(EpisodeType.ACTIVITY, new TPS_SimplePlanState("activity_"+identifier, state_machine));

        return state_mappings;
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
