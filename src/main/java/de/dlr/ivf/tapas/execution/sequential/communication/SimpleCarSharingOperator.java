package de.dlr.ivf.tapas.execution.sequential.communication;

import de.dlr.ivf.tapas.person.TPS_Car;
import de.dlr.ivf.tapas.runtime.server.SimTimeProvider;
import de.dlr.ivf.tapas.util.FuncUtils;
import de.dlr.ivf.tapas.util.parameters.ParamValue;
import de.dlr.ivf.tapas.util.parameters.TPS_ParameterClass;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntSupplier;
import java.util.function.Predicate;
import java.util.stream.IntStream;

/**
 * This is a basic implementation of a car sharing operator and provides request/checkout/checkin functionality
 */
public class SimpleCarSharingOperator implements SharingMediator<TPS_Car>{

    private final Map<TPS_Car, AtomicBoolean> fleet = new ConcurrentHashMap<>();
    private SimTimeProvider sim_time_provider;

    /**
     * @param initial_fleet_size size of the fleet at start of a simulation
     * @param id_provider the unique id provider for the cars
     */
    public SimpleCarSharingOperator(int initial_fleet_size, IntSupplier id_provider, TPS_ParameterClass parameters){

        initFleet(initial_fleet_size, id_provider, parameters);
    }


    private TPS_Car generateSimpleCar(IntSupplier id_provider, double cost_per_km, int entry_time) {

        TPS_Car car = new TPS_Car(-id_provider.getAsInt(),0,cost_per_km,false,0);
        car.setEntryTime(entry_time);
        return car;
    }

    private void initFleet(int initial_fleet_size, IntSupplier id_provider, TPS_ParameterClass parameters) {

        double cost_per_km = parameters.getDoubleValue(ParamValue.CAR_SHARING_COST_PER_KM);

        int entry_time = parameters.isDefined(ParamValue.CAR_SHARING_CHECKOUT_PENALTY) ? 0 - FuncUtils.secondsToRoundedMinutes.apply(parameters.getIntValue(ParamValue.CAR_SHARING_CHECKOUT_PENALTY)) : 0;

        IntStream.range(0,initial_fleet_size).forEach(i -> fleet.put(generateSimpleCar(id_provider, cost_per_km, entry_time), new AtomicBoolean(false)));
    }

    @Override
    public Optional<TPS_Car> request(Predicate<TPS_Car> external_filter) {

        return fleet.entrySet()
                    .stream()
                    .sorted(Map.Entry.comparingByKey(Comparator.comparing(TPS_Car::getEntryTime).reversed())) //sort before filtering otherwise we lose lazy evaluation and all cars will be marked as requested
                    .filter(entry -> external_filter.test(entry.getKey()))
                    .filter(Predicate.not(entry -> entry.getValue().getAndSet(true))) //find those that have not been requested yet
                    .map(Map.Entry::getKey)
                    .findFirst();
    }

    @Override
    public void checkIn(TPS_Car used_vehicle) {

        used_vehicle.setEntryTime(sim_time_provider.getSimTime());
        this.fleet.put(used_vehicle, new AtomicBoolean(false));
    }

    @Override
    public void checkOut(TPS_Car requested_vehicle) {
        this.fleet.remove(requested_vehicle);
    }

    @Override
    public void release(TPS_Car requested_vehicle) {
        if(!this.fleet.get(requested_vehicle).getAndSet(false))
            throw new IllegalArgumentException("Car has not been requested before so we cannot release it");
    }

    public void setSimTimeProvider(SimTimeProvider sim_time_provider){
        this.sim_time_provider = sim_time_provider;
    }
}
