package de.dlr.ivf.tapas.environment.model;

import de.dlr.ivf.tapas.environment.dto.SimulationEntry;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

@AllArgsConstructor
public class Simulation {
    private final SimulationEntry simulationEntry;

    @Getter
    private final Map<String, String> parameters;

    public int getId(){
        return simulationEntry.getId();
    }

    public String getIdentifier(){
        return simulationEntry.getSimKey();
    }

    public SimulationState getSimulationState(){
        return simulationEntry.getSimState();
    }

}
