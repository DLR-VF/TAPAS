package de.dlr.ivf.tapas.plan.state.communication;

import de.dlr.ivf.tapas.person.TPS_Car;
import de.dlr.ivf.tapas.person.TPS_Household;
import de.dlr.ivf.tapas.person.TPS_Person;
import javafx.beans.InvalidationListener;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;


//This class is intended to handle communication between multiple members of a household that want to use one of the cars.
//The idea:
//- someone requests a car supplying a score (eg.: travel times / manageability with the next best mode alternative / pt ticket and so on...)
//- in return we ask everyone eligible in using this car for their score as in how badly they need it
public class TPS_HouseholdCarMediator implements TPS_Mediator {

    private ConcurrentMap<TPS_Car, TPS_Person> car_set;
    private List<TPS_Person> persons;


    public TPS_HouseholdCarMediator(TPS_Household hh){
        this.car_set = new ConcurrentHashMap<>();


        //if a car is always bound to one person, we can make a choice here and put that person into the map instead of null
        //null just means that no person has taken this car and it is free to use
        Arrays.stream(hh.getAllCars()).forEach(car -> car_set.put(car,null));

        //now get all the persons that are allowed to drive any of the cars car.
        this.persons = hh.getMembers(TPS_Household.Sorting.PRIMARY_DRIVER)
                         .stream()
                         .filter(TPS_Person::mayDriveACar)
                         .collect(Collectors.toList());
    }

    @Override
    public void request(double score) {

    }

    @Override
    public void offer() {

    }
}
