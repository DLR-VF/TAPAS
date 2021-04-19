package de.dlr.ivf.tapas.execution.sequential.action;

import de.dlr.ivf.tapas.execution.sequential.statemachine.TPS_StateMachine;

public class TransitionToEndstateAction implements TPS_PlanStateAction{

    private TPS_StateMachine state_machine;

    public TransitionToEndstateAction(TPS_StateMachine state_machine){

        this.state_machine = state_machine;
    }
    @Override
    public void run() {

        this.state_machine.transitionToEndState();
    }
}
