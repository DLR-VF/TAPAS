package de.dlr.ivf.tapas.execution.sequential.action;

import de.dlr.ivf.tapas.execution.sequential.context.TourContext;
import de.dlr.ivf.tapas.mode.TPS_ModeValidator;
import de.dlr.ivf.tapas.person.TPS_Car;
import de.dlr.ivf.tapas.plan.TPS_PlannedTrip;
import de.dlr.ivf.tapas.plan.TPS_PlanningContext;
import de.dlr.ivf.tapas.runtime.server.SimTimeProvider;

import java.util.function.Predicate;

public class ValidateModeAction implements TPS_PlanStateAction{

    private final TPS_ModeValidator mode_validator;
    private final TourContext tour_context;
    private final TPS_PlanningContext planning_context;
    private final TPS_PlannedTrip planning_trip;
    private final Predicate<TPS_Car> car_filter;

    public ValidateModeAction(TPS_ModeValidator mode_validator, TourContext tour_context, TPS_PlanningContext planing_context, TPS_PlannedTrip planned_trip, Predicate<TPS_Car> car_filter){

        this.mode_validator = mode_validator;
        this.tour_context = tour_context;
        this.planning_context = planing_context;
        this.planning_trip = planned_trip;
        this.car_filter = car_filter;
    }
    @Override
    public void run() {

        this.mode_validator.validateMode(planning_context, tour_context.getModeContext() ,tour_context.getCurrentLocation(), tour_context.getNextLocation(), planning_trip, car_filter);

    }
}
