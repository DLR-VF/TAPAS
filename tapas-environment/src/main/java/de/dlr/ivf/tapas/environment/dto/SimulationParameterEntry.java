package de.dlr.ivf.tapas.environment.dto;

import de.dlr.ivf.api.io.Column;
import lombok.Getter;

@Getter
public class SimulationParameterEntry {

    @Column("sim_key")
    private String simKey;

    @Column("param_key")
    private String paramKey;

    @Column("param_value")
    private String paramValue;
}
