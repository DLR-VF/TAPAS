package de.dlr.ivf.tapas.execution.sequential.communication;

import de.dlr.ivf.tapas.person.TPS_Car;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.stream.IntStream;

public class SimpleCarSharingOperator implements SharingMediator<TPS_Car>{

    private Map<TPS_Car, AtomicBoolean> fleet = new ConcurrentHashMap<>();

    public SimpleCarSharingOperator(int initial_fleet_size){
        initFleet(initial_fleet_size);
    }


    private TPS_Car generateSimpleCar(int i) {

        return new TPS_Car(-i,0.0, 0.64,false,800);
    }

    private void initFleet(int initial_fleet_size) {
        IntStream.range(0,initial_fleet_size).forEach(i -> fleet.put(generateSimpleCar(i), new AtomicBoolean(false)));
    }

    @Override
    public Optional<TPS_Car> request(Predicate<TPS_Car> filter) {

        return fleet.entrySet()
                    .stream()
                    .sorted(Map.Entry.comparingByKey(Comparator.comparing(TPS_Car::getRangeLeft))) //sort before filtering otherwise we lose lazy evaluation and all cars will be marked as requested
                    .filter(entry -> filter.test(entry.getKey()))
                    .filter(Predicate.not(entry -> entry.getValue().getAndSet(true))) //find those that have not been requested yet
                    .map(Map.Entry::getKey)
                    .findFirst();
    }

    @Override
    public void checkIn(TPS_Car used_vehicle) {
        this.fleet.put(used_vehicle, new AtomicBoolean(false));
    }

    @Override
    public void checkOut(TPS_Car requested_vehicle) {
        this.fleet.remove(requested_vehicle);
    }

    @Override
    public void release(TPS_Car requested_vehicle) {
        if(this.fleet.get(requested_vehicle).getAndSet(false) == false)
            throw new IllegalArgumentException("Car has not been requested before so we cannot release it");
    }
}
