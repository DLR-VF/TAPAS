package de.dlr.ivf.tapas.execution.sequential.statemachine;

import de.dlr.ivf.tapas.execution.sequential.action.TPS_PlanStateAction;
import de.dlr.ivf.tapas.execution.sequential.communication.EndOfSimulationCallback;
import de.dlr.ivf.tapas.execution.sequential.event.TPS_Event;

import java.util.List;
import java.util.Map;
import java.util.Objects;


/**
 * This class represents a state machine that should act as a mealy automata.
 * The state machine does not have a transition table of any kind. This implementation passes the event to be handled
 * down to its current state and lets the state decide whether it needs to transition by checking for an appropriate handler.
 * When the state has a handler for the event, it will pass its handler back to the state machine which will then invoke
 * the transition
 *
 */
public class TPS_StateMachine implements TPS_PlanStatemachineEventHandler, TransitionErrorHandler {


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
     * this state machine will get into this state whenever an error or any other problems occur
     */
    private TPS_PlanState error_state;

    /**
     * the current state that the state machine is in
     */
    private TPS_PlanState current_state;

    private EndOfSimulationCallback controller;

    /**
     * a list containing all states
     */
    private List<TPS_PlanState> states;

    private Map<EpisodeType, TPS_PlanState> statemappings;

    public TPS_StateMachine(String name){
        this.name = name;
    }

    public TPS_StateMachine(Map<EpisodeType,TPS_PlanState> state_mappings, String name){

        this.statemappings = state_mappings;

        this.name = name;
    }

    public TPS_StateMachine(TPS_PlanState initialState, List<TPS_PlanState> states, TPS_PlanState end_state, String name){
        Objects.requireNonNull(initialState);
        Objects.requireNonNull(states);
        Objects.requireNonNull(end_state);
        Objects.requireNonNull(name);

        this.initial_state = initialState;
        this.current_state = initialState;
        this.states = states;
        this.name = name;
        this.end_state = end_state;
    }

    public void setController(EndOfSimulationCallback controller){
        this.controller = controller;
    }

    public EndOfSimulationCallback getController(){
        return this.controller;
    }

    /**
     * When an event is sent to the state machine we pass it to the state itself and let it
     * @param event the event to be handled
     */
    @Override
    public void handleEvent(TPS_Event event) {

        current_state.handle(event);
    }

    @Override
    public void handleEventSafely(TPS_Event event) {
        current_state.handleSafely(event);
    }

    @Override
    public boolean willHandleEvent(TPS_Event event) {
        return current_state.willHandleEvent(event);
    }


    /**
     * this method should be called from a {@link TPS_PlanState} and let this state machine to make the transition
     *
     * @param handler
     * the handler managing the transition
     */
    protected void makeTransition(TPS_PlanStateTransitionHandler handler){

        TPS_PlanState previous_state = current_state;
        //first we exit the current state
        exitState();

        //then we make the transition
        if(handler.getActions() != null)
            try {
                executeActions(handler.getActions());
            }catch (RuntimeException e){
                //we could transition to error state here but might want to do other things in case of an exception
                throw new TPS_StateTransitionException("STATEMACHINE "+this.name+": Error transitioning from "+previous_state.getName()+
                                                       " to "+handler.getTargetState().getName(), e, this);
            }

        //now we enter the new state
        enterState(handler);
    }


    private void enterState(TPS_PlanStateTransitionHandler handler){
        //if a state is supplied that is not part of this statemachine, we put the machine into the end state
        if(states.contains(handler.getTargetState()))
            current_state = handler.getTargetState();
        else {
            current_state = error_state;
            System.err.println("a state has been supplied that is not part of the state machine...");
        }
        current_state.enter();
    }

    private void executeActions(List<TPS_PlanStateAction> actions){
        actions.forEach(TPS_PlanStateAction::run);
    }

    private void exitState(){
        current_state.exit();
    }

    public boolean hasFinished(){
        return this.current_state == this.end_state || this.current_state == error_state;
    }


    public void setInitialStateAndReset(TPS_PlanState initial_state){
        this.initial_state = initial_state;
        this.current_state = initial_state;
    }

    public void setEndState(TPS_PlanState end_state){
        this.end_state = end_state;
    }

    public void setAllStates(List<TPS_PlanState> states){
        this.states = states;
    }

    public TPS_PlanState getCurrentState(){
        return this.current_state;
    }

    public TPS_PlanState getEndState(){
        return this.end_state;
    }

    public void transitionToEndState() {
        this.current_state.exit();
        this.current_state = end_state;
    }

    @Override
    public void transitionToErrorState() {
        this.current_state = error_state;
    }

    public void setErrorState(TPS_SimplePlanState error_state) {
        this.error_state = error_state;
    }

    public String getName(){
        return this.name;
    }
}
