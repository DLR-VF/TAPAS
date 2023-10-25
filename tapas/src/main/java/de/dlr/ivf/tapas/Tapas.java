package de.dlr.ivf.tapas;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import de.dlr.ivf.tapas.simulation.Simulator;
import de.dlr.ivf.tapas.simulation.implementation.SimulationWorker;
import lombok.Builder;

import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


@Builder
public class Tapas implements Runnable{

    private final Logger logger = System.getLogger(Tapas.class.getName());

    private final Lock lock = new ReentrantLock(true);
    private final Condition tapasFinishedCondition = lock.newCondition();

    private final Runnable onFinish;
    private final AtomicBoolean tapasFinished = new AtomicBoolean();

    private final Collection<SimulationWorker<?>> simulationWorkers;
    private final Simulator<?,?> simulator;
    private final CountDownLatch countDownLatch;

    public void stop(){
        try{
            logger.log(Level.INFO, "Shutting down...");
            lock.lock();
            while (!tapasFinished.get()){
                tapasFinishedCondition.await();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }finally {
            lock.unlock();
        }
    }

    @Override
    public void run() {
        tapasFinished.set(false);

        simulationWorkers.stream()
                .map(Thread::new)
                .forEach(Thread::start);

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        try{
            lock.lock();
            onFinish.run();
            tapasFinished.set(true);
            logger.log(Level.INFO, "Tapas finished.");
            tapasFinishedCondition.signalAll();
        }finally {
            lock.unlock();
        }

    }
}
