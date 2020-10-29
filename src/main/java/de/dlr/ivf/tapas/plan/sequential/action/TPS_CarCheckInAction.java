package de.dlr.ivf.tapas.plan.sequential.action;

import de.dlr.ivf.tapas.person.TPS_Car;
import de.dlr.ivf.tapas.plan.TPS_PlanningContext;
import de.dlr.ivf.tapas.plan.sequential.communication.TPS_HouseholdCarMediator;

public class TPS_CarCheckInAction implements TPS_PlanStateAction {

    private TPS_HouseholdCarMediator car_mediator;
    private TPS_PlanningContext pc;

    public TPS_CarCheckInAction(TPS_HouseholdCarMediator car_mediator, TPS_PlanningContext pc){
        this.car_mediator = car_mediator;
        this.pc = pc;
    }


    @Override
    public void run() {
        this.car_mediator.checkinCar(this.pc.carForThisPlan);
        pc.carForThisPlan = null;
    }
}
