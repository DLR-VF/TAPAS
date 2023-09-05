package de.dlr.ivf.tapas.daemon;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.concurrent.locks.Lock;

import de.dlr.ivf.tapas.daemon.managers.TapasManager;
import de.dlr.ivf.tapas.daemon.monitors.ServerStateMonitor;
import de.dlr.ivf.tapas.environment.dao.SimulationsDao;
import de.dlr.ivf.tapas.environment.model.ServerState;
import lombok.Builder;

/**
 * The TAPAS daemon is responsible for handling TAPAS server state changes. The daemon runs in an infinite loop and does
 * not observe state of the server. Instead, the {@link ServerStateMonitor} exposes lock conditions that the daemon can await on.
 * In its current form, a server can only have one of two states, 'run' or 'stop'.
 * In 'stop' state the daemon will await a change to 'start' state.
 * In 'start' state the daemon will start a {@link TapasManager} in a separate thread. Switching the daemon back to 'stop'
 * state will stop the {@link TapasManager} and the daemon will await on a finished signal from the manager.
 */
@Builder
public class TapasDaemon implements Runnable{

    private final ServerStateMonitor serverStateMonitor;

    private final SimulationsDao simulationsDao;

    private final String serverIdentifier;

    private final Logger logger = System.getLogger(TapasDaemon.class.getName());

    @Override
    public void run() {

        logger.log(Level.INFO,"Running");

        TapasManager tapasManager = TapasManager.builder()
                .simulationsDao(simulationsDao)
                .serverIdentifier(serverIdentifier)
                .build();

        Lock serverStateLock = serverStateMonitor.getLock();
        Lock tapasManagerLock = tapasManager.getLock();

        //noinspection InfiniteLoopStatement
        while (true) {

            try {
                serverStateLock.lock();
                while (serverStateMonitor.getServerState() == ServerState.STOP) {
                    logger.log(Level.INFO, "Waiting for server start signal...");

                    serverStateMonitor.getServerStartSignal().await();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            } finally {
                serverStateLock.unlock();
            }

            try{
                serverStateLock.lock();

                //check if the run state still holds after acquiring the lock
                if(serverStateMonitor.getServerState() == ServerState.RUN) {
                    Thread t = new Thread(tapasManager);
                    t.start();
                }

                while(serverStateMonitor.getServerState() == ServerState.RUN){

                    //this will block the daemon until it receives a stop signal
                    serverStateMonitor.getServerStopSignal().await();
                }

                tapasManagerLock.lock();
                if(tapasManager.getKeepRunning().get()) {
                    tapasManager.stop();
                }

                while(tapasManager.getKeepRunning().get()){
                    tapasManager.getTapasFinishedSignal().await();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            } finally {
                serverStateLock.unlock();
                tapasManagerLock.unlock();
            }
            logger.log(Level.INFO, "Done running");
        }
    }
}
