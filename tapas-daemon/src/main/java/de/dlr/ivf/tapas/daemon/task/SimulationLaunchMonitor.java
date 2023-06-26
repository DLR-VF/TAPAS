package de.dlr.ivf.tapas.daemon.task;

import de.dlr.ivf.tapas.environment.dao.SimulationsDao;
import lombok.Builder;
import lombok.Getter;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Getter
@Builder
public class SimulationLaunchMonitor implements Runnable{
    private final Lock lock = new ReentrantLock();
    private final Condition simulationStartSignal = lock.newCondition();
    private final Condition simulationStopSignal = lock.newCondition();

    private final SimulationsDao simulationsDao;
    private final String serverIp;

    public SimulationLaunchMonitor(SimulationsDao simulationsDao, String serverIp) {

        this.simulationsDao = simulationsDao;
        this.serverIp = serverIp;
    }

    @Override
    public void run() {

    }
}
