package de.dlr.ivf.tapas.mode;

import de.dlr.ivf.tapas.model.mode.SharingMediator;
import de.dlr.ivf.tapas.model.vehicle.TPS_Car;

import de.dlr.ivf.tapas.model.vehicle.Vehicle;
import de.dlr.ivf.tapas.runtime.server.SimTimeProvider;
import de.dlr.ivf.tapas.util.FuncUtils;
import de.dlr.ivf.tapas.model.parameter.ParamValue;
import de.dlr.ivf.tapas.model.parameter.TPS_ParameterClass;

import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.IntSupplier;
import java.util.function.Predicate;

/**
 * This is a basic implementation of a car sharing operator and provides request/checkout/checkin functionality
 */
public class SimpleCarSharingOperator implements SharingMediator<Vehicle> {

    //private final Map<TPS_Car, AtomicBoolean> fleet = new ConcurrentHashMap<>();
    private final BlockingDeque<Vehicle> fleet = new LinkedBlockingDeque<>();
    private SimTimeProvider sim_time_provider;

    /**
     * @param initial_fleet_size size of the fleet at start of a simulation
     * @param id_provider the unique id provider for the cars
     */
    public SimpleCarSharingOperator(int initial_fleet_size, IntSupplier id_provider, TPS_ParameterClass parameters){

        initFleet(initial_fleet_size, id_provider, parameters);
    }


    private Vehicle generateSimpleCar(IntSupplier id_provider, double cost_per_km, int entry_time) {

        Vehicle car = TPS_Car.builder()
                .id(-id_provider.getAsInt())
                .fixCosts(0)
                .cost_per_kilometer(cost_per_km)
                .restricted(false)
                .build();
        //car.setEntryTime(entry_time);
        return car;
    }

    private void initFleet(int initial_fleet_size, IntSupplier id_provider, TPS_ParameterClass parameters) {

        double cost_per_km = parameters.getDoubleValue(ParamValue.CAR_SHARING_COST_PER_KM);

        int entry_time = parameters.isDefined(ParamValue.CAR_SHARING_CHECKOUT_PENALTY) ? -FuncUtils.secondsToRoundedMinutes.apply(parameters.getIntValue(ParamValue.CAR_SHARING_CHECKOUT_PENALTY)) : 0;

        try {
            for (int i = 0; i < initial_fleet_size; i++) {
                fleet.put(generateSimpleCar(id_provider, cost_per_km, entry_time));
            }
        }catch(Exception e){
            e.printStackTrace();
        }


        //IntStream.range(0,initial_fleet_size).forEach(i -> fleet.put(generateSimpleCar(id_provider, cost_per_km, entry_time), new AtomicBoolean(false)));
    }

    @Override
    public Optional<Vehicle> request(Predicate<Vehicle> external_filter) {

        Vehicle available_car = null;

        synchronized (fleet){

            Iterator<Vehicle> car_iterator = fleet.descendingIterator();

            while(available_car == null && car_iterator.hasNext()){
                Vehicle potential_car = car_iterator.next();

                if(external_filter.test(potential_car)){
                    available_car = potential_car;

                    if(!fleet.remove(potential_car))
                        throw new RuntimeException("An error occurred when trying to remove a car sharing car.");
                }
            }
        }
        return Optional.ofNullable(available_car);
    }

    @Override
    public void checkIn(Vehicle used_vehicle) {

        //used_vehicle.setEntryTime(sim_time_provider.getSimTime());
        synchronized (fleet) {
            this.fleet.add(used_vehicle);
        }
    }

    @Override
    public void checkOut(Vehicle requested_vehicle) {
        this.fleet.remove(requested_vehicle);
    }

    @Override
    public void release(Vehicle requested_vehicle) {

    }

    public void setSimTimeProvider(SimTimeProvider sim_time_provider){
        this.sim_time_provider = sim_time_provider;
    }
}
