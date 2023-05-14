package de.dlr.ivf.tapas.dto;


import de.dlr.ivf.api.io.Column;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class BlockNextPtStopDto {

    @Column("next_pt_stop_blk_id")
    private int nextPtStopBlockId;

    @Column("next_pt_stop")
    private double nextPtStopDistance;

    @Column("next_pt_stop_name")
    private String nextPtStopName;
}
