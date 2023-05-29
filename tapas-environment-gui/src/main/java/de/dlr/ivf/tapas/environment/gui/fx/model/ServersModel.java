package de.dlr.ivf.tapas.environment.gui.fx.model;

import de.dlr.ivf.tapas.environment.dto.ServerEntry;
import de.dlr.ivf.tapas.environment.gui.fx.viewmodel.implementation.ServerEntryViewModel;

import java.util.Collection;

public interface ServersModel {

    Collection<ServerEntry> getServerData();

    void save(ServerEntryViewModel server);


}
