package de.dlr.ivf.tapas.daemon.monitors;

import de.dlr.ivf.tapas.environment.dao.SimulationsDao;
import de.dlr.ivf.tapas.environment.dao.exception.DaoReadException;
import de.dlr.ivf.tapas.environment.dto.SimulationEntry;
import de.dlr.ivf.tapas.environment.model.SimulationState;
import lombok.Getter;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Getter
public class SimulationStateMonitor implements Runnable{

    private final Lock lock = new ReentrantLock();
    private final Condition simulationStopSignal = lock.newCondition();
    private final Condition simulationStartSignal = lock.newCondition();
    private final SimulationEntry simulationEntry;
    private final SimulationsDao simulationsDao;

    private final System.Logger logger = System.getLogger(ServerStateMonitor.class.getName());

    public SimulationStateMonitor(SimulationsDao simulationsDao, SimulationEntry simulationEntry){
        this.simulationsDao = simulationsDao;
        this.simulationEntry = simulationEntry;
    }

    @Override
    public void run() {

        //todo implement Thread.interrupted loop
        try {
            simulationsDao.getById(simulationEntry.getId()).ifPresentOrElse(
                    this::updateSimulationEntry,
                    () -> logger.log(System.Logger.Level.WARNING,"Simulation Id {0} not present in the environment.",simulationEntry.getId())
            );

        } catch (DaoReadException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private void updateSimulationEntry(SimulationEntry newSimulationEntry) {
        SimulationState newSimulationState = newSimulationEntry.getSimState();

        //the state has been changed externally
        if(this.simulationEntry.getSimState() != newSimulationState){
            try{
                lock.lock();
                logger.log(System.Logger.Level.INFO,"Simulation state for simulation id: {0} has changed from {1} to {2}",
                        simulationEntry.getId(), simulationEntry.getSimState(), newSimulationEntry.getSimState());

                updateState(simulationEntry, newSimulationState);
                fireStateChanges(newSimulationState);

            }finally {
                lock.unlock();
            }
        }
    }

    private void fireStateChanges(SimulationState newSimulationState) {
        switch (newSimulationState){
            case PAUSED -> simulationStopSignal.signalAll();
            case READY -> simulationStartSignal.signalAll();
        }
    }

    private void updateState(SimulationEntry simulationEntry, SimulationState newSimulationState) {
        simulationEntry.setSimState(newSimulationState);
    }
}
