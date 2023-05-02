package de.dlr.ivf.tapas.choice;

import de.dlr.ivf.tapas.model.location.Locatable;

public interface TravelDistanceFunction {

    double apply(Locatable start, Locatable end);
}
