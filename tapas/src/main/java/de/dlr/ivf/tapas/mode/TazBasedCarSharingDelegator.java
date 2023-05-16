package de.dlr.ivf.tapas.mode;

import de.dlr.ivf.tapas.model.mode.SharingMediator;
import de.dlr.ivf.tapas.model.vehicle.TPS_Car;

import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

public class TazBasedCarSharingDelegator implements SharingDelegator<TPS_Car> {

    private Map<Integer, SharingMediator<TPS_Car>> car_sharing_mediators;

    public TazBasedCarSharingDelegator(Map<Integer, SharingMediator<TPS_Car>> car_sharing_mediators){

        this.car_sharing_mediators = car_sharing_mediators;
    }

    @Override
    public Optional<TPS_Car> request(int start_id, int end_id, Predicate<TPS_Car> filter) {
        SharingMediator<TPS_Car> start_fleet = this.car_sharing_mediators.get(start_id);
        SharingMediator<TPS_Car> end_fleet = this.car_sharing_mediators.get(end_id);

        if(start_fleet == null || end_fleet == null)
            return Optional.empty();
        else
            return start_fleet.request(filter);
    }

    @Override
    public void checkIn(int end_id, TPS_Car used_vehicle) {
        this.car_sharing_mediators.get(end_id).checkIn(used_vehicle);
    }

    @Override
    public void checkOut(int start_id, TPS_Car requested_vehicle) {
        this.car_sharing_mediators.get(start_id).checkOut(requested_vehicle);
    }
}
