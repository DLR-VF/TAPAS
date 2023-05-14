package de.dlr.ivf.tapas.dto;

import de.dlr.ivf.api.io.Column;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class PersonDto {

    @Column("p_id")
    private int personId;

    @Column("p_hh_id")
    private int hhId;

    @Column("p_group")
    private int personGroup;

    @Column("p_sex")
    private int sex;

    @Column("p_age")
    private int age;

    @Column("p_age_stba")
    private int ageStba;

    @Column("p_work_id")
    private int workId;

    @Column("p_working")
    private int workingAmount;

    @Column("p_abo")
    private int hasAbo;

    @Column("p_budget_pt")
    private int budgetPt;

    @Column("p_budget_it")
    private int budgetIt;

    @Column("p_budget_it_fi")
    private int budgetItFi;

    @Column("p_key")
    private String key;

    @Column("p_driver_license")
    private int driverLicense;

    @Column("p_has_bike")
    private boolean hasBike;

    @Column("p_education")
    private int education;

    @Column("p_professional")
    private int professional;
}
