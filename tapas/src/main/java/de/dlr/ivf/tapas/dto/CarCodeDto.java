package de.dlr.ivf.tapas.dto;

import de.dlr.ivf.api.io.Column;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class CarCodeDto {

    @Column("name")
    private String name;

    @Column("description")
    private String description;

    @Column("class")
    private String className;

    @Column("name_cars")
    String nameCars;

    @Column("code_cars")
    int codeCars;
}
