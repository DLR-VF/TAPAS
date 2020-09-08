package de.dlr.ivf.tapas.plan.state.event;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.WorkHandler;

import java.util.concurrent.CyclicBarrier;

public class TPS_StateMachineHandler implements WorkHandler<TPS_StateMachineEvent> {

    private String name;
    public TPS_StateMachineHandler(String worker_name){
        this.name = worker_name;
    }


    @Override
    public void onEvent(TPS_StateMachineEvent event) throws Exception {
            event.getStateMachine().handleEvent(event.getEvent());
    }

    public String getName(){
        return this.name;
    }
}
