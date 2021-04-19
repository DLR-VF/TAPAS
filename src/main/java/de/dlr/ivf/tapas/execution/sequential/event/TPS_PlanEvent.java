package de.dlr.ivf.tapas.execution.sequential.event;

public class TPS_PlanEvent {

    private int data;
    private TPS_EventType type;

    public TPS_PlanEvent(TPS_EventType type, int data){

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
