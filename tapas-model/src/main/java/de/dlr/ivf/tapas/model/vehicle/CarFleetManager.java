package de.dlr.ivf.tapas.model.vehicle;

import lombok.Builder;
import lombok.Singular;

import java.util.Collection;
import java.util.Comparator;
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

    /**
     * Returns the number of cars available in the household. The returned result will be a sorted List containing
     * unrestricted cars first.
     * @param start start time the car should be used
     * @param duration duration of car usage
     * @return a List of CarControllers ordered by restriction flag. Non-restricted cars first.
     */
    public List<CarController> getAvailableCarsNonRestrictedFirst(int start, int duration){
        return getAvailableCars(start, duration)
                .stream()
                .sorted(Comparator.comparing(CarController::isRestricted))
                .collect(Collectors.toList());
    }

    public Collection<CarController> getCars(){
        return this.fleet;
    }


}
