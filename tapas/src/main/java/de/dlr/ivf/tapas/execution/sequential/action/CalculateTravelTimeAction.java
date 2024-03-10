package de.dlr.ivf.tapas.execution.sequential.action;

import de.dlr.ivf.tapas.model.plan.TPS_LocatedStay;
import de.dlr.ivf.tapas.model.plan.TPS_PlannedTrip;

public class CalculateTravelTimeAction implements TPS_PlanStateAction {

    private final TPS_LocatedStay current_stay;
    private final TPS_LocatedStay next_stay;
    private final TPS_PlannedTrip next_planned_trip;

    public CalculateTravelTimeAction(TPS_LocatedStay current_stay, TPS_LocatedStay next_stay, TPS_PlannedTrip trip) {

        this.current_stay = current_stay;
        this.next_stay = next_stay;
        this.next_planned_trip = trip;

    }

    @Override
    public void run() {
        //todo revise
        //next_planned_trip.setTravelTime(current_stay, next_stay);

    }
}
