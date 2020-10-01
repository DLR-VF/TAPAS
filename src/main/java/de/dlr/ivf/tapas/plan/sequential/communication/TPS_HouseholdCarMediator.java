package de.dlr.ivf.tapas.plan.sequential.communication;

import de.dlr.ivf.tapas.person.TPS_Car;
import de.dlr.ivf.tapas.person.TPS_Household;
import de.dlr.ivf.tapas.person.TPS_Person;
import de.dlr.ivf.tapas.plan.TPS_Plan;

import java.util.*;
import java.util.stream.Collectors;


//This class is intended to handle communication between multiple members of a household that want to use one of the cars.
//The idea:
//- someone requests a car supplying a score (eg.: travel times / manageability with the next best mode alternative / pt ticket and so on...)
//- actually we'd rather wait for all other members so request a car themselves and then we decide who gets one ....in return we ask everyone eligible in using this car for their score as in how badly they need it
public class TPS_HouseholdCarMediator{

    private SortedMap<TPS_Car, TPS_Person> car_set = new TreeMap<>(Comparator.comparing(TPS_Car::isRestricted));
    private List<TPS_Person> persons;
    private TPS_Car previous_requested_car;

    private SortedMap<TPS_Person,Integer> potential_car_requests = new TreeMap<>(Comparator.comparingDouble(TPS_Person::primaryDriver));


    public TPS_HouseholdCarMediator(TPS_Household hh){

        //if a car is always bound to one person, we can make a choice here and put that person into the map instead of null
        //null just means that no person has taken this car and it is free to use
        Arrays.stream(hh.getAllCars()).forEach(car -> car_set.put(car,null));

        //now get all the persons that are allowed to drive any of the cars car.
        this.persons = new ArrayList<>();

        for(TPS_Person person : hh.getMembers(TPS_Household.Sorting.PRIMARY_DRIVER)){

            if(person.mayDriveACar()){
                persons.add(person);
            }

        }

    }

    public TPS_Car request(TPS_Person requesting_person, int planned_tourpart_end) {

        SortedMap<TPS_Person, Integer> higher_ranked_car_requests = this.potential_car_requests.tailMap(requesting_person);

        long overlapping_request_count = higher_ranked_car_requests.entrySet().stream().skip(1).filter(request_entry -> request_entry.getValue() < planned_tourpart_end && !car_set.containsValue(request_entry.getKey())).count();

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

    public void checkinCarAndUpdateNextRequest(TPS_Car car, int time, TPS_Person person){
        if(car != null && this.car_set.containsKey(car))
            this.car_set.replace(car,null);

        potential_car_requests.put(person ,time);

    }

    private List<TPS_Car> availableCars(){

        return this.car_set.entrySet().stream().filter(entry -> entry.getValue() == null).map(entry -> entry.getKey()).collect(Collectors.toList());

    }

    public void initializeFirstRequests(List<TPS_Plan> plans){

        plans.stream().filter(plan -> plan.getPerson().mayDriveACar()).forEach(plan -> potential_car_requests.put(plan.getPerson(), plan.getFirstTourPartStart()));
    }
}
