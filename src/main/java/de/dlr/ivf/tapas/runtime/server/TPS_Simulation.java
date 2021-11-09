package de.dlr.ivf.tapas.runtime.server;

import de.dlr.ivf.tapas.persistence.db.TPS_DB_Connector;
import de.dlr.ivf.tapas.util.parameters.TPS_ParameterClass;

public class TPS_Simulation {

    private final String sim_key;
    private final TPS_ParameterClass simulation_parameters;

    public TPS_Simulation(String sim_key, TPS_ParameterClass simulation_parameters){

        this.sim_key = sim_key;
        this.simulation_parameters = simulation_parameters;
    }


    public String getSimulationKey() {
        return this.sim_key;
    }

    public TPS_ParameterClass getParameters(){
        return this.simulation_parameters;
    }

}
