package de.dlr.ivf.tapas.execution.sequential.action;

import de.dlr.ivf.tapas.model.mode.SharingMediator;
import de.dlr.ivf.tapas.model.vehicle.TPS_Car;
import de.dlr.ivf.tapas.model.plan.TPS_PlanningContext;
import de.dlr.ivf.tapas.model.vehicle.Vehicle;

public class ReleaseCarAndResetAction implements TPS_PlanStateAction {

    private final SharingMediator<Vehicle> household_car_provider;
    private final TPS_PlanningContext planning_context;

    public ReleaseCarAndResetAction(SharingMediator<Vehicle> householdCarProvider, TPS_PlanningContext planning_context) {
        this.household_car_provider = householdCarProvider;
        this.planning_context = planning_context;

    }

    @Override
    public void run() {


        Vehicle household_car = planning_context.getHouseHoldCar();
        household_car_provider.release(household_car);

        planning_context.setHouseHoldCar(null);

    }
}
