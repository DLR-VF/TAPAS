package de.dlr.ivf.tapas.runtime.server;

import de.dlr.ivf.tapas.choice.FeasibilityCalculator;
import de.dlr.ivf.tapas.choice.LocationAndModeChooser;
import de.dlr.ivf.tapas.logger.legacy.TPS_Logger;
import de.dlr.ivf.tapas.logger.legacy.SeverityLogLevel;
import de.dlr.ivf.tapas.persistence.TPS_PersistenceManager;
import de.dlr.ivf.tapas.person.TPS_Worker;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class HierarchicalSimulator implements TPS_Simulator {

    private final TPS_PersistenceManager pm;
    private final LocationAndModeChooser locationAndModeChooser;
    private final FeasibilityCalculator feasibilityCalculator;
    private List<TPS_Worker> workers;
    private ExecutorService es;

    public HierarchicalSimulator(TPS_PersistenceManager pm, LocationAndModeChooser locationAndModeChooser, FeasibilityCalculator feasibilityCalculator){
        this.pm = pm;
        this.locationAndModeChooser = locationAndModeChooser;
        this.feasibilityCalculator = feasibilityCalculator;
    }

    @Override
    public void run(int num_threads) {

        try {

            this.workers = new ArrayList<>(num_threads);

            // start worker threads
            if (TPS_Logger.isLogging(SeverityLogLevel.INFO)) {
                TPS_Logger.log(SeverityLogLevel.INFO, "Initialize " + num_threads + " working threads ...");
            }


            this.es = Executors.newFixedThreadPool(num_threads);
            List<Future<Exception>> col = new LinkedList<>();

            for (int activeThreads = 0; activeThreads < num_threads; activeThreads++) {

                TPS_Worker worker = new TPS_Worker(this.pm,"TPS_Worker #"+activeThreads, this.locationAndModeChooser,this.feasibilityCalculator);

                this.workers.add(worker);
                col.add(es.submit(worker));
            }

            // wait for worker threads
            if (TPS_Logger.isLogging(SeverityLogLevel.INFO)) {
                TPS_Logger.log(SeverityLogLevel.INFO, "... wait for all working threads ...");
            }

            for (Future<Exception> future : col) {
                future.get();
            }
            es.shutdown();

        } catch (Exception e) {

            TPS_Logger.log(SeverityLogLevel.FATAL, "Application shutdown: unhandable exception", e);
        }
    }

    @Override
    public void shutdown() {
        TPS_Logger.log(getClass(),SeverityLogLevel.INFO,"Stopping worker threads...");
        this.workers.forEach(TPS_Worker::finish);
    }

    @Override
    public boolean isRunningSimulation() {
        return !this.es.isTerminated();
    }
}
