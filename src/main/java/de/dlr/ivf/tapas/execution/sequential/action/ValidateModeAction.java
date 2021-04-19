package de.dlr.ivf.tapas.execution.sequential.action;

import de.dlr.ivf.tapas.execution.sequential.context.TourContext;
import de.dlr.ivf.tapas.mode.TPS_ModeValidator;
import de.dlr.ivf.tapas.plan.TPS_PlanningContext;

public class ValidateModeAction implements TPS_PlanStateAction{

    private final TPS_ModeValidator mode_validator;
    private final TourContext tour_context;
    private final TPS_PlanningContext planning_context;

    public ValidateModeAction(TPS_ModeValidator mode_validator, TourContext tour_context, TPS_PlanningContext planing_context){

        this.mode_validator = mode_validator;
        this.tour_context = tour_context;
        this.planning_context = planing_context;

    }
    @Override
    public void run() {

        this.mode_validator.validateMode(planning_context, tour_context.getModeContext() ,tour_context.getCurrentLocation(), tour_context.getNextLocation());

    }
}
