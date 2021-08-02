package de.dlr.ivf.tapas.execution.sequential.event;

import com.lmax.disruptor.ExceptionHandler;

public class StateMachineEventExceptionHandler implements ExceptionHandler<TPS_StateMachineEvent> {
    @Override
    public void handleEventException(Throwable ex, long sequence, TPS_StateMachineEvent event) {

        TPS_Event handled_event = event.getEvent();

        if(handled_event.getEventType() == TPS_EventType.SIMULATION_STEP)
            event.getStateMachineController().handleError(ex);
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
