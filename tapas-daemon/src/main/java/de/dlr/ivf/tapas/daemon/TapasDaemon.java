package de.dlr.ivf.tapas.daemon;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.concurrent.locks.Lock;

import de.dlr.ivf.tapas.daemon.task.ServerStateMonitor;
import de.dlr.ivf.tapas.daemon.task.SimulationLaunchMonitor;
import de.dlr.ivf.tapas.environment.TapasEnvironment;
import de.dlr.ivf.tapas.environment.dto.ServerEntry;
import de.dlr.ivf.tapas.environment.model.ServerState;
import lombok.Builder;

@Builder
public class TapasDaemon implements Runnable{

    private final int serverUpdateRate;
    private final int simTablePollingRate;

    private final TapasEnvironment tapasEnvironment;

    private final ServerEntry serverEntry;

    private final ServerStateMonitor serverStateMonitor;

    private final SimulationLaunchMonitor simulationLaunchMonitor;

    private final Logger logger = System.getLogger(TapasDaemon.class.getName());

    @Override
    public void run() {
        logger.log(Level.INFO,"Running");


        while (true) {
            Lock serverStateLock = serverStateMonitor.getLock();
            try {

                serverStateLock.lock();
                while (serverEntry.getServerState() == ServerState.SLEEPING) {
                    serverStateMonitor.getServerStartSignal().await();

                    logger.log(Level.INFO, "Daemon is waiting for new simulation");

                    Lock simulationLaunchLock = simulationLaunchMonitor.getLock();

//                try{
//                    while(simulationLaunchMonitor.)
//                }finally {
//
//                }

                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                serverStateMonitor.getServerSleepingSignal().signalAll();
                serverStateLock.unlock();
            }
            logger.log(Level.INFO, "Done running");
        }
    }
}
