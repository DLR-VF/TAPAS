package de.dlr.ivf.tapas.execution.sequential.communication;

import de.dlr.ivf.tapas.person.TPS_Car;
import de.dlr.ivf.tapas.person.TPS_Household;
import de.dlr.ivf.tapas.person.TPS_Person;
import de.dlr.ivf.tapas.plan.TPS_Plan;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class TPS_HouseholdCarMediator implements SharingMediator<TPS_Car> {

    private SortedMap<TPS_Car, AtomicBoolean> car_occupancy_map;

    public TPS_HouseholdCarMediator(TPS_Household hh){

        Supplier<TreeMap<TPS_Car, AtomicBoolean>> map_supplier = () -> new TreeMap<>(Comparator.comparing(TPS_Car::getFixCostPerKilometer));

        car_occupancy_map = Arrays.stream(hh.getAllCars())
                                  .collect(Collectors.toMap(
                                           Function.identity(),
                                           car -> new AtomicBoolean(false),
                                           (car1, car2) -> car1,
                                           map_supplier));
    }

    @Override
    public Optional<TPS_Car> request(Predicate<TPS_Car> filter) {
        return this.car_occupancy_map
                .entrySet()
                .stream()
                .filter(Predicate.not(entry -> entry.getValue().getAndSet(true)))
                .map(Map.Entry::getKey)
                .findFirst();
    }

    @Override
    public void checkIn(TPS_Car used_vehicle) {

    }

    @Override
    public void checkOut(TPS_Car requested_vehicle) {

    }

    @Override
    public void release(TPS_Car requested_vehicle) {
        if(this.car_occupancy_map.get(requested_vehicle).getAndSet(false) == false)
            throw new IllegalArgumentException("Car has not been requested before so we cannot release it");
    }
}
