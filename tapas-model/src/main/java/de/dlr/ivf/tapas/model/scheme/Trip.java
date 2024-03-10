package de.dlr.ivf.tapas.model.scheme;

import de.dlr.ivf.tapas.model.constants.TPS_ActivityConstant;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class Trip {

    private final TPS_ActivityConstant fromActivity;
    private final TPS_ActivityConstant toActivity;

    private final int startTime;
    private final int duration;
}
