package de.dlr.ivf.tapas.execution.sequential.communication;

import de.dlr.ivf.tapas.person.TPS_Car;
import de.dlr.ivf.tapas.person.TPS_Household;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class TPS_HouseholdCarMediator implements SharingMediator<TPS_Car> {

    private Map<TPS_Car, AtomicBoolean> car_occupancy_map;

    public TPS_HouseholdCarMediator(TPS_Household hh){

        car_occupancy_map = Arrays.stream(hh.getAllCars())
                                  .collect(Collectors.toMap(
                                           Function.identity(),
                                           car -> new AtomicBoolean(false)
                                          ));
    }

    @Override
    public Optional<TPS_Car> request(Predicate<TPS_Car> infilter) {
        return this.car_occupancy_map
                .entrySet()
                .stream()
                .filter(Predicate.not(entry -> entry.getValue().getAndSet(true)))
                .map(Map.Entry::getKey)
                .findFirst();
    }

    @Override
    public void checkIn(TPS_Car used_vehicle) {
        if(this.car_occupancy_map.get(used_vehicle).getAndSet(false) == false)
            throw new IllegalArgumentException("Car has neither been requested nor checked out before");
    }

    @Override
    public void checkOut(TPS_Car requested_vehicle) {
        if(this.car_occupancy_map.get(requested_vehicle).getAndSet(true) == false)
            throw new IllegalArgumentException("Car has not been requested before and cannot be checked out");
    }

    @Override
    public void release(TPS_Car requested_vehicle) {
        if(this.car_occupancy_map.get(requested_vehicle).getAndSet(false) == false)
            throw new IllegalArgumentException("Car has not been requested before and cannot be released");
    }
}
