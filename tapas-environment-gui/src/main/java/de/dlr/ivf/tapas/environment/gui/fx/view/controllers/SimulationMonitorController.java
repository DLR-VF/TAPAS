package de.dlr.ivf.tapas.environment.gui.fx.view.controllers;

import de.dlr.ivf.tapas.environment.dto.ServerEntry;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.Initializable;
import java.net.URL;
import java.util.ResourceBundle;

public class SimulationMonitorController implements Initializable {


    private ObservableList<ServerEntry> serverEntries = FXCollections.observableArrayList();


    public SimulationMonitorController(){

    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }
}
