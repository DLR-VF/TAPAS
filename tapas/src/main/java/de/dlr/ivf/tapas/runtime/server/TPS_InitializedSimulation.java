package de.dlr.ivf.tapas.runtime.server;

import de.dlr.ivf.tapas.model.parameter.TPS_ParameterClass;

public class TPS_InitializedSimulation {

    private final String sim_key;
    private final TPS_ParameterClass parameters;

    public TPS_InitializedSimulation(String sim_key, TPS_ParameterClass parameters){

        this.sim_key = sim_key;
        this.parameters = parameters;
    }

    public TPS_ParameterClass getParameters() {
        return this.parameters;
    }
}
