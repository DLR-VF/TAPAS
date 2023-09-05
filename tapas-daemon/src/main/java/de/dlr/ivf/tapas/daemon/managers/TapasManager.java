package de.dlr.ivf.tapas.daemon.managers;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import de.dlr.ivf.tapas.daemon.monitors.SimulationRequestTask;
import de.dlr.ivf.tapas.daemon.monitors.SimulationStateMonitor;
import de.dlr.ivf.tapas.environment.dao.SimulationsDao;
import de.dlr.ivf.tapas.environment.dto.SimulationEntry;
import lombok.Builder;
import lombok.Getter;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The TapasManager runs in the background and waits for new simulations to process. While not being stopped, the manager
 * will await
 * background task polls the backend for new simulations.
 */

@Builder
@Getter
public class TapasManager implements Runnable{

    private final SimulationsDao simulationsDao;
    private final String serverIdentifier;

    private final Lock lock = new ReentrantLock();
    private final Condition tapasFinishedSignal = lock.newCondition();

    private final AtomicBoolean keepRunning = new AtomicBoolean(true);
    private final Logger logger = System.getLogger(TapasManager.class.getName());

    @Override
    public void run() {

        SimulationRequestTask simulationRequestTask = SimulationRequestTask.builder()
                .serverIdentifier(serverIdentifier)
                .simulationsDao(simulationsDao)
                .pollingIntervalSeconds(5)
                .build();

        while(keepRunning.get()) {

            logger.log(Level.INFO, "Waiting for new simulation...");
            simulationRequestTask.start();

            try {
                //this will block until a simulation from the simulation request background task is available.
                SimulationEntry simulationEntry = simulationRequestTask.getAvailableSimulation();

                if (simulationEntry != null) {
                    SimulationStateMonitor simulationStateMonitor = new SimulationStateMonitor(simulationsDao, simulationEntry);
                    SimulationManager simulationManager = new SimulationManager(simulationStateMonitor, simulationEntry);

                    Thread t = new Thread(simulationManager);
                    logger.log(Level.INFO,"Running simulation {0}", simulationEntry.getSimKey());
                    t.start();

                    ScheduledFuture<?> simulationState = scheduleService(scheduledExecutorService, simulationStateMonitor);

                    //this will block until a simulation has stopped.
                    simulationStateMonitor.getSimulationStopSignal().await();
                    simulationState.cancel(true);
                }else{
                    logger.log(Level.ERROR,"Simulation request task signaled for an available simulation but none was present.");
                }


            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private ScheduledFuture<?> scheduleService(ScheduledExecutorService simulationRequestService, Runnable simulationRequestTask) {
        return simulationRequestService.scheduleAtFixedRate(simulationRequestTask, 1, 5, TimeUnit.SECONDS);
    }

    public void stop(){

        //proceed if the manager has not been stopped before
        if(keepRunning.compareAndSet(true,false)){

        }
    }
}
