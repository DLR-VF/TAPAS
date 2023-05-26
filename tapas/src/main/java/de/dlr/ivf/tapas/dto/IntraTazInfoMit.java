package de.dlr.ivf.tapas.dto;

import de.dlr.ivf.api.io.Column;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class IntraTazInfoMit {

    @Column("info_taz_id")
    private int tazId;

    @Column("beeline_factor_mit")
    private double beelineFactorMit;

    @Column("average_speed_mit")
    private double avgSpeedMit;

    @Column("info_name")
    private String name;

    @Column("has intra_traffic_mit")
    private boolean hasIntraTrafficMit;
}