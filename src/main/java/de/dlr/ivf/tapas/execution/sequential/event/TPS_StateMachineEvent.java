package de.dlr.ivf.tapas.execution.sequential.event;

import de.dlr.ivf.tapas.execution.sequential.statemachine.HouseholdBasedStateMachineController;
import de.dlr.ivf.tapas.execution.sequential.statemachine.TPS_StateMachine;

public class TPS_StateMachineEvent {

    private HouseholdBasedStateMachineController state_machine_controller;
    private TPS_PlanEvent event;

    public TPS_StateMachineEvent(){}

    public HouseholdBasedStateMachineController getStateMachineController(){
        return this.state_machine_controller;
    }

    public void setStateMachineController(HouseholdBasedStateMachineController state_machine){
        this.state_machine_controller = state_machine;
    }

    public TPS_PlanEvent getEvent(){
        return this.event;
    }

    public void setEvent(TPS_PlanEvent event){
        this.event = event;
    }

    public void clear(){

        this.state_machine_controller = null;
        this.event = null;
    }
}
