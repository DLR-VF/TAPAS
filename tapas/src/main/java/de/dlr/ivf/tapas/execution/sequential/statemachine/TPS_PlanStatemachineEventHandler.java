package de.dlr.ivf.tapas.execution.sequential.statemachine;

import de.dlr.ivf.tapas.execution.sequential.event.TPS_Event;

public interface TPS_PlanStatemachineEventHandler {

    void handleEvent(TPS_Event event);
    void handleEventSafely(TPS_Event event);
    boolean willHandleEvent(TPS_Event event);
}
