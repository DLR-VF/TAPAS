package de.dlr.ivf.tapas.mode;

import de.dlr.ivf.tapas.execution.sequential.communication.SharingDelegator;
import de.dlr.ivf.tapas.execution.sequential.communication.SharingMediator;
import de.dlr.ivf.tapas.person.TPS_Car;
import java.util.*;
import java.util.function.Predicate;

public class TazBasedCarSharingDelegator implements SharingDelegator<TPS_Car> {

    private Map<Integer, SharingMediator<TPS_Car>> car_sharing_mediators;

    public TazBasedCarSharingDelegator(Map<Integer, SharingMediator<TPS_Car>> car_sharing_mediators){

        this.car_sharing_mediators = car_sharing_mediators;
    }

    @Override
    public Optional<TPS_Car> request(int start_id, int end_id, Predicate<TPS_Car> filter) {
        SharingMediator start_fleet = this.car_sharing_mediators.get(start_id);
        SharingMediator end_fleet = this.car_sharing_mediators.get(end_id);

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
