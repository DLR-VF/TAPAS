package de.dlr.ivf.tapas.environment.gui.tasks;

import de.dlr.ivf.tapas.environment.dto.ServerEntry;
import de.dlr.ivf.tapas.environment.gui.SimulationControl;
import de.dlr.ivf.tapas.environment.model.SimulationServerData;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;

import java.net.UnknownHostException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class SimulationServerDataUpdateTask extends ScheduledService<Void> {

    private final Supplier<Collection<ServerEntry>> serverDataSupplier;
    private final Map<String, ServerEntry> serverEntriesMap;

    private final ObservableList<ServerEntry> serverEntries = FXCollections.observableArrayList();

    public SimulationServerDataUpdateTask(Supplier<Collection<ServerEntry>> serverDataSupplier) {

        this.serverDataSupplier = serverDataSupplier;
        this.serverEntriesMap = new HashMap<>();
    }


    public void run() {
        // Select all available servers from the database
//        SimulationServerData data;
//        if (!SimulationControl.this.dbConnection.checkConnection(this)) return;
//
//        String query = "SELECT * FROM " + SimulationControl.this.dbConnection.getParameters().getString(
//                ParamString.DB_TABLE_SERVERS) + " ORDER BY server_ip";
//
//        try (ResultSet rs = SimulationControl.this.dbConnection.executeQuery(query, this)) {
//
//            while (rs.next()) {
//
//                // receive or create SimulationServerData from database ResultSet
//                String hostname = rs.getString("server_name");
//                if (SimulationControl.this.simulationServerDataMap.containsKey(hostname)) {
//                    data = SimulationControl.this.simulationServerDataMap.get(hostname);
//                    data.update(rs);
//                } else {
//                    data = new SimulationServerData(rs);
//                    SimulationControl.this.simulationServerDataMap.put(hostname, data);
//                }
//
//                SimulationControl.this.gui.updateServerData(data);
//            }
//
//        } catch (UnknownHostException | SQLException e) {
//            e.printStackTrace();
//        }
//
//        query = "SELECT * FROM " + SimulationControl.this.dbConnection.getParameters().getString(
//                ParamString.DB_TABLE_PROCESSES) + " WHERE end_time IS NULL";
//        try (ResultSet rs = SimulationControl.this.dbConnection.executeQuery(query, this)) {
//
//            while (rs.next()) {
//                String hostname = rs.getString("host");
//                if (SimulationControl.this.simulationServerDataMap.containsKey(hostname)) {
//
//                    data = SimulationControl.this.simulationServerDataMap.get(hostname);
//                    data.setServerProcessInfo(rs);
//                    data.setServerState(
//                            rs.getBoolean("shutdown") ? ServerControlState.STOP : ServerControlState.BOOT);
//                }
//            }
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
    }

    @Override
    protected Task<Void> createTask() {
        return new Task<>() {
            @Override
            protected Void call() throws Exception {

                Collection<ServerEntry> entries = serverDataSupplier.get();

                Platform.runLater(() -> {
                    serverEntries.clear();
                    serverEntries.addAll(entries);
                    System.out.println(serverEntries);
                });
                return null;
            }
        };
    }

    public final ObservableList<ServerEntry> getServerEntries(){
        return this.serverEntries;
    }
}
