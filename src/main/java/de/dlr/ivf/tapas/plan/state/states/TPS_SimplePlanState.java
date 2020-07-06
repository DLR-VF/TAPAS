package de.dlr.ivf.tapas.plan.state.states;

import de.dlr.ivf.tapas.plan.state.*;
import de.dlr.ivf.tapas.plan.state.action.TPS_PlanStateAction;
import de.dlr.ivf.tapas.plan.state.action.TPS_PlanStateNoAction;
import de.dlr.ivf.tapas.plan.state.event.TPS_PlanEvent;
import de.dlr.ivf.tapas.plan.state.event.TPS_PlanEventType;
import de.dlr.ivf.tapas.plan.state.guard.TPS_PlanStateGuard;
import de.dlr.ivf.tapas.plan.state.statemachine.TPS_Callback;
import de.dlr.ivf.tapas.plan.state.statemachine.TPS_NoCallback;

import java.util.EnumMap;

public class TPS_SimplePlanState implements TPS_PlanState{

    private EnumMap<TPS_PlanEventType, TPS_PlanStateTransitionHandler> handlers;
    private String name;
    private TPS_Callback callback; //this will be the statemachine that we want to call back
    private TPS_PlanStateAction enter_action;
    private TPS_PlanStateAction exit_action;

    public TPS_SimplePlanState(String name){
        this(name, new TPS_NoCallback());
    }

    public TPS_SimplePlanState(String name, TPS_Callback callback){
        this(name,callback, new TPS_PlanStateNoAction(), new TPS_PlanStateNoAction());
    }
    public TPS_SimplePlanState(String name, TPS_Callback callback, TPS_PlanStateAction enter_action, TPS_PlanStateAction exit_action){

        this.handlers = new EnumMap<>(TPS_PlanEventType.class);
        this.name = name;
        this.callback = callback;
        this.enter_action = enter_action;
        this.exit_action = exit_action;
    }

    @Override
    public void enter() {
        this.enter_action.run();
    }

    @Override
    public void exit() {
        this.exit_action.run();
    }

    @Override
    public boolean handle(TPS_PlanEvent event) {
        if (handlers.containsKey(event.getEventType()) && handlers.get(event.getEventType()).check(event.getData())) {
            //there has an event happened that triggered a guard, inform the statemachine
            System.out.println("Transitioning from state: " + name + " to state: " + handlers.get(event.getEventType()).getTargetState());
            callback.call(handlers.get(event.getEventType()));
            return true;
        }
        return false;
    }

    @Override
    public void addHandler(TPS_PlanEventType event_type, TPS_PlanState target_state, TPS_PlanStateAction action, TPS_PlanStateGuard guard) {
        this.handlers.put(event_type, new TPS_PlanStateTransitionHandler(target_state,guard,action));
    }

    @Override
    public void removeHandler(TPS_PlanEventType event) {
        handlers.remove(event);
    }
}
