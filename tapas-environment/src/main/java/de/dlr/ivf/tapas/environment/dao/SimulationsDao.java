package de.dlr.ivf.tapas.environment.dao;

import de.dlr.ivf.tapas.environment.dto.SimulationEntry;

import java.util.Collection;

/**
 * Interface for simulation entries data access object implementation to handle loading a collection of {@link SimulationEntry}
 */

public interface SimulationsDao {
    Collection<SimulationEntry> load();
    int save(SimulationEntry simulationEntry);

    void update(int simId, SimulationEntry simulation);

    void remove(int simId);
}
