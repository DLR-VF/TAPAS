package de.dlr.ivf.tapas.model.mode;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ModeParameters {

    private final double velocity;
    private final double costPerKm;
    private final double costPerKmBase;
    private final double variableCostPerKm;
    private final double variableCostPerKmBase;
    private final boolean useBase;
    private final double beelineFactor;
}
