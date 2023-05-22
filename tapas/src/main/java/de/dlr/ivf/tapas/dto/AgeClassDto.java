package de.dlr.ivf.tapas.dto;

import de.dlr.ivf.api.io.Column;
import lombok.Getter;

@Getter
public class AgeClassDto {

    @Column("id")
    private int id;

    @Column("description")
    private String description;

    @Column("class")
    private String className;

    @Column("name_stba")
    private String nameStba;

    @Column("code_stba")
    private int codeStba;

    @Column("type_stba")
    private String typeStba;

    @Column("name_persgroup")
    private String namePersgroup;

    @Column("code_persgroup")
    private int codePersgroup;

    @Column("type_persgroup")
    private String typePersGroup;

    @Column("min")
    private int min;

    @Column("max")
    private int max;
}