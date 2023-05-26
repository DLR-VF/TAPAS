package de.dlr.ivf.tapas.environment.gui.fx.viewmodel.implementation;

import de.dlr.ivf.tapas.environment.dto.SimulationEntry;
import de.dlr.ivf.tapas.environment.gui.fx.model.SimulationsModel;
import de.dlr.ivf.tapas.environment.gui.tasks.SimulationDataUpdateTask;
import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.File;

public class SimulationsViewModel {

    private final SimulationsModel dataModel;

    private final ObservableList<SimulationEntryViewModel> simulations;
    private final SimulationDataUpdateTask simulationDataUpdateTask;


    public SimulationsViewModel(SimulationsModel dataModel, SimulationDataUpdateTask simulationDataUpdateTask){

        this.dataModel = dataModel;
        this.simulationDataUpdateTask = simulationDataUpdateTask;

        //Note: an extractor has been specified in order to fire property changes when nested properties change.
        this.simulations = FXCollections.observableArrayList(simRow -> new Observable[]{
                simRow.descriptionProperty(), simRow.elapsedTimeProperty(), simRow.endDateTimeProperty(),
                simRow.isFinishedProperty(), simRow.isReadyProperty(), simRow.isStartedProperty(),
                simRow.progressProperty(), simRow.serverProperty(), simRow.startDateTimeProperty()
        });

        simulationDataUpdateTask.setOnSucceeded( e -> {
            if(this.simulationDataUpdateTask.getValue() != null)
                this.simulationDataUpdateTask.getValue().forEach(this::updateSimulationEntry);
        });
    }


    public void addSimulation(File simFile) {

    }


    public ObservableList<SimulationEntryViewModel> observableSimulations(){
        return this.simulations;
    }

    private void updateSimulationEntry(SimulationEntry entry){
        var viewEntry = simulations.stream()
                .filter(m -> m.idProperty().getValue() == entry.getId())
                .findFirst();

        if(viewEntry.isPresent()){
            var sim = viewEntry.get();
            updateSimulationEntryViewModel(sim,entry);
        }else {
            var sim = new SimulationEntryViewModel();
            updateSimulationEntryViewModel(sim, entry);
            simulations.add(sim);
        }

    }

    private void updateSimulationEntryViewModel(SimulationEntryViewModel sim, SimulationEntry entry) {
        sim.isFinishedProperty().set(entry.isSimFinished());
        sim.isReadyProperty().set(entry.isSimReady());
        sim.isStartedProperty().set(entry.isSimStarted());
        sim.nameProperty().set(entry.getSimKey());
        sim.idProperty().set(entry.getId());
        //sim.progressProperty().set(entry.getSimProgress());
    }
}
