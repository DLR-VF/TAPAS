package de.dlr.ivf.tapas.daemon;

import de.dlr.ivf.tapas.daemon.configuration.DaemonConfiguration;
import lombok.Builder;

@Builder
public class TapasDaemon implements Runnable{

    private final int serverUpdateRate;
    private final int simTablePollingRate;

    private final DaemonConfiguration daemonConfiguration;

    @Override
    public void run() {

    }
}
