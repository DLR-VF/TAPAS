package de.dlr.ivf.tapas.runtime.server;

public interface TPS_Simulator {

    void run(int num_threads);
    void shutdown();

    boolean isRunningSimulation();
}
