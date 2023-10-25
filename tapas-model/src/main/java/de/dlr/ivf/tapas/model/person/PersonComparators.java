package de.dlr.ivf.tapas.model.person;

import java.util.Comparator;


public class PersonComparators {

    public static Comparator<TPS_Person> ofSorting(TPS_Household.Sorting sorting){

        return switch (sorting){
            case AGE -> Comparator
                    .comparingInt(TPS_Person::getAge)
                    .reversed();
            case PRIMARY_DRIVER -> Comparator
                    .comparingDouble(TPS_Person::primaryDriver)
                    .reversed();
            case null, default -> Comparator
                    .comparingInt(TPS_Person::getId);
        };
    }
}
