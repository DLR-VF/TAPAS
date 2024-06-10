package de.dlr.ivf.tapas.simulation;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;

//@ComponentScan("de.dlr.ivf.tapas.simulation")
public class Simulation implements Stepper{


    SimulationRunner simulationRunner;

    private final System.Logger logger = System.getLogger(Simulation.class.getName());
    public void step(){
        logger.log(System.Logger.Level.INFO,"Simulation step.");
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
