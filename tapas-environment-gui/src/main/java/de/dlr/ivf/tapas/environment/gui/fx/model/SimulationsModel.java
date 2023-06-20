package de.dlr.ivf.tapas.environment.gui.fx.model;

import de.dlr.ivf.tapas.environment.dto.SimulationEntry;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

/**
 * defines the contract on how the data model can be accessed.
 */
public interface SimulationsModel {


    Collection<SimulationEntry> getSimulations();

    SimulationEntry getSimulation(int simId);

    Collection<SimulationEntry> reload() throws IOException;

    void insert(File parameterFile) throws IOException;

    void remove(Collection<SimulationEntry> simulationsToRemove) throws IOException;
}
