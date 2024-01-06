package de.dlr.ivf.tapas.environment.dao;

import de.dlr.ivf.tapas.environment.dao.exception.DaoDeleteException;
import de.dlr.ivf.tapas.environment.dao.exception.DaoInsertException;
import de.dlr.ivf.tapas.environment.dto.ParameterEntry;

import java.util.Collection;

public interface ParametersDao {

    Collection<ParameterEntry> readSimulationParameters(int id);

    void insert(Collection<ParameterEntry> parameters) throws DaoInsertException;

    void removeBySimulationId(int simulationId) throws DaoDeleteException;


}
