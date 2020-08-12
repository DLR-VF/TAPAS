package de.dlr.ivf.tapas.plan.state.statemachine;

import de.dlr.ivf.tapas.plan.state.action.TPS_PlanStateAction;
import de.dlr.ivf.tapas.plan.state.event.TPS_PlanEvent;

import java.util.List;
import java.util.Objects;

//a bare state machine;
// T = type of the object that is linked to this state machine (eg. a TPS_Plan)
public class TPS_PlanStateMachine<T> implements TPS_PlanStatemachineEventHandler {

    private String name;
    private TPS_PlanState initial_state;
    private TPS_PlanState end_state;

    //TODO implement this for exception handling
    private TPS_PlanState error_state;
    private TPS_PlanState current_state;
    private List<TPS_PlanState> states;
    private T representing_object;

    public TPS_PlanStateMachine(TPS_PlanState initialState, List<TPS_PlanState> states, TPS_PlanState end_state, String name, T representing_object){
        Objects.requireNonNull(initialState);
        Objects.requireNonNull(states);
        Objects.requireNonNull(end_state);
        Objects.requireNonNull(name);
        Objects.requireNonNull(representing_object);


        this.initial_state = initialState;
        this.current_state = initialState;
        this.states = states;
        this.name = name;
        this.representing_object = representing_object;
        this.end_state = end_state;
    }

    @Override
    public void handleEvent(TPS_PlanEvent event) {
        current_state.handle(event);
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
        //if a state is supplied that is not part of this statemachine, we put the machine into the end state
        if(states.contains(handler.getTargetState()))
            current_state = handler.getTargetState();
        else {
            current_state = end_state;
            System.err.println("a state has been supplied that is not part of the state machine...");
        }
        current_state.enter();
    }

    private void executeAction(TPS_PlanStateAction action){
        action.run();
    }

    private void exitState(){
        current_state.exit();
    }

    public TPS_PlanState getInitial_state(){
        return this.initial_state;
    }

    public TPS_PlanState getCurrent_state(){
        return this.current_state;
    }

    public boolean hasFinished(){
        return this.current_state == this.end_state;
    }
}
