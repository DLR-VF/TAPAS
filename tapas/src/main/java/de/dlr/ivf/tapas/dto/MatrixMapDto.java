package de.dlr.ivf.tapas.dto;

import de.dlr.ivf.api.io.annotation.Column;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class MatrixMapDto {

    @Column("matrixMap_name")
    private String matrixMapName;

    @Column("matrixMap_num")
    private int matrixMapNum;

    @Column("matrixMap_matrixNames")
    private String[] matrixNames;

    @Column("matrixMap_distribtuion")
    private double[] matrixMapDistribution;
}
