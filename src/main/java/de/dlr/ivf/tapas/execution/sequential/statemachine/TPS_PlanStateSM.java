package de.dlr.ivf.tapas.execution.sequential.statemachine;


import de.dlr.ivf.tapas.execution.sequential.action.TPS_PlanStateAction;
import de.dlr.ivf.tapas.execution.sequential.event.TPS_PlanEvent;
import de.dlr.ivf.tapas.execution.sequential.event.TPS_PlanEventType;
import de.dlr.ivf.tapas.execution.sequential.guard.TPS_StateGuard;

import java.util.List;

//bridge pattern to decouple an abstract state from its implementation and let it function as a state machine. eg. when we are in an activity state machine, we are still in plan execution state for example.
//all events will be passed down to the sub state machine

//todo implement delegation methods
public class TPS_PlanStateSM<S> implements TPS_PlanState, TPS_PlanStatemachineEventHandler{

    private final TPS_PlanStateMachine stateSM;

    private String name;
    private TPS_PlanState initialState;
    private TPS_PlanState currentState;
    private List<TPS_PlanState> states;
    private S representing_object;

    public TPS_PlanStateSM(TPS_PlanStateMachine stateSM) {

        this.stateSM = stateSM;
    }


    @Override
    public void enter() {

    }

    @Override
    public void exit() {

    }

    @Override
    public boolean handle(TPS_PlanEvent event) {
        return false;
    }

    @Override
    public boolean handleSafely(TPS_PlanEvent event) {
        return false;
    }

    @Override
    public boolean willHandleEvent(TPS_PlanEvent event) {
        return false;
    }

    @Override
    public void addHandler(TPS_PlanEventType event, TPS_PlanState target, TPS_PlanStateAction action, TPS_StateGuard guard) {

    }

    @Override
    public void removeHandler(TPS_PlanEventType event) {

    }

    @Override
    public TPS_PlanStateTransitionHandler getHandler(TPS_PlanEventType event_type) {
        return null;
    }

    @Override
    public void addOnEnterAction(TPS_PlanStateAction action) {

    }

    @Override
    public void addOnExitAction(TPS_PlanStateAction action) {

    }

    @Override
    public void removeOnEnterAction(TPS_PlanStateAction action) {

    }

    @Override
    public void removeOnExitAction(TPS_PlanStateAction action) {

    }


    @Override
    public String getName() {
        return null;
    }


    @Override
    public void setStateMachine(TPS_PlanStateMachine stateMachine) {

    }


    @Override
    public void handleEvent(TPS_PlanEvent event) {

    }

    @Override
    public void handleEventSafely(TPS_PlanEvent event) {

    }
}
