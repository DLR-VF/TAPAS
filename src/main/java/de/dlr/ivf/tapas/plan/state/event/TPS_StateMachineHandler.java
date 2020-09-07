package de.dlr.ivf.tapas.plan.state.event;

import com.lmax.disruptor.EventHandler;

public class TPS_StateMachineHandler implements EventHandler<TPS_StateMachineEvent> {
    private int id;
    private int worker_count;
    public TPS_StateMachineHandler(int id, int worker_count){
        this.id = id;
        this.worker_count = worker_count;
    }


    @Override
    public void onEvent(TPS_StateMachineEvent event, long sequence, boolean endOfBatch) throws Exception {
        if(sequence % worker_count == id){
            //System.out.println("handling event: "+sequence+" with worker: "+id);
            event.getStateMachine().handleEvent(event.getEvent());
        }
    }
}
