package de.dlr.ivf.tapas;

import de.dlr.ivf.tapas.model.parameter.TPS_ParameterClass;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import lombok.Builder;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


@Builder
public class Tapas implements Runnable{

    private final Logger logger = System.getLogger(Tapas.class.getName());

    private final Lock lock = new ReentrantLock(true);
    private final Condition tapasFinishedCondition = lock.newCondition();

    private final TPS_ParameterClass parameterClass;
    private final Runnable onFinish;
    private final AtomicBoolean keepRunning  = new AtomicBoolean();
    private final AtomicBoolean tapasFinished = new AtomicBoolean();

    private Tapas(TPS_ParameterClass parameterClass, Runnable onFinish){
        this.parameterClass = parameterClass;
        this.onFinish = onFinish;
    }

    public static Tapas init(TPS_ParameterClass parameterClass, Runnable onFinish){

        return new Tapas(parameterClass, onFinish);
    }

    public void stop(){
        try{
            logger.log(Level.INFO, "Shutting down...");
            lock.lock();
            keepRunning.set(false);
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
        keepRunning.set(true);
        tapasFinished.set(false);
        Simulation simulation = new Simulation();

        int cnt = 0;
        while(keepRunning.get() && cnt++ <= 4){
            simulation.step();
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
