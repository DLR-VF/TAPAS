package de.dlr.ivf.tapas.environment.gui.services;

import de.dlr.ivf.tapas.environment.gui.fx.model.SimulationsModel;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

import java.io.File;

public class SimulationInsertionService extends Service<Void> {

    private final File simFile;
    private final SimulationsModel simulationsModel;

    public SimulationInsertionService(SimulationsModel simulationsModel, File simFile){
        this.simulationsModel = simulationsModel;
        this.simFile = simFile;
    }
    @Override
    protected Task<Void> createTask() {

        return new Task<>() {
            @Override
            protected Void call() throws Exception {
                simulationsModel.insert(simFile);

                return null;
            }
        };
    }
}
