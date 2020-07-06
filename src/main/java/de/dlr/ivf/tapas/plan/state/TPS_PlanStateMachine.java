package de.dlr.ivf.tapas.plan.state;

import de.dlr.ivf.tapas.plan.state.action.TPS_PlanStateAction;
import de.dlr.ivf.tapas.plan.state.event.TPS_PlanEvent;
import de.dlr.ivf.tapas.plan.state.statemachine.TPS_Callback;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

//a bare state machine;
// T = type of the object that is linked to this state machine (eg. a TPS_Plan)
public class TPS_PlanStateMachine<T> implements TPS_PlanStatemachineEventHandler, TPS_Callback {

    private String name;
    private TPS_PlanState initialState;
    private TPS_PlanState currentState;
    private Set<TPS_PlanState> states;
    private T representing_object;
    private CountDownLatch countDownLatch;

    public TPS_PlanStateMachine(TPS_PlanState initialState, String name, T representing_object){
        this(initialState,new HashSet<TPS_PlanState>(),name,representing_object);
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
        if(!currentState.handle(event) && countDownLatch != null){
            countDownLatch.countDown();
        }
    }

    @Override
    public void call(TPS_PlanStateTransitionHandler handler) {
        makeTransition(handler);
    }

    private void makeTransition(TPS_PlanStateTransitionHandler handler){
        //first we exit the current state
        exitState();
        //then we make the transition
        executeAction(handler.getAction());
        //now we enter the new state
        enterState(handler);
        //finally we tell the simulation that we are ready to proceed
        if(countDownLatch != null)
            countDownLatch.countDown();
    }

    private void enterState(TPS_PlanStateTransitionHandler handler){
        //if a state is supplied that is not part of this statemachine, we reset the machine
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

    public void setCountDownLatch(CountDownLatch countDownLatch){
        this.countDownLatch = countDownLatch;
    }
}
