package de.dlr.ivf.tapas.execution.sequential.statemachine;


import de.dlr.ivf.tapas.execution.sequential.action.TPS_PlanStateAction;
import de.dlr.ivf.tapas.execution.sequential.event.TPS_PlanEvent;
import de.dlr.ivf.tapas.execution.sequential.event.TPS_EventType;
import de.dlr.ivf.tapas.execution.sequential.guard.Guard;

import java.util.List;
import java.util.function.Supplier;


public interface TPS_PlanState {
    void enter();
    void exit();
    boolean handle(TPS_PlanEvent event);
    boolean handleSafely(TPS_PlanEvent event);
    boolean willHandleEvent(TPS_PlanEvent event);
    void addHandler(TPS_EventType event_type, TPS_PlanState target_state, Supplier<List<TPS_PlanStateAction>> actions, Guard guard);
    void addHandler(TPS_EventType event_type, TPS_PlanStateTransitionHandler handler);
    void removeHandler(TPS_EventType event);
    TPS_PlanStateTransitionHandler getHandler(TPS_EventType event_type);
    void addOnEnterAction(Supplier<TPS_PlanStateAction> action);
    void addOnExitAction(Supplier<TPS_PlanStateAction> action);
    void removeOnEnterAction(TPS_PlanStateAction action);
    void removeOnExitAction(TPS_PlanStateAction action);

    String getName();

    void setStateMachine(TPS_StateMachine stateMachine);

    EpisodeType getStateType();
}
