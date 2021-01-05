package de.dlr.ivf.tapas.execution.sequential.event;

import de.dlr.ivf.tapas.execution.sequential.statemachine.TPS_PlanStateMachine;

public class TPS_StateMachineEvent {

    private TPS_PlanStateMachine state_machine;
    private TPS_PlanEvent event;

    public TPS_StateMachineEvent(){}

    public TPS_PlanStateMachine getStateMachine(){
        return this.state_machine;
    }

    public void setStateMachine(TPS_PlanStateMachine state_machine){
        this.state_machine = state_machine;
    }

    public TPS_PlanEvent getEvent(){
        return this.event;
    }

    public void setEvent(TPS_PlanEvent event){
        this.event = event;
    }

    public void clear(){

        this.state_machine = null;
        this.event = null;
    }
}
