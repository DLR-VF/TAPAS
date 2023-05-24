package de.dlr.ivf.tapas.environment.gui.fx.viewmodel.implementation;

import javafx.beans.property.*;
import javafx.scene.control.ProgressBar;

import java.time.Duration;
import java.time.LocalDateTime;

public class SimulationEntryViewModel {

    private final ObjectProperty<Duration> elapsedTime = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalDateTime> endDateTime = new SimpleObjectProperty<>();
    private final BooleanProperty isFinished = new SimpleBooleanProperty();
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty description = new SimpleStringProperty();
    private final ObjectProperty<ProgressBar> progress = new SimpleObjectProperty<>();
    private final BooleanProperty isReady = new SimpleBooleanProperty();
    private final ObjectProperty<LocalDateTime> startDateTime = new SimpleObjectProperty<>();
    private final BooleanProperty isStarted = new SimpleBooleanProperty();

    private final StringProperty server = new SimpleStringProperty();

    public ObjectProperty<Duration> elapsedTimeProperty() {
        return elapsedTime;
    }

    public ObjectProperty<LocalDateTime> endDateTimeProperty() {
        return endDateTime;
    }

    public BooleanProperty isFinishedProperty() {
        return isFinished;
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

    public BooleanProperty isReadyProperty() {
        return isReady;
    }

    public ObjectProperty<LocalDateTime> startDateTimeProperty() {
        return startDateTime;
    }

    public BooleanProperty isStartedProperty() {
        return isStarted;
    }

    public StringProperty serverProperty() {
        return server;
    }
}
