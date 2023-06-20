package de.dlr.ivf.tapas.environment.gui.fx.viewmodel.implementation;

import de.dlr.ivf.tapas.environment.dto.SimulationEntry;
import de.dlr.ivf.tapas.environment.gui.fx.model.SimulationsModel;
import de.dlr.ivf.tapas.environment.gui.fx.viewmodel.util.LocalDateTimeUtils;
import de.dlr.ivf.tapas.environment.gui.services.SimulationDataUpdateService;
import de.dlr.ivf.tapas.environment.gui.services.SimulationInsertionService;
import de.dlr.ivf.tapas.environment.gui.services.SimulationRemovalService;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;

import java.io.File;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;

public class SimulationsViewModel {

    private final SimulationsModel dataModel;

    private final ObservableList<SimulationEntryViewModel> simulations;
    private final SimulationDataUpdateService simulationDataUpdateService;

    private final DateTimeFormatter dateTimeFormatter;

    private final BooleanProperty insertionServiceRunningProperty = new SimpleBooleanProperty(false);
    private final BooleanProperty removalServiceRunningProperty = new SimpleBooleanProperty(false);

    public SimulationsViewModel(SimulationsModel dataModel, SimulationDataUpdateService simulationDataUpdateService){

        this.dataModel = dataModel;
        this.simulationDataUpdateService = simulationDataUpdateService;

        this.dateTimeFormatter =  LocalDateTimeUtils.dateTimeFormatter();

        //Note: an extractor has been specified in order to fire property changes when nested properties change.
        this.simulations = FXCollections.observableArrayList(simRow -> new Observable[]{ simRow.idProperty(),
                simRow.descriptionProperty(), simRow.elapsedTimeProperty(), simRow.endDateTimeProperty(),
                simRow.progressProperty(), simRow.serverProperty(), simRow.startDateTimeProperty()
        });

        simulationDataUpdateService.setOnSucceeded(e -> {
            if(this.simulationDataUpdateService.getValue() != null)
                this.simulationDataUpdateService.getValue().forEach(this::updateSimulationEntry);
        });
    }


    public void addSimulation(File simFile) {
        Service<Void> simulationInsertionService = new SimulationInsertionService(dataModel, simFile);
        this.insertionServiceRunningProperty.bind(simulationInsertionService.runningProperty());
        simulationInsertionService.start();
    }

    public void removeSimulations(Collection<Integer> simIds){

        Collection<SimulationEntry> simulationsToRemove = new ArrayList<>(simIds.size());

        for(int simId : simIds){
            SimulationEntry simulationEntry = dataModel.getSimulation(simId);

            if(simulationEntry == null){
                throw new IllegalArgumentException("Simulation with id: "+simId+ "is not present in the data model");
            }
            simulationsToRemove.add(simulationEntry);
        }

        Service<Void> simulationRemovalService = new SimulationRemovalService(dataModel, simulationsToRemove);
        removalServiceRunningProperty.bind(simulationRemovalService.runningProperty());

        simulationRemovalService.setOnSucceeded(e ->
                simulationsToRemove.forEach(simEntry -> this.simulations.removeIf(sim -> sim.idProperty().get() == simEntry.getId())));
        simulationRemovalService.start();
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

        sim.startDateTimeProperty().set(startDate == null ? null : startDate.format(dateTimeFormatter));
        sim.endDateTimeProperty().set(endDate == null ? null : endDate.format(dateTimeFormatter));

        Duration duration = startDate == null || endDate == null ? Duration.ofSeconds(0) : Duration.between(startDate,endDate);

        String elapsedTime = LocalDateTimeUtils.durationToFormattedTime(duration);
        sim.elapsedTimeProperty().set(elapsedTime);
    }

    public ReadOnlyBooleanProperty simulationInsertionServiceRunningProperty(){
        return this.insertionServiceRunningProperty;
    }

    public ReadOnlyBooleanProperty simulationRemovalServiceRunningProperty(){
        return this.removalServiceRunningProperty;
    }
}
