package de.dlr.ivf.tapas.environment.strategy.implementation;

import de.dlr.ivf.tapas.environment.dao.ParametersDao;
import de.dlr.ivf.tapas.environment.dao.SimulationsDao;
import de.dlr.ivf.tapas.environment.strategy.InsertStrategy;

import java.io.File;

public class InsertSimulationStrategy implements InsertStrategy<File> {

    ParametersDao parametersDao;
    SimulationsDao simulationsDao;
    @Override
    public void insert(File configFile) {

    }
}
