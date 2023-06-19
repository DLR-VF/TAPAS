package de.dlr.ivf.tapas.environment.gui.fx.viewmodel.implementation;

import de.dlr.ivf.tapas.environment.dto.SimulationEntry;
import de.dlr.ivf.tapas.environment.gui.fx.model.SimulationsModel;
import de.dlr.ivf.tapas.environment.gui.fx.viewmodel.util.LocalDateTimeUtils;
import de.dlr.ivf.tapas.environment.gui.services.SimulationDataUpdateService;
import de.dlr.ivf.tapas.environment.gui.services.SimulationInsertionService;
import de.dlr.ivf.tapas.environment.gui.services.SimulationRemovalService;
import javafx.beans.Observable;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.File;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;

public class SimulationsViewModel {

    private final SimulationsModel dataModel;

    private final ObservableList<SimulationEntryViewModel> simulations;
    private final SimulationDataUpdateService simulationDataUpdateService;

    private final SimulationInsertionService simulationInsertionService;

    private final SimulationRemovalService simulationRemovalService;

    private final DateTimeFormatter dateTimeFormatter;


    public SimulationsViewModel(SimulationsModel dataModel, SimulationDataUpdateService simulationDataUpdateService,
                                SimulationInsertionService simulationInsertionService, SimulationRemovalService simulationRemovalService){

        this.dataModel = dataModel;
        this.simulationDataUpdateService = simulationDataUpdateService;
        this.simulationInsertionService = simulationInsertionService;
        this.simulationRemovalService = simulationRemovalService;

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
        this.simulationInsertionService.setSimFile(simFile);
        this.simulationInsertionService.start();
    }

    public void removeSimulations(Collection<Integer> simIds){

        Collection<SimulationEntry> simulationsToRemove = new ArrayList<>(simIds.size());

        for(int simId : simIds){
            SimulationEntry simulationEntry = dataModel.getSimulations()
                    .stream()
                    .filter(sim -> sim.getId() == simId)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Simulation with id: "+simId+ "is not present in the data model"));
            simulationsToRemove.add(simulationEntry);
        }

        this.simulationRemovalService.setSimulations(simulationsToRemove);
        this.simulationRemovalService.start();
        this.simulationRemovalService.setOnSucceeded(e ->
                simulationsToRemove.forEach(simEntry -> this.simulations.removeIf(sim -> sim.idProperty().get() == simEntry.getId())));
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
        return this.simulationInsertionService.runningProperty();
    }

    public ReadOnlyBooleanProperty simulationRemovalServiceRunningProperty(){
        return this.simulationRemovalService.runningProperty();
    }
}
