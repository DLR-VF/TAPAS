package de.dlr.ivf.tapas.daemon.services.implementation;

import de.dlr.ivf.tapas.daemon.services.Service;
import de.dlr.ivf.tapas.environment.dao.SimulationsDao;
import de.dlr.ivf.tapas.environment.dto.SimulationEntry;
import de.dlr.ivf.tapas.util.VirtualThreadFactory;
import lombok.Getter;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;


import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This service is intended to retrieve available simulations from the backend using a polling mechanism. When starting
 * this service, a new {@link Thread} is created that will request unprocessed simulations.
 * Whenever a simulation is returned from the backend, this service stops itself so no further request calls are made
 * until it is started again.
 * An available simulation is put into a {@link BlockingQueue} that external threads can await on, see
 * {@link  #awaitNewSimulation()}.
 */

public class SimulationRequestService implements Service, Runnable {

    private final Logger logger = System.getLogger(SimulationRequestService.class.getName());

    private final Lock lock = new ReentrantLock();
    private final Condition serviceFinishedCondition = lock.newCondition();
    private final AtomicBoolean serviceHasFinished = new AtomicBoolean(false);
    private final AtomicBoolean keepRunning = new AtomicBoolean(false);
    private final BlockingQueue<Object> availableSimulation = new ArrayBlockingQueue<>(1);

    //todo use a virtual thread with java 21
    private Thread requestThread;

    private final SimulationsDao simulationsDao;
    private final String serverIdentifier;
    private final Duration pollingRate;

    @Getter
    private final Object endOfServiceObject = new Object();

    public SimulationRequestService(SimulationsDao simulationsDao, String serverIdentifier, Duration pollingRate){
        this.simulationsDao = simulationsDao;
        this.serverIdentifier = serverIdentifier;
        this.pollingRate = pollingRate;
    }

    @Override
    public void run() {

        keepRunning.set(true);
        serviceHasFinished.set(false);

        logger.log(Level.INFO, "Running...");
        logger.log(Level.INFO, "Waiting for new simulation...");

        while(keepRunning.get()) {
           Optional<SimulationEntry> potentialSimulation = simulationsDao.requestSimulation(serverIdentifier);

            if (potentialSimulation.isPresent()) {
                var simulation = potentialSimulation.get();
                if(this.availableSimulation.offer(simulation)){
                    logger.log(Level.INFO,"Simulation: {0} available.",simulation.getSimKey());
                }else{
                    logger.log(Level.WARNING, "Simulation request retrieved an available simulation even though a" +
                            " simulation was still in queue.");
                }
                keepRunning.set(false);
            }else {
                try {
                    //noinspection BusyWait
                    Thread.sleep(pollingRate.toMillis());
                } catch (InterruptedException e) {
                    throw new RuntimeException("Uncaught", e);
                }
            }
        }

        try{
            lock.lock();
            serviceHasFinished.set(true);
            serviceFinishedCondition.signalAll();
        }finally {
            lock.unlock();
        }
        logger.log(Level.INFO, "Shut down!");

    }

    /**
     * Starts a new background thread that periodically polls a backend for new simulations. The background thread can't
     * be started twice without being stopped before.
     */
    @Override
    public void start(){
        try {
            lock.lock();

            if (keepRunning.compareAndSet(false, true)) {
                this.requestThread = VirtualThreadFactory.startVirtualThread(this);
            } else {
                if(requestThread != null && requestThread.isAlive()) {
                    logger.log(Level.WARNING, "Can't start request service, service already running.");
                }
            }
        }finally {
            lock.unlock();
        }
    }

    /**
     * Stopping the service will set a flag to exit the loop in the {@link #run()} method and will wait for the service
     * to finish until a timeout hits. The timeout value is equal to the polling rate. Synchronization is accomplished
     * by a lock.
     */
    @Override
    public void stop(){
        try {
            lock.lock();

            if(keepRunning.compareAndSet(true,false)) {
                logger.log(Level.INFO, "Shutting down...");

                while (!serviceHasFinished.get()) {
                    this.serviceFinishedCondition.await();
                }
                availableSimulation.put(endOfServiceObject);
            }
        }catch (InterruptedException ignored){
            //nothing to do
        }finally {
            lock.unlock();
        }
    }

    public Object awaitNewSimulation() throws InterruptedException {
        return availableSimulation.take();
    }
}
