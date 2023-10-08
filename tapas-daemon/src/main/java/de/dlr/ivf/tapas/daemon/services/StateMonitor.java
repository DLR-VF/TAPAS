package de.dlr.ivf.tapas.daemon.services;


import de.dlr.ivf.tapas.environment.dao.exception.DaoReadException;
import de.dlr.ivf.tapas.util.VirtualThreadFactory;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class StateMonitor<T> implements Service, Runnable {

    private final Logger logger = System.getLogger(StateMonitor.class.getName());
    private final Lock lock = new ReentrantLock();
    private final Condition serviceFinishedSignal = lock.newCondition();
    private final BlockingQueue<T> stateBlockingQueue;

    private final AtomicBoolean serviceFinished = new AtomicBoolean(false);
    private final AtomicBoolean keepRunning = new AtomicBoolean(true);

    private final Duration pollingRate;
    private final String monitorName;

    private final Callable<Optional<T>> stateRequest;

    private Thread requestThread;

    private T currentState;

    public StateMonitor(Callable<Optional<T>> stateRequest, String monitorName, Duration pollingRate, T startState){

        this.stateRequest = stateRequest;
        this.monitorName = monitorName;
        this.pollingRate = pollingRate;
        this.currentState = startState;
        this.stateBlockingQueue = new ArrayBlockingQueue<>(1, true);
    }

    @Override
    public void start() {
        try {
            lock.lock();
            if(requestThread != null && requestThread.isAlive()){
                logger.log(Level.ERROR, "{0} - a state monitor can not be restarted.", monitorName);
            }else{
                stateBlockingQueue.put(currentState);
                this.requestThread = VirtualThreadFactory.startVirtualThread(this);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void stop() {
        logger.log(Level.INFO, "{0} - Shutting down...", monitorName);
        try {
            lock.lock();
            keepRunning.set(false);
            while (!serviceFinished.get()) {
                serviceFinishedSignal.await();  //blocking call
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void run() {

        serviceFinished.set(false);
        logger.log(Level.INFO, "{0} - Running...", monitorName);

        while(keepRunning.get()) {
            try {
                stateRequest.call().ifPresentOrElse(
                        this::handleState,
                        () -> logger.log(Level.WARNING, "{0} - state request didn't yield a result.", monitorName)
                );

                pauseFor(pollingRate);

            } catch (DaoReadException e) {
                logger.log(Level.WARNING, "{0} - unable to read state from the backend.", monitorName);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        try{
            lock.lock();
            serviceFinished.set(true);
            serviceFinishedSignal.signalAll();
        } finally {
            lock.unlock();
        }
    }

    private void pauseFor(Duration pollingRate) throws InterruptedException {
        Thread.sleep(pollingRate.toMillis());
    }

    private void handleState(T newState){
        if(newState != currentState){
            logger.log(Level.INFO,"{0} - state has changed from {1} to {2}",
                    monitorName, currentState, newState);

            if(!this.stateBlockingQueue.offer(newState)){
                logger.log(Level.WARNING, "{0} - state change could not be added to queue.", monitorName);
            }else{
                this.currentState = newState;
            }
        }
    }

    public void signalExternalStateChange(T newState){
        try {
            stateBlockingQueue.put(newState);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public T awaitStateChange() throws InterruptedException {
        return stateBlockingQueue.take();
    }
}
