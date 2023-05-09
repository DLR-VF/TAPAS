package de.dlr.ivf.tapas.dto;

import de.dlr.ivf.api.io.Column;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class ActivityToLocationDto {

    @Column("act_code")
    private int actCode;

    @Column("loc_code")
    private int locCode;

    @Column("loc_capa_percentage")
    private double locCapacityPercentage;

    @Column("key")
    private String key;

}
