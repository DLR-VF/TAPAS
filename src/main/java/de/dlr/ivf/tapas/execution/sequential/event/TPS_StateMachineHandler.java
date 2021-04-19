package de.dlr.ivf.tapas.execution.sequential.event;

import com.lmax.disruptor.WorkHandler;

public class TPS_StateMachineHandler implements WorkHandler<TPS_StateMachineEvent> {

    private String name;
    public TPS_StateMachineHandler(String worker_name){
        this.name = worker_name;
    }


    @Override
    public void onEvent(TPS_StateMachineEvent event) {
        event.getStateMachineController().delegateSimulationEvent(event.getEvent());

        event.clear();
    }

    public String getName(){
        return this.name;
    }
}
