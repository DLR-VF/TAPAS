package de.dlr.ivf.tapas.execution.sequential.action;

import de.dlr.ivf.tapas.execution.sequential.choice.LocationContext;
import de.dlr.ivf.tapas.execution.sequential.context.ContextUpdateable;
import de.dlr.ivf.tapas.execution.sequential.context.ModeContext;
import de.dlr.ivf.tapas.execution.sequential.context.TourContext;
import de.dlr.ivf.tapas.execution.sequential.context.PlanContext;
import de.dlr.ivf.tapas.execution.sequential.guard.Guard;
import de.dlr.ivf.tapas.execution.sequential.statemachine.TPS_StateMachine;
import de.dlr.ivf.tapas.mode.*;
import de.dlr.ivf.tapas.persistence.TPS_PersistenceManager;
import de.dlr.ivf.tapas.persistence.db.TPS_DB_IOManager;
import de.dlr.ivf.tapas.persistence.db.TPS_TripWriter;
import de.dlr.ivf.tapas.person.TPS_Car;
import de.dlr.ivf.tapas.person.TPS_Person;
import de.dlr.ivf.tapas.plan.TPS_LocatedStay;
import de.dlr.ivf.tapas.plan.TPS_Plan;
import de.dlr.ivf.tapas.plan.TPS_PlannedTrip;
import de.dlr.ivf.tapas.plan.TPS_PlanningContext;
import de.dlr.ivf.tapas.runtime.server.SimTimeProvider;
import de.dlr.ivf.tapas.scheme.TPS_Episode;
import de.dlr.ivf.tapas.scheme.TPS_Stay;
import de.dlr.ivf.tapas.scheme.TPS_Trip;
import java.util.ArrayList;
import java.util.List;

import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * This class provides all the actionable behaviour for state machine transitions
 */
public class ActionProvider {

    private final TPS_PersistenceManager pm;
    private final TPS_TripWriter writer;
    private final BiFunction<TPS_Episode, Supplier<Integer>, Integer> guard_adaption_function;
    private final TPS_ModeValidator mode_validator;
    private final TazBasedCarSharingDelegator car_sharing_delegator;
    private Predicate<TPS_Car> car_filter;
    private SimTimeProvider sim_time_provider;

    /**
     *
     * @param pm the IO managing instance
     * @param writer the writer to write trips to
     * @param guard_adaption_function function that adapts transition guards
     * @param mode_validator mode validating instance
     * @param car_sharing_delegator the simulations car sharing manager
     */
    public ActionProvider(TPS_DB_IOManager pm, TPS_TripWriter writer, BiFunction<TPS_Episode,Supplier<Integer>,
            Integer> guard_adaption_function, TPS_ModeValidator mode_validator, TazBasedCarSharingDelegator car_sharing_delegator) {

        this.pm = pm;
        this.writer = writer;
        this.guard_adaption_function = guard_adaption_function;
        this.mode_validator = mode_validator;
        this.car_sharing_delegator = car_sharing_delegator;
    }

    /**
     *
     * @param plan_context the plan context of a specific person
     * @param trip_to_activity_guard the guard that triggers the transition after this one
     * @return a list of actions to be executed when transitioning from an activity to a trip
     */
    public List<TPS_PlanStateAction> getActivityToTripActions(PlanContext plan_context, Guard trip_to_activity_guard){

        List<TPS_PlanStateAction> transition_actions = new ArrayList<>();

        plan_context
                .getTourContext()
                .ifPresent(tc -> transition_actions.addAll(generateAndGetActivityToTripActions(plan_context,tc, trip_to_activity_guard)));

      return transition_actions;
    }

    /**
     *
     * @param plan_context the plan context of a specific person
     * @param activity_to_trip_guard the guard that triggers the transition after this one
     * @param state_machine the state machine that represents a specific person
     * @return a list of actions to be executed when transitioning from a trip to an activity
     */
    public List<TPS_PlanStateAction> getTripToActivityActions(PlanContext plan_context, Guard activity_to_trip_guard, TPS_StateMachine state_machine){

        List<TPS_PlanStateAction> transition_actions = new ArrayList<>();

        plan_context.getTourContext().ifPresent(tc -> transition_actions.addAll(generateAndGetTripToActivityActions(plan_context,tc,activity_to_trip_guard, state_machine)));

        return transition_actions;
    }

    /**
     *
     * @param plan_context the plan context of a specific person
     * @param tour_context the current tour context of a specific person in the simulation
     * @param trip_to_activity_guard the guard that triggers the transition after this one
     * @return a list of actions to be executed when transitioning from an activity to a trip
     */
    public List<TPS_PlanStateAction> generateAndGetActivityToTripActions(PlanContext plan_context, TourContext tour_context, Guard trip_to_activity_guard){

        TPS_Plan plan = plan_context.getPlan();
        TPS_Person person = plan.getPerson();
        TPS_PlanningContext pc = plan.getPlanningContext();

        LocationContext location_context = plan_context.getLocationContext();
        TPS_Stay current_stay = tour_context.getCurrentStay();
        TPS_Stay next_stay = tour_context.getNextStay();
        TPS_LocatedStay current_located_stay = plan.getLocatedStay(current_stay);
        TPS_LocatedStay next_located_stay = plan.getLocatedStay(next_stay);

        TPS_Trip next_trip = tour_context.getNextTrip();
        TPS_PlannedTrip next_planned_trip = plan.getPlannedTrip(next_trip);

        TPS_ModeSet mode_set = pm.getModeSet();
        ModeContext mode_context = tour_context.getModeContext();

        List<TPS_PlanStateAction> transition_actions = new ArrayList<>();

        transition_actions.add(new SetupAvailableModesAction(tour_context, plan_context.getHouseholdCarProvider(), person, pc));
        transition_actions.add(new UpdateLocationChoicePlanAttributesAction(plan,person,pc, next_stay));
        transition_actions.add(new SelectLocationAction(tour_context, location_context, plan_context));
        transition_actions.add(new UpdateModeChoicePlanAttributesAction(plan, next_located_stay));
        transition_actions.add(new SelectModeAction(tour_context, plan_context, mode_set));
        transition_actions.add(new ValidateModeAction(mode_validator, tour_context, pc, plan.getPlannedTrip(next_trip),this.car_filter));
        transition_actions.add(new UpdateDepartureAndArrivalModesAction(current_located_stay,next_located_stay, mode_context.getNextMode()));
        transition_actions.add(new CalculateTravelTimeAction(current_located_stay, next_located_stay, next_planned_trip));
        transition_actions.add(new UpdateTimeDeviationAndTimesAction(next_trip, next_planned_trip, plan_context, next_located_stay));
        transition_actions.add(new AdaptGuardAction(trip_to_activity_guard, guard_adaption_function, next_trip, plan_context::getTimeDeviation));

        //todo at a later stage we should add an atomic field in form of a reservation to the actual capacity
        //immediately check out the next location
        transition_actions.add(new UpdateCapacityAction(location_context::getNextLocation, -1));

        //add current stay dependant actions
        if(!current_stay.isAtHome()) { //we are not at home and leave a location
            //increment capacity
            transition_actions.add(new UpdateCapacityAction(location_context::getCurrentLocation, 1));
        }

        //checkout a potential car sharing car that has been requested
        transition_actions.add(new CheckOutSharedVehiclesAction(plan_context.getHouseholdCarProvider(), pc, tour_context, car_sharing_delegator));

        return transition_actions;
    }

    /**
     *
     * @param plan_context the plan context of a specific person
     * @param tour_context the current tour context of a specific person in the simulation
     * @param activity_to_trip_guard he guard that triggers the transition after this one
     * @param state_machine the state machine that represents a specific person
     * @return a list of actions to be executed when transitioning from a trip to an activity
     */
    public List<TPS_PlanStateAction> generateAndGetTripToActivityActions(PlanContext plan_context, TourContext tour_context, Guard activity_to_trip_guard, TPS_StateMachine state_machine){

        ArrayList<TPS_PlanStateAction> transition_actions = new ArrayList<>();

        transition_actions.add(new TripPersistenceAction(this.writer,plan_context,tour_context, pm));

        LocationContext location_context = plan_context.getLocationContext();
        ModeContext mode_context = tour_context.getModeContext();

        TPS_PlanningContext pc = plan_context.getPlan().getPlanningContext();

        transition_actions.add(new CheckInSharedVehiclesAction(plan_context.getHouseholdCarProvider(), pc, tour_context, car_sharing_delegator, sim_time_provider));

        TPS_Stay next_stay = tour_context.getNextStay();
        transition_actions.add(new AdaptGuardAction(activity_to_trip_guard, guard_adaption_function, next_stay, plan_context::getTimeDeviation));

        List<ContextUpdateable> contexts_to_update = new ArrayList<>(List.of(tour_context, mode_context, location_context));

        //tour is finished, update plan context
        if(tour_context.isFinished())
            contexts_to_update.add(plan_context);

        transition_actions.add(new UpdateContextsAction(contexts_to_update));

        //this action will only transition the state machine to end state when the plan is finished
        transition_actions.add(new TransitionToEndstateAction(state_machine,plan_context));

        return transition_actions;
    }

    public void setCarFilter(Predicate<TPS_Car> car_filter) {
        this.car_filter = car_filter;
    }

    public void setSimTimeProvider(SimTimeProvider sim_time_provider) {
        this.sim_time_provider = sim_time_provider;
    }
}

