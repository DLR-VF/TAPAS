package de.dlr.ivf.tapas.plan.sequential.action;

import de.dlr.ivf.tapas.person.TPS_Car;
import de.dlr.ivf.tapas.person.TPS_Person;
import de.dlr.ivf.tapas.plan.sequential.communication.TPS_HouseholdCarMediator;

public class TPS_CarCheckInAction implements TPS_PlanStateAction {

    private TPS_HouseholdCarMediator car_mediator;
    private TPS_Car car;
    private int next_trip_start;
    private TPS_Person person;

    public TPS_CarCheckInAction(TPS_HouseholdCarMediator car_mediator, TPS_Car car, int next_trip_start, TPS_Person person){
        this.car_mediator = car_mediator;
        this.car = car;
        this.next_trip_start = next_trip_start;
        this.person = person;
    }


    @Override
    public void run() {
        this.car_mediator.checkinCarAndUpdateNextRequest(this.car, this.next_trip_start, this.person);
    }
}
