package de.dlr.ivf.tapas.environment.gui.services;

import de.dlr.ivf.tapas.environment.dto.ServerEntry;

import de.dlr.ivf.tapas.environment.gui.fx.model.ServersModel;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;

import java.util.Collection;

public class ServerDataUpdateService extends ScheduledService<Collection<ServerEntry>> {

    private final ServersModel serversModel;

    public ServerDataUpdateService(ServersModel serversModel) {

        this.serversModel = serversModel;
    }

    @Override
    protected Task<Collection<ServerEntry>> createTask() {
        return new Task<>() {
            @Override
            protected Collection<ServerEntry> call() throws Exception {
                return serversModel.reload();
            }
        };
    }
}
