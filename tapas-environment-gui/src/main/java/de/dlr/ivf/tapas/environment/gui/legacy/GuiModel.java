package de.dlr.ivf.tapas.environment.gui.legacy;

import de.dlr.ivf.tapas.environment.dto.ServerEntry;
import de.dlr.ivf.tapas.environment.dto.SimulationEntry;

import javax.swing.event.SwingPropertyChangeSupport;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;

public class GuiModel {

    private final Map<String, SimulationEntry> simulationEntryMap = new HashMap<>();

    private final Map<String, ServerEntry> serverEntryMap = new HashMap<>();

    private final SwingPropertyChangeSupport pcSupport = new SwingPropertyChangeSupport(this);


    public void addSimulationEntry(SimulationEntry simulationEntry){
        SimulationEntry oldEntry = simulationEntryMap.get(simulationEntry.getSimKey());

    }

    public void addServeEntry(ServerEntry serverEntry){
        ServerEntry oldEntry = serverEntryMap.get(serverEntry.getServerName());

    }

    public void addPropertyChangeListener(PropertyChangeListener listener){
        pcSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener){
        pcSupport.removePropertyChangeListener(listener);
    }

}
