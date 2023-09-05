package de.dlr.ivf.tapas.daemon.managers;

import de.dlr.ivf.tapas.daemon.monitors.SimulationStateMonitor;
import de.dlr.ivf.tapas.environment.dto.SimulationEntry;
import lombok.Builder;

@Builder
public class SimulationManager implements Runnable {

    private final SimulationStateMonitor simulationStateMonitor;
    private final SimulationEntry simulationEntry;


    public SimulationManager(SimulationStateMonitor simulationStateMonitor, SimulationEntry simulationEntry){
        this.simulationStateMonitor = simulationStateMonitor;
        this.simulationEntry = simulationEntry;
    }

    @Override
    public void run() {
    }
}
