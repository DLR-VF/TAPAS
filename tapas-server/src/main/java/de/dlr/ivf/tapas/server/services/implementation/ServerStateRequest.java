package de.dlr.ivf.tapas.server.services.implementation;

import de.dlr.ivf.tapas.environment.TapasEnvironment;
import de.dlr.ivf.tapas.environment.model.ServerState;

import java.util.Optional;
import java.util.concurrent.Callable;

public class ServerStateRequest implements Callable<Optional<ServerState>> {

    private final TapasEnvironment tapasEnvironment;
    private final String serverIdentifier;

    public ServerStateRequest(TapasEnvironment tapasEnvironment, String serverIdentifier){
        this.tapasEnvironment = tapasEnvironment;
        this.serverIdentifier = serverIdentifier;
    }
    @Override
    public Optional<ServerState> call() throws Exception {
        return tapasEnvironment.serverState(serverIdentifier);
    }
}
