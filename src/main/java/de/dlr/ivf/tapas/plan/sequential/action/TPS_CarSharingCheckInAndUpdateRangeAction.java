package de.dlr.ivf.tapas.plan.sequential.action;

import de.dlr.ivf.tapas.mode.TPS_CarSharingMediator;
import de.dlr.ivf.tapas.person.TPS_Car;

public class TPS_CarSharingCheckInAndUpdateRangeAction implements TPS_PlanStateAction {
    private TPS_Car car;
    private  TPS_CarSharingMediator car_sharing_station;
    private double travel_distance;
    private int source_taz_id;

    public TPS_CarSharingCheckInAndUpdateRangeAction(TPS_Car car, double travel_distance, TPS_CarSharingMediator car_sharing_station, int source_taz_id){
        this.car_sharing_station = car_sharing_station;
        this.travel_distance = travel_distance;
        this.car = car;
        this.source_taz_id = source_taz_id;
    }

    @Override
    public void run() {
        car.reduceRange(travel_distance);
        car_sharing_station.checkInCar(car, source_taz_id);
    }

}
