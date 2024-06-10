package de.dlr.ivf.tapas.model.constants;

import lombok.Builder;
import lombok.Singular;

import java.util.Collection;
import java.util.Map;

@Builder
public class PersonGroups {

    @Singular
    private final Map<Integer, TPS_PersonGroup> personGroups;

    public TPS_PersonGroup getPersonGroupByCode(int code) {
        return this.personGroups.get(code);
    }

    public Collection<TPS_PersonGroup> getPersonGroups() {
        return this.personGroups.values();
    }
}
