package de.dlr.ivf.tapas.dto;

import de.dlr.ivf.api.io.annotation.Column;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class SchemeClassDto {

    @Column("scheme_class_id")
    private int id;

    @Column("avg_travel_time")
    private double avgTravelTime;

    @Column("proz_std_dev")
    private double procStdDev;

    @Column("key")
    private String key;
}
