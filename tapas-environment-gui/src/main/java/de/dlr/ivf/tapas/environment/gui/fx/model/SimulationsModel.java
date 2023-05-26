package de.dlr.ivf.tapas.environment.gui.fx.model;

import de.dlr.ivf.tapas.environment.dto.SimulationEntry;

import java.util.Collection;

/**
 * defines the contract on how the data model can be accessed.
 */
public interface SimulationsModel {


    Collection<SimulationEntry> getSimulations();
}
