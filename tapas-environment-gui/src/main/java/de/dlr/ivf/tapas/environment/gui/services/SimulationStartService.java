package de.dlr.ivf.tapas.environment.gui.services;

import javafx.concurrent.Service;
import javafx.concurrent.Task;

public class SimulationStartService extends Service<Void> {
    @Override
    protected Task<Void> createTask() {
        return new Task<>() {
            @Override
            protected Void call() throws Exception {
                return null;
            }
        };
    }
}
