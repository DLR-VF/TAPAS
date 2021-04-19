package de.dlr.ivf.tapas.execution.sequential.action;

import de.dlr.ivf.tapas.execution.sequential.communication.SharingMediator;
import de.dlr.ivf.tapas.person.TPS_Car;
import de.dlr.ivf.tapas.person.TPS_Person;
import de.dlr.ivf.tapas.execution.sequential.communication.TPS_HouseholdCarMediator;

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
