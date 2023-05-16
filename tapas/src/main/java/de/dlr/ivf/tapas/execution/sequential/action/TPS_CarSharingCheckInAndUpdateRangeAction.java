package de.dlr.ivf.tapas.execution.sequential.action;

import de.dlr.ivf.tapas.model.vehicle.CarController;
import de.dlr.ivf.tapas.mode.TazBasedCarSharingDelegator;

public class TPS_CarSharingCheckInAndUpdateRangeAction implements TPS_PlanStateAction {
    private CarController car;
    private TazBasedCarSharingDelegator car_sharing_station;
    private double travel_distance;
    private int source_taz_id;

    public TPS_CarSharingCheckInAndUpdateRangeAction(CarController car, double travel_distance, TazBasedCarSharingDelegator car_sharing_station, int source_taz_id){
        this.car_sharing_station = car_sharing_station;
        this.travel_distance = travel_distance;
        this.car = car;
        this.source_taz_id = source_taz_id;
    }

    @Override
    public void run() {
        car.reduceRange(travel_distance);
        //car_sharing_station.checkInCar(car, source_taz_id);
    }

}
