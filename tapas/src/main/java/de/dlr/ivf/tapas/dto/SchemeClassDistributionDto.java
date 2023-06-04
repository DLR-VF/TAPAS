package de.dlr.ivf.tapas.dto;

import de.dlr.ivf.api.io.annotation.Column;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class SchemeClassDistributionDto {

    @Column("name")
    private String name;

    @Column("scheme_class_id")
    private int schemeClassId;

    @Column("person_group")
    private int personGroup;

    @Column("probability")
    private double probability;

    @Column("key")
    private String key;
}
