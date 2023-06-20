package de.dlr.ivf.tapas.environment.gui.services;

import de.dlr.ivf.tapas.environment.dto.SimulationEntry;
import de.dlr.ivf.tapas.environment.gui.fx.model.SimulationsModel;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

import java.util.Collection;


public class SimulationRemovalService extends Service<Void> {

    private final SimulationsModel simulationsModel;

    private final Collection<SimulationEntry> simulations;

    public SimulationRemovalService(SimulationsModel simulationsModel, Collection<SimulationEntry> simulationEntries){

        this.simulationsModel = simulationsModel;
        this.simulations = simulationEntries;
    }
    @Override
    protected Task<Void> createTask() {
        return new Task<>() {
            @Override
            protected Void call() throws Exception {
                simulationsModel.remove(simulations);

                return null;
            }
        };
    }
}
