package de.dlr.ivf.tapas.execution.sequential.event;

import de.dlr.ivf.tapas.execution.sequential.statemachine.HouseholdBasedStateMachineController;

public class TPS_StateMachineEvent {

    private HouseholdBasedStateMachineController event_delegator;
    private TPS_Event event;

    public TPS_StateMachineEvent(){}

    public HouseholdBasedStateMachineController getStateMachineController(){
        return this.event_delegator;
    }

    public void setEventDelegator(HouseholdBasedStateMachineController event_delegator){
        this.event_delegator = event_delegator;
    }

    public TPS_Event getEvent(){
        return this.event;
    }

    public void setEvent(TPS_Event event){
        this.event = event;
    }

    public void clear(){

        this.event_delegator = null;
        this.event = null;
    }
}
