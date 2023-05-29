package de.dlr.ivf.tapas.environment.gui.fx.viewmodel.implementation;

import de.dlr.ivf.tapas.environment.model.ServerState;
import javafx.beans.property.*;

public class ServerEntryViewModel {

    private final IntegerProperty serverCoreCount = new SimpleIntegerProperty();
    private final DoubleProperty serverCpuUsage = new SimpleDoubleProperty();
    private final StringProperty serverIpAddress = new SimpleStringProperty();
    private final StringProperty serverName = new SimpleStringProperty();
    private final BooleanProperty serverOnline = new SimpleBooleanProperty();
    private final ObjectProperty<ServerState> serverState = new SimpleObjectProperty<>();


    public IntegerProperty serverCoreCountProperty() {
        return serverCoreCount;
    }

    public DoubleProperty serverCpuUsageProperty() {
        return serverCpuUsage;
    }

    public StringProperty serverIpAddressProperty() {
        return serverIpAddress;
    }

    public StringProperty serverNameProperty() {
        return serverName;
    }

    public ObjectProperty<ServerState> serverStateProperty(){
        return serverState;
    }

    public BooleanProperty serverOnlineProperty() {
        return serverOnline;
    }


}
