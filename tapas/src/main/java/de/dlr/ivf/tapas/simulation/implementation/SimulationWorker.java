package de.dlr.ivf.tapas.simulation.implementation;

import de.dlr.ivf.tapas.simulation.Processor;
import lombok.Builder;

import java.util.Queue;
import java.util.concurrent.CountDownLatch;

@Builder
public class SimulationWorker<S> implements Runnable{

    private final Queue<S> householdsToProcess;
    private boolean keepRunning = true;
    private final Processor<S,?> processor;
    private final CountDownLatch countDownLatch;
    @Override
    public void run() {
        while(keepRunning){
            S hh = householdsToProcess.poll();

            if(hh == null){
                keepRunning = false;
            } else {
                var result = processor.process(hh);
            }

        }

        countDownLatch.countDown();
    }
}
