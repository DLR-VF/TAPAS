package de.dlr.ivf.tapas.simulation.runner;

import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import de.dlr.ivf.tapas.model.person.TPS_Household;
import de.dlr.ivf.tapas.simulation.SimulationRunner;
import de.dlr.ivf.tapas.simulation.implementation.HouseholdProcessor;

import de.dlr.ivf.tapas.simulation.implementation.SimulationWorker;
import de.dlr.ivf.tapas.simulation.trafficgeneration.SchemeProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Lazy
@Component("tripPriorityRunner")
public class TripPriorityRunner implements SimulationRunner {
    private final System.Logger logger = System.getLogger(TripPriorityRunner.class.getName());

    private final Queue<TPS_Household> householdsToProcess;
    private final HouseholdProcessor householdProcessor;
    private final SchemeProvider schemeProvider;
    private final int workerCount;

    @Autowired
    public TripPriorityRunner(Collection<TPS_Household> householdsToProcess, SchemeProvider schemeProvider,
                              @Qualifier("workerCount") int workerCount, HouseholdProcessor householdProcessor){
        this.householdsToProcess = new ConcurrentLinkedDeque<>(householdsToProcess);
        this.schemeProvider = schemeProvider;
        this.workerCount = workerCount;
        this.householdProcessor = householdProcessor;

    }



    @Override
    public void run() {

        CountDownLatch countDownLatch = new CountDownLatch(workerCount);

        Collection<SimulationWorker<?>> simulationWorkers = IntStream.range(0,workerCount)
                .mapToObj(i -> SimulationWorker.<TPS_Household>builder()
                        .name("Worker #" + i)
                        .countDownLatch(countDownLatch)
                        .processor(householdProcessor)
                        .keepRunning(true)
                        .householdsToProcess(householdsToProcess)
                        .build())
                .collect(Collectors.toCollection(ArrayList::new));

        logger.log(Level.INFO,"Running TripPriority Simulation...");
        simulationWorkers.stream()
                .map(worker -> new Thread(worker, worker.getName()))
                .forEach(Thread::start);
        try {
            countDownLatch.await(); //blocking call
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        logger.log(Level.INFO,"Done running TripPriority Simulation...");
    }
}
