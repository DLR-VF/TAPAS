package de.dlr.ivf.tapas.model.vehicle;

import lombok.Builder;
import lombok.Singular;

import java.util.Collection;
import java.util.Map;

@Builder
public class Cars {

    @Singular
    private final Map<Integer, TPS_Car> cars;

    public Collection<TPS_Car> getCars(){
        return cars.values();
    }

    public TPS_Car getCar(int carId){
        return this.cars.get(carId);
    }
}
