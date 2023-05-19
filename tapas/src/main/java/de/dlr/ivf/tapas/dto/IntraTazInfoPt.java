package de.dlr.ivf.tapas.dto;

import de.dlr.ivf.api.io.Column;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class IntraTazInfoPt {

    @Column("info_taz_id")
    private int tazId;

    @Column("average_speed_pt")
    private double avgSpeedPt;

    @Column("info_name")
    private String name;

    @Column("has_intra_traffic_pt")
    private boolean hasIntraTrafficPt;

    @Column("pt_zone")
    private int ptZone;
}
