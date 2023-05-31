package de.dlr.ivf.tapas.environment.dao;

import java.io.File;

public interface EnvironmentDao {

    void addSimulation(File parameterFile);
    void removeSimulation(int id);
}
