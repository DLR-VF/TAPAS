package de.dlr.ivf.tapas.dto;

import de.dlr.ivf.api.io.annotation.Column;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * This class maps to the tapas 'core.global_mode_codes' table
 *
 * @author Alain Schengen
 */

@NoArgsConstructor
@Getter
public class ModeDto {

    @Column("name")
    String name;

    @Column("description")
    String description;

    @Column("class")
    String className;

    @Column("name_mct")
    String nameMct;

    @Column("code_mct")
    int codeMct;

    @Column("type_mct")
    String typeMct;

    @Column("name_vot")
    String nameVot;

    @Column("code_vot")
    int codeVot;

    @Column("type_vot")
    String typeVot;

    @Column("isfix")
    boolean isFix;


}
