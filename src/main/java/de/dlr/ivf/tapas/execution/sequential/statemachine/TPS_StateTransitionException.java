package de.dlr.ivf.tapas.execution.sequential.statemachine;

public class TPS_StateTransitionException extends RuntimeException {


    private final Exception exception;
    private final String message;
    private final TPS_StateMachine state_machine;

    public TPS_StateTransitionException(String s, Exception e, TPS_StateMachine state_machine) {
        this.exception = e;
        this.message = s;
        this.state_machine = state_machine;
    }

    public Exception getException() {
        return exception;
    }

    public String getMessageString() {
        return message;
    }

    public TPS_StateMachine getStateMachine(){
        return this.getStateMachine();
    }


}
