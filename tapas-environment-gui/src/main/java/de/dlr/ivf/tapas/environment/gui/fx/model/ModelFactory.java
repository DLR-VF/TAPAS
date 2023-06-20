package de.dlr.ivf.tapas.environment.gui.fx.model;

import de.dlr.ivf.tapas.environment.TapasEnvironment;
import de.dlr.ivf.tapas.environment.dao.ServersDao;
import de.dlr.ivf.tapas.environment.dao.SimulationsDao;
import de.dlr.ivf.tapas.environment.gui.fx.model.implementation.ServersModelManager;
import de.dlr.ivf.tapas.environment.gui.fx.model.implementation.SimulationsModelManager;

public class ModelFactory {

    private SimulationsModel simulationsModel;

    private ServersModel serversModel;

    public SimulationsModel getSimulationEntryModel(TapasEnvironment tapasEnvironment){
        if(simulationsModel == null){
            simulationsModel = new SimulationsModelManager(tapasEnvironment);
        }
        return simulationsModel;
    }

    public ServersModel getServerEntryModel(TapasEnvironment tapasEnvironment){
        if(serversModel == null){
            serversModel = new ServersModelManager(tapasEnvironment);
        }
        return serversModel;
    }
}
