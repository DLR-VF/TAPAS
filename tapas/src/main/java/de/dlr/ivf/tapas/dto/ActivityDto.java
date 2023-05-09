package de.dlr.ivf.tapas.dto;

import de.dlr.ivf.api.io.Column;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ActivityDto {

    @Column("id")
    private int id;

    @Column("description")
    private String description;

    @Column("class")
    private String className;

    @Column("name_zbe")
    private String nameZbe;

    @Column("code_zbe")
    private int codeZbe;

    @Column("type_zbe")
    private String typeZbe;

    @Column("name_vot")
    private String nameVot;

    @Column("code_vot")
    private int codeVot;

    @Column("type_vot")
    private String typeVot;

    @Column("name_tapas")
    private String nameTapas;

    @Column("code_tapas")
    private int codeTapas;

    @Column("type_tapas")
    private String typeTapas;

    @Column("name_mct")
    private String nameMct;

    @Column("code_mct")
    private int codeMct;

    @Column("type_mct")
    private String typeMct;

    @Column("name_priority")
    private String namePriority;

    @Column("code_priority")
    private int codePriority;

    @Column("type_priority")
    private String typePriority;

    @Column("istrip")
    private boolean isTrip;

    @Column("isfix")
    private boolean isFix;

    @Column("attribute")
    private String attribute;
}
