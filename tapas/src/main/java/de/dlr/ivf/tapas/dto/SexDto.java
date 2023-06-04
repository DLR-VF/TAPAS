package de.dlr.ivf.tapas.dto;

import de.dlr.ivf.api.io.annotation.Column;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class SexDto {

    @Column("name")
    private String name;

    @Column("description")
    private String description;

    @Column("class")
    private String className;

    @Column("name_sex")
    private String nameSex;

    @Column("code_sex")
    private int codeSex;
}
