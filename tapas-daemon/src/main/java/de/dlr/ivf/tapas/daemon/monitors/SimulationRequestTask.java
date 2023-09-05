package de.dlr.ivf.tapas.daemon.monitors;

import de.dlr.ivf.tapas.daemon.TapasDaemon;
import de.dlr.ivf.tapas.environment.dao.SimulationsDao;
import de.dlr.ivf.tapas.environment.dto.SimulationEntry;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import lombok.Builder;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This service is intended to retrieve available simulations from the backend using a polling mechanism. When starting
 * this service, a new {@link Thread} is created that will request unprocessed simulations.
 * Whenever a simulation is returned from the backend, this service stops itself so no further request calls are made
 * until it is started again.
 * An available simulation is put into a {@link BlockingQueue} that external threads can await on by calling
 * {@link  #getAvailableSimulation()}.
 */


//todo wrap the service/thread into this class
@Builder
public class SimulationRequestTask implements Runnable {

    private final SimulationsDao simulationsDao;
    private final String serverIdentifier;
    private final BlockingQueue<SimulationEntry> availableSimulation = new ArrayBlockingQueue<>(1);
    private final Logger logger = System.getLogger(TapasDaemon.class.getName());
    private final AtomicBoolean keepRunning = new AtomicBoolean(true);
    private final long pollingIntervalSeconds;

    private Thread requestThread;

    @Override
    public void run() {

        if(keepRunning.get()) {
            SimulationEntry simulation = simulationsDao.requestSimulation(serverIdentifier);
            if (simulation != null) {
                keepRunning.set(false);
                if(this.availableSimulation.offer(simulation)){
                    logger.log(Level.INFO,"Simulation : {0} available.",simulation.getSimKey());
                }else{
                    logger.log(Level.WARNING, "Simulation request retrieved an available simulation even though an" +
                            " available simulation was still in queue.");
                }
            }
        }else {
            try {
                Thread.sleep(pollingIntervalSeconds * 1000);
            } catch (InterruptedException e) {
                logger.log(Level.WARNING,"Simulation request thread has been interrupted.");
            }
        }
    }

    public void start(){
        if(keepRunning.compareAndSet(false, true)){
            //todo check if thread alive?
            this.requestThread = new Thread(this);
            this.requestThread.start();
        }else{
            logger.log(Level.WARNING,"Can't start request service, service already running.");
        }

    }

    public void stop(){
        this.keepRunning.set(false);
    }


    public SimulationEntry getAvailableSimulation() throws InterruptedException {
        return availableSimulation.take();
    }
}
