package de.dlr.ivf.tapas.execution.sequential.event;

import de.dlr.ivf.tapas.execution.sequential.statemachine.TPS_StateMachine;

public class TPS_Event {

    private int data;
    private TPS_EventType type;

    public TPS_Event(TPS_EventType type, int data){

        this.data = data;
        this.type = type;
    }

    public int getData(){
        return this.data;
    }

    public TPS_EventType getEventType(){
        return this.type;
    }
}
