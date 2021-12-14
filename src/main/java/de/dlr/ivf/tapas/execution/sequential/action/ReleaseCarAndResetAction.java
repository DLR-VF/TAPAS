package de.dlr.ivf.tapas.execution.sequential.action;

import de.dlr.ivf.tapas.execution.sequential.communication.SharingMediator;
import de.dlr.ivf.tapas.execution.sequential.communication.TPS_HouseholdCarMediator;
import de.dlr.ivf.tapas.person.TPS_Car;
import de.dlr.ivf.tapas.plan.TPS_PlanningContext;

public class ReleaseCarAndResetAction implements TPS_PlanStateAction {

    private final SharingMediator<TPS_Car> household_car_provider;
    private final TPS_PlanningContext planning_context;

    public ReleaseCarAndResetAction(SharingMediator<TPS_Car> householdCarProvider, TPS_PlanningContext planning_context) {
        this.household_car_provider = householdCarProvider;
        this.planning_context = planning_context;

    }

    @Override
    public void run() {


        TPS_Car household_car = planning_context.getHouseHoldCar();
        household_car_provider.release(household_car);

        planning_context.setHouseHoldCar(null);

    }
}
