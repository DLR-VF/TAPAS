package de.dlr.ivf.tapas.environment.gui.services;

import de.dlr.ivf.tapas.environment.TapasEnvironment;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import lombok.Setter;

import java.io.File;

public class SimulationInsertionService extends Service<Void> {

    @Setter
    private File simFile;
    private final TapasEnvironment tapasEnvironment;

    public SimulationInsertionService(TapasEnvironment tapasEnvironment){
        this.tapasEnvironment = tapasEnvironment;
    }
    @Override
    protected Task<Void> createTask() {

        return new Task<>() {
            @Override
            protected void failed() {
                super.failed();
                reset();
            }

            @Override
            protected void succeeded() {
                super.succeeded();
                reset();
            }

            @Override
            protected Void call() throws Exception {

                tapasEnvironment.addSimulation(simFile);

                return null;
            }
        };
    }
}
