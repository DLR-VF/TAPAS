package de.dlr.ivf.tapas.dto;

import de.dlr.ivf.api.io.Column;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class CarDto {

    @Column("car_id")
    private int id;

    @Column("kba_no")
    private int kbaNo;

    @Column("engine_type")
    private int engineType;

    @Column("is_company_car")
    private boolean isCompanyCar;

    @Column("car_key")
    private String carKey;

    @Column("emission_type")
    private int emissionType;

    @Column("restriction")
    private boolean restriction;

    @Column("fix_costs")
    private double fixCosts;

    @Column("automation_level")
    private int automationLevel;

    @Column("vtype_id")
    private String vTypeId;

}
