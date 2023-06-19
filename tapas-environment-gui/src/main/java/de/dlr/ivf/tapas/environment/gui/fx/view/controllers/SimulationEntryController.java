package de.dlr.ivf.tapas.environment.gui.fx.view.controllers;

import de.dlr.ivf.tapas.environment.gui.fx.view.factories.SimulationActionButtonCell;
import de.dlr.ivf.tapas.environment.gui.fx.view.factories.SimulationProgress;
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
import java.util.EnumMap;
import java.util.ResourceBundle;

public class SimulationEntryController implements Initializable {

    @FXML
    private TableView<SimulationEntryViewModel> simulationTable;

    @FXML
    public Button addSimulationButton;

    @FXML
    public Button removeSimulationButton;

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
    private TableColumn<SimulationEntryViewModel, String> simStart;
    @FXML
    private TableColumn<SimulationEntryViewModel, String> simEnd;
    @FXML
    private TableColumn<SimulationEntryViewModel, String> simElapsedTime;
    @FXML
    private TableColumn<SimulationEntryViewModel, SimulationState> simAction;

    @FXML
    private ScrollPane simScrollPane;

    private final SimulationsViewModel viewModel;

    private final EnumMap<SimulationState, PseudoClass> simStatePseudoClasses = new EnumMap<>(SimulationState.class);
    private final EnumMap<SimulationState, PseudoClass> simActionPseudoClasses = new EnumMap<>(SimulationState.class);

    public SimulationEntryController(SimulationsViewModel viewModel){

        this.viewModel = viewModel;

        //add simulation state pseudo classes
        simStatePseudoClasses.put(SimulationState.READY, PseudoClass.getPseudoClass("ready"));
        simStatePseudoClasses.put(SimulationState.RUNNING, PseudoClass.getPseudoClass("running"));
        simStatePseudoClasses.put(SimulationState.PAUSED, PseudoClass.getPseudoClass("paused"));
        simStatePseudoClasses.put(SimulationState.FINISHED, PseudoClass.getPseudoClass("finished"));

        //add simulation state action pseudo classes
        simActionPseudoClasses.put(SimulationState.READY, PseudoClass.getPseudoClass("start-sim"));
        simActionPseudoClasses.put(SimulationState.PAUSED, PseudoClass.getPseudoClass("start-sim"));
        simActionPseudoClasses.put(SimulationState.RUNNING, PseudoClass.getPseudoClass("stop-sim"));
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        simId.setCellValueFactory(cell -> cell.getValue().idProperty());
        simName.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        simDescription.setCellValueFactory(cellData -> cellData.getValue().descriptionProperty());
        simState.setCellValueFactory(cellData -> cellData.getValue().simulationStateProperty());
        simProgress.setCellValueFactory(cellData -> cellData.getValue().progressValueProperty().asObject());
        simStart.setCellValueFactory(cellData -> cellData.getValue().startDateTimeProperty());
        simEnd.setCellValueFactory(cellData -> cellData.getValue().endDateTimeProperty());
        simElapsedTime.setCellValueFactory(cellData -> cellData.getValue().elapsedTimeProperty());
        simAction.setCellFactory(cell -> new SimulationActionButtonCell(simActionPseudoClasses));
        simAction.setCellValueFactory(cellData -> cellData.getValue().simulationStateProperty());

        simulationTable.itemsProperty().bind(new SimpleListProperty<>(viewModel.observableSimulations()));
        simulationTable.setRowFactory(row -> new SimulationTableRow(simStatePseudoClasses));
        simProgress.setCellFactory(new SimulationProgress<>());

        simulationTable.prefHeightProperty().bind(simScrollPane.heightProperty());

        simulationTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        addSimulationButton.disableProperty().bind(viewModel.simulationInsertionServiceRunningProperty());
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

    @FXML
    public void removeSimulation(ActionEvent actionEvent){
        simulationTable.getSelectionModel().getSelectedItems().forEach(sim -> viewModel.removeSimulation(sim.idProperty().get()));
    }
}
