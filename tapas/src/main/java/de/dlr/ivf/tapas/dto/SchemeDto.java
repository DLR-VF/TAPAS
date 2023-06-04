package de.dlr.ivf.tapas.dto;

import de.dlr.ivf.api.io.annotation.Column;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class SchemeDto {

    @Column("scheme_id")
    private int id;

    @Column("scheme_class_id")
    private int schemeClassId;

    @Column("homework")
    private boolean isHomework;

    @Column("key")
    private String key;
}
