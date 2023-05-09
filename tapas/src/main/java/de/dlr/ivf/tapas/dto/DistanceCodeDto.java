package de.dlr.ivf.tapas.dto;

import de.dlr.ivf.api.io.Column;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class DistanceCodeDto {

    @Column("id")
    private int id;

    @Column("description")
    private String description;

    @Column("class")
    private String className;

    @Column("name_vot")
    private String nameVot;

    @Column("code_vot")
    private int codeVot;

    @Column("type_vot")
    private String typeVot;

    @Column("name_mct")
    private String nameMct;

    @Column("code_mct")
    private int codeMct;

    @Column("type_mct")
    private String typeMct;

    @Column("max")
    private int max;
}
