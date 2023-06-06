package de.dlr.ivf.tapas.environment.gui.services;

import de.dlr.ivf.tapas.environment.dto.SimulationEntry;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;

import java.util.Collection;
import java.util.function.Supplier;

public class SimulationDataUpdateService extends ScheduledService<Collection<SimulationEntry>> {

    private final Supplier<Collection<SimulationEntry>> simulationDataSupplier;

    public SimulationDataUpdateService(Supplier<Collection<SimulationEntry>> simulationDataSupplier){

        this.simulationDataSupplier = simulationDataSupplier;
    }

    @Override
    protected Task<Collection<SimulationEntry>> createTask() {
        return new Task<>() {
            @Override
            protected Collection<SimulationEntry> call() throws Exception {
                return simulationDataSupplier.get();
            }
        };
    }
}
