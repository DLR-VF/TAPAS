package de.dlr.ivf.tapas.model.mode;

import de.dlr.ivf.tapas.model.person.TPS_Car;
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
public class SimpleCarSharingOperator implements SharingMediator<TPS_Car> {

    //private final Map<TPS_Car, AtomicBoolean> fleet = new ConcurrentHashMap<>();
    private final BlockingDeque<TPS_Car> fleet = new LinkedBlockingDeque<>();
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
    public Optional<TPS_Car> request(Predicate<TPS_Car> external_filter) {

        TPS_Car available_car = null;

        synchronized (fleet){

            Iterator<TPS_Car> car_iterator = fleet.descendingIterator();

            while(available_car == null && car_iterator.hasNext()){
                TPS_Car potential_car = car_iterator.next();

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
    public void checkIn(TPS_Car used_vehicle) {

        used_vehicle.setEntryTime(sim_time_provider.getSimTime());
        synchronized (fleet) {
            this.fleet.add(used_vehicle);
        }
    }

    @Override
    public void checkOut(TPS_Car requested_vehicle) {
        this.fleet.remove(requested_vehicle);
    }

    @Override
    public void release(TPS_Car requested_vehicle) {

    }

    public void setSimTimeProvider(SimTimeProvider sim_time_provider){
        this.sim_time_provider = sim_time_provider;
    }
}
