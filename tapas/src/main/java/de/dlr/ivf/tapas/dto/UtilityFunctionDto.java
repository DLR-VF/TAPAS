package de.dlr.ivf.tapas.dto;

import de.dlr.ivf.api.io.annotation.Column;

public class UtilityFunctionDto {

    @Column("mode_class")
    private String modeClass;

    @Column("utility_function_class")
    private String utilityFunctionClass;

    @Column("parameters")
    private double[] parameters;

    @Column("key")
    String key;
}
