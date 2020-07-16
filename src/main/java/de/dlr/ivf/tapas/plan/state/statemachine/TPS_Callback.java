package de.dlr.ivf.tapas.plan.state.statemachine;

public interface TPS_Callback {
    void call(TPS_PlanStateTransitionHandler handler);
}
