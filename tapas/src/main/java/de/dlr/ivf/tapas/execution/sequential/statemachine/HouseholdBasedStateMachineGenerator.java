package de.dlr.ivf.tapas.execution.sequential.statemachine;

import de.dlr.ivf.tapas.execution.sequential.io.HouseholdBasedPlanGenerator;
import de.dlr.ivf.tapas.persistence.TPS_PersistenceManager;
import de.dlr.ivf.tapas.persistence.db.TPS_TripWriter;

public class HouseholdBasedStateMachineGenerator {

    TPS_PersistenceManager pm;
    TPS_TripWriter writer;

    public HouseholdBasedStateMachineGenerator(TPS_TripWriter  writer, TPS_PersistenceManager pm, HouseholdBasedPlanGenerator plan_generator){
        this.writer = writer;
        this.pm = pm;

    }
}
