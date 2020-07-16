package de.dlr.ivf.tapas.plan.state.statemachine;

import de.dlr.ivf.tapas.plan.state.action.TPS_PlanStateAction;
import de.dlr.ivf.tapas.plan.state.guard.TPS_PlanStateGuard;

public class TPS_PlanStateTransitionHandler {

    private final TPS_PlanState targetState;
    private final TPS_PlanStateGuard guard;
    private final TPS_PlanStateAction action;



    public TPS_PlanStateTransitionHandler(TPS_PlanState targetState, TPS_PlanStateGuard guard, TPS_PlanStateAction action){
        this.targetState = targetState;
        this.guard = guard;
        this.action = action;
    }

    public <T> boolean check(T data){
        return this.guard.test(data);
    }

    public TPS_PlanState getTargetState(){
        return this.targetState;
    }

    public TPS_PlanStateAction getAction(){
        return this.action;
    }

}
