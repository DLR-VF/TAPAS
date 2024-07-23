package de.dlr.ivf.tapas.choice.location;

import de.dlr.ivf.tapas.model.location.TPS_Location;

import java.util.ArrayList;
import java.util.Collection;

public class LocationChoiceSet {

    private final Collection<TPS_Location> locationSet;

    public LocationChoiceSet(int maxLocationChoiceSetSize) {

        this.locationSet = new ArrayList<>(maxLocationChoiceSetSize);
    }

    public void addLocation(TPS_Location location){
        this.locationSet.add(location);
    }

    public int size(){
        return locationSet.size();
    }
}
