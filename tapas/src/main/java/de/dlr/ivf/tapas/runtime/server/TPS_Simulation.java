package de.dlr.ivf.tapas.runtime.server;

import de.dlr.ivf.tapas.persistence.db.TPS_DB_Connector;

public class TPS_Simulation {

    private final String sim_key;
    private final TPS_DB_Connector dbConnector;
    private boolean initialized = false;

    public TPS_Simulation(String sim_key, TPS_DB_Connector dbConnector){

        this.sim_key = sim_key;
        this.dbConnector = dbConnector;

    }

    public TPS_InitializedSimulation initialize(){

        dbConnector.readRuntimeParametersFromDB(sim_key);

        return new TPS_InitializedSimulation(this.sim_key, dbConnector.getParameters());
    }


    public String getSimulationKey() {
        return this.sim_key;
    }

    public TPS_DB_Connector getDbConnector(){
        return this.dbConnector;
    }
}
