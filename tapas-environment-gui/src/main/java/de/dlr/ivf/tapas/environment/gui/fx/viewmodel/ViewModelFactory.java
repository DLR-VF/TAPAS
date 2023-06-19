package de.dlr.ivf.tapas.environment.gui.fx.viewmodel;

import de.dlr.ivf.tapas.environment.dao.ServersDao;
import de.dlr.ivf.tapas.environment.dao.SimulationsDao;
import de.dlr.ivf.tapas.environment.gui.fx.model.ModelFactory;
import de.dlr.ivf.tapas.environment.gui.fx.viewmodel.implementation.ServersViewModel;
import de.dlr.ivf.tapas.environment.gui.fx.viewmodel.implementation.SimulationsViewModel;
import de.dlr.ivf.tapas.environment.gui.services.ServerDataUpdateService;
import de.dlr.ivf.tapas.environment.gui.services.SimulationDataUpdateService;
import de.dlr.ivf.tapas.environment.gui.services.SimulationInsertionService;
import de.dlr.ivf.tapas.environment.gui.services.SimulationRemovalService;

public class ViewModelFactory {

    private SimulationsViewModel simulationsViewModel;
    private ServersViewModel serversViewModel;

    private final ModelFactory modelFactory;

    public ViewModelFactory(ModelFactory modelFactory){
        this.modelFactory = modelFactory;
    }

    public SimulationsViewModel getSimulationEntryViewModel(SimulationDataUpdateService updateTask, SimulationsDao dao,
                                                            SimulationInsertionService insertionService, SimulationRemovalService simulationRemovalService){
        if(simulationsViewModel == null){
           simulationsViewModel = new SimulationsViewModel(modelFactory.getSimulationEntryModel(dao), updateTask, insertionService, simulationRemovalService);
        }
        return simulationsViewModel;
    }

    public ServersViewModel getServerEntryViewModel(ServerDataUpdateService updateTask, ServersDao dao){
        if(serversViewModel == null){
            serversViewModel = new ServersViewModel(modelFactory.getServerEntryModel(dao), updateTask);
        }
        return serversViewModel;
    }
}
