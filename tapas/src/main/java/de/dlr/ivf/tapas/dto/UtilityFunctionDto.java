package de.dlr.ivf.tapas.dto;

import de.dlr.ivf.api.io.annotation.Column;
import lombok.Getter;

@Getter
public class UtilityFunctionDto {

    @Column("mode_class")
    private String mode;

    @Column("utility_function_class")
    private String utilityFunctionClass;

    @Column("parameters")
    private double[] parameters;

    @Column("key")
    String key;
}
