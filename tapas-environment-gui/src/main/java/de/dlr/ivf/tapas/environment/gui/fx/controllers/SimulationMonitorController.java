package de.dlr.ivf.tapas.environment.gui.fx.controllers;

import de.dlr.ivf.tapas.environment.dto.ServerEntry;
import de.dlr.ivf.tapas.environment.dto.SimulationEntry;
import javafx.beans.Observable;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.net.URL;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ResourceBundle;

public class SimulationMonitorController implements Initializable {

    @FXML
    TableView<ServerEntry> serverTable;
    @FXML
    TableView<SimulationEntry> simulationTable;
    @FXML
    private TableColumn<ServerEntry, Integer> serverCoreCount;
    @FXML
    private TableColumn<ServerEntry, Double> serverCpuUsage;
    @FXML
    private TableColumn<ServerEntry, String> serverIpAddress;
    @FXML
    private TableColumn<ServerEntry, String> serverName;
    @FXML
    private TableColumn<ServerEntry, Boolean> serverOnline;
    @FXML
    private TableColumn<SimulationEntry, String> simCount;
    @FXML
    private TableColumn<SimulationEntry, Duration> simElapsedTime;
    @FXML
    private TableColumn<SimulationEntry, LocalDateTime> simEnd;
    @FXML
    private TableColumn<SimulationEntry, Boolean> simFinished;
    @FXML
    private TableColumn<SimulationEntry, Integer> simId;
    @FXML
    private TableColumn<SimulationEntry, String> simName;
    @FXML
    private TableColumn<SimulationEntry, String> simDescription;
    @FXML
    private TableColumn<SimulationEntry, ProgressBar> simProgress;
    @FXML
    private TableColumn<SimulationEntry, Boolean> simReady;
    @FXML
    private TableColumn<SimulationEntry, LocalDateTime> simStart;
    @FXML
    private TableColumn<SimulationEntry, Boolean> simStarted;

    private ObservableList<ServerEntry> serverEntries = FXCollections.observableArrayList();


    @Override
    public void initialize(URL location, ResourceBundle resources) {

        //set server table cell factories
        this.serverName.setCellValueFactory(serverEntry -> new SimpleStringProperty(serverEntry.getValue().getServerName()));
        this.serverIpAddress.setCellValueFactory(serverEntry -> new SimpleStringProperty(serverEntry.getValue().getServerIp()));
        this.serverOnline.setCellValueFactory(serverEntry -> new SimpleBooleanProperty(serverEntry.getValue().isServerOnline()));
        this.serverCpuUsage.setCellValueFactory(serverEntry -> new SimpleDoubleProperty(serverEntry.getValue().getServerUsage()).asObject());
        this.serverCoreCount.setCellValueFactory(serverEntry -> new SimpleIntegerProperty(serverEntry.getValue().getServerCores()).asObject());

        //set simulation table cell factories
        this.simId.setCellValueFactory(simEntry -> new SimpleIntegerProperty(simEntry.getValue().getId()).asObject());
        this.simName.setCellValueFactory(simEntry -> new SimpleStringProperty(simEntry.getValue().getSimKey()));
        this.simDescription.setCellValueFactory(simEntry -> new SimpleStringProperty(simEntry.getValue().getSimDescription()));
        this.simReady.setCellValueFactory(simEntry -> new SimpleBooleanProperty(simEntry.getValue().isSimReady()));
        this.simStarted.setCellValueFactory(simEntry -> new SimpleBooleanProperty(simEntry.getValue().isSimStarted()));
        this.simStarted.setCellValueFactory(simEntry -> new SimpleBooleanProperty(simEntry.getValue().isSimStarted()));
        this.simFinished.setCellValueFactory(simEntry -> new SimpleBooleanProperty(simEntry.getValue().isSimFinished()));
        this.simReady.setCellValueFactory(simEntry -> new SimpleBooleanProperty(simEntry.getValue().isSimReady()));
        this.simEnd.setCellValueFactory(simEntry -> new SimpleObjectProperty<>(simEntry.getValue().getSimFinishedTime().toLocalDateTime()));
        this.simStart.setCellValueFactory(simEntry -> new SimpleObjectProperty<>(simEntry.getValue().getSimFinishedTime().toLocalDateTime()));

        //todo need to rethink this
        this.simElapsedTime.setCellValueFactory(simEntry -> new SimpleObjectProperty<>(Duration.between(
                simEntry.getValue().isSimStarted() ? simEntry.getValue().getSimStartedTime().toLocalDateTime() :  LocalDateTime.now(),
                simEntry.getValue().isSimFinished() ? simEntry.getValue().getSimFinishedTime().toLocalDateTime() : LocalDateTime.now())));
    }
}
