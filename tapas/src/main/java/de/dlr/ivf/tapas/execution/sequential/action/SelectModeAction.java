package de.dlr.ivf.tapas.execution.sequential.action;

import de.dlr.ivf.tapas.execution.sequential.context.ModeContext;
import de.dlr.ivf.tapas.execution.sequential.context.PlanContext;
import de.dlr.ivf.tapas.execution.sequential.context.TourContext;
import de.dlr.ivf.tapas.model.mode.TPS_ExtMode;
import de.dlr.ivf.tapas.model.mode.TPS_ModeSet;
import de.dlr.ivf.tapas.model.plan.TPS_LocatedStay;
import de.dlr.ivf.tapas.model.plan.TPS_Plan;

public class SelectModeAction implements TPS_PlanStateAction {

    private final TourContext tour_context;
    private final TPS_ModeSet mode_set;
    private final PlanContext plan_context;
    public SelectModeAction(TourContext tour_context, PlanContext plan_context, TPS_ModeSet mode_set){

        this.tour_context = tour_context;
        this.mode_set = mode_set;
        this.plan_context = plan_context;
    }

    @Override
    public void run() {
        TPS_Plan plan = plan_context.getPlan();

        TPS_LocatedStay next_located_stay = plan.getLocatedStay(tour_context.getNextStay());

        ModeContext mode_context = tour_context.getModeContext();

        //mode_set.selectMode(plan, () -> tour_context.getCurrentStay(),plan.getLocatedStay(tour_context.getNextStay()),() -> tour_context.getLastStay(),plan.getPlanningContext());
        TPS_ExtMode next_mode = mode_set.selectDepartureMode(plan,plan.getLocatedStay(tour_context.getCurrentStay()), next_located_stay, plan.getPlanningContext());

        mode_context.setNextMode(next_mode);
    }
}
