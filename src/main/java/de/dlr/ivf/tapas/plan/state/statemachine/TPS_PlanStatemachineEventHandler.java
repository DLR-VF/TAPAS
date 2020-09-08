package de.dlr.ivf.tapas.plan.state.statemachine;

import de.dlr.ivf.tapas.plan.state.event.TPS_PlanEvent;

public interface TPS_PlanStatemachineEventHandler {

    void handleEvent(TPS_PlanEvent event);
    void handleEventSafely(TPS_PlanEvent event);
    boolean willHandleEvent(TPS_PlanEvent event);
}
