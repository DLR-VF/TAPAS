package de.dlr.ivf.tapas.environment.gui.fx.model.implementation;

import de.dlr.ivf.tapas.environment.dao.ServersDao;
import de.dlr.ivf.tapas.environment.dto.ServerEntry;
import de.dlr.ivf.tapas.environment.gui.fx.model.ServersModel;
import de.dlr.ivf.tapas.environment.gui.fx.viewmodel.implementation.ServerEntryViewModel;

import java.util.Collection;

public class ServersModelManager implements ServersModel {
    private final ServersDao dao;

    public ServersModelManager(ServersDao dao) {
        this.dao = dao;
    }

    @Override
    public Collection<ServerEntry> getServerData() {
        return dao.load();
    }

    @Override
    public void save(ServerEntryViewModel server) {
        //dao.
    }
}
