package de.dlr.ivf.tapas.runtime.server;

import de.dlr.ivf.tapas.log.TPS_Logger;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface.SeverenceLogLevel;
import de.dlr.ivf.tapas.persistence.TPS_PersistenceManager;
import de.dlr.ivf.tapas.person.TPS_Worker;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class HierarchicalSimulator implements TPS_Simulator{

    private final TPS_PersistenceManager pm;
    private List<TPS_Worker> workers;
    private ExecutorService es;

    public HierarchicalSimulator(TPS_PersistenceManager pm){
        this.pm = pm;
    }

    @Override
    public void run(int num_threads) {

        try {

            this.workers = new ArrayList<>(num_threads);

            // start worker threads
            if (TPS_Logger.isLogging(SeverenceLogLevel.INFO)) {
                TPS_Logger.log(SeverenceLogLevel.INFO, "Initialize " + num_threads + " working threads ...");
            }


            this.es = Executors.newFixedThreadPool(num_threads);
            List<Future<Exception>> col = new LinkedList<>();
            for (int activeThreads = 0; activeThreads < num_threads; activeThreads++) {

                TPS_Worker worker = new TPS_Worker(this.pm,"TPS_Worker #"+activeThreads);

                this.workers.add(worker);
                col.add(es.submit(worker));
            }

            // wait for worker threads
            if (TPS_Logger.isLogging(SeverenceLogLevel.INFO)) {
                TPS_Logger.log(SeverenceLogLevel.INFO, "... wait for all working threads ...");
            }

            for (Future<Exception> future : col) {
                future.get();
            }
            es.shutdown();

        } catch (Exception e) {

            TPS_Logger.log(SeverenceLogLevel.FATAL, "Application shutdown: unhandable exception", e);
        }
    }

    @Override
    public void shutdown() {
        TPS_Logger.log(getClass(),SeverenceLogLevel.INFO,"Stopping worker threads...");
        this.workers.forEach(TPS_Worker::finish);
    }

    @Override
    public boolean isRunningSimulation() {
        return !this.es.isTerminated();
    }
}
