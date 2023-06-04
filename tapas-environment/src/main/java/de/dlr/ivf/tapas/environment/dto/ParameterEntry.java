package de.dlr.ivf.tapas.environment.dto;

import de.dlr.ivf.api.io.annotation.Column;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ParameterEntry {

    @Column("sim_key")
    private String simKey;

    @Column("param_key")
    private String paramKey;

    @Column("param_value")
    private String paramValue;

    @Column("sim_id")
    private int simId;
}
