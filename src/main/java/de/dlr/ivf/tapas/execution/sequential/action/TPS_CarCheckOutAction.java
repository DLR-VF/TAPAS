package de.dlr.ivf.tapas.execution.sequential.action;

import de.dlr.ivf.tapas.person.TPS_Car;
import de.dlr.ivf.tapas.person.TPS_Person;
import de.dlr.ivf.tapas.execution.sequential.communication.TPS_HouseholdCarMediator;

public class TPS_CarCheckOutAction implements TPS_PlanStateAction {

    private TPS_HouseholdCarMediator car_mediator;
    private TPS_Person person;
    private TPS_Car car;

    public TPS_CarCheckOutAction(TPS_HouseholdCarMediator car_mediator, TPS_Person person, TPS_Car car){
        this.car_mediator = car_mediator;
        this.person = person;
        this.car = car;
    }
    @Override
    public void run() {
        this.car_mediator.checkoutCar(car,person);
    }
}
