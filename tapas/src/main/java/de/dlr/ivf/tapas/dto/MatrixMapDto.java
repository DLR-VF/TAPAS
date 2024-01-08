package de.dlr.ivf.tapas.dto;

import de.dlr.ivf.api.io.annotation.Column;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class MatrixMapDto {

    @Column("matrixmap_name")
    private String matrixMapName;

    @Column("matrixmap_num")
    private int matrixMapNum;

    @Column("matrixmap_matrixnames")
    private String[] matrixNames;

    @Column("matrixmap_distribution")
    private double[] matrixMapDistribution;
}
