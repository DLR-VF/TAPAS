package de.dlr.ivf.tapas.model.plan;

import de.dlr.ivf.tapas.model.location.TPS_Location;
import de.dlr.ivf.tapas.model.person.TPS_Person;
import de.dlr.ivf.tapas.model.scheme.Tour;

import java.util.Set;

public record PlanningContext(
        TPS_Person person,
        TPS_Location homeLocation,
        Set<Tour> tours

) {
}
