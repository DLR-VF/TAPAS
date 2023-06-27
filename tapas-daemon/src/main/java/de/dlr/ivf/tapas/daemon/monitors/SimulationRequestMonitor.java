package de.dlr.ivf.tapas.daemon.monitors;

import de.dlr.ivf.tapas.environment.dao.SimulationsDao;
import de.dlr.ivf.tapas.environment.dto.SimulationEntry;
import lombok.Builder;
import lombok.Getter;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Getter
@Builder
public class SimulationRequestMonitor implements Runnable {
    private final Lock lock = new ReentrantLock();
    private final Condition simulationStartSignal = lock.newCondition();


    private final SimulationsDao simulationsDao;
    private final String serverIp;
    private final BlockingQueue<SimulationEntry> simulationsToRun;

    public SimulationRequestMonitor(SimulationsDao simulationsDao, String serverIp, BlockingQueue<SimulationEntry> simulationsToRun) {

        this.simulationsDao = simulationsDao;
        this.serverIp = serverIp;
        this.simulationsToRun = simulationsToRun;
    }

    @Override
    public void run() {
        SimulationEntry simulation = simulationsDao.requestSimulation(serverIp);
        if(simulation != null){
            lock.lock();
            try {
                simulationsToRun.put(simulation);
                simulationStartSignal.signalAll();
            } catch (InterruptedException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }finally {
                lock.unlock();
            }
        }
    }
}
