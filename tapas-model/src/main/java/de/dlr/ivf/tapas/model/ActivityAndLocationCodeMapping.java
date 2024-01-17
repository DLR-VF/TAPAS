package de.dlr.ivf.tapas.model;

import de.dlr.ivf.tapas.model.constants.TPS_ActivityConstant;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ActivityAndLocationCodeMapping {

    private final Map<TPS_ActivityConstant, Integer> activityToLocationTypes = new HashMap<>();

    private final Map<Integer, Collection<TPS_ActivityConstant>> locationToActivityTypes = new HashMap<>();

    public void addActivityToLocationMapping(TPS_ActivityConstant actCode, int locCode) {
        this.activityToLocationTypes.put(actCode, locCode);
    }

    public void addLocationCodeToActivityMapping(int locCode, TPS_ActivityConstant actCode) {
        this.locationToActivityTypes.computeIfAbsent(locCode, code -> new ArrayList<>()).add(actCode);
    }

    public Collection<TPS_ActivityConstant> getActivitiesByLocationCode(int locationCode){
        return locationToActivityTypes.get(locationCode);
    }
}

