package de.dlr.ivf.tapas.model.constants;


import lombok.AllArgsConstructor;

import java.util.Collection;

@AllArgsConstructor
public class Activities {

    private final Collection<TPS_ActivityConstant> activities;

    public TPS_ActivityConstant getActivity(TPS_ActivityConstant.TPS_ActivityCodeType type, int code){
        return activities.stream()
                .filter(a -> a.getCode(type) == code)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("activity not found"));
    }
}
