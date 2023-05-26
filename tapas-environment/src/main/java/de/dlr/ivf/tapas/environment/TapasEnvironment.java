package de.dlr.ivf.tapas.environment;

import de.dlr.ivf.tapas.environment.dao.ParametersDao;
import de.dlr.ivf.tapas.environment.dao.ServersDao;
import de.dlr.ivf.tapas.environment.dao.SimulationsDao;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class TapasEnvironment {

    private final SimulationsDao simulationsDao;
    private final ServersDao serversDao;
    private final ParametersDao parametersDao;

}
