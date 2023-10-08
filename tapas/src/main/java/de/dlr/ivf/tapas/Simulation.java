package de.dlr.ivf.tapas;


public class Simulation {

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
