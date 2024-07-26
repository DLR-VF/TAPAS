package de.dlr.ivf.tapas.choice.location;

import de.dlr.ivf.tapas.model.location.WeightedLocation;

import java.util.ArrayList;
import java.util.Collection;

public class LocationChoiceSet {

    private final Collection<WeightedLocation> locationSet;

    public LocationChoiceSet(int maxLocationChoiceSetSize) {

        this.locationSet = new ArrayList<>(maxLocationChoiceSetSize);
    }

    public void addWeightedLocation(WeightedLocation weightedLocation){
        this.locationSet.add(weightedLocation);
    }

    public int size(){
        return locationSet.size();
    }
}
