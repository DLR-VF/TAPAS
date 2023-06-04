package de.dlr.ivf.tapas.dto;

import de.dlr.ivf.api.io.annotation.Column;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class CfnxDto {

    @Column("current_taz_settlement_code_tapas")
    private int currentTazSettlementCodeTapas;

    @Column("value")
    private double value;

    @Column("key")
    String key;
}
