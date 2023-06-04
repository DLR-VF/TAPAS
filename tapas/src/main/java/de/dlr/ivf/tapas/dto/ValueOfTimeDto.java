package de.dlr.ivf.tapas.dto;

import de.dlr.ivf.api.io.annotation.Column;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class ValueOfTimeDto {

    @Column("name")
    private String name;

    @Column("household_income_class_code")
    private int hhIncomeClassCode;

    @Column("current_episode_activity_code_vot")
    private int currentEpisodeActivityCodeVot;

    @Column("current_mode_code_vot")
    private int currentModeCodeVot;

    @Column("current_distance_class_code_vot")
    private int currentDistanceClassCodeVot;

    @Column("value")
    private double value;
}
