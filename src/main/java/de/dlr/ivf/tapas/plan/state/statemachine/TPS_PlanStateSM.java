package de.dlr.ivf.tapas.plan.state.statemachine;


import de.dlr.ivf.tapas.plan.TPS_Plan;
import de.dlr.ivf.tapas.plan.state.action.TPS_PlanStateAction;
import de.dlr.ivf.tapas.plan.state.event.TPS_PlanEvent;
import de.dlr.ivf.tapas.plan.state.event.TPS_PlanEventType;
import de.dlr.ivf.tapas.plan.state.guard.TPS_PlanStateGuard;

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
    public void addHandler(TPS_PlanEventType event, TPS_PlanState target, TPS_PlanStateAction action, TPS_PlanStateGuard guard) {

    }

    @Override
    public void removeHandler(TPS_PlanEventType event) {

    }

    @Override
    public void setOnEnterAction(TPS_PlanStateAction action) {

    }

    @Override
    public void setOnExitAction(TPS_PlanStateAction action) {

    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public TPS_PlanStateMachine<TPS_Plan> getStateMachine() {
        return null;
    }


    @Override
    public void handleEvent(TPS_PlanEvent event) {

    }
}
