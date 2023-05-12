package de.dlr.ivf.tapas.dto;

import de.dlr.ivf.api.io.Column;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class CfnFourDto {

    @Column("current_episode_activity_code_tapas")
    private int currentEpisodeActivityCodeTapas;

    @Column("current_taz_settlement_code_tapas")
    private int currentTazSettlementCodeTapas;

    @Column("value")
    private double value;

    @Column("comment")
    private String comment;

    @Column("raumtyp")
    private String raumtyp;

    @Column("key")
    private String key;
}
