package de.dlr.ivf.tapas.dto;

import de.dlr.ivf.api.io.Column;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class TrafficAnalysisZoneScoreDto {

    @Column("score_taz_id")
    private int tazId;

    @Column("score")
    private double score;

    @Column("score_cat")
    private int scoreCat;

    @Column("score_name")
    private String scoreName;
}
