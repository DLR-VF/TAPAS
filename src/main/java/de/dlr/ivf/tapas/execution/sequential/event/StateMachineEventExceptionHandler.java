package de.dlr.ivf.tapas.execution.sequential.event;

import com.lmax.disruptor.ExceptionHandler;

public class StateMachineEventExceptionHandler implements ExceptionHandler<TPS_StateMachineEvent> {
    @Override
    public void handleEventException(Throwable ex, long sequence, TPS_StateMachineEvent event) {

        throw new RuntimeException(ex);
    }

    @Override
    public void handleOnStartException(Throwable ex) {

        throw new RuntimeException(ex);
    }

    @Override
    public void handleOnShutdownException(Throwable ex) {

        throw new RuntimeException(ex);
    }
}
