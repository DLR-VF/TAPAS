package de.dlr.ivf.tapas.plan.state.event;

import com.lmax.disruptor.ExceptionHandler;

public class StateMachineEventExceptionHandler implements ExceptionHandler<TPS_StateMachineEvent> {
    @Override
    public void handleEventException(Throwable ex, long sequence, TPS_StateMachineEvent event) {
        ex.printStackTrace();
    }

    @Override
    public void handleOnStartException(Throwable ex) {
        ex.printStackTrace();
    }

    @Override
    public void handleOnShutdownException(Throwable ex) {
        ex.printStackTrace();
    }
}
