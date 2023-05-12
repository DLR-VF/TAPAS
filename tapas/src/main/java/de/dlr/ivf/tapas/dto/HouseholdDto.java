package de.dlr.ivf.tapas.dto;

import de.dlr.ivf.api.io.Column;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class HouseholdDto {

    @Column("hh_id")
    private int hhId;

    @Column("hh_type")
    private int hhType;

    @Column("hh_persons")
    private int hhPersons;

    @Column("hh_cars")
    private int hhCars;

    @Column("hh_car_ids")
    private int[] carIds;

    @Column("hh_income")
    private double hhIncome;

    @Column("hh_taaz_id")
    private int tazId;

    @Column("hh_key")
    private String key;

    @Column("hh_has_child")
    private boolean hasChild;

    @Column("block")
    private int blockId;

    @Column("x")
    private double xCoordinate;

    @Column("y")
    private double yCoordinate;
}
