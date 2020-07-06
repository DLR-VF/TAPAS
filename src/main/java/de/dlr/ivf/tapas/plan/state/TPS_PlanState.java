package de.dlr.ivf.tapas.plan.state;


import de.dlr.ivf.tapas.plan.state.action.TPS_PlanStateAction;
import de.dlr.ivf.tapas.plan.state.event.TPS_PlanEvent;
import de.dlr.ivf.tapas.plan.state.event.TPS_PlanEventType;
import de.dlr.ivf.tapas.plan.state.guard.TPS_PlanStateGuard;


public interface TPS_PlanState {
    void enter();
    void exit();
    boolean handle(TPS_PlanEvent event);
    void addHandler(TPS_PlanEventType event_type, TPS_PlanState target_state, TPS_PlanStateAction action, TPS_PlanStateGuard guard);
    void removeHandler(TPS_PlanEventType event);
}
