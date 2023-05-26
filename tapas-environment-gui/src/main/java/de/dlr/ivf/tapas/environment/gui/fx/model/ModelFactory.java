package de.dlr.ivf.tapas.environment.gui.fx.model;

import de.dlr.ivf.tapas.environment.dao.ServersDao;
import de.dlr.ivf.tapas.environment.dao.SimulationsDao;
import de.dlr.ivf.tapas.environment.gui.fx.model.implementation.ServersModelManager;
import de.dlr.ivf.tapas.environment.gui.fx.model.implementation.SimulationsModelManager;

public class ModelFactory {

    private SimulationsModel simulationsModel;

    private ServersModel serversModel;

    public SimulationsModel getSimulationEntryModel(SimulationsDao dao){
        if(simulationsModel == null){
            simulationsModel = new SimulationsModelManager(dao);
        }
        return simulationsModel;
    }

    public ServersModel getServerEntryModel(ServersDao dao){
        if(serversModel == null){
            serversModel = new ServersModelManager(dao);
        }
        return serversModel;
    }
}
