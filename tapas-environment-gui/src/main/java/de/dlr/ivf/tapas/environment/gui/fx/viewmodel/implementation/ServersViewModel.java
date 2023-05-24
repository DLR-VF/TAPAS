package de.dlr.ivf.tapas.environment.gui.fx.viewmodel.implementation;

import de.dlr.ivf.tapas.environment.gui.fx.model.ServersModel;
import javafx.beans.property.*;

public class ServersViewModel {

    private final ServersModel dataModel;

    private IntegerProperty serverCoreCount = new SimpleIntegerProperty();
    private DoubleProperty serverCpuUsage = new SimpleDoubleProperty();
    private StringProperty serverIpAddress = new SimpleStringProperty();
    private StringProperty serverName = new SimpleStringProperty();
    private BooleanProperty serverOnline = new SimpleBooleanProperty();

    public ServersViewModel(ServersModel dataModel){
        this.dataModel = dataModel;
    }
}
