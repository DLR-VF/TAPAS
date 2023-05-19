package de.dlr.ivf.tapas.dto;

import de.dlr.ivf.api.io.Column;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class TazFeesAndTollsDto {

    @Column("ft_name")
    private String name;

    @Column("ft_taz_id")
    private int tazId;

    @Column("has_toll_base")
    private boolean hasTollBase;

    @Column("toll_type_base")
    private int tollTypeBase;

    @Column("has_fee_base")
    private boolean hasFeeBase;

    @Column("fee_type_base")
    private int feeTypeBase;

    @Column("has_toll_scen")
    private boolean hasTollScenario;

    @Column("toll_type_scen")
    private int tollTypeScenario;

    @Column("has_fee_scen")
    private boolean hasFeeScenario;

    @Column("fee_type_scen")
    private int feeTypeScenario;

    @Column("has_car_sharing")
    private boolean hasCarSharingScenario;

    @Column("has_car_sharing_base")
    private boolean hasCarSharingBase;

    @Column("is_restricted")
    private boolean isRestricted;

    @Column("is_park_ride")
    private boolean isParkAndRide;

    @Column("car_sharing_capacity")
    private int carSharingCapacity;
}
