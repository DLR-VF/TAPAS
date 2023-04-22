package de.dlr.ivf.tapas.execution.sequential.statemachine;

import de.dlr.ivf.tapas.execution.sequential.event.TPS_Event;

public interface EventDelegator {

    void delegateEvent(TPS_Event event);
    boolean willPassEvent(TPS_Event simulation_event);
}
