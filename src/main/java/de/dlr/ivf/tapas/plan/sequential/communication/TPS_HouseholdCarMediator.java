package de.dlr.ivf.tapas.plan.sequential.communication;

import de.dlr.ivf.tapas.person.TPS_Car;
import de.dlr.ivf.tapas.person.TPS_Household;
import de.dlr.ivf.tapas.person.TPS_Person;
import de.dlr.ivf.tapas.plan.TPS_Plan;

import java.util.*;
import java.util.stream.Collectors;

public class TPS_HouseholdCarMediator{

    private SortedMap<TPS_Car, TPS_Person> car_set = new TreeMap<>(Comparator.comparing(TPS_Car::isRestricted));

    private SortedMap<TPS_Person,Integer> potential_car_requests = new TreeMap<>(Comparator.comparingDouble(TPS_Person::primaryDriver));


    public TPS_HouseholdCarMediator(TPS_Household hh){

        //if a car is always bound to one person, we can make a choice here and put that person into the map instead of null
        //null just means that no person has taken this car and it is free to use
        Arrays.stream(hh.getAllCars()).forEach(car -> car_set.put(car,null));

    }

    public TPS_Car request(TPS_Person requesting_person, int planned_tourpart_end) {

        SortedMap<TPS_Person, Integer> higher_ranked_car_requests = this.potential_car_requests.tailMap(requesting_person);

        long overlapping_request_count = higher_ranked_car_requests
                                            .entrySet()
                                            .stream()
                                            .skip(1)
                                            .filter(request_entry -> request_entry.getValue() < planned_tourpart_end  //remove higher ranked persons that might need the car before our estimated end of tour
                                                    && !car_set.containsValue(request_entry.getKey())) //remove higher ranked persons that currently are on tour with a car
                                            .count();

        List<TPS_Car> available_cars = availableCars();

        TPS_Car chosen_car = null;

        if(available_cars.size() > overlapping_request_count) {
            chosen_car = available_cars.get(0);
        }

        return chosen_car;
    }

    public void checkoutCar(TPS_Car chosen_car, TPS_Person person){

        if(this.car_set.containsKey(chosen_car))
            if(this.car_set.get(chosen_car) != null)
                throw new RuntimeException("Can't checkout a car that has already been checked out!");
            else
                car_set.replace(chosen_car, person);
        else
            throw new IllegalArgumentException("Can't checkout a car that does not belong to the household");

    }

    public void checkinCar(TPS_Car car){
        if(car != null && this.car_set.containsKey(car))
            this.car_set.replace(car,null);
    }

    public void updateNextRequest(TPS_Person person, int time){
        potential_car_requests.put(person ,time);

    }

    private List<TPS_Car> availableCars(){

        return this.car_set.entrySet().stream().filter(entry -> entry.getValue() == null).map(entry -> entry.getKey()).collect(Collectors.toList());

    }

    public void initializeFirstRequests(List<TPS_Plan> plans){

        plans.stream().filter(plan -> plan.getPerson().mayDriveACar()).forEach(plan -> potential_car_requests.put(plan.getPerson(), plan.getFirstTourPartStart()));
    }

    public boolean isHouseholdCar(TPS_Car car) {
        return car_set.containsKey(car);
    }
}
