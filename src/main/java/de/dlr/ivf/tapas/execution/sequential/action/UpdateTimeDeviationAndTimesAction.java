package de.dlr.ivf.tapas.execution.sequential.action;

import de.dlr.ivf.tapas.execution.sequential.context.PlanContext;
import de.dlr.ivf.tapas.plan.TPS_LocatedStay;
import de.dlr.ivf.tapas.plan.TPS_PlannedTrip;
import de.dlr.ivf.tapas.scheme.TPS_Trip;

public class UpdateTimeDeviationAndTimesAction implements TPS_PlanStateAction{


    private final TPS_Trip next_trip;
    private final TPS_PlannedTrip next_planned_trip;
    private final PlanContext plan_context;
    private final TPS_LocatedStay next_located_stay;

    public UpdateTimeDeviationAndTimesAction(TPS_Trip next_trip, TPS_PlannedTrip next_planned_trip, PlanContext plan_context, TPS_LocatedStay next_located_stay) {
        this.next_trip = next_trip;
        this.next_planned_trip= next_planned_trip;
        this.plan_context = plan_context;
        this.next_located_stay = next_located_stay;

    }

    @Override
    public void run() {


        int cumulative_delta_tt = this.plan_context.getTimeDeviation();

        //the start time of the trip will be the current cumulative time offset towards the original plan plus the original start time of the trip
        int next_start_time = cumulative_delta_tt + next_planned_trip.getStart();
        next_planned_trip.setStart(next_start_time);

        //determine time difference between the planned trip duration and the trip duration according to plan
        int delta_traveltime = next_planned_trip.getDuration() - next_trip.getOriginalDuration();

        //add the time difference to the cumulative time offset
        this.plan_context.updateTimeDeviation(delta_traveltime);

        //Note: set the next activity start after updating the cumulative time offset with the travel time of the prior trip
        next_located_stay.setStart(next_located_stay.getStart() + this.plan_context.getTimeDeviation());

    }
}
