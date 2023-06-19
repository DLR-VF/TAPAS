package de.dlr.ivf.tapas.environment.gui.services;

import de.dlr.ivf.tapas.environment.TapasEnvironment;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import lombok.Setter;


public class SimulationRemovalService extends Service<Void> {

    private final TapasEnvironment tapasEnvironment;

    @Setter
    private int simulationId;

    public SimulationRemovalService(TapasEnvironment tapasEnvironment){

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

                tapasEnvironment.removeSimulation(simulationId);
                return null;
            }
        };
    }
}
