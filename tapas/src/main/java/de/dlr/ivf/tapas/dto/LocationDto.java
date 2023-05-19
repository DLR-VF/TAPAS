package de.dlr.ivf.tapas.dto;

import de.dlr.ivf.api.io.Column;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class LocationDto {

    @Column("loc_id")
    private int locId;

    @Column("loc_blk_id")
    private int blockId;

    @Column("loc_taz_id")
    private int tazId;

    @Column("loc_code")
    private int locCode;

    @Column("loc_enterprise")
    private String locEnterprise;

    @Column("loc_capacity")
    private int locCapacity;

    @Column("loc_has_fix_capacity")
    private boolean hasFixCapacity;

    @Column("loc_group_id")
    private int locGroupId;

    @Column("loc_type")
    private String locType;

    @Column("loc_unit")
    private String locUnit;

    @Column("key")
    private String key;

    @Column("x")
    private double x;

    @Column("y")
    private double y;
}
