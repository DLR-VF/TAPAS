package de.dlr.ivf.tapas.execution.sequential.action;

import de.dlr.ivf.tapas.execution.sequential.context.PlanContext;
import de.dlr.ivf.tapas.execution.sequential.statemachine.TPS_StateMachine;

public class TransitionToEndstateAction implements TPS_PlanStateAction{

    private final PlanContext plan_context;
    private TPS_StateMachine state_machine;

    public TransitionToEndstateAction(TPS_StateMachine state_machine, PlanContext plan_context){

        this.state_machine = state_machine;
        this.plan_context = plan_context;
    }
    @Override
    public void run() {

        if(!plan_context.getTourContext().isPresent())
            this.state_machine.getController().endOfSimulationFor(this.state_machine);
    }
}
