package de.dlr.ivf.tapas.dto;

import de.dlr.ivf.api.io.Column;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class BlockDto {

    @Column("blk_id")
    private int blockId;

    @Column("blk_taz_id")
    private int tazId;

    @Column("x")
    private double x;

    @Column("y")
    private double y;
}
