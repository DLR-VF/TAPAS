package de.dlr.ivf.tapas.environment.gui.fx.viewmodel;

import de.dlr.ivf.tapas.environment.TapasEnvironment;
import de.dlr.ivf.tapas.environment.gui.fx.model.ModelFactory;
import de.dlr.ivf.tapas.environment.gui.fx.viewmodel.implementation.ServersViewModel;
import de.dlr.ivf.tapas.environment.gui.fx.viewmodel.implementation.SimulationsViewModel;
import de.dlr.ivf.tapas.environment.gui.services.ServerDataUpdateService;
import de.dlr.ivf.tapas.environment.gui.services.SimulationDataUpdateService;

public class ViewModelFactory {

    private SimulationsViewModel simulationsViewModel;
    private ServersViewModel serversViewModel;

    private final ModelFactory modelFactory;

    public ViewModelFactory(ModelFactory modelFactory){
        this.modelFactory = modelFactory;
    }

    public SimulationsViewModel getSimulationEntryViewModel(SimulationDataUpdateService updateTask, TapasEnvironment tapasEnvironment){
        if(simulationsViewModel == null){
           simulationsViewModel = new SimulationsViewModel(modelFactory.getSimulationEntryModel(tapasEnvironment), updateTask);
        }
        return simulationsViewModel;
    }

    public ServersViewModel getServerEntryViewModel(ServerDataUpdateService updateTask, TapasEnvironment tapasEnvironment){
        if(serversViewModel == null){
            serversViewModel = new ServersViewModel(modelFactory.getServerEntryModel(tapasEnvironment), updateTask);
        }
        return serversViewModel;
    }
}
