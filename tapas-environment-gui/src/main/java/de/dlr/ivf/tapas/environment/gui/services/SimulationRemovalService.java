package de.dlr.ivf.tapas.environment.gui.services;

import de.dlr.ivf.tapas.environment.TapasEnvironment;
import de.dlr.ivf.tapas.environment.dto.SimulationEntry;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import lombok.Setter;

import java.util.Collection;


public class SimulationRemovalService extends Service<Void> {

    private final TapasEnvironment tapasEnvironment;

    @Setter
    private Collection<SimulationEntry> simulations;

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

                for(SimulationEntry simulationEntry : simulations){
                    tapasEnvironment.removeSimulation(simulationEntry);
                }
                return null;
            }
        };
    }
}
