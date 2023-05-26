package de.dlr.ivf.tapas.environment.gui.fx.viewmodel.implementation;

import javafx.beans.property.*;

public class ServerEntryViewModel {

    private final IntegerProperty serverCoreCount = new SimpleIntegerProperty();
    private final DoubleProperty serverCpuUsage = new SimpleDoubleProperty();
    private final StringProperty serverIpAddress = new SimpleStringProperty();
    private final StringProperty serverName = new SimpleStringProperty();
    private final BooleanProperty serverOnline = new SimpleBooleanProperty();


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

    public BooleanProperty serverOnlineProperty() {
        return serverOnline;
    }


}
