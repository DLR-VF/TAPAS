package de.dlr.ivf.tapas.environment.dao;

import de.dlr.ivf.tapas.environment.dao.exception.DaoDeleteException;
import de.dlr.ivf.tapas.environment.dao.exception.DaoInsertException;
import de.dlr.ivf.tapas.environment.dao.exception.DaoReadException;
import de.dlr.ivf.tapas.environment.dto.ServerEntry;
import de.dlr.ivf.tapas.environment.model.ServerState;

import java.util.Collection;
import java.util.Optional;

public interface ServersDao {

    Collection<ServerEntry> load() throws DaoReadException;

    void removeServers(Collection<ServerEntry> serverEntries) throws DaoDeleteException;

    void insert(ServerEntry serverEntry) throws DaoInsertException;

    void update(ServerEntry serverEntry);

    default Optional<ServerEntry> getByName(String name) throws DaoReadException {
        Collection<ServerEntry> serverEntries = this.load();

        return serverEntries.stream()
                .filter(entry -> name.equals(entry.getServerName()))
                .findFirst();
    }

    default Optional<ServerState> serverState(String serverIdentifier) throws DaoReadException{
        var serverEntry = getByName(serverIdentifier);

        if(serverEntry.isPresent()){
            return serverEntry.map(ServerEntry::getServerState);
        }

        return Optional.empty();
    }
}
