package de.dlr.ivf.tapas.dto;

import de.dlr.ivf.api.io.annotation.Column;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class LocationCodeDto {

    @Column("id")
    private int id;

    @Column("description")
    private String description;

    @Column("class")
    private String className;

    @Column("name_general")
    private String nameGeneral;

    @Column("code_general")
    private int codeGeneral;

    @Column("type_general")
    private String typeGeneral;

    @Column("name_tapas")
    private String nameTapas;

    @Column("code_tapas")
    private int codeTapas;

    @Column("type_tapas")
    private String typeTapas;
}
