package de.dlr.ivf.tapas.dto;

import de.dlr.ivf.api.io.Column;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class IntMatrixDto {

    @Column("matrix_name")
    String name;

    @Column("matrix_values")
    int[] matrix;
}
