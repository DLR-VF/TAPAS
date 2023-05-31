package de.dlr.ivf.tapas.environment;

import de.dlr.ivf.tapas.environment.dao.ParametersDao;
import de.dlr.ivf.tapas.environment.dao.ServersDao;
import de.dlr.ivf.tapas.environment.dao.SimulationsDao;
import de.dlr.ivf.tapas.environment.dto.SimulationEntry;
import lombok.Builder;
import lombok.Getter;

import java.io.File;

@Builder
@Getter

//todo think about extracting this to an interface
public class TapasEnvironment {

    private final SimulationsDao simulationsDao;
    private final ServersDao serversDao;
    private final ParametersDao parametersDao;

    public SimulationEntry addSimulation(File simConfig){
        return null;
    }

}
