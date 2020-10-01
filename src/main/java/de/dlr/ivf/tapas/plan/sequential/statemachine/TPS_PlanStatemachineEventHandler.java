package de.dlr.ivf.tapas.plan.sequential.statemachine;

import de.dlr.ivf.tapas.plan.sequential.event.TPS_PlanEvent;

public interface TPS_PlanStatemachineEventHandler {

    void handleEvent(TPS_PlanEvent event);
    void handleEventSafely(TPS_PlanEvent event);
    boolean willHandleEvent(TPS_PlanEvent event);
}
