package de.dlr.ivf.tapas.dto;

import de.dlr.ivf.api.io.annotation.Column;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class SettlementSystemDto {

    @Column("id")
    private int id;

    @Column("description")
    private String description;

    @Column("name_fordcp")
    private String nameFordcp;

    @Column("code_fordcp")
    private int codeFordcp;

    @Column("type_fordcp")
    private String typeFordcp;

    @Column("name_tapas")
    private  String nameTapas;

    @Column("code_tapas")
    private int codeTapas;

    @Column("type_tapas")
    private String typeTapas;
}
