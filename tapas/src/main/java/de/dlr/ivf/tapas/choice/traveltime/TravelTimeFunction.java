package de.dlr.ivf.tapas.choice.traveltime;

import de.dlr.ivf.tapas.model.location.Locatable;

public interface TravelTimeFunction {
    double calculateTravelTime(Locatable start, Locatable end, int time);
}
