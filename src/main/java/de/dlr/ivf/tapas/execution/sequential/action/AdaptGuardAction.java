package de.dlr.ivf.tapas.execution.sequential.action;

import de.dlr.ivf.tapas.execution.sequential.guard.Guard;
import de.dlr.ivf.tapas.scheme.TPS_Stay;

public class AdaptGuardAction implements TPS_PlanStateAction{

    private final Guard guard;
    private final int delta_time;

    public AdaptGuardAction(Guard activity_to_trip_guard, int delta_time) {
        this.guard = activity_to_trip_guard;
        this.delta_time = delta_time;
    }

    @Override
    public void run() {

        this.guard.setValueToTest(this.guard.getValueToTest()+delta_time);

    }
}
