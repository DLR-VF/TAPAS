package de.dlr.ivf.tapas.execution.sequential.statemachine;

import de.dlr.ivf.tapas.execution.sequential.action.TPS_PlanStateAction;
import de.dlr.ivf.tapas.execution.sequential.event.TPS_Event;
import de.dlr.ivf.tapas.execution.sequential.event.TPS_EventType;
import de.dlr.ivf.tapas.execution.sequential.guard.Guard;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.function.Supplier;

/**

This class represents a very simple state that has some basic functionality implemented to be usable in a state machine

 **/
public class TPS_SimplePlanState implements TPS_PlanState{

    /**
     * Will hold any handler
     */
    private EnumMap<TPS_EventType, TPS_PlanStateTransitionHandler> handlers;

    /**
     * The name
     */
    private String name;

    /**
     * The enter action that will run when this state is being entered
     */
    private List<Supplier<TPS_PlanStateAction>> enter_actions;

    /**
     * The exit action that will run when this state is being exited
     */

    private List<Supplier<TPS_PlanStateAction>> exit_actions;

    /**
     * a Reference to the parent state machine
     */
    private TPS_StateMachine stateMachine;

    /**
     *

     * simple name to identify the state
     * @param stateMachine
     * reference to the state machine
     */
    public TPS_SimplePlanState(String name, TPS_StateMachine stateMachine){
        this(name,stateMachine, new ArrayList<>(), new ArrayList<>());
    }

    /**
     *

     * simple name to the identify the state
     * @param stateMachine
     * reference to the state machine
     * @param enter_actions
     * the enter action to perform
     * @param exit_actions
     * the exit action to perform
     */
    public TPS_SimplePlanState(String name, TPS_StateMachine stateMachine, List<Supplier<TPS_PlanStateAction>> enter_actions, List<Supplier<TPS_PlanStateAction>> exit_actions){

        this.handlers = new EnumMap<>(TPS_EventType.class);
        this.name = name;
        this.stateMachine = stateMachine;
        this.enter_actions = enter_actions;
        this.exit_actions = exit_actions;
    }

    /**
     * will invoke the enter actions
     */
    @Override
    public void enter() {
        this.enter_actions.stream().map(Supplier::get).forEach(TPS_PlanStateAction::run);
    }

    /**
     * will invoke the exit actions
     */
    @Override
    public void exit() {
        this.exit_actions.stream().map(Supplier::get).forEach(TPS_PlanStateAction::run);
    }


    @Override
    public boolean handle(TPS_Event event) {

            stateMachine.makeTransition(handlers.get(event.getEventType()));
            return true;
    }

    /**
     *
     * @param event
     * the event to handle
     * @return
     * true if it will be handled, false otherwise
     */
    @Override
    public boolean handleSafely(TPS_Event event) {
        if (willHandleEvent(event)) {
            //there has an event happened that triggered a guard, inform the state machine
            stateMachine.makeTransition(handlers.get(event.getEventType()));
            return true;
        }
        return false;
    }

    @Override
    public boolean willHandleEvent(TPS_Event event) {
        return handlers.containsKey(event.getEventType()) && handlers.get(event.getEventType()).check(event.getData());
    }

    /**
     *
     * @param event_type
     * the type of event that needs to be handled
     * @param target_state
     * the target state to transition to when the event occurs
     * @param actions
     * the transition action to invoke
     * @param guard
     * the guard that evaluates the condition to transition
     */
    @Override
    public void addHandler(TPS_EventType event_type, TPS_PlanState target_state, Supplier<List<TPS_PlanStateAction>> actions, Guard guard) {
        this.handlers.put(event_type, new TPS_PlanStateTransitionHandler(target_state,guard,actions));
    }

    @Override
    public String getName() {
        return this.name;
    }


    /**
     * set the state machine manually
     * @param stateMachine the state machine which this state is member of
     */
    @Override
    public void setStateMachine(TPS_StateMachine stateMachine) {
        this.stateMachine = stateMachine;
    }
}
