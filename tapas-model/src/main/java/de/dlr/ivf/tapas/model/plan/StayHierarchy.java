package de.dlr.ivf.tapas.model.plan;

import de.dlr.ivf.tapas.model.scheme.Stay;

import java.util.HashMap;
import java.util.Map;

public class StayHierarchy {

    private final Map<Stay, Stay> precedingStays;
    private final Map<Stay, Stay> succeedingStays;

    public StayHierarchy() {
        this.precedingStays = new HashMap<>();
        this.succeedingStays = new HashMap<>();
    }

    public void addPrecedingStay(Stay stay, Stay precedingStay) {
        precedingStays.put(stay, precedingStay);
    }

    public void addSucceedingStay(Stay stay, Stay succeedingStay) {
        succeedingStays.put(stay, succeedingStay);
    }

    public Stay getPrecedingStay(Stay stay) {
        return precedingStays.get(stay);
    }

    public Stay getSucceedingStay(Stay stay) {
        return succeedingStays.get(stay);
    }
}
