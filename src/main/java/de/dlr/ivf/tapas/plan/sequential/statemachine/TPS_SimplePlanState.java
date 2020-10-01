package de.dlr.ivf.tapas.plan.sequential.statemachine;

import de.dlr.ivf.tapas.plan.sequential.action.TPS_PlanStateAction;
import de.dlr.ivf.tapas.plan.sequential.action.TPS_PlanStateNoAction;
import de.dlr.ivf.tapas.plan.sequential.event.TPS_PlanEvent;
import de.dlr.ivf.tapas.plan.sequential.event.TPS_PlanEventType;
import de.dlr.ivf.tapas.plan.sequential.guard.TPS_PlanStateGuard;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

/**

This class represents a very simple state that has some basic functionality implemented to be usable in a state machine

 **/
public class TPS_SimplePlanState implements TPS_PlanState{

    /**
     * Will hold any handler
     */
    private EnumMap<TPS_PlanEventType, TPS_PlanStateTransitionHandler> handlers;

    /**
     * The name
     */
    private String name;

    /**
     * The enter action that will run when this state is being entered
     */
    private List<TPS_PlanStateAction> enter_actions;

    /**
     * The exit action that will run when this state is being exited
     */

    private List<TPS_PlanStateAction> exit_actions;

    /**
     * a Reference to the parent state machine
     */
    private TPS_PlanStateMachine stateMachine;

    /**
     *
     * @param name
     * simple name to identify the state
     */
    public TPS_SimplePlanState(String name){
        this(name, null);
    }

    /**
     *
     * @param name
     * simple name to identify the state
     * @param stateMachine
     * reference to the state machine
     */
    public TPS_SimplePlanState(String name, TPS_PlanStateMachine stateMachine){
        this(name,stateMachine, new ArrayList<TPS_PlanStateAction>(), new ArrayList<TPS_PlanStateAction>());
    }

    /**
     *
     * @param name
     * simple name to the identify the state
     * @param stateMachine
     * reference to the state machine
     * @param enter_actions
     * the enter action to perform
     * @param exit_actions
     * the exit action to perform
     */
    public TPS_SimplePlanState(String name, TPS_PlanStateMachine stateMachine, List<TPS_PlanStateAction> enter_actions, List<TPS_PlanStateAction> exit_actions){

        this.handlers = new EnumMap<>(TPS_PlanEventType.class);
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
        this.enter_actions.forEach(TPS_PlanStateAction::run);
    }

    /**
     * will invoke the exit actions
     */
    @Override
    public void exit() {
        this.exit_actions.forEach(TPS_PlanStateAction::run);
    }

    /**
     *
     * @param event
     * the event to handle
     * @return
     * true if it will be handles, false otherwise
     */
    @Override
    public boolean handle(TPS_PlanEvent event) {

            stateMachine.makeTransition(handlers.get(event.getEventType()));
            return true;
    }

    @Override
    public boolean handleSafely(TPS_PlanEvent event) {
        if (willHandleEvent(event)) {
            //there has an event happened that triggered a guard, inform the state machine
            stateMachine.makeTransition(handlers.get(event.getEventType()));
            return true;
        }
        return false;
    }

    @Override
    public boolean willHandleEvent(TPS_PlanEvent event) {
        return handlers.containsKey(event.getEventType()) && handlers.get(event.getEventType()).check(event.getData());
    }

    /**
     *
     * @param event_type
     * the type of event that needs to be handled
     * @param target_state
     * the target state to transition to when the event occurs
     * @param action
     * the transition action to invoke
     * @param guard
     * the guard that evaluates the condition to transition
     */
    @Override
    public void addHandler(TPS_PlanEventType event_type, TPS_PlanState target_state, TPS_PlanStateAction action, TPS_PlanStateGuard guard) {
        this.handlers.put(event_type, new TPS_PlanStateTransitionHandler(target_state,guard,action));
    }

    /**
     * This method will remove a specified handler
     * Note that only one handler of a specific event type can be handled
     * @param event
     * the event type to be removed from handling
     */
    @Override
    public void removeHandler(TPS_PlanEventType event) {
        handlers.remove(event);
    }

    /**
     * return a specific handler to a specific plan event type
     * @param event_type
     * @return
     */
    @Override
    public TPS_PlanStateTransitionHandler getHandler(TPS_PlanEventType event_type) {
        return handlers.get(event_type);
    }

    /**
     * set the enter action
     * @param action
     */
    @Override
    public void addOnEnterAction(TPS_PlanStateAction action) {
        this.enter_actions.add(action);
    }

    /**
     * set the exit action
     * @param action
     */
    @Override
    public void addOnExitAction(TPS_PlanStateAction action) {
        this.exit_actions.add(action);
    }

    @Override
    public void removeOnEnterAction(TPS_PlanStateAction action) {
        this.enter_actions.remove(action);
    }

    @Override
    public void removeOnExitAction(TPS_PlanStateAction action) {
        this.exit_actions.remove(action);
    }

    /**
     * returns name
     * @return
     */
    @Override
    public String getName() {
        return this.name;
    }


    /**
     * set the state machine manually
     * @param stateMachine
     */
    @Override
    public void setStateMachine(TPS_PlanStateMachine stateMachine) {
        this.stateMachine = stateMachine;
    }
}
