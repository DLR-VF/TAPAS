package de.dlr.ivf.tapas.model.vehicle;

import lombok.Builder;
import lombok.Singular;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Builder
public class CarFleetManager {

    @Singular("addCarController")
    private final Collection<CarController> fleet;

    /**
     * Returns the number of cars available in the household for the time period specified
     *
     * @param start time period start
     * @param duration   time period
     * @return a List of available cars, null if no cars are available
     */
    public List<CarController> getAvailableCars(int start, int duration) {

        return fleet.stream()
                .filter(carController -> carController.isCarAvailable(start, duration))
                .collect(Collectors.toList());
    }

    public Collection<CarController> getCars(){
        return this.fleet;
    }


}
