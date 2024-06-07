package de.dlr.ivf.tapas.server;

import de.dlr.ivf.tapas.server.services.StateMonitor;

public class DaemonShutDownStrategy implements Runnable{

    private final StateMonitor<?> serverStateMonitoringService;
    private final TapasServer tapasServer;


    public DaemonShutDownStrategy(StateMonitor<?> serverStateMonitoringService, TapasServer tapasServer){
        this.serverStateMonitoringService = serverStateMonitoringService;
        this.tapasServer = tapasServer;
    }
    @Override
    public void run() {
        this.serverStateMonitoringService.stop();
        this.tapasServer.stop();
    }
}
