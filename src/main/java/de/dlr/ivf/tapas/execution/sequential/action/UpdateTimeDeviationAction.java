package de.dlr.ivf.tapas.execution.sequential.action;

import de.dlr.ivf.tapas.execution.sequential.context.PlanContext;
import de.dlr.ivf.tapas.execution.sequential.context.TourContext;
import de.dlr.ivf.tapas.plan.TPS_PlannedTrip;
import de.dlr.ivf.tapas.scheme.TPS_Trip;

public class UpdateTimeDeviationAction implements TPS_PlanStateAction{


    private final TPS_Trip next_trip;
    private final TPS_PlannedTrip next_planned_trip;
    private final PlanContext plan_context;

    public UpdateTimeDeviationAction(TPS_Trip next_trip, TPS_PlannedTrip next_planned_trip, PlanContext plan_context) {
        this.next_trip = next_trip;
        this.next_planned_trip= next_planned_trip;
        this.plan_context = plan_context;

    }

    @Override
    public void run() {

        int delta_traveltime = next_planned_trip.getDuration() - next_trip.getOriginalDuration();
        this.plan_context.updateTimeDeviation(delta_traveltime);

    }
}
