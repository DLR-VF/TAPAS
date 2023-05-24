package de.dlr.ivf.tapas.environment.gui.fx.viewmodel;

import de.dlr.ivf.tapas.environment.gui.fx.model.ModelFactory;
import de.dlr.ivf.tapas.environment.gui.fx.viewmodel.implementation.ServersViewModel;
import de.dlr.ivf.tapas.environment.gui.fx.viewmodel.implementation.SimulationsViewModel;

public class ViewModelFactory {

    private SimulationsViewModel simulationsViewModel;
    private ServersViewModel serversViewModel;

    private final ModelFactory modelFactory;

    public ViewModelFactory(ModelFactory modelFactory){
        this.modelFactory = modelFactory;
    }

    public SimulationsViewModel getSimulationEntryViewModel(){
        if(simulationsViewModel == null){
           simulationsViewModel = new SimulationsViewModel(modelFactory.getSimulationEntryModel());
        }
        return simulationsViewModel;
    }

    public ServersViewModel getServerEntryViewModel(){
        if(serversViewModel == null){
            serversViewModel = new ServersViewModel(modelFactory.getServerEntryModel());
        }
        return serversViewModel;
    }
}
