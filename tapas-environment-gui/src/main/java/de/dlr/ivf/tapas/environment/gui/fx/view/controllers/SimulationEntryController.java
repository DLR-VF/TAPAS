package de.dlr.ivf.tapas.environment.gui.fx.view.controllers;

import de.dlr.ivf.tapas.environment.gui.fx.viewmodel.implementation.SimulationEntryViewModel;
import de.dlr.ivf.tapas.environment.gui.fx.viewmodel.implementation.SimulationsViewModel;
import javafx.beans.property.SimpleListProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ResourceBundle;

public class SimulationEntryController implements Initializable {

    @FXML
    private TableView<SimulationEntryViewModel> simulationTable;

    @FXML
    public Button addSimulationButton;

    @FXML
    private TableColumn<SimulationEntryViewModel, Number> simId;
    @FXML
    private TableColumn<SimulationEntryViewModel, String> simName;
    @FXML
    private TableColumn<SimulationEntryViewModel, String> simDescription;
    @FXML
    private TableColumn<SimulationEntryViewModel, ProgressBar> simProgress;
    @FXML
    private TableColumn<SimulationEntryViewModel, Boolean> simReady;
    @FXML
    private TableColumn<SimulationEntryViewModel, Boolean> simStarted;
    @FXML
    private TableColumn<SimulationEntryViewModel, Boolean> simFinished;
    @FXML
    private TableColumn<SimulationEntryViewModel, LocalDateTime> simStart;
    @FXML
    private TableColumn<SimulationEntryViewModel, LocalDateTime> simEnd;
    @FXML
    private TableColumn<SimulationEntryViewModel, Duration> simElapsedTime;

    private final SimulationsViewModel viewModel;

    public SimulationEntryController(SimulationsViewModel viewModel){
        this.viewModel = viewModel;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        simId.setCellValueFactory(cell -> cell.getValue().idProperty());
        simName.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        simDescription.setCellValueFactory(cellData -> cellData.getValue().descriptionProperty());
        simProgress.setCellValueFactory(cellData -> cellData.getValue().progressProperty());
        simReady.setCellValueFactory(cellData -> cellData.getValue().isReadyProperty());
        simStarted.setCellValueFactory(cellData -> cellData.getValue().isStartedProperty());
        simFinished.setCellValueFactory(cellData -> cellData.getValue().isFinishedProperty());
        simStart.setCellValueFactory(cellData -> cellData.getValue().startDateTimeProperty());
        simEnd.setCellValueFactory(cellData -> cellData.getValue().endDateTimeProperty());
        simElapsedTime.setCellValueFactory(cellData -> cellData.getValue().elapsedTimeProperty());

        simulationTable.itemsProperty().bind(new SimpleListProperty<>(viewModel.observableSimulations()));


    }

    @FXML
    public void addSimulation(ActionEvent actionEvent) {
        Window window = addSimulationButton.getScene().getWindow();
        FileChooser fileChooser = new FileChooser();
        File simFile = fileChooser.showOpenDialog(window);

        if (simFile != null)
            this.viewModel.addSimulation(simFile);
        actionEvent.consume();
    }
}
