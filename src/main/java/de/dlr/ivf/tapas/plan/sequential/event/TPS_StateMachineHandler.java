package de.dlr.ivf.tapas.plan.sequential.event;

import com.lmax.disruptor.WorkHandler;

public class TPS_StateMachineHandler implements WorkHandler<TPS_StateMachineEvent> {

    private String name;
    public TPS_StateMachineHandler(String worker_name){
        this.name = worker_name;
    }


    @Override
    public void onEvent(TPS_StateMachineEvent event) {
        event.getStateMachine().handleEvent(event.getEvent());

        //clear the event
        event.clear();
    }

    public String getName(){
        return this.name;
    }
}
