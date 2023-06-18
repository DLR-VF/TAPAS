package de.dlr.ivf.tapas.environment.dao;

import de.dlr.ivf.tapas.environment.dao.exception.DaoInsertException;
import de.dlr.ivf.tapas.environment.dao.exception.DaoUpdateException;
import de.dlr.ivf.tapas.environment.dto.SimulationEntry;

import java.util.Collection;

/**
 * Interface for simulation entries data access object implementation to handle loading a collection of {@link SimulationEntry}
 */

public interface SimulationsDao {
    Collection<SimulationEntry> load();
    int save(SimulationEntry simulationEntry) throws DaoInsertException;

    void update(int simId, SimulationEntry simulation) throws DaoUpdateException;

    void remove(int simId);
}
