package de.dlr.ivf.tapas.model;

import de.dlr.ivf.tapas.model.constants.TPS_Distance;
import lombok.Builder;
import lombok.Singular;

import java.util.NavigableMap;

@Builder
public class DistanceClasses {

    @Singular
    private final NavigableMap<Integer, TPS_Distance> distanceMctMappings;

    @Singular
    private final NavigableMap<Integer, TPS_Distance> distanceVotMappings;

    public TPS_Distance getDistanceMctClass(int distance){
        return distanceMctMappings.ceilingEntry(distance).getValue();
    }

    public TPS_Distance getDistanceVotClass(int distance){
        return distanceVotMappings.ceilingEntry(distance).getValue();
    }
}
