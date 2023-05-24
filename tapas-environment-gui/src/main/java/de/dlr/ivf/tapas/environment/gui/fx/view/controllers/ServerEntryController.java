package de.dlr.ivf.tapas.environment.gui.fx.view.controllers;

import de.dlr.ivf.tapas.environment.dto.ServerEntry;
import de.dlr.ivf.tapas.environment.gui.fx.viewmodel.implementation.ServersViewModel;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.net.URL;
import java.util.ResourceBundle;

public class ServerEntryController implements Initializable {

    @FXML
    TableView<ServerEntry> serverTable;
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

    private ServersViewModel viewModel;

    public ServerEntryController(ServersViewModel serversViewModel) {
        this.viewModel = serversViewModel;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        this.serverName.setCellValueFactory(serverEntry -> new SimpleStringProperty(serverEntry.getValue().getServerName()));
        this.serverIpAddress.setCellValueFactory(serverEntry -> new SimpleStringProperty(serverEntry.getValue().getServerIp()));
        this.serverOnline.setCellValueFactory(serverEntry -> new SimpleBooleanProperty(serverEntry.getValue().isServerOnline()));
        this.serverCpuUsage.setCellValueFactory(serverEntry -> new SimpleDoubleProperty(serverEntry.getValue().getServerUsage()).asObject());
        this.serverCoreCount.setCellValueFactory(serverEntry -> new SimpleIntegerProperty(serverEntry.getValue().getServerCores()).asObject());

    }
}
