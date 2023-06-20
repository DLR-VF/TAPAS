package de.dlr.ivf.tapas.environment.gui.services;

import de.dlr.ivf.tapas.environment.dto.SimulationEntry;
import de.dlr.ivf.tapas.environment.gui.fx.model.SimulationsModel;

import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;

import java.util.Collection;

public class SimulationDataUpdateService extends ScheduledService<Collection<SimulationEntry>> {

    private final SimulationsModel simulationsModel;

    public SimulationDataUpdateService(SimulationsModel simulationsModel){

        this.simulationsModel = simulationsModel;
    }

    @Override
    protected Task<Collection<SimulationEntry>> createTask() {
        return new Task<>() {
            @Override
            protected Collection<SimulationEntry> call() throws Exception {
                return simulationsModel.reload();
            }
        };
    }
}
