package de.dlr.ivf.tapas.environment.dao;

import de.dlr.ivf.tapas.environment.dao.exception.DaoDeleteException;
import de.dlr.ivf.tapas.environment.dao.exception.DaoInsertException;
import de.dlr.ivf.tapas.environment.dao.exception.DaoReadException;
import de.dlr.ivf.tapas.environment.dao.exception.DaoUpdateException;
import de.dlr.ivf.tapas.environment.dto.SimulationEntry;
import de.dlr.ivf.tapas.environment.model.SimulationState;

import java.util.Collection;
import java.util.Optional;

/**
 * Interface for simulation entries data access object implementation to handle loading a collection of {@link SimulationEntry}
 */

public interface SimulationsDao {
    Collection<SimulationEntry> load() throws DaoReadException;
    int save(SimulationEntry simulationEntry) throws DaoInsertException;

    void update(int simId, SimulationEntry simulation) throws DaoUpdateException;

    void remove(int simId) throws DaoDeleteException;

    Optional<SimulationEntry> requestSimulation(String serverIp);

    default Optional<SimulationEntry> simulationById(int id) throws DaoReadException{
        Collection<SimulationEntry> simulationEntries = this.load();

        return simulationEntries.stream()
                .filter(entry -> entry.getId() == id)
                .findFirst();
    }

    default Optional<SimulationState> simulationState(int simulationId) throws DaoReadException{
        var simulationEntry = simulationById(simulationId);

        if(simulationEntry.isPresent()){
            return simulationEntry.map(SimulationEntry::getSimState);
        }

        return Optional.empty();
    }
}
