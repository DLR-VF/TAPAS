package de.dlr.ivf.tapas.execution.sequential.statemachine;


import de.dlr.ivf.tapas.execution.sequential.action.TPS_PlanStateAction;
import de.dlr.ivf.tapas.execution.sequential.event.TPS_Event;
import de.dlr.ivf.tapas.execution.sequential.event.TPS_EventType;
import de.dlr.ivf.tapas.execution.sequential.guard.Guard;

import java.util.List;
import java.util.function.Supplier;


public interface TPS_PlanState {
    void enter();
    void exit();
    boolean handle(TPS_Event event);
    boolean handleSafely(TPS_Event event);
    boolean willHandleEvent(TPS_Event event);
    void addHandler(TPS_EventType event_type, TPS_PlanState target_state, Supplier<List<TPS_PlanStateAction>> actions, Guard guard);
    String getName();
    void setStateMachine(TPS_StateMachine stateMachine);
}
