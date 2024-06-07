package de.dlr.ivf.tapas.server.services.implementation;

import de.dlr.ivf.tapas.environment.TapasEnvironment;
import de.dlr.ivf.tapas.environment.model.SimulationState;

import java.util.Optional;
import java.util.concurrent.Callable;

public class SimulationStateRequest implements Callable<Optional<SimulationState>> {

    private final TapasEnvironment tapasEnvironment;
    private final int simulationId;

    public SimulationStateRequest(TapasEnvironment tapasEnvironment, int simulationId){
        this.tapasEnvironment = tapasEnvironment;
        this.simulationId = simulationId;
    }
    @Override
    public Optional<SimulationState> call() throws Exception {
        return tapasEnvironment.simulationState(simulationId);
    }
}
