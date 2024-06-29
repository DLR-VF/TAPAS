package de.dlr.ivf.tapas.model.constants;


import de.dlr.ivf.tapas.model.scheme.Activity;

import java.util.Collection;

public class Activities {

    private final Collection<TPS_ActivityConstant> activityConstants;
    private final Collection<Activity> activities;

    public Activities(Collection<TPS_ActivityConstant> activityConstants, Collection<Activity> activities) {
        this.activityConstants = activityConstants;
        this.activities = activities;
    }

    public TPS_ActivityConstant getActivity(TPS_ActivityConstant.TPS_ActivityCodeType type, int code){
        return activityConstants.stream()
                .filter(a -> a.getCode(type) == code)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("activity not found"));
    }

    public Activity getActivityByZbeCode(int code) {
        return activities.stream()
                .filter(a -> a.codeZbe() == code)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("activity not found"));
    }
}
