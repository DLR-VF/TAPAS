package de.dlr.ivf.tapas.environment.gui.fx.model;

import de.dlr.ivf.tapas.environment.gui.fx.model.implementation.ServersModelManager;
import de.dlr.ivf.tapas.environment.gui.fx.model.implementation.SimulationsModelManager;

public class ModelFactory {

    private SimulationsModel simulationsModel;

    private ServersModel serversModel;

    public SimulationsModel getSimulationEntryModel(){
        if(simulationsModel == null){
            simulationsModel = new SimulationsModelManager();
        }
        return simulationsModel;
    }

    public ServersModel getServerEntryModel(){
        if(serversModel == null){
            serversModel = new ServersModelManager();
        }
        return serversModel;
    }
}
