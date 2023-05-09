package de.dlr.ivf.tapas.model;

import de.dlr.ivf.tapas.model.constants.TPS_ActivityConstant;
import de.dlr.ivf.tapas.model.constants.TPS_LocationConstant;
import lombok.Getter;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;

@Getter
public class ActivityAndLocationCodeMapping {

    private final ArrayListValuedHashMap<TPS_ActivityConstant, TPS_LocationConstant> activityToLocationTypes = new ArrayListValuedHashMap<>();

    private final ArrayListValuedHashMap<TPS_LocationConstant, TPS_ActivityConstant> locationToActivityTypes = new ArrayListValuedHashMap<>();

    public void addActivityToLocationMapping(TPS_ActivityConstant actCode, TPS_LocationConstant locCode) {
        this.activityToLocationTypes.put(actCode, locCode);
    }

    public void addLocationToActivityMapping(TPS_LocationConstant locCode, TPS_ActivityConstant actCode) {
        this.locationToActivityTypes.put(locCode, actCode);
    }
}

