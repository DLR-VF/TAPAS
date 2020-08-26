package de.dlr.ivf.tapas.plan.state.statemachine;

import de.dlr.ivf.tapas.plan.state.action.TPS_PlanStateAction;
import de.dlr.ivf.tapas.plan.state.event.TPS_PlanEvent;

import java.util.List;
import java.util.Objects;


/**
 * This class represents a state machine that should act as a mealy automata.
 * The state machine does not have a transition table of any kind. This implementation passes the event to be handled
 * down to its current state and lets the state decide whether it needs to transition by checking for an appropriate handler.
 * When the state has a handler for the event, it will pass its handler back to the state machine which will then invoke
 * the transition
 * @param <T>
 *     Type of the object being encapsulated
 */
public class TPS_PlanStateMachine<T> implements TPS_PlanStatemachineEventHandler {

    /**
     * the name of the machine
     */
    private String name;

    /**
     * the initial state that the state machine will be in right of the bat
     */
    private TPS_PlanState initial_state;

    /**
     * the end_state
     */
    private TPS_PlanState end_state;

    //TODO implement this for exception handling
    /**
     * this state machine will get into this state whenever an error of any other problems occur
     */
    private TPS_PlanState error_state;

    /**
     * the current state that the state machine is in
     */
    private TPS_PlanState current_state;

    /**
     * a list containing all states
     */
    private List<TPS_PlanState> states;

    /**
     * the object wrapped into the machine
     */
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

    /**
     * When an event is sent to the state machine we pass it to the state itself and let it
     * @param event
     */
    @Override
    public void handleEvent(TPS_PlanEvent event) {
        current_state.handle(event);
    }


    /**
     * this method should be called from a {@link TPS_PlanState} and let this state machine to make the transition
     *
     * @param handler
     * the handler managing the transition
     */
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

    public T getRepresenting_object(){
        return this.representing_object;
    }
}
