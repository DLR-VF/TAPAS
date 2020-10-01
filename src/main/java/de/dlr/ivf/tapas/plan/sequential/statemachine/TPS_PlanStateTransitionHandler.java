package de.dlr.ivf.tapas.plan.sequential.statemachine;

import de.dlr.ivf.tapas.plan.sequential.action.TPS_PlanStateAction;
import de.dlr.ivf.tapas.plan.sequential.guard.TPS_PlanStateGuard;

public class TPS_PlanStateTransitionHandler {

    private TPS_PlanState targetState;
    private TPS_PlanStateGuard guard;
    private TPS_PlanStateAction action;



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
    public void setTargetState(TPS_PlanState target_state){
        this.targetState = target_state;
    }

    public TPS_PlanStateAction getAction(){
        return this.action;
    }

}
