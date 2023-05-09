package de.dlr.ivf.tapas;

import de.dlr.ivf.tapas.model.parameter.TPS_ParameterClass;

public class TapasInitializer {

    private final TPS_ParameterClass parameterClass;

    public TapasInitializer(TPS_ParameterClass parameterClass){
        this.parameterClass = parameterClass;
    }

    /**
     * this will be mostly stuff from TPS_DB_IO
     * @return a fully initialized TAPAS instance.
     */
    public Tapas init(){


        return new Tapas();
    }
}
