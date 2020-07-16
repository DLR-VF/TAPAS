package de.dlr.ivf.tapas.plan.state.statemachine;

import de.dlr.ivf.tapas.plan.state.action.TPS_PlanStateAction;
import de.dlr.ivf.tapas.plan.state.event.TPS_PlanEvent;

import java.util.HashSet;
import java.util.Set;

//a bare state machine;
// T = type of the object that is linked to this state machine (eg. a TPS_Plan)
public class TPS_PlanStateMachine<T> implements TPS_PlanStatemachineEventHandler, TPS_Callback {

    private String name;
    private TPS_PlanState initialState;
    private TPS_PlanState currentState;
    private Set<TPS_PlanState> states;
    private T representing_object;
    private int sim_start_time = -1000;

    public TPS_PlanStateMachine(TPS_PlanState initialState, String name, T representing_object){
        this(initialState, new HashSet<>(),name,representing_object);
    }


    public TPS_PlanStateMachine(TPS_PlanState initialState, Set<TPS_PlanState> states, String name, T representing_object){
        this.initialState = initialState;
        this.currentState = initialState;
        this.states = states;
        this.name = name;
        this.representing_object = representing_object;
    }

    @Override
    public void handleEvent(TPS_PlanEvent event) {
        currentState.handle(event);
    }

    @Override
    public void call(TPS_PlanStateTransitionHandler handler) {
        makeTransition(handler);
    }

    protected void makeTransition(TPS_PlanStateTransitionHandler handler){
        //first we exit the current state
        exitState();
        //then we make the transition
        executeAction(handler.getAction());
        //now we enter the new state
        enterState(handler);
    }

    private void enterState(TPS_PlanStateTransitionHandler handler){
        //if a state is supplied that is not part of this statemachine, we reset the machine
        System.out.println("entering: "+handler.getTargetState());
        if(states.contains(handler.getTargetState()))
            currentState = handler.getTargetState();
        else
            currentState = initialState;

        currentState.enter();
    }

    private void executeAction(TPS_PlanStateAction action){
        action.run();
    }

    private void exitState(){
        currentState.exit();
    }

    public void setStates(Set<TPS_PlanState> states){
        this.states = states;
    }
    public void setSimStartTime(int start_time){
        if(start_time < this.sim_start_time)
            this.sim_start_time = start_time;
    }

    public int getSimStartTime(){
        return this.sim_start_time;
    }
}
