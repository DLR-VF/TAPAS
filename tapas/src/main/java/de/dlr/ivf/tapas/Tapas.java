package de.dlr.ivf.tapas;

import de.dlr.ivf.tapas.model.parameter.TPS_ParameterClass;
import lombok.Builder;

@Builder
public class Tapas {

    private final TPS_ParameterClass parameterClass;

    public Tapas(TPS_ParameterClass parameterClass){
        this.parameterClass = parameterClass;
    }


}
