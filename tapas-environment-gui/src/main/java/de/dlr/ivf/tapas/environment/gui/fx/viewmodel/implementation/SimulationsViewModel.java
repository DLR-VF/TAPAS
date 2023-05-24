package de.dlr.ivf.tapas.environment.gui.fx.viewmodel.implementation;

import de.dlr.ivf.tapas.environment.gui.fx.model.SimulationsModel;
import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.File;

public class SimulationsViewModel {

    private final SimulationsModel dataModel;

    private final ObservableList<SimulationEntryViewModel> simulations;



    public SimulationsViewModel(SimulationsModel dataModel){

        this.dataModel = dataModel;

        //Note: an extractor has been specified in order to fire property changes when nested properties change.
        this.simulations = FXCollections.observableArrayList(simRow -> new Observable[]{
                simRow.descriptionProperty(), simRow.elapsedTimeProperty(), simRow.endDateTimeProperty(),
                simRow.isFinishedProperty(), simRow.isReadyProperty(), simRow.isStartedProperty(),
                simRow.progressProperty(), simRow.serverProperty(), simRow.startDateTimeProperty()
        });
    }


    public void addSimulation(File simFile) {

    }
}
