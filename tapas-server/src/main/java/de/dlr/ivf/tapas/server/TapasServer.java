package de.dlr.ivf.tapas.server;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import de.dlr.ivf.api.io.connection.ConnectionPool;
import de.dlr.ivf.api.service.Service;
import de.dlr.ivf.tapas.Tapas;
import de.dlr.ivf.tapas.TapasInitializer;
import de.dlr.ivf.tapas.server.services.StateRequestFactory;
import de.dlr.ivf.tapas.server.services.implementation.SimulationRequestService;
import de.dlr.ivf.tapas.server.services.implementation.SimulationStateRequest;
import de.dlr.ivf.tapas.server.services.StateMonitor;

import de.dlr.ivf.tapas.environment.model.Simulation;
import de.dlr.ivf.tapas.environment.model.SimulationState;
import de.dlr.ivf.tapas.util.VirtualThreadFactory;
import lombok.Builder;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The TapasServer starts the simulation request service and will block until a new simulation is available. When a
 * simulation is available, the server will start {@link Tapas} and await on {@link SimulationState} changes.
 * Note:
 * Since the TapasServer blocks during the simulation request until one is available, shutting it down gracefully
 * requires the {@link SimulationRequestService} to provide an end of service object.
 */

@Builder
public class TapasServer implements Service, Runnable {
    private final Logger logger = System.getLogger(TapasServer.class.getName());

    private final ConnectionPool connectionPool;

    /**
     * background service that provides simulations to process
     */
    private final SimulationRequestService simulationRequestService;

    /**
     * rate to poll for simulation state changes
     */
    private final Duration pollingRate;

    /**
     * factory to build state requests for a specific simulation
     */
    private final StateRequestFactory stateRequestFactory;

    /**
     * state monitor retrieving simulation states
     */
    private StateMonitor<SimulationState> simulationStateMonitor;

    private final Lock lock = new ReentrantLock();
    private final Condition tapasFinishedSignal = lock.newCondition();

    /**
     * flag indicating that the service should keep running
     */
    private final AtomicBoolean keepRunning = new AtomicBoolean(false);

    /**
     * flag indicating that the service is finished
     */
    private final AtomicBoolean serviceHasFinished = new AtomicBoolean(true);

    /**
     * the actual background thread
     */
    private Thread serviceThread;

    /**
     * currently running Tapas simulation
     */
    private Tapas tapas;

    @Override
    public void run() {
        logger.log(Level.INFO, "Running...");

        serviceHasFinished.set(false);
        simulationRequestService.start();

        while(keepRunning.get()) {

            try {
                Simulation simulation = simulationRequestService.awaitNewSimulation(); //blocking call

                if(simulation == simulationRequestService.getEndOfServiceObject()) {
                    keepRunning.set(false);
                    continue;
                }

                this.simulationStateMonitor = startSimulationStateMonitor(simulation.getId(), simulation.getSimulationState());

                Runnable runWhenTapasFinished = () -> simulationStateMonitor.signalExternalStateChange(SimulationState.FINISHED);
                logger.log(Level.INFO, "Running simulation: {0}, awaiting state change...",simulation.getIdentifier());
                this.tapas = startTapas(runWhenTapasFinished, simulation);

                while (simulation.getSimulationState() == SimulationState.RUNNING){

                    SimulationState simulationState = simulationStateMonitor.awaitStateChange(); //blocking call

                    switch(simulationState){
                        case PAUSED -> {
                            simulationStateMonitor.stop();
                            tapas.stop();
                            logger.log(Level.INFO, "Simulation: {0} aborted.", simulation.getIdentifier());
                            if(keepRunning.get()) simulationRequestService.start();
                        }
                        case FINISHED -> {
                            simulationStateMonitor.stop();
                            logger.log(Level.INFO, "Simulation: {0} finished.", simulation.getIdentifier());
                            if (keepRunning.get()) simulationRequestService.start();
                        }
                    }
                }


            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        try{
            lock.lock();
            serviceHasFinished.set(true);
            tapasFinishedSignal.signalAll();
        }finally {
            lock.unlock();
        }
        logger.log(Level.INFO, "Shut down!");
    }

    @Override
    public void start() {
        try {
            lock.lock();

            if (keepRunning.compareAndSet(false, true)) {

                this.serviceThread = VirtualThreadFactory.startVirtualThread(this);
            } else {
                if(serviceThread != null && serviceThread.isAlive()) {
                    logger.log(Level.WARNING, "Can't start server because it is already running.");
                }
            }
        }finally {
            lock.unlock();
        }
    }

    @Override
    public void stop(){

        try {
            lock.lock();

            //proceed if the manager has not been stopped before
            if(keepRunning.compareAndSet(true,false)){
                logger.log(Level.INFO, "Shutting down...");
                simulationRequestService.stop();
                if(simulationStateMonitor != null && simulationStateMonitor.isRunning()) {
                    simulationStateMonitor.signalExternalStateChange(SimulationState.PAUSED);
                }

                while (!serviceHasFinished.get()){
                    tapasFinishedSignal.await();
                }
            }else{
                logger.log(Level.WARNING, "Controller already shutting down.");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean isRunning() {
        return !serviceHasFinished.get();
    }

    private StateMonitor<SimulationState> startSimulationStateMonitor(int simulationId, SimulationState simulationState){

        SimulationStateRequest stateRequest = stateRequestFactory.newSimulationStateRequest(simulationId);
        StateMonitor<SimulationState> simulationStateMonitor =
                new StateMonitor<>(stateRequest, "SimulationStateMonitor",pollingRate, simulationState);

        simulationStateMonitor.start();
        return simulationStateMonitor;
    }

    private Tapas startTapas(Runnable runWhenDone, Simulation simulation){
        TapasInitializer tapasInitializer = new TapasInitializer(simulation.getParameters(), connectionPool, runWhenDone);

        Tapas tapas = tapasInitializer.init();

        Thread t = new Thread(tapas);

        t.start();
        return tapas;
    }
}