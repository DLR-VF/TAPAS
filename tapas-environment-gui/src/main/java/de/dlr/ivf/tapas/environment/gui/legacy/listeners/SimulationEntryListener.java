package de.dlr.ivf.tapas.environment.gui.legacy.listeners;

import de.dlr.ivf.tapas.environment.gui.legacy.GuiModel;
import de.dlr.ivf.tapas.environment.gui.legacy.SimulationMonitorOld;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class SimulationEntryListener implements PropertyChangeListener {

    private final SimulationMonitorOld view;
    private final GuiModel model;


    public SimulationEntryListener(SimulationMonitorOld view, GuiModel model){
        this.view = view;
        this.model = model;
    }
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        //view.
    }
}
