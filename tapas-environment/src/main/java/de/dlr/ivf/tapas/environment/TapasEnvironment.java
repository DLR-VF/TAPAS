package de.dlr.ivf.tapas.environment;

import de.dlr.ivf.tapas.environment.configuration.EnvironmentConfiguration;
import de.dlr.ivf.tapas.environment.dao.ParametersDao;
import de.dlr.ivf.tapas.environment.dao.ServersDao;
import de.dlr.ivf.tapas.environment.dao.SimulationsDao;
import lombok.Builder;

@Builder
public class TapasEnvironment {

    private final SimulationsDao simulationsDao;
    private final ServersDao serversDao;
    private final ParametersDao parametersDao;

    public void update(){

    }

    public void persist(){}



    public static TapasEnvironment fromConfiguration(EnvironmentConfiguration environmentConfiguration){

       // Collection<ServerEntry> serverEntries =
        return null;
    }
}
