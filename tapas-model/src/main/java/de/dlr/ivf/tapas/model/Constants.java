package de.dlr.ivf.tapas.model;

import de.dlr.ivf.tapas.model.constants.TPS_ActivityConstant;
import de.dlr.ivf.tapas.model.constants.TPS_AgeClass;
import lombok.Builder;

import java.util.HashMap;

@Builder
public class Constants {

    private final HashMap<Integer, TPS_ActivityConstant> activityConstantHashMap;
    private final HashMap<Integer, TPS_AgeClass> ageClassMap;

}
