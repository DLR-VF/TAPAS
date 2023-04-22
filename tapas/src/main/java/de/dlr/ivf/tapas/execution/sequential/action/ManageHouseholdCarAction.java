package de.dlr.ivf.tapas.execution.sequential.action;

import de.dlr.ivf.tapas.execution.sequential.communication.TPS_HouseholdCarMediator;
import de.dlr.ivf.tapas.execution.sequential.context.ModeContext;
import de.dlr.ivf.tapas.execution.sequential.context.TourContext;
import de.dlr.ivf.tapas.mode.TPS_Mode;
import de.dlr.ivf.tapas.plan.TPS_PlanningContext;

public class ManageHouseholdCarAction implements TPS_PlanStateAction {


    private final TPS_HouseholdCarMediator household_car_provider;
    private final TPS_PlanningContext planning_context;
    private final TourContext tour_context;

    public ManageHouseholdCarAction(TPS_HouseholdCarMediator householdCarProvider, TPS_PlanningContext pc, TourContext tour_context) {
        this.household_car_provider = householdCarProvider;
        this.planning_context = pc;
        this.tour_context = tour_context;
    }

    @Override
    public void run() {
        ModeContext mode_context = tour_context.getModeContext();
        if(mode_context.getNextMode().primary == TPS_Mode.get(TPS_Mode.ModeType.MIT))
            household_car_provider.checkOut(planning_context.getHouseHoldCar());
        else {
            household_car_provider.release(planning_context.getHouseHoldCar());
            planning_context.setHouseHoldCar(null);
        }
    }
}
