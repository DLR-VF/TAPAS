package de.dlr.ivf.tapas.execution.sequential.statemachine;

public class TPS_StateTransitionException extends RuntimeException {


    private final Throwable throwable;
    private final String message;
    private final TPS_StateMachine state_machine;

    public TPS_StateTransitionException(String s, Throwable t, TPS_StateMachine state_machine) {
        this.throwable = t;
        this.message = s;
        this.state_machine = state_machine;
    }

    public Throwable getException() {
        return throwable;
    }

    @Override
    public String getMessage() {
        return message+" \n Reason:  "+throwable.getMessage();
    }

    public TPS_StateMachine getStateMachine(){
        return this.state_machine;
    }


}
