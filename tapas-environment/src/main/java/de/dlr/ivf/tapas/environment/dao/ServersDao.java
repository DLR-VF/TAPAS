package de.dlr.ivf.tapas.environment.dao;

import de.dlr.ivf.tapas.environment.dao.exception.DaoDeleteException;
import de.dlr.ivf.tapas.environment.dao.exception.DaoInsertException;
import de.dlr.ivf.tapas.environment.dao.exception.DaoReadException;
import de.dlr.ivf.tapas.environment.dto.ServerEntry;

import java.util.Collection;

public interface ServersDao {

    Collection<ServerEntry> load() throws DaoReadException;

    void removeServers(Collection<ServerEntry> serverEntries) throws DaoDeleteException;

    void insert(ServerEntry serverEntry) throws DaoInsertException;

    void update(ServerEntry serverEntry);
}
