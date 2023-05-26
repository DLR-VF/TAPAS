package de.dlr.ivf.tapas.environment.gui.fx.viewmodel.implementation;

import de.dlr.ivf.tapas.environment.gui.fx.model.ServersModel;
import de.dlr.ivf.tapas.environment.gui.tasks.ServerDataUpdateTask;
import javafx.beans.property.*;

public class ServersViewModel {

    private final ServersModel dataModel;
    private final ServerDataUpdateTask updateTask;


    public ServersViewModel(ServersModel dataModel, ServerDataUpdateTask updateTask){
        this.dataModel = dataModel;
        this.updateTask = updateTask;
    }
}
