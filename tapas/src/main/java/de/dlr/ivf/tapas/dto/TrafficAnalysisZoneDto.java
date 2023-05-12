package de.dlr.ivf.tapas.dto;

import de.dlr.ivf.api.io.Column;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class TrafficAnalysisZoneDto {

    @Column("taz_id")
    private int tazId;

    @Column("taz_bbr_type")
    private int bbrType;

    @Column("taz_statistical_area")
    private int statisticalArea;

    @Column("taz_num_id")
    private int numId;

    @Column("x")
    private double x;

    @Column("y")
    private double y;
}
