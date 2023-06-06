package de.dlr.ivf.tapas.environment.gui.fx.viewmodel.implementation;

import de.dlr.ivf.tapas.environment.dto.ServerEntry;
import de.dlr.ivf.tapas.environment.gui.fx.model.ServersModel;
import de.dlr.ivf.tapas.environment.gui.services.ServerDataUpdateService;
import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class ServersViewModel {

    private final ServersModel dataModel;
    private final ServerDataUpdateService updateTask;

    private final ObservableList<ServerEntryViewModel> servers;


    public ServersViewModel(ServersModel dataModel, ServerDataUpdateService updateTask){
        this.dataModel = dataModel;
        this.updateTask = updateTask;

        //Note: an extractor has been specified in order to fire property changes when nested properties change.
        this.servers = FXCollections.observableArrayList(serverRow -> new Observable[]{ serverRow.serverOnlineProperty(),
                serverRow.serverCpuUsageProperty()
        });

        updateTask.setOnSucceeded( e -> {
            if(this.updateTask.getValue() != null)
                this.updateTask.getValue().forEach(this::updateServerEntry);
        });
    }

    public ObservableList<ServerEntryViewModel> observableServers() {
        return this.servers;
    }

    private void updateServerEntry(ServerEntry entry){
        var viewEntry = servers.stream()
                .filter(server -> server.serverIpAddressProperty().getValue() != null)
                .filter(server -> server.serverIpAddressProperty().getValue().equals(entry.getServerIp()))
                .findFirst();

        if(viewEntry.isPresent()){
            var server = viewEntry.get();
            updateServerEntryViewModel(server,entry);
        }else {
            var server = new ServerEntryViewModel();
            updateServerEntryViewModel(server, entry);
            servers.add(server);
        }
    }

    private void updateServerEntryViewModel(ServerEntryViewModel server, ServerEntry entry) {
        server.serverOnlineProperty().set(entry.isServerOnline());
        server.serverCpuUsageProperty().set(entry.getServerUsage());
        server.serverIpAddressProperty().set(entry.getServerIp());
        server.serverNameProperty().set(entry.getServerName());
        server.serverCoreCountProperty().set(entry.getServerCores());
        server.serverStateProperty().set(entry.getServerState());
    }
}
