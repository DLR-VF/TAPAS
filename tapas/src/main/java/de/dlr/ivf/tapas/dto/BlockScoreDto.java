package de.dlr.ivf.tapas.dto;

import de.dlr.ivf.api.io.annotation.Column;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class BlockScoreDto {

    @Column("score_blk_id")
    private int blockId;

    @Column("score")
    private double score;

    @Column("score_cat")
    private int scoreCat;

    @Column("score_name")
    private String scoreName;
}
