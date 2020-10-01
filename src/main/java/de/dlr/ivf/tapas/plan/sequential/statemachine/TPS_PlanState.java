package de.dlr.ivf.tapas.plan.sequential.statemachine;


import de.dlr.ivf.tapas.plan.sequential.action.TPS_PlanStateAction;
import de.dlr.ivf.tapas.plan.sequential.event.TPS_PlanEvent;
import de.dlr.ivf.tapas.plan.sequential.event.TPS_PlanEventType;
import de.dlr.ivf.tapas.plan.sequential.guard.TPS_PlanStateGuard;


public interface TPS_PlanState {
    void enter();
    void exit();
    boolean handle(TPS_PlanEvent event);
    boolean handleSafely(TPS_PlanEvent event);
    boolean willHandleEvent(TPS_PlanEvent event);
    void addHandler(TPS_PlanEventType event_type, TPS_PlanState target_state, TPS_PlanStateAction action, TPS_PlanStateGuard guard);
    void removeHandler(TPS_PlanEventType event);
    TPS_PlanStateTransitionHandler getHandler(TPS_PlanEventType event_type);
    void addOnEnterAction(TPS_PlanStateAction action);
    void addOnExitAction(TPS_PlanStateAction action);
    void removeOnEnterAction(TPS_PlanStateAction action);
    void removeOnExitAction(TPS_PlanStateAction action);

    String getName();

    void setStateMachine(TPS_PlanStateMachine stateMachine);
}
