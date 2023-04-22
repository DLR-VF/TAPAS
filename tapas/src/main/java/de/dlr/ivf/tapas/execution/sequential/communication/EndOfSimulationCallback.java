package de.dlr.ivf.tapas.execution.sequential.communication;

import de.dlr.ivf.tapas.execution.sequential.statemachine.TPS_StateMachine;

public interface EndOfSimulationCallback {

    void endOfSimulationFor(TPS_StateMachine state_machine);

}
