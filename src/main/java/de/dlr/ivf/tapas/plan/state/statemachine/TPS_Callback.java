package de.dlr.ivf.tapas.plan.state.statemachine;

import de.dlr.ivf.tapas.plan.state.TPS_PlanStateTransitionHandler;

public interface TPS_Callback {
    void call(TPS_PlanStateTransitionHandler handler);
}
