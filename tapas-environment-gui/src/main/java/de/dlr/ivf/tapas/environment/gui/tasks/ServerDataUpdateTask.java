package de.dlr.ivf.tapas.environment.gui.tasks;

import de.dlr.ivf.tapas.environment.dto.ServerEntry;

import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;

import java.util.Collection;
import java.util.function.Supplier;

public class ServerDataUpdateTask extends ScheduledService<Collection<ServerEntry>> {

    private final Supplier<Collection<ServerEntry>> serverDataSupplier;

    public ServerDataUpdateTask(Supplier<Collection<ServerEntry>> serverDataSupplier) {

        this.serverDataSupplier = serverDataSupplier;
    }

    @Override
    protected Task<Collection<ServerEntry>> createTask() {
        return new Task<>() {
            @Override
            protected Collection<ServerEntry> call() throws Exception {
                return serverDataSupplier.get();
            }
        };
    }
}
