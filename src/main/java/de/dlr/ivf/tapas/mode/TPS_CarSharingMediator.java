package de.dlr.ivf.tapas.mode;

import de.dlr.ivf.tapas.person.TPS_Car;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

public class TPS_CarSharingMediator {

    private final NavigableSet<TPS_Car> available_cars = new TreeSet<>(Comparator.comparingDouble(TPS_Car::getRangeLeft));
    private Map<Integer, AtomicInteger> destination_taz_counts = new HashMap<>();
    private Map<Integer, AtomicInteger> source_taz_counts = new HashMap<>();
    private TPS_Car random_car = new TPS_Car(-1);

    public TPS_CarSharingMediator(int car_count, Collection<Integer> taz_ids, TPS_Car random_car){
        generateCars(car_count);
        setupTAZStatistics(taz_ids);
        this.random_car.cloneCar(random_car);
    }


    private void generateCars(int car_count){

        IntStream.range(0,car_count).forEach(i -> {
            TPS_Car car = new TPS_Car(i);
            car.cloneCar(random_car);
            available_cars.add(car);
        });
    }

    private void setupTAZStatistics(Collection<Integer> taz_ids){
        taz_ids.forEach(taz_id -> {
            destination_taz_counts.put(taz_id,new AtomicInteger(0));
            source_taz_counts.put(taz_id,new AtomicInteger(0));
        });
    }

    public TPS_Car request(double traveldistance, int destination_taz_id){
        TPS_Car requested_car;
        synchronized (available_cars) {
            requested_car = available_cars.stream().filter(car -> car.getRangeLeft() > traveldistance).findFirst().orElseGet(() -> null);
            if(requested_car != null){
                available_cars.remove(requested_car);
                destination_taz_counts.get(destination_taz_id).incrementAndGet();
            }
        }
        return requested_car;
    }

    public void checkInCar(TPS_Car car, int source_taz_id){
        this.available_cars.add(car);
        this.source_taz_counts.get(source_taz_id).incrementAndGet();
    }

    public TPS_Car getRandomCar(){
        return this.random_car;
    }
}
