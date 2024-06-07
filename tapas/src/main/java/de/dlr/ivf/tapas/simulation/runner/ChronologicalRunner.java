package de.dlr.ivf.tapas.simulation.runner;

import java.lang.System.Logger.Level;

import de.dlr.ivf.tapas.simulation.SimulationRunner;

public class ChronologicalRunner implements SimulationRunner {
    private final System.Logger logger = System.getLogger(ChronologicalRunner.class.getName());

    @Override
    public void run() {

        logger.log(Level.INFO,"Running chronological Simulation...");
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        logger.log(Level.INFO,"Done running chronological Simulation...");
    }
}
