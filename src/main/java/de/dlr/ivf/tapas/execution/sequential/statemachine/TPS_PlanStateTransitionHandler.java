package de.dlr.ivf.tapas.execution.sequential.statemachine;

import de.dlr.ivf.tapas.execution.sequential.action.TPS_PlanStateAction;
import de.dlr.ivf.tapas.execution.sequential.guard.TPS_StateGuard;

public class TPS_PlanStateTransitionHandler {

    private TPS_PlanState targetState;
    private TPS_StateGuard guard;
    private TPS_PlanStateAction action;



    public TPS_PlanStateTransitionHandler(TPS_PlanState targetState, TPS_StateGuard guard, TPS_PlanStateAction action){
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
