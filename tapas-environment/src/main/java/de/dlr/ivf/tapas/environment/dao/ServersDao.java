package de.dlr.ivf.tapas.environment.dao;

import de.dlr.ivf.tapas.environment.dto.ServerEntry;

import java.util.Collection;

public interface ServersDao {

    Collection<ServerEntry> load();

    void removeServer(ServerEntry serverEntry);
}