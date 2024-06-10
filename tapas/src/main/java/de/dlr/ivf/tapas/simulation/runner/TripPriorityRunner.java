package de.dlr.ivf.tapas.simulation.runner;

import java.lang.System.Logger.Level;
import java.util.Collection;

import de.dlr.ivf.tapas.model.person.TPS_Household;
import de.dlr.ivf.tapas.simulation.SimulationRunner;
import de.dlr.ivf.tapas.simulation.implementation.HouseholdProcessor;

import de.dlr.ivf.tapas.simulation.trafficgeneration.SchemeProvider;
import org.springframework.beans.factory.annotation.Autowired;

public class TripPriorityRunner implements SimulationRunner {
    private final System.Logger logger = System.getLogger(TripPriorityRunner.class.getName());

    private final Collection<TPS_Household> households;
    //private final HouseholdProcessor householdProcessor;
    private final SchemeProvider schemeProvider;

    @Autowired
    public TripPriorityRunner(Collection<TPS_Household> households, SchemeProvider schemeProvider){
        this.households = households;
        this.schemeProvider = schemeProvider;
    }



    @Override
    public void run() {

        logger.log(Level.INFO,"Running TripPriority Simulation...");
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        logger.log(Level.INFO,"Done running TripPriority Simulation...");
    }
}
