package de.dlr.ivf.tapas.simulation;

public interface SimulationRunner extends Runnable{
    @Override
    default void run() {
        System.out.println("simulation is running");
    }
}
