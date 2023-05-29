package de.dlr.ivf.tapas.environment.gui.fx.viewmodel.implementation;

import de.dlr.ivf.tapas.environment.model.SimulationState;
import javafx.beans.property.*;
import javafx.scene.control.ProgressBar;

import java.time.LocalDateTime;

public class SimulationEntryViewModel {

    private final StringProperty elapsedTime = new SimpleStringProperty();
    private final StringProperty endDateTime = new SimpleStringProperty();
    private final ObjectProperty<SimulationState> simulationState = new SimpleObjectProperty<>();
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty description = new SimpleStringProperty();
    private final ObjectProperty<ProgressBar> progress = new SimpleObjectProperty<>();
    private final DoubleProperty progressValue = new SimpleDoubleProperty();
    private final StringProperty startDateTime = new SimpleStringProperty();
    private final StringProperty server = new SimpleStringProperty();

    public SimulationEntryViewModel(){

        //this.progress.get().progressProperty().bind(this.progressValue);
    }

    public StringProperty elapsedTimeProperty() {
        return elapsedTime;
    }

    public StringProperty endDateTimeProperty() {
        return endDateTime;
    }


    public IntegerProperty idProperty() {
        return id;
    }

    public StringProperty nameProperty() {
        return name;
    }

    public StringProperty descriptionProperty() {
        return description;
    }

    public ObjectProperty<ProgressBar> progressProperty() {
        return progress;
    }

    public DoubleProperty progressValueProperty(){return progressValue;}


    public StringProperty startDateTimeProperty() {
        return startDateTime;
    }


    public StringProperty serverProperty() {
        return server;
    }

    public ObjectProperty<SimulationState> simulationStateProperty(){
        return simulationState;
    }

}
