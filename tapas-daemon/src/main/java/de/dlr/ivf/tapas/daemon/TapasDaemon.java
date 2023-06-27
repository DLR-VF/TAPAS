package de.dlr.ivf.tapas.daemon;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.locks.Lock;

import de.dlr.ivf.tapas.daemon.monitors.ServerStateMonitor;
import de.dlr.ivf.tapas.daemon.monitors.SimulationRequestMonitor;
import de.dlr.ivf.tapas.environment.TapasEnvironment;
import de.dlr.ivf.tapas.environment.dto.ServerEntry;
import de.dlr.ivf.tapas.environment.dto.SimulationEntry;
import de.dlr.ivf.tapas.environment.model.ServerState;
import lombok.Builder;

@Builder
public class TapasDaemon implements Runnable{

    private final int serverUpdateRate;
    private final int simTablePollingRate;

    private final TapasEnvironment tapasEnvironment;

    private final ServerEntry serverEntry;

    private final ServerStateMonitor serverStateMonitor;

    private final SimulationRequestMonitor simulationRequestMonitor;

    private final BlockingQueue<SimulationEntry> simulationsToRun;

    private final Logger logger = System.getLogger(TapasDaemon.class.getName());

    @Override
    public void run() {

        logger.log(Level.INFO,"Running");


        while (true) {

            Lock serverStateLock = serverStateMonitor.getLock();
            try {
                serverStateLock.lock();
                while (serverEntry.getServerState() == ServerState.SLEEPING) {
                    logger.log(Level.INFO, "Daemon is in sleep mode.");

                    serverStateMonitor.getServerStartSignal().await();
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                serverStateMonitor.getServerSleepingSignal().signalAll();
                serverStateLock.unlock();
            }

            try{
                serverStateLock.lock();
                while(serverEntry.getServerState() == ServerState.WAITING){
                    logger.log(Level.INFO, "Daemon is waiting for new simulation");
                    Lock simulationRequestLock = simulationRequestMonitor.getLock();

                    try{
                        simulationRequestLock.lock();
                        while(simulationsToRun.isEmpty()){

                            simulationRequestMonitor.getSimulationStartSignal().await();
                        }
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    } finally {
                        simulationRequestLock.unlock();
                    }

                    SimulationEntry simulationEntry = simulationsToRun.take();
                    //serverEntry.setServerState(ServerState.RUNNING);
                    //todo update the environment with new server state

                    logger.log(Level.INFO, "Running simulation with id: "+simulationEntry.getId());
                    try{
                        simulationRequestLock.lock();

                        logger.log(Level.INFO, "Long running task.");
                        Thread.sleep(30000);
                    }finally {
                        simulationRequestLock.unlock();
                    }
                    logger.log(Level.INFO, "Done running simulation with id: "+simulationEntry.getId());
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                serverStateLock.unlock();
            }




            logger.log(Level.INFO, "Done running");
        }
    }
}
