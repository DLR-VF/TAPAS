package de.dlr.ivf.tapas.plan.state.event;

public class TPS_PlanEvent {

    private Object data;
    private TPS_PlanEventType type;

    public TPS_PlanEvent(TPS_PlanEventType type, Object data){

        this.data = data;
        this.type = type;
    }

    public TPS_PlanEvent() {

    }

    public Object getData(){
        return this.data;
    }

    public TPS_PlanEventType getEventType(){
        return this.type;
    }
}
