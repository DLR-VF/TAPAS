package de.dlr.ivf.tapas.environment.gui.tasks;

import de.dlr.ivf.tapas.environment.dto.ServerEntry;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ServerDataUpdateTask extends ScheduledService<Void> {

    private final Supplier<Collection<ServerEntry>> serverDataSupplier;
    private final Map<String, ServerEntry> serverEntriesMap;

    private final ObservableList<ServerEntry> serverEntries = FXCollections.observableArrayList();

    public ServerDataUpdateTask(Supplier<Collection<ServerEntry>> serverDataSupplier) {

        this.serverDataSupplier = serverDataSupplier;
        this.serverEntriesMap = new HashMap<>();
    }

    @Override
    protected Task<Void> createTask() {
        return new Task<>() {
            @Override
            protected Void call() throws Exception {
                Collection<ServerEntry> entries = serverDataSupplier.get();
                Map<String, ServerEntry> dbServerEntries = entries.stream().collect(Collectors.toMap(
                        e -> e.getServerName(),
                        e -> e
                ));

//                Collection<ServerEntry> updatedEntries = new ArrayList<>();
                for(ServerEntry dbServerEntry : entries) {
                    ServerEntry oldEntry = serverEntriesMap.put(dbServerEntry.getServerName(),dbServerEntry);
//
                    if(oldEntry == null){
                        Platform.runLater(() -> serverEntries.add(dbServerEntry));
                    }
//
//                   // serverEntriesMap.computeIfPresent(newEntry.getServerName(), (name, oldEntry) -> update(newEntry, oldEntry));
                }
                Platform.runLater(() -> {
                    serverEntries.forEach(entry -> entry.setServerUsage(dbServerEntries.get(entry.getServerName()).getServerUsage()));
                });
               // Platform.runLater(() -> serverEntries.replaceAll( entry -> serverEntriesMap.get(entry.getServerName())));


                    //serverEntries.clear();
                   // serverEntries.addAll(entries);
                    System.out.println(serverEntries);

                return null;
            }
        };
    }

    private boolean update(ServerEntry oldEntry, ServerEntry newEntry) {

        //update cpu usage
        oldEntry.setServerUsage(newEntry.getServerUsage());
        oldEntry.setServerOnline(newEntry.isServerOnline());
        return true;
    }

    public final ObservableList<ServerEntry> getServerEntries(){
        return this.serverEntries;
    }
}
