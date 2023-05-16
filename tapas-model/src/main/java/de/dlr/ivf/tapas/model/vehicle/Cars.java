package de.dlr.ivf.tapas.model.vehicle;

import lombok.Builder;
import lombok.Singular;

import java.util.Collection;
import java.util.Map;

@Builder
public class Cars {

    @Singular
    private final Map<Integer, Vehicle> cars;

    public Collection<Vehicle> getCars(){
        return cars.values();
    }

    public Vehicle getCar(int carId){
        return this.cars.get(carId);
    }
}
