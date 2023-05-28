package de.dlr.ivf.tapas.environment.gui.fx.view.controllers;

import de.dlr.ivf.tapas.environment.gui.fx.view.factories.SimulationActionButtonCell;
import de.dlr.ivf.tapas.environment.gui.fx.view.factories.ProgressCell;
import de.dlr.ivf.tapas.environment.gui.fx.view.factories.SimulationTableRow;
import de.dlr.ivf.tapas.environment.gui.fx.viewmodel.implementation.SimulationEntryViewModel;
import de.dlr.ivf.tapas.environment.gui.fx.viewmodel.implementation.SimulationsViewModel;
import de.dlr.ivf.tapas.environment.model.SimulationState;
import javafx.beans.property.SimpleListProperty;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.EnumMap;
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
    private TableColumn<SimulationEntryViewModel, SimulationState> simState;
    @FXML
    private TableColumn<SimulationEntryViewModel, Double> simProgress;
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
    @FXML
    private TableColumn<SimulationEntryViewModel, SimulationState> simAction;

    @FXML
    private ScrollPane simScrollPane;

    private final SimulationsViewModel viewModel;

    private final EnumMap<SimulationState, PseudoClass> simStatePseudoClasses = new EnumMap<>(SimulationState.class);

    public SimulationEntryController(SimulationsViewModel viewModel){

        this.viewModel = viewModel;

        //add simulation state pseudo classes
        simStatePseudoClasses.put(SimulationState.READY, PseudoClass.getPseudoClass("ready"));
        simStatePseudoClasses.put(SimulationState.RUNNING, PseudoClass.getPseudoClass("running"));
        simStatePseudoClasses.put(SimulationState.PAUSED, PseudoClass.getPseudoClass("paused"));
        simStatePseudoClasses.put(SimulationState.FINISHED, PseudoClass.getPseudoClass("finished"));
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        simId.setCellValueFactory(cell -> cell.getValue().idProperty());
        simName.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        simDescription.setCellValueFactory(cellData -> cellData.getValue().descriptionProperty());
        simState.setCellValueFactory(cellData -> cellData.getValue().simulationStateProperty());
        simProgress.setCellValueFactory(cellData -> cellData.getValue().progressValueProperty().asObject());
        simReady.setCellValueFactory(cellData -> cellData.getValue().isReadyProperty());
        simStarted.setCellValueFactory(cellData -> cellData.getValue().isStartedProperty());
        simFinished.setCellValueFactory(cellData -> cellData.getValue().isFinishedProperty());
        simStart.setCellValueFactory(cellData -> cellData.getValue().startDateTimeProperty());
        simEnd.setCellValueFactory(cellData -> cellData.getValue().endDateTimeProperty());
        simElapsedTime.setCellValueFactory(cellData -> cellData.getValue().elapsedTimeProperty());
        simAction.setCellFactory(cell -> new SimulationActionButtonCell<>());
        simAction.setCellValueFactory(cellData -> cellData.getValue().simulationStateProperty());

        simulationTable.itemsProperty().bind(new SimpleListProperty<>(viewModel.observableSimulations()));
        simulationTable.setRowFactory(row -> new SimulationTableRow(simStatePseudoClasses));
        simProgress.setCellFactory(cell -> new ProgressCell<>());

        simulationTable.prefWidthProperty().bind(simScrollPane.widthProperty());
        simulationTable.prefHeightProperty().bind(simScrollPane.heightProperty());

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
