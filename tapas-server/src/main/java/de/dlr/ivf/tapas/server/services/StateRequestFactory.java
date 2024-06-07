package de.dlr.ivf.tapas.server.services;

import de.dlr.ivf.tapas.server.services.implementation.SimulationStateRequest;
import de.dlr.ivf.tapas.environment.TapasEnvironment;

public class StateRequestFactory {

    private final TapasEnvironment tapasEnvironment;

    public StateRequestFactory(TapasEnvironment tapasEnvironment){
        this.tapasEnvironment = tapasEnvironment;
    }

    public SimulationStateRequest newSimulationStateRequest(int simulationId){
        return new SimulationStateRequest(tapasEnvironment, simulationId);
    }
}
