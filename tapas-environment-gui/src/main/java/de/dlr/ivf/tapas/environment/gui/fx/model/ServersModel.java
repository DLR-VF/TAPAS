package de.dlr.ivf.tapas.environment.gui.fx.model;

import de.dlr.ivf.tapas.environment.dto.ServerEntry;

import java.io.IOException;
import java.util.Collection;

public interface ServersModel {

    Collection<ServerEntry> getServerData();

    ServerEntry getServer(String serverIp);

    Collection<ServerEntry> reload() throws IOException;

    void remove(Collection<ServerEntry> serversToRemove) throws IOException;

    void insert(ServerEntry server) throws IOException;


}
