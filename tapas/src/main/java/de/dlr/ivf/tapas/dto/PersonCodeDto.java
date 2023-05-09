package de.dlr.ivf.tapas.dto;

import de.dlr.ivf.api.io.Column;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class PersonCodeDto {

    @Column("description")
    private String description;

    @Column("code")
    private int code;

    @Column("code_ageclass")
    private int codeAgeClass;

    @Column("code_sex")
    private int codeSex;

    @Column("code_cars")
    private int codeCars;

    @Column("person_type")
    private String personType;

    @Column("key")
    private String key;

    @Column("has_child")
    private String hasChild;

    @Column("min_age")
    private int minAge;

    @Column("max_age")
    private int maxAge;

    @Column("work_status")
    private String workStatus;
}
