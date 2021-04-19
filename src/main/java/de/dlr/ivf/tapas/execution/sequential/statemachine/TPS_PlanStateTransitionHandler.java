package de.dlr.ivf.tapas.execution.sequential.statemachine;

import de.dlr.ivf.tapas.execution.sequential.action.ActionProvider;
import de.dlr.ivf.tapas.execution.sequential.action.TPS_PlanStateAction;
import de.dlr.ivf.tapas.execution.sequential.context.PlanContext;
import de.dlr.ivf.tapas.execution.sequential.guard.Guard;

import java.util.List;
import java.util.function.Supplier;

public class TPS_PlanStateTransitionHandler {

    private TPS_PlanState targetState;
    private Guard guard;
    Supplier<List<TPS_PlanStateAction>> actions;



    public TPS_PlanStateTransitionHandler(TPS_PlanState targetState, Guard guard, Supplier<List<TPS_PlanStateAction>> actions){
        this.targetState = targetState;
        this.guard = guard;
        this.actions = actions;
    }

    public boolean check(int data){
        return this.guard.test(data);
    }

    public TPS_PlanState getTargetState(){
        return this.targetState;
    }

    public List<TPS_PlanStateAction> getActions(){
        return this.actions.get();
    }

}
