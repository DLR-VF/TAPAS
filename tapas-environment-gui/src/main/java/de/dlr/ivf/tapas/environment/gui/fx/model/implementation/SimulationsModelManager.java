package de.dlr.ivf.tapas.environment.gui.fx.model.implementation;

import de.dlr.ivf.tapas.environment.dao.SimulationsDao;
import de.dlr.ivf.tapas.environment.dto.SimulationEntry;
import de.dlr.ivf.tapas.environment.gui.fx.model.SimulationsModel;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class SimulationsModelManager implements SimulationsModel {

    private final SimulationsDao dao;
    private final Map<Integer, SimulationEntry> simulationEntries;

    public SimulationsModelManager(SimulationsDao dao){
        this.simulationEntries = new HashMap<>();
        this.dao = dao;
    }
    @Override
    public Collection<SimulationEntry> getSimulations() {
        return dao.load();
    }
}
