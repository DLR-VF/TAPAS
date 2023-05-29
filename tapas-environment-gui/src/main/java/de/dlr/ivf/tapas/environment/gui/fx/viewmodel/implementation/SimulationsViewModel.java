package de.dlr.ivf.tapas.environment.gui.fx.viewmodel.implementation;

import de.dlr.ivf.tapas.environment.dto.SimulationEntry;
import de.dlr.ivf.tapas.environment.gui.fx.model.SimulationsModel;
import de.dlr.ivf.tapas.environment.gui.fx.viewmodel.util.LocalDateTimeUtils;
import de.dlr.ivf.tapas.environment.gui.tasks.SimulationDataUpdateTask;
import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.File;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SimulationsViewModel {

    private final SimulationsModel dataModel;

    private final ObservableList<SimulationEntryViewModel> simulations;
    private final SimulationDataUpdateTask simulationDataUpdateTask;

    private final DateTimeFormatter dateTimeFormatter;


    public SimulationsViewModel(SimulationsModel dataModel, SimulationDataUpdateTask simulationDataUpdateTask){

        this.dataModel = dataModel;
        this.simulationDataUpdateTask = simulationDataUpdateTask;
        this.dateTimeFormatter =  LocalDateTimeUtils.dateTimeFormatter();

        //Note: an extractor has been specified in order to fire property changes when nested properties change.
        this.simulations = FXCollections.observableArrayList(simRow -> new Observable[]{ simRow.idProperty(),
                simRow.descriptionProperty(), simRow.elapsedTimeProperty(), simRow.endDateTimeProperty(),
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
            simulations.add(sim);
            updateSimulationEntryViewModel(sim, entry);
        }
    }

    private void updateSimulationEntryViewModel(SimulationEntryViewModel sim, SimulationEntry entry) {
        sim.nameProperty().set(entry.getSimKey());
        sim.idProperty().set(entry.getId());
        sim.simulationStateProperty().set(entry.getSimState());
        sim.progressValueProperty().set(entry.getSimProgress() / 100);

        var startDate = entry.getSimStartedTime() == null ? null : entry.getSimStartedTime().toLocalDateTime();
        var endDate = entry.getSimFinishedTime() == null ? null  : entry.getSimFinishedTime().toLocalDateTime();

        sim.startDateTimeProperty().set(startDate.format(dateTimeFormatter));
        sim.endDateTimeProperty().set(endDate.format(dateTimeFormatter));

        Duration duration = startDate == null || endDate == null ? Duration.ofSeconds(0) : Duration.between(startDate,endDate);

        String elapsedTime = LocalDateTimeUtils.durationToFormattedTime(duration);
        sim.elapsedTimeProperty().set(elapsedTime);
    }
}
