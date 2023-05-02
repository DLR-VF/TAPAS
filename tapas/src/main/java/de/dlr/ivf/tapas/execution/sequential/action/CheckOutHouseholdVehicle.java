package de.dlr.ivf.tapas.execution.sequential.action;

import de.dlr.ivf.tapas.model.mode.SharingMediator;
import de.dlr.ivf.tapas.model.person.TPS_Car;

public class CheckOutHouseholdVehicle implements TPS_PlanStateAction {

    private final SharingMediator<TPS_Car> car_mediator;
    private TPS_Car car;

    public CheckOutHouseholdVehicle(SharingMediator<TPS_Car> car_mediator, TPS_Car car){
        this.car_mediator = car_mediator;
        this.car = car;
    }
    @Override
    public void run() {
        this.car_mediator.checkOut(car);
    }
}
