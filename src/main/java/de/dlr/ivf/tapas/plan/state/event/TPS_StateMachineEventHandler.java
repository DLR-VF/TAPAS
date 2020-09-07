package de.dlr.ivf.tapas.plan.state.event;

import com.lmax.disruptor.EventHandler;

public class TPS_StateMachineEventHandler implements EventHandler<TPS_StateMachineEvent> {
    @Override
    public void onEvent(TPS_StateMachineEvent event, long sequence, boolean endOfBatch) throws Exception {

    }
}
