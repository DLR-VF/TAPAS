package de.dlr.ivf.tapas.environment.gui.fx.model.implementation;

import de.dlr.ivf.tapas.environment.dao.SimulationsDao;
import de.dlr.ivf.tapas.environment.dto.SimulationEntry;
import de.dlr.ivf.tapas.environment.gui.fx.model.SimulationsModel;

import java.util.Collection;

public class SimulationsModelManager implements SimulationsModel {

    private final SimulationsDao dao;

    public SimulationsModelManager(SimulationsDao dao){
        this.dao = dao;
    }
    @Override
    public Collection<SimulationEntry> getSimulations() {
        return dao.load();
    }
}
