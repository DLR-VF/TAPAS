package de.dlr.ivf.tapas.environment.gui.fx.view.controllers;

import de.dlr.ivf.tapas.environment.gui.fx.viewmodel.implementation.ServerEntryViewModel;
import de.dlr.ivf.tapas.environment.gui.fx.viewmodel.implementation.ServersViewModel;
import de.dlr.ivf.tapas.environment.model.ServerState;
import javafx.beans.property.*;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.net.URL;
import java.util.ResourceBundle;

public class ServersController implements Initializable {

    @FXML
    TableView<ServerEntryViewModel> serverTable;
    @FXML
    private TableColumn<ServerEntryViewModel, Integer> serverCoreCount;
    @FXML
    private TableColumn<ServerEntryViewModel, Double> serverCpuUsage;
    @FXML
    private TableColumn<ServerEntryViewModel, String> serverIpAddress;
    @FXML
    private TableColumn<ServerEntryViewModel, String> serverName;
    @FXML
    private TableColumn<ServerEntryViewModel, Boolean> serverOnline;
    @FXML
    private TableColumn<ServerEntryViewModel, ServerState> serverState;
    @FXML
    ScrollPane serverScrollPane;

    private final ServersViewModel viewModel;

    public ServersController(ServersViewModel serversViewModel) {
        this.viewModel = serversViewModel;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        this.serverName.setCellValueFactory(serverEntry -> serverEntry.getValue().serverNameProperty());
        this.serverIpAddress.setCellValueFactory(serverEntry -> serverEntry.getValue().serverIpAddressProperty());
        this.serverOnline.setCellValueFactory(serverEntry -> serverEntry.getValue().serverOnlineProperty());
        this.serverCpuUsage.setCellValueFactory(serverEntry -> serverEntry.getValue().serverCpuUsageProperty().asObject());
        this.serverCoreCount.setCellValueFactory(serverEntry -> serverEntry.getValue().serverCoreCountProperty().asObject());
        this.serverState.setCellValueFactory(serverEntry -> serverEntry.getValue().serverStateProperty());

        serverTable.itemsProperty().bind(new SimpleListProperty<>(viewModel.observableServers()));
        serverTable.prefWidthProperty().bind(serverScrollPane.widthProperty());
        serverTable.prefHeightProperty().bind(serverScrollPane.heightProperty());
    }
}
